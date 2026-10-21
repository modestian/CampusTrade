package com.campus.task;

import com.campus.common.constant.RedisConstants;
import com.campus.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;

/**
 * 超时订单回退任务（仅 timeout-strategy=redis 时启用）
 * 每 10 秒轮询 delay:queue:order，将到期的待支付订单自动取消并回滚商品
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "campus.order.timeout-strategy", havingValue = "redis")
public class OrderTimeoutTask {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private OrderService orderService;

    @Scheduled(fixedDelay = 10000)
    public void processTimeoutOrders() {
        long now = System.currentTimeMillis() / 1000;
        Set<String> orderIds = stringRedisTemplate.opsForZSet()
                .rangeByScore(RedisConstants.DELAY_QUEUE_ORDER_KEY, 0, now, 0, 20);
        if (orderIds == null || orderIds.isEmpty()) {
            return;
        }
        for (String orderIdStr : orderIds) {
            long orderId;
            try {
                orderId = Long.parseLong(orderIdStr);
            } catch (NumberFormatException e) {
                // 脏数据：无法解析的成员直接出队，避免永远滞留队头
                log.error("延迟队列存在非法订单 ID，已丢弃：{}", orderIdStr);
                stringRedisTemplate.opsForZSet().remove(RedisConstants.DELAY_QUEUE_ORDER_KEY, orderIdStr);
                continue;
            }
            try {
                orderService.cancelTimeoutOrder(orderId);
                // 处理成功才出队；失败保留在队列，下一轮重试（避免失败即丢导致订单永不取消）
                stringRedisTemplate.opsForZSet().remove(RedisConstants.DELAY_QUEUE_ORDER_KEY, orderIdStr);
            } catch (Exception e) {
                log.error("处理超时订单失败，将在下一轮重试：orderId={}", orderIdStr, e);
            }
        }
        log.info("超时订单任务处理完成，本轮处理 {} 条", orderIds.size());
    }
}
