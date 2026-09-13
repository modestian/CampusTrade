package com.campus.task;

import com.campus.common.constant.RedisConstants;
import com.campus.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;

/**
 * 超时订单任务（Spring Task + Redis 延迟队列）
 * 每 10 秒轮询 delay:queue:order，将到期的待支付订单自动取消并回滚商品
 */
@Slf4j
@Component
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
            try {
                orderService.cancelTimeoutOrder(Long.valueOf(orderIdStr));
            } catch (Exception e) {
                log.error("处理超时订单失败：orderId={}", orderIdStr, e);
            } finally {
                stringRedisTemplate.opsForZSet().remove(RedisConstants.DELAY_QUEUE_ORDER_KEY, orderIdStr);
            }
        }
        log.info("超时订单任务处理完成，本轮处理 {} 条", orderIds.size());
    }
}
