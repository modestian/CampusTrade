package com.campus.config;

/** 订单消息拓扑常量 */
public final class RabbitMqConstants {

    private RabbitMqConstants() {
    }

    public static final String ORDER_EXCHANGE = "campus.order.exchange";

    public static final String ORDER_TIMEOUT_DELAY_QUEUE = "campus.order.timeout.delay.queue";
    public static final String ORDER_TIMEOUT_DELAY_ROUTING_KEY = "order.timeout.delay";

    public static final String ORDER_TIMEOUT_PROCESS_QUEUE = "campus.order.timeout.process.queue";
    public static final String ORDER_TIMEOUT_PROCESS_ROUTING_KEY = "order.timeout.execute";

    public static final String ORDER_TIMEOUT_RETRY_QUEUE = "campus.order.timeout.retry.queue";
    public static final String ORDER_TIMEOUT_RETRY_ROUTING_KEY = "order.timeout.retry";

    public static final String ORDER_TIMEOUT_DEAD_QUEUE = "campus.order.timeout.dead.queue";
    public static final String ORDER_TIMEOUT_DEAD_ROUTING_KEY = "order.timeout.dead";

    public static final String EVENT_ORDER_TIMEOUT = "ORDER_TIMEOUT";
    public static final String HEADER_EVENT_ID = "x-event-id";
    public static final String HEADER_RETRY_COUNT = "x-retry-count";
}
