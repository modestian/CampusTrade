package com.campus.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RabbitMqConfigTest {

    private final RabbitMqConfig config = new RabbitMqConfig();

    @Test
    void delayQueueShouldExpireIntoProcessRoutingKey() {
        Queue queue = config.orderTimeoutDelayQueue(15);

        assertEquals(900000, queue.getArguments().get("x-message-ttl"));
        assertEquals(RabbitMqConstants.ORDER_EXCHANGE,
                queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals(RabbitMqConstants.ORDER_TIMEOUT_PROCESS_ROUTING_KEY,
                queue.getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    void retryQueueShouldReturnToProcessQueueAfterDelay() {
        Queue queue = config.orderTimeoutRetryQueue(30000);

        assertEquals(30000, queue.getArguments().get("x-message-ttl"));
        assertEquals(RabbitMqConstants.ORDER_TIMEOUT_PROCESS_ROUTING_KEY,
                queue.getArguments().get("x-dead-letter-routing-key"));
    }
}
