package com.campus.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.constant.MessageConstant;
import com.campus.common.constant.RedisConstants;
import com.campus.common.context.UserContext;
import com.campus.common.enumeration.ProductStatus;
import com.campus.common.enumeration.WsMessageType;
import com.campus.common.exception.BusinessException;
import com.campus.common.exception.NoAuthException;
import com.campus.common.result.PageResult;
import com.campus.common.utils.ColorPalette;
import com.campus.mapper.CategoryMapper;
import com.campus.mapper.OrderMapper;
import com.campus.mapper.ProductMapper;
import com.campus.mapper.ReviewMapper;
import com.campus.mapper.UserMapper;
import com.campus.pojo.dto.AuditDTO;
import com.campus.pojo.dto.ProductDTO;
import com.campus.pojo.entity.Category;
import com.campus.pojo.entity.Product;
import com.campus.pojo.entity.ProductOrder;
import com.campus.pojo.entity.Review;
import com.campus.pojo.entity.User;
import com.campus.pojo.vo.ProductDetailVO;
import com.campus.pojo.vo.ProductVO;
import com.campus.pojo.vo.ReviewVO;
import com.campus.pojo.vo.SellerHomeVO;
import com.campus.pojo.vo.UserVO;
import com.campus.service.ProductService;
import com.campus.service.SocialService;
import com.campus.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商品服务实现
 * 核心：缓存穿透（空值缓存）/ 击穿（互斥锁重建）/ 雪崩（随机 TTL）防护
 */
