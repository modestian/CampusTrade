package com.campus.task;

import com.campus.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 自动确认收货任务（Spring Task）
 * 每 10 分钟扫描一次：发货满 N 天（默认 7 天）仍未确认收货的订单自动完成
 */
@Slf4j
@Component
public class AutoReceiveTask {

    @Resource
    private OrderService orderService;

    @Scheduled(fixedDelay = 600000, initialDelay = 60000)
    public void autoReceiveOrders() {
        int count = orderService.autoReceiveOrders();
        if (count > 0) {
            log.info("自动确认收货完成，共处理 {} 条订单", count);
        }
    }
}
