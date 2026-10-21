package com.campus.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.constant.MessageConstant;
import com.campus.common.constant.RedisConstants;
import com.campus.common.context.UserContext;
import com.campus.common.enumeration.OrderStatus;
import com.campus.common.enumeration.ProductStatus;
import com.campus.common.enumeration.WsMessageType;
import com.campus.common.exception.BusinessException;
import com.campus.common.exception.NoAuthException;
import com.campus.common.result.PageResult;
import com.campus.common.utils.RedisIdWorker;
import com.campus.mapper.OrderMapper;
import com.campus.mapper.ProductMapper;
import com.campus.mapper.ReviewMapper;
import com.campus.mapper.UserMapper;
import com.campus.pojo.dto.OrderSubmitDTO;
import com.campus.pojo.dto.RefundHandleDTO;
import com.campus.pojo.entity.Product;
import com.campus.pojo.entity.ProductOrder;
import com.campus.pojo.entity.Review;
import com.campus.pojo.entity.User;
import com.campus.pojo.vo.OrderVO;
import com.campus.service.OrderService;
import com.campus.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务实现
 * 核心：Redisson 分布式锁 + 商品状态 CAS 双层防超卖；Redis 延迟队列 + Spring Task 超时取消；
 * 订单状态机全流程 WebSocket 实时通知。
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ReviewMapper reviewMapper;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisIdWorker redisIdWorker;

    /** 用于获取自身代理：同类内自调用不经过事务代理，REQUIRES_NEW 等传播行为会失效 */
    @Resource
    private ApplicationContext applicationContext;

    @Value("${campus.order.pay-timeout-minutes:15}")
    private long payTimeoutMinutes;

    @Value("${campus.order.auto-receive-days:7}")
    private long autoReceiveDays;

    /* ==================== 交易主流程 ==================== */

    @Override
    @Transactional
    public OrderVO submit(OrderSubmitDTO dto) {
        if (dto.getProductId() == null) {
            throw new BusinessException("商品 ID 不能为空");
        }
        Long buyerId = UserContext.getUserId();
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new BusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        if (!ProductStatus.ON_SALE.getValue().equals(product.getStatus())) {
            throw new BusinessException(MessageConstant.PRODUCT_NOT_ON_SALE);
        }
        if (product.getSellerId().equals(buyerId)) {
            throw new BusinessException(MessageConstant.CANNOT_BUY_OWN);
        }

        // 分布式锁：串行化同一商品的下单请求
        RLock lock = redissonClient.getLock(RedisConstants.LOCK_ORDER_KEY + product.getId());
        boolean locked = false;
        boolean unlockAfterCommit = false;
        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException("当前抢购人数较多，请稍后再试");
            }
            // CAS 最终防线：在售(1) -> 交易中(2)
            int rows = productMapper.casOnSaleToTrading(product.getId());
            if (rows == 0) {
                throw new BusinessException(MessageConstant.PRODUCT_SOLD_OUT);
            }

            ProductOrder order = new ProductOrder();
            order.setOrderNo(redisIdWorker.nextOrderNo());
            order.setProductId(product.getId());
            order.setBuyerId(buyerId);
            order.setSellerId(product.getSellerId());
            order.setAmount(product.getPrice());
            order.setStatus(OrderStatus.PENDING_PAYMENT.getValue());
            order.setDeliveryType(dto.getDeliveryType() == null ? product.getDeliveryType() : dto.getDeliveryType());
            order.setRemark(dto.getRemark());
            orderMapper.insert(order);

            // 加入延迟队列（score = 过期时间戳，秒）
            long expireAt = System.currentTimeMillis() / 1000 + payTimeoutMinutes * 60;
            stringRedisTemplate.opsForZSet().add(RedisConstants.DELAY_QUEUE_ORDER_KEY,
                    order.getId().toString(), expireAt);
            // 删除商品缓存（数据一致性）
            stringRedisTemplate.delete(RedisConstants.CACHE_PRODUCT_KEY + product.getId());
            // 通知卖家：新订单
            WebSocketServer.sendToUser(product.getSellerId(), WsMessageType.NEW_ORDER.name(),
                    "买家已下单《" + product.getTitle() + "》，等待付款");

            // 正常路径：锁延迟到事务提交后再释放，消除“锁已释放、事务未提交”的窗口
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                });
                unlockAfterCommit = true;
            }
            return buildOrderVO(order);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(MessageConstant.UNKNOWN_ERROR);
        } finally {
            // 异常或无事务路径立即释放；正常路径由事务完成后回调释放
            if (locked && !unlockAfterCommit && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional
    public void pay(Long id) {
        ProductOrder order = getOrder(id);
        checkBuyer(order);
        if (!OrderStatus.PENDING_PAYMENT.getValue().equals(order.getStatus())) {
            throw new BusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 超时校验（任务可能存在延迟）
        if (order.getCreateTime() != null
                && order.getCreateTime().plusMinutes(payTimeoutMinutes).isBefore(LocalDateTime.now())) {
            // 取消必须在独立事务（REQUIRES_NEW）中完成：
            // 若与当前事务共用，随后抛出的“支付超时”异常回滚会把取消与商品回滚一并撤销
            applicationContext.getBean(OrderService.class).cancelTimeoutOrder(id);
            throw new BusinessException(MessageConstant.PAY_TIMEOUT);
        }
        ProductOrder update = new ProductOrder();
        update.setStatus(OrderStatus.PENDING_DELIVERY.getValue());
        update.setPayTime(LocalDateTime.now());
        // CAS：仅当仍为待支付时才允许置为已支付（与超时取消并发时唯一赢家）
        int rows = orderMapper.update(update, new LambdaUpdateWrapper<ProductOrder>()
                .eq(ProductOrder::getId, id)
                .eq(ProductOrder::getStatus, OrderStatus.PENDING_PAYMENT.getValue()));
        if (rows == 0) {
            throw new BusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        WebSocketServer.sendToUser(order.getSellerId(), WsMessageType.ORDER_PAID.name(),
                "订单 " + order.getOrderNo() + " 已付款，请尽快发货");
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        ProductOrder order = getOrder(id);
        checkBuyer(order);
        if (!OrderStatus.PENDING_PAYMENT.getValue().equals(order.getStatus())) {
            throw new BusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        if (!doCancel(order, "买家主动取消")) {
            throw new BusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    @Override
    @Transactional
    public void deliver(Long id) {
        ProductOrder order = getOrder(id);
        checkSeller(order);
        if (!OrderStatus.PENDING_DELIVERY.getValue().equals(order.getStatus())) {
            throw new BusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        ProductOrder update = new ProductOrder();
        update.setStatus(OrderStatus.PENDING_RECEIVE.getValue());
        update.setDeliverTime(LocalDateTime.now());
        // CAS：仅当仍为待发货时才允许置为待收货
        int rows = orderMapper.update(update, new LambdaUpdateWrapper<ProductOrder>()
                .eq(ProductOrder::getId, id)
                .eq(ProductOrder::getStatus, OrderStatus.PENDING_DELIVERY.getValue()));
        if (rows == 0) {
            throw new BusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        WebSocketServer.sendToUser(order.getBuyerId(), WsMessageType.ORDER_DELIVERED.name(),
                "卖家已发货，订单 " + order.getOrderNo() + "，请及时确认收货");
    }

    @Override
    @Transactional
    public void receive(Long id) {
        ProductOrder order = getOrder(id);
        checkBuyer(order);
        if (!OrderStatus.PENDING_RECEIVE.getValue().equals(order.getStatus())) {
            throw new BusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        ProductOrder update = new ProductOrder();
        update.setStatus(OrderStatus.COMPLETED.getValue());
        update.setReceiveTime(LocalDateTime.now());
        // CAS：仅当仍为待收货时才允许置为已完成
        int rows = orderMapper.update(update, new LambdaUpdateWrapper<ProductOrder>()
                .eq(ProductOrder::getId, id)
                .eq(ProductOrder::getStatus, OrderStatus.PENDING_RECEIVE.getValue()));
        if (rows == 0) {
            throw new BusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 商品：交易中 -> 已售出
        checkProductCas(productMapper.casTradingToSold(order.getProductId()),
                order.getId(), order.getProductId(), "交易中 -> 已售出");
        stringRedisTemplate.delete(RedisConstants.CACHE_PRODUCT_KEY + order.getProductId());
        WebSocketServer.sendToUser(order.getSellerId(), WsMessageType.ORDER_RECEIVED.name(),
                "买家已确认收货，订单 " + order.getOrderNo() + " 交易完成，别忘了给个评价哦");
    }

    @Override
    public void urge(Long id) {
        ProductOrder order = getOrder(id);
        checkBuyer(order);
        if (!OrderStatus.PENDING_DELIVERY.getValue().equals(order.getStatus())) {
            throw new BusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        WebSocketServer.sendToUser(order.getSellerId(), WsMessageType.URGE.name(),
                "买家催发货啦！订单 " + order.getOrderNo() + "，请尽快处理");
    }

    @Override
    public void applyRefund(Long id, String reason) {
        ProductOrder order = getOrder(id);
        checkBuyer(order);
        Integer status = order.getStatus();
        if (!OrderStatus.PENDING_DELIVERY.getValue().equals(status)
                && !OrderStatus.PENDING_RECEIVE.getValue().equals(status)) {
            throw new BusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        ProductOrder update = new ProductOrder();
        update.setStatus(OrderStatus.REFUNDING.getValue());
        update.setRefundReason(StrUtil.isBlank(reason) ? "买家申请退款" : reason);
        // CAS：仅当待发货/待收货时才允许进入退款中
        int rows = orderMapper.update(update, new LambdaUpdateWrapper<ProductOrder>()
                .eq(ProductOrder::getId, id)
                .in(ProductOrder::getStatus,
                        OrderStatus.PENDING_DELIVERY.getValue(), OrderStatus.PENDING_RECEIVE.getValue()));
        if (rows == 0) {
            throw new BusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        WebSocketServer.sendToUser(order.getSellerId(), WsMessageType.REFUND_APPLY.name(),
                "买家申请退款：订单 " + order.getOrderNo() + "，原因：" + update.getRefundReason());
    }

    @Override
    @Transactional
    public void handleRefund(Long id, RefundHandleDTO dto) {
        ProductOrder order = getOrder(id);
        checkSeller(order);
        if (!OrderStatus.REFUNDING.getValue().equals(order.getStatus())) {
            throw new BusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        ProductOrder update = new ProductOrder();
        boolean agree = Boolean.TRUE.equals(dto.getAgree());
        // CAS：仅当仍为退款中时才允许处理（与买家/平台并发操作时唯一赢家）
        LambdaUpdateWrapper<ProductOrder> cas = new LambdaUpdateWrapper<ProductOrder>()
                .eq(ProductOrder::getId, id)
                .eq(ProductOrder::getStatus, OrderStatus.REFUNDING.getValue());
        if (agree) {
            // 同意退款：订单已退款，商品恢复在售
            update.setStatus(OrderStatus.REFUNDED.getValue());
            if (orderMapper.update(update, cas) == 0) {
                throw new BusinessException(MessageConstant.ORDER_STATUS_ERROR);
            }
            checkProductCas(productMapper.casTradingToOnSale(order.getProductId()),
                    order.getId(), order.getProductId(), "交易中 -> 在售");
            stringRedisTemplate.delete(RedisConstants.CACHE_PRODUCT_KEY + order.getProductId());
            WebSocketServer.sendToUser(order.getBuyerId(), WsMessageType.REFUND_RESULT.name(),
                    "卖家已同意退款，订单 " + order.getOrderNo() + " 退款成功");
        } else {
            // 拒绝退款：根据发货时间恢复原状态（未发货 -> 待发货；已发货 -> 待收货）
            if (StrUtil.isBlank(dto.getRejectReason())) {
                throw new BusinessException("拒绝退款时必须填写理由");
            }
            update.setStatus(order.getDeliverTime() == null
                    ? OrderStatus.PENDING_DELIVERY.getValue()
                    : OrderStatus.PENDING_RECEIVE.getValue());
            update.setRefundRejectReason(dto.getRejectReason());
            if (orderMapper.update(update, cas) == 0) {
                throw new BusinessException(MessageConstant.ORDER_STATUS_ERROR);
            }
            WebSocketServer.sendToUser(order.getBuyerId(), WsMessageType.REFUND_RESULT.name(),
                    "卖家拒绝了退款申请：" + dto.getRejectReason());
        }
    }

    @Override
    public OrderVO getDetail(Long id) {
        ProductOrder order = getOrder(id);
        Long currentUserId = UserContext.getUserId();
        if (!currentUserId.equals(order.getBuyerId()) && !currentUserId.equals(order.getSellerId())) {
            throw new NoAuthException(MessageConstant.ORDER_NO_AUTH);
        }
        return buildOrderVO(order);
    }

    /* ==================== 列表 ==================== */

    @Override
    public PageResult<OrderVO> myBought(Integer status, int page, int size) {
        Page<OrderVO> mpPage = new Page<>(page, size);
        IPage<OrderVO> result = orderMapper.pageOrders(mpPage, UserContext.getUserId(), null,
                status, null, null, null);
        result.getRecords().forEach(this::fillStatusText);
        return new PageResult<>(result.getTotal(), result.getPages(), result.getRecords());
    }

    @Override
    public PageResult<OrderVO> mySold(Integer status, int page, int size) {
        Page<OrderVO> mpPage = new Page<>(page, size);
        IPage<OrderVO> result = orderMapper.pageOrders(mpPage, null, UserContext.getUserId(),
                status, null, null, null);
        result.getRecords().forEach(this::fillStatusText);
        return new PageResult<>(result.getTotal(), result.getPages(), result.getRecords());
    }

    /* ==================== 定时任务 ==================== */

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelTimeoutOrder(Long orderId) {
        ProductOrder order = orderMapper.selectById(orderId);
        if (order == null || !OrderStatus.PENDING_PAYMENT.getValue().equals(order.getStatus())) {
            return;
        }
        // REQUIRES_NEW：支付入口在抛“支付超时”异常前调用本方法，
        // 取消需独立提交，不能被外层事务的回滚撤销；CAS 失败说明已被其他路径处理，幂等忽略
        doCancel(order, "超时未支付，系统自动取消");
    }

    @Override
    @Transactional
    public int autoReceiveOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(autoReceiveDays);
        // 分批处理，避免一次扫描过多订单（未处理的下一轮任务继续）
        List<ProductOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<ProductOrder>()
                .eq(ProductOrder::getStatus, OrderStatus.PENDING_RECEIVE.getValue())
                .isNotNull(ProductOrder::getDeliverTime)
                .le(ProductOrder::getDeliverTime, deadline)
                .last("LIMIT 200"));
        int processed = 0;
        for (ProductOrder order : orders) {
            ProductOrder update = new ProductOrder();
            update.setStatus(OrderStatus.COMPLETED.getValue());
            update.setReceiveTime(LocalDateTime.now());
            // CAS：仅当仍为待收货时才自动完成（并发下用户可能已手动确认收货）
            int rows = orderMapper.update(update, new LambdaUpdateWrapper<ProductOrder>()
                    .eq(ProductOrder::getId, order.getId())
                    .eq(ProductOrder::getStatus, OrderStatus.PENDING_RECEIVE.getValue()));
            if (rows == 0) {
                continue;
            }
            checkProductCas(productMapper.casTradingToSold(order.getProductId()),
                    order.getId(), order.getProductId(), "交易中 -> 已售出");
            stringRedisTemplate.delete(RedisConstants.CACHE_PRODUCT_KEY + order.getProductId());
            WebSocketServer.sendToUser(order.getSellerId(), WsMessageType.ORDER_RECEIVED.name(),
                    "订单 " + order.getOrderNo() + " 已超时自动确认收货");
            processed++;
        }
        return processed;
    }

    /* ==================== 管理端 ==================== */

    @Override
    public PageResult<OrderVO> adminPageOrders(Integer status, String keyword, String beginTime,
                                               String endTime, int page, int size) {
        Page<OrderVO> mpPage = new Page<>(page, size);
        IPage<OrderVO> result = orderMapper.pageOrders(mpPage, null, null, status, keyword, beginTime, endTime);
        result.getRecords().forEach(this::fillStatusText);
        return new PageResult<>(result.getTotal(), result.getPages(), result.getRecords());
    }

    @Override
    public OrderVO adminGetDetail(Long id) {
        return buildOrderVO(getOrder(id));
    }

    @Override
    @Transactional
    public void adminClose(Long id, String reason) {
        ProductOrder order = getOrder(id);
        ProductOrder update = new ProductOrder();
        update.setStatus(OrderStatus.CANCELED.getValue());
        update.setCancelReason("平台关闭：" + (StrUtil.isBlank(reason) ? "纠纷处理" : reason));
        update.setCancelTime(LocalDateTime.now());
        // CAS：已完成/已取消/已退款的终态订单不可关闭（并发变化同样拒绝）
        int rows = orderMapper.update(update, new LambdaUpdateWrapper<ProductOrder>()
                .eq(ProductOrder::getId, id)
                .notIn(ProductOrder::getStatus, OrderStatus.COMPLETED.getValue(),
                        OrderStatus.CANCELED.getValue(), OrderStatus.REFUNDED.getValue()));
        if (rows == 0) {
            throw new BusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        checkProductCas(productMapper.casTradingToOnSale(order.getProductId()),
                order.getId(), order.getProductId(), "交易中 -> 在售");
        stringRedisTemplate.delete(RedisConstants.CACHE_PRODUCT_KEY + order.getProductId());
        // 双向通知
        WebSocketServer.sendToUser(order.getBuyerId(), WsMessageType.ORDER_CANCELED.name(),
                "订单 " + order.getOrderNo() + " 已被平台关闭：" + update.getCancelReason());
        WebSocketServer.sendToUser(order.getSellerId(), WsMessageType.ORDER_CANCELED.name(),
                "订单 " + order.getOrderNo() + " 已被平台关闭：" + update.getCancelReason());
    }

    /* ==================== 私有方法 ==================== */

    /**
     * 取消订单并回滚商品（买家取消 / 超时取消共用）
     * @return true 表示本次取消生效；false 表示订单已被其他路径处理（支付/取消），幂等忽略
     */
    private boolean doCancel(ProductOrder order, String reason) {
        ProductOrder update = new ProductOrder();
        update.setStatus(OrderStatus.CANCELED.getValue());
        update.setCancelReason(reason);
        update.setCancelTime(LocalDateTime.now());
        // CAS：仅当仍为待支付时才允许取消（与支付/其他取消路径并发时唯一赢家）
        int rows = orderMapper.update(update, new LambdaUpdateWrapper<ProductOrder>()
                .eq(ProductOrder::getId, order.getId())
                .eq(ProductOrder::getStatus, OrderStatus.PENDING_PAYMENT.getValue()));
        if (rows == 0) {
            return false;
        }
        // 商品回滚：交易中 -> 在售
        checkProductCas(productMapper.casTradingToOnSale(order.getProductId()),
                order.getId(), order.getProductId(), "交易中 -> 在售");
        stringRedisTemplate.delete(RedisConstants.CACHE_PRODUCT_KEY + order.getProductId());
        WebSocketServer.sendToUser(order.getSellerId(), WsMessageType.ORDER_CANCELED.name(),
                "订单 " + order.getOrderNo() + " 已取消（" + reason + "），商品重新上架");
        return true;
    }

    /** 检查商品状态 CAS 影响行数（0 = 商品不在预期状态，记录日志暴露不一致，不影响订单流转） */
    private void checkProductCas(int rows, Long orderId, Long productId, String action) {
        if (rows == 0) {
            log.error("商品状态流转失败（期望 {}）：orderId={}, productId={}", action, orderId, productId);
        }
    }

    private ProductOrder getOrder(Long id) {
        ProductOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return order;
    }

    private void checkBuyer(ProductOrder order) {
        if (!UserContext.getUserId().equals(order.getBuyerId())) {
            throw new NoAuthException(MessageConstant.ORDER_NO_AUTH);
        }
    }

    private void checkSeller(ProductOrder order) {
        if (!UserContext.getUserId().equals(order.getSellerId())) {
            throw new NoAuthException(MessageConstant.ORDER_NO_AUTH);
        }
    }

    private OrderVO buildOrderVO(ProductOrder order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        fillStatusText(vo);
        Product product = productMapper.selectById(order.getProductId());
        if (product != null) {
            vo.setProductTitle(product.getTitle());
            vo.setProductCoverColor(product.getCoverColor());
        }
        User buyer = userMapper.selectById(order.getBuyerId());
        if (buyer != null) {
            vo.setBuyerNickname(buyer.getNickname());
            vo.setBuyerAvatarColor(buyer.getAvatarColor());
        }
        User seller = userMapper.selectById(order.getSellerId());
        if (seller != null) {
            vo.setSellerNickname(seller.getNickname());
            vo.setSellerAvatarColor(seller.getAvatarColor());
        }
        Integer reviewCount = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, order.getId()));
        vo.setReviewed(reviewCount != null && reviewCount > 0);
        return vo;
    }

    private void fillStatusText(OrderVO vo) {
        vo.setStatusText(OrderStatus.descOf(vo.getStatus()));
    }
}
