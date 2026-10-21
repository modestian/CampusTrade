package com.campus.mq;

import com.campus.config.RabbitMqConstants;
import com.campus.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

/** 订单超时消息消费者：业务成功后手动 ACK，失败延迟重试，最终进入死信队列。 */
@Slf4j
@Component
@ConditionalOnProperty(name = "campus.order.timeout-strategy", havingValue = "rabbitmq", matchIfMissing = true)
public class OrderTimeoutConsumer {

    private static final int MAX_RETRY_COUNT = 3;

    @Resource
    private OrderService orderService;

    @Resource
    private ReliableRabbitPublisher publisher;

    @RabbitListener(queues = RabbitMqConstants.ORDER_TIMEOUT_PROCESS_QUEUE, ackMode = "MANUAL")
    public void consume(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String orderIdText = new String(message.getBody(), StandardCharsets.UTF_8);
        String eventId = headerAsString(message, RabbitMqConstants.HEADER_EVENT_ID);
        int retryCount = headerAsInt(message, RabbitMqConstants.HEADER_RETRY_COUNT);

        try {
            long orderId = Long.parseLong(orderIdText);
            // cancelTimeoutOrder 内部先检查状态并使用 CAS，重复消息可安全幂等消费。
            orderService.cancelTimeoutOrder(orderId);
            channel.basicAck(deliveryTag, false);
            log.info("订单超时消息处理完成：orderId={}, eventId={}", orderId, eventId);
        } catch (Exception businessError) {
            try {
                if (retryCount < MAX_RETRY_COUNT) {
                    publisher.publish(RabbitMqConstants.ORDER_TIMEOUT_RETRY_ROUTING_KEY,
                            orderIdText, eventId, retryCount + 1);
                    log.warn("订单超时消息处理失败，已进入延迟重试：eventId={}, retryCount={}",
                            eventId, retryCount + 1, businessError);
                } else {
                    publisher.publish(RabbitMqConstants.ORDER_TIMEOUT_DEAD_ROUTING_KEY,
                            orderIdText, eventId, retryCount);
                    log.error("订单超时消息超过重试上限，已进入死信队列：eventId={}",
                            eventId, businessError);
                }
                channel.basicAck(deliveryTag, false);
            } catch (Exception republishError) {
                // 重发未被 Broker 确认时保留原消息，避免消息丢失。
                channel.basicNack(deliveryTag, false, true);
                log.error("订单超时消息重发失败，原消息已重新入队：eventId={}", eventId, republishError);
            }
        }
    }

    private String headerAsString(Message message, String name) {
        Object value = message.getMessageProperties().getHeaders().get(name);
        return value == null ? message.getMessageProperties().getMessageId() : value.toString();
    }

    private int headerAsInt(Message message, String name) {
        Object value = message.getMessageProperties().getHeaders().get(name);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }
}
