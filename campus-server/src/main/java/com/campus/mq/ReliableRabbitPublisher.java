package com.campus.mq;

import com.campus.config.RabbitMqConstants;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** 使用 Publisher Confirm/Return 同步确认 Broker 已接管消息。 */
@Component
@ConditionalOnProperty(name = "campus.order.timeout-strategy", havingValue = "rabbitmq", matchIfMissing = true)
public class ReliableRabbitPublisher {

    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void publish(String routingKey, String body, String eventId, int retryCount) throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setMessageId(eventId);
        properties.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setHeader(RabbitMqConstants.HEADER_EVENT_ID, eventId);
        properties.setHeader(RabbitMqConstants.HEADER_RETRY_COUNT, retryCount);

        Message message = new Message(body.getBytes(StandardCharsets.UTF_8), properties);
        CorrelationData correlationData = new CorrelationData(eventId);
        rabbitTemplate.send(RabbitMqConstants.ORDER_EXCHANGE, routingKey, message, correlationData);

        CorrelationData.Confirm confirm = correlationData.getFuture()
                .get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!confirm.isAck()) {
            throw new IllegalStateException("RabbitMQ NACK: " + confirm.getReason());
        }
        if (correlationData.getReturned() != null) {
            throw new IllegalStateException("RabbitMQ 消息无法路由: "
                    + correlationData.getReturned().getReplyText());
        }
    }
}