@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private ReviewMapper reviewMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private SocialService socialService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    /** 缓存重建锁等待重试上限（超过后回源数据库兜底，避免无限等待） */
    private static final int MAX_REBUILD_RETRY = 3;

    /* ==================== 买家侧 ==================== */

    @Override
    public PageResult<ProductVO> pageProducts(Long categoryId, String keyword, String sort, int page, int size) {
        Page<ProductVO> mpPage = new Page<>(page, size);
        IPage<ProductVO> result = productMapper.pageProducts(mpPage, categoryId, keyword,
                null, ProductStatus.ON_SALE.getValue(), sort);
        result.getRecords().forEach(this::fillText);
        return new PageResult<>(result.getTotal(), result.getPages(), result.getRecords());
    }

    @Override
    public ProductDetailVO getDetail(Long id) {
        return getDetail(id, 0);
    }

    private ProductDetailVO getDetail(Long id, int retryCount) {
        String key = RedisConstants.CACHE_PRODUCT_KEY + id;
        String cacheJson = stringRedisTemplate.opsForValue().get(key);

        ProductDetailVO vo;
        if (cacheJson != null) {
            // 命中：包括命中的空值（防穿透）
            if (cacheJson.isEmpty()) {
                throw new BusinessException(MessageConstant.PRODUCT_NOT_FOUND);
            }
            vo = JSON.parseObject(cacheJson, ProductDetailVO.class);
        } else {
            // 未命中：互斥锁重建（防击穿）
            RLock lock = redissonClient.getLock(RedisConstants.LOCK_PRODUCT_REBUILD_KEY + id);
            try {
                boolean locked = lock.tryLock(0, 10, TimeUnit.SECONDS);
                if (!locked) {
                    if (retryCount < MAX_REBUILD_RETRY) {
                        // 未拿到锁：短暂等待后重试
                        Thread.sleep(100);
                        return getDetail(id, retryCount + 1);
                    }
                    // 超过重试上限仍拿不到锁：回源数据库兜底（不再递归，避免无限等待）
                    vo = productMapper.selectProductDetail(id);
                    if (vo == null) {
                        // 空值缓存（防穿透）
                        stringRedisTemplate.opsForValue().set(key, "",
                                RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                        throw new BusinessException(MessageConstant.PRODUCT_NOT_FOUND);
                    }
                } else {
                    // 双重检查
                    cacheJson = stringRedisTemplate.opsForValue().get(key);
                    if (cacheJson != null && !cacheJson.isEmpty()) {
                        vo = JSON.parseObject(cacheJson, ProductDetailVO.class);
                    } else {
                        vo = productMapper.selectProductDetail(id);
                        if (vo == null) {
                            // 空值缓存（防穿透）
                            stringRedisTemplate.opsForValue().set(key, "",
                                    RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                            throw new BusinessException(MessageConstant.PRODUCT_NOT_FOUND);
                        }
                        // 随机 TTL（防雪崩）
                        long ttl = RedisConstants.CACHE_PRODUCT_TTL + ThreadLocalRandom.current().nextInt(5);
                        stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(vo), ttl, TimeUnit.MINUTES);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(MessageConstant.UNKNOWN_ERROR);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

        if (vo == null) {
            throw new BusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }

        // 待审核 / 已驳回商品仅卖家本人可见
        Long currentUserId = UserContext.getUserId();
        if ((vo.getStatus() == 0 || vo.getStatus() == 5)
                && (currentUserId == null || !currentUserId.equals(vo.getSellerId()))) {
            throw new BusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }

        // 浏览量 +1 与热度计分
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .setSql("view_count = view_count + 1")
                .eq(Product::getId, id));
        vo.setViewCount(vo.getViewCount() == null ? 1 : vo.getViewCount() + 1);
        try {
            stringRedisTemplate.opsForZSet().incrementScore(RedisConstants.RANK_HOT_PRODUCT_KEY, id.toString(), 1);
        } catch (Exception e) {
            log.warn("热度计分失败：{}", e.getMessage());
        }

        // 个性化标记
        fillText(vo);
        if (currentUserId != null) {
            vo.setCollected(socialService.isCollected(currentUserId, id));
            vo.setLiked(socialService.isLiked(currentUserId, id));
            vo.setFollowed(socialService.isFollowed(currentUserId, vo.getSellerId()));
        }
        // 卖家成交量
        Integer soldCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getSellerId, vo.getSellerId())
                .eq(Product::getStatus, ProductStatus.SOLD.getValue()));
        vo.setSellerSoldCount(soldCount);
        return vo;
    }

    @Override
    public List<ProductVO> hotProducts() {
        Set<String> ids = stringRedisTemplate.opsForZSet()
                .reverseRange(RedisConstants.RANK_HOT_PRODUCT_KEY, 0, 9);
        List<ProductVO> result = new ArrayList<>();
        if (ids != null && !ids.isEmpty()) {
            List<Long> idList = ids.stream().map(Long::valueOf).collect(Collectors.toList());
            List<ProductVO> products = productMapper.selectByIds(idList);
            // 按热度顺序返回（过滤非在售商品，避免已售出/下架商品出现在榜单）
            for (Long id : idList) {
                products.stream()
                        .filter(p -> p.getId().equals(id)
                                && ProductStatus.ON_SALE.getValue().equals(p.getStatus()))
                        .findFirst().ifPresent(result::add);
            }
            result.forEach(this::fillText);
        }
        if (result.isEmpty()) {
            // ZSet 为空时回退：按浏览量排序
            Page<ProductVO> mpPage = new Page<>(1, 10);
            IPage<ProductVO> page = productMapper.pageProducts(mpPage, null, null,
                    null, ProductStatus.ON_SALE.getValue(), "hot");
            result = page.getRecords();
            result.forEach(this::fillText);
        }
        return result;
    }

    @Override
    public SellerHomeVO sellerHome(Long sellerId) {
        User seller = userMapper.selectById(sellerId);
        if (seller == null) {
            throw new BusinessException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        SellerHomeVO vo = new SellerHomeVO();
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(seller, userVO);
        vo.setUser(userVO);

        vo.setOnSaleCount(Math.toIntExact(productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getSellerId, sellerId)
                .eq(Product::getStatus, ProductStatus.ON_SALE.getValue()))));
        vo.setSoldCount(Math.toIntExact(productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getSellerId, sellerId)
                .eq(Product::getStatus, ProductStatus.SOLD.getValue()))));

        List<ProductVO> onSale = productMapper.selectSellerOnSaleProducts(sellerId, 8);
        onSale.forEach(this::fillText);
        vo.setOnSaleProducts(onSale);

        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getSellerId, sellerId)
                .orderByDesc(Review::getCreateTime)
                .last("LIMIT 5"));
        List<ReviewVO> reviewVOS = reviews.stream().map(this::toReviewVO).collect(Collectors.toList());
        vo.setLatestReviews(reviewVOS);

        Integer reviewCount = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getSellerId, sellerId));
        vo.setReviewCount(reviewCount);
        if (reviewCount > 0) {
            List<Review> all = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                    .eq(Review::getSellerId, sellerId).select(Review::getScore));
            int sum = all.stream().mapToInt(Review::getScore).sum();
            vo.setAvgScore(BigDecimal.valueOf(sum)
                    .divide(BigDecimal.valueOf(all.size()), 1, RoundingMode.HALF_UP));
        } else {
            vo.setAvgScore(BigDecimal.valueOf(5.0));
        }

        Long currentUserId = UserContext.getUserId();
        if (currentUserId != null) {
            vo.setFollowed(socialService.isFollowed(currentUserId, sellerId));
        }
        return vo;
    }

    /* ==================== 卖家侧 ==================== */

    @Override
    public void publish(ProductDTO productDTO) {
        validateProduct(productDTO);
        Product product = new Product();
        BeanUtils.copyProperties(productDTO, product);
        product.setSellerId(UserContext.getUserId());
        product.setCoverColor(ColorPalette.normalize(productDTO.getCoverColor()));
        product.setDeliveryType(productDTO.getDeliveryType() == null ? 1 : productDTO.getDeliveryType());
        product.setConditionLevel(productDTO.getConditionLevel() == null ? 3 : productDTO.getConditionLevel());
        product.setViewCount(0);
        product.setCollectCount(0);
        product.setLikeCount(0);
        product.setStatus(ProductStatus.PENDING_AUDIT.getValue());
        productMapper.insert(product);
    }

    @Override
    public PageResult<ProductVO> myProducts(Integer status, int page, int size) {
        Page<ProductVO> mpPage = new Page<>(page, size);
        IPage<ProductVO> result = productMapper.pageProducts(mpPage, null, null,
                UserContext.getUserId(), status, "latest");
        result.getRecords().forEach(this::fillText);
        return new PageResult<>(result.getTotal(), result.getPages(), result.getRecords());
    }

    @Override
    public void edit(Long id, ProductDTO productDTO) {
        validateProduct(productDTO);
        Product exist = checkOwner(id);
        if (ProductStatus.TRADING.getValue().equals(exist.getStatus())
                || ProductStatus.SOLD.getValue().equals(exist.getStatus())) {
            throw new BusinessException("交易中/已售出的商品不可编辑");
        }
        Product update = new Product();
        BeanUtils.copyProperties(productDTO, update);
        update.setId(id);
        update.setCoverColor(ColorPalette.normalize(productDTO.getCoverColor()));
        if (ProductStatus.REJECTED.getValue().equals(exist.getStatus())) {
            // 驳回商品编辑后重新送审
            update.setStatus(ProductStatus.PENDING_AUDIT.getValue());
            update.setAuditRemark("");
        }
        productMapper.updateById(update);
        clearProductCache(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Product exist = checkOwner(id);
        Product update = new Product();
        update.setId(id);
        if (Integer.valueOf(4).equals(status) && ProductStatus.ON_SALE.getValue().equals(exist.getStatus())) {
            update.setStatus(ProductStatus.OFF_SHELF.getValue());
        } else if (Integer.valueOf(1).equals(status) && ProductStatus.OFF_SHELF.getValue().equals(exist.getStatus())) {
            update.setStatus(ProductStatus.ON_SALE.getValue());
        } else {
            throw new BusinessException("当前商品状态不允许该操作");
        }
        productMapper.updateById(update);
        clearProductCache(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        checkOwner(id);
        Integer orderCount = orderMapper.selectCount(new LambdaQueryWrapper<ProductOrder>()
                .eq(ProductOrder::getProductId, id));
        if (orderCount > 0) {
            throw new BusinessException(MessageConstant.PRODUCT_HAS_ORDER);
        }
        productMapper.deleteById(id);
        clearProductCache(id);
    }

    /* ==================== 管理端 ==================== */

    @Override
    public PageResult<ProductVO> adminPageProducts(Integer status, String keyword, Long categoryId, int page, int size) {
        Page<ProductVO> mpPage = new Page<>(page, size);
        IPage<ProductVO> result = productMapper.pageProducts(mpPage, categoryId, keyword, null, status, "latest");
        result.getRecords().forEach(this::fillText);
        return new PageResult<>(result.getTotal(), result.getPages(), result.getRecords());
    }

    @Override
    public ProductDetailVO adminDetail(Long id) {
        ProductDetailVO vo = productMapper.selectProductDetail(id);
        if (vo == null) {
            throw new BusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        fillText(vo);
        return vo;
    }

    @Override
    public void audit(Long id, AuditDTO auditDTO) {
        Product exist = productMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        if (!ProductStatus.PENDING_AUDIT.getValue().equals(exist.getStatus())) {
            throw new BusinessException("该商品不处于待审核状态");
        }
        Product update = new Product();
        update.setId(id);
        boolean pass = Boolean.TRUE.equals(auditDTO.getPass());
        if (pass) {
            update.setStatus(ProductStatus.ON_SALE.getValue());
            update.setAuditRemark("");
        } else {
            if (StrUtil.isBlank(auditDTO.getRemark())) {
                throw new BusinessException("驳回时必须填写原因");
            }
            update.setStatus(ProductStatus.REJECTED.getValue());
            update.setAuditRemark(auditDTO.getRemark());
        }
        productMapper.updateById(update);
        clearProductCache(id);

        // WebSocket 通知卖家审核结果
        String data = (pass ? "商品《" + exist.getTitle() + "》审核通过，已上架"
                : "商品《" + exist.getTitle() + "》审核未通过：" + auditDTO.getRemark());
        WebSocketServer.sendToUser(exist.getSellerId(), WsMessageType.AUDIT_RESULT.name(), data);

        // 审核通过后推送到粉丝 Feed 流
        if (pass) {
            socialService.pushToFans(exist.getSellerId(), id);
        }
    }

    @Override
    public void offline(Long id) {
        Product exist = productMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        Product update = new Product();
        update.setId(id);
        update.setStatus(ProductStatus.REJECTED.getValue());
        update.setAuditRemark("违规下架");
        productMapper.updateById(update);
        clearProductCache(id);
        WebSocketServer.sendToUser(exist.getSellerId(), WsMessageType.AUDIT_RESULT.name(),
                "商品《" + exist.getTitle() + "》因违规已被平台下架");
    }

    @Override
    public void restore(Long id) {
        Product exist = productMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        Product update = new Product();
        update.setId(id);
        update.setStatus(ProductStatus.ON_SALE.getValue());
        productMapper.updateById(update);
        clearProductCache(id);
    }

    /* ==================== 私有方法 ==================== */

    private void validateProduct(ProductDTO dto) {
        if (StrUtil.isBlank(dto.getTitle())) {
            throw new BusinessException("商品标题不能为空");
        }
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("商品价格必须大于 0");
        }
        if (dto.getCategoryId() == null || categoryMapper.selectById(dto.getCategoryId()) == null) {
            throw new BusinessException("商品分类不存在");
        }
    }

    private Product checkOwner(Long id) {
        Product exist = productMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        if (!exist.getSellerId().equals(UserContext.getUserId())) {
            throw new NoAuthException(MessageConstant.CANNOT_OPERATE_OTHERS_PRODUCT);
        }
        return exist;
    }

    private void clearProductCache(Long id) {
        stringRedisTemplate.delete(RedisConstants.CACHE_PRODUCT_KEY + id);
    }

    private void fillText(ProductVO vo) {
        vo.setStatusText(ProductStatus.descOf(vo.getStatus()));
        String[] conditionTexts = {"全新", "几乎全新", "轻微使用痕迹", "明显使用痕迹", "磨损明显"};
        if (vo.getConditionLevel() != null && vo.getConditionLevel() >= 1 && vo.getConditionLevel() <= 5) {
            vo.setConditionText(conditionTexts[vo.getConditionLevel() - 1]);
        }
    }

    private ReviewVO toReviewVO(Review review) {
        ReviewVO vo = new ReviewVO();
        BeanUtils.copyProperties(review, vo);
        if (review.getIsAnonymous() != null && review.getIsAnonymous() == 1) {
            vo.setReviewerNickname("匿名同学");
            vo.setReviewerAvatarColor("#DFE6E9");
        } else {
            User reviewer = userMapper.selectById(review.getReviewerId());
            if (reviewer != null) {
                vo.setReviewerNickname(reviewer.getNickname());
                vo.setReviewerAvatarColor(reviewer.getAvatarColor());
            }
        }
        Product product = productMapper.selectById(review.getProductId());
        if (product != null) {
            vo.setProductTitle(product.getTitle());
        }
        return vo;
    }
}
