package com.campus.mq;

import com.campus.config.RabbitMqConstants;
import com.campus.service.OrderService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OrderTimeoutConsumerTest {

    private OrderService orderService;
    private ReliableRabbitPublisher publisher;
    private Channel channel;
    private OrderTimeoutConsumer consumer;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        publisher = mock(ReliableRabbitPublisher.class);
        channel = mock(Channel.class);
        consumer = new OrderTimeoutConsumer();
        ReflectionTestUtils.setField(consumer, "orderService", orderService);
        ReflectionTestUtils.setField(consumer, "publisher", publisher);
    }

    @Test
    void shouldAckAfterSuccessfulCancellation() throws Exception {
        Message message = message("42", "event-1", 0, 7L);

        consumer.consume(message, channel);

        verify(orderService).cancelTimeoutOrder(42L);
        verify(channel).basicAck(7L, false);
        verifyNoInteractions(publisher);
    }

    @Test
    void shouldPublishToRetryQueueWhenBusinessHandlingFails() throws Exception {
        doThrow(new IllegalStateException("db unavailable"))
                .when(orderService).cancelTimeoutOrder(42L);
        Message message = message("42", "event-2", 1, 8L);

        consumer.consume(message, channel);

        verify(publisher).publish(RabbitMqConstants.ORDER_TIMEOUT_RETRY_ROUTING_KEY,
                "42", "event-2", 2);
        verify(channel).basicAck(8L, false);
    }

    @Test
    void shouldPublishToDeadQueueAfterRetryLimit() throws Exception {
        doThrow(new IllegalStateException("db unavailable"))
                .when(orderService).cancelTimeoutOrder(42L);
        Message message = message("42", "event-3", 3, 9L);

        consumer.consume(message, channel);

        verify(publisher).publish(RabbitMqConstants.ORDER_TIMEOUT_DEAD_ROUTING_KEY,
                "42", "event-3", 3);
        verify(channel).basicAck(9L, false);
    }

    private Message message(String body, String eventId, int retryCount, long deliveryTag) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        properties.setMessageId(eventId);
        properties.setHeader(RabbitMqConstants.HEADER_EVENT_ID, eventId);
        properties.setHeader(RabbitMqConstants.HEADER_RETRY_COUNT, retryCount);
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }
}
