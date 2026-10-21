package com.campus.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单超时 RabbitMQ 拓扑。
 * 固定 TTL 队列到期后经 DLX 投递处理队列，无需额外延迟消息插件。
 */
@Configuration
@ConditionalOnProperty(name = "campus.order.timeout-strategy", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitMqConfig {

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(RabbitMqConstants.ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderTimeoutDelayQueue(
            @Value("${campus.order.pay-timeout-minutes:15}") long timeoutMinutes) {
        return QueueBuilder.durable(RabbitMqConstants.ORDER_TIMEOUT_DELAY_QUEUE)
                .ttl(Math.toIntExact(timeoutMinutes * 60 * 1000))
                .deadLetterExchange(RabbitMqConstants.ORDER_EXCHANGE)
                .deadLetterRoutingKey(RabbitMqConstants.ORDER_TIMEOUT_PROCESS_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue orderTimeoutProcessQueue() {
        return QueueBuilder.durable(RabbitMqConstants.ORDER_TIMEOUT_PROCESS_QUEUE).build();
    }

    @Bean
    public Queue orderTimeoutRetryQueue(
            @Value("${campus.mq.order-timeout.retry-delay-ms:30000}") long retryDelayMs) {
        return QueueBuilder.durable(RabbitMqConstants.ORDER_TIMEOUT_RETRY_QUEUE)
                .ttl(Math.toIntExact(retryDelayMs))
                .deadLetterExchange(RabbitMqConstants.ORDER_EXCHANGE)
                .deadLetterRoutingKey(RabbitMqConstants.ORDER_TIMEOUT_PROCESS_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue orderTimeoutDeadQueue() {
        return QueueBuilder.durable(RabbitMqConstants.ORDER_TIMEOUT_DEAD_QUEUE).build();
    }

    @Bean
    public Binding orderTimeoutDelayBinding(DirectExchange orderExchange, Queue orderTimeoutDelayQueue) {
        return BindingBuilder.bind(orderTimeoutDelayQueue).to(orderExchange)
                .with(RabbitMqConstants.ORDER_TIMEOUT_DELAY_ROUTING_KEY);
    }

    @Bean
    public Binding orderTimeoutProcessBinding(DirectExchange orderExchange, Queue orderTimeoutProcessQueue) {
        return BindingBuilder.bind(orderTimeoutProcessQueue).to(orderExchange)
                .with(RabbitMqConstants.ORDER_TIMEOUT_PROCESS_ROUTING_KEY);
    }

    @Bean
    public Binding orderTimeoutRetryBinding(DirectExchange orderExchange, Queue orderTimeoutRetryQueue) {
        return BindingBuilder.bind(orderTimeoutRetryQueue).to(orderExchange)
                .with(RabbitMqConstants.ORDER_TIMEOUT_RETRY_ROUTING_KEY);
    }

    @Bean
    public Binding orderTimeoutDeadBinding(DirectExchange orderExchange, Queue orderTimeoutDeadQueue) {
        return BindingBuilder.bind(orderTimeoutDeadQueue).to(orderExchange)
                .with(RabbitMqConstants.ORDER_TIMEOUT_DEAD_ROUTING_KEY);
    }
}
