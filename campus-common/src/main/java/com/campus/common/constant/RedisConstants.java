package com.campus.common.constant;

/**
 * Redis 键常量
 */
public class RedisConstants {

    /* ---------- 登录态 ---------- */
    public static final String LOGIN_TOKEN_KEY = "login:token:";
    public static final String LOGIN_ADMIN_KEY = "login:admin:";
    public static final Long LOGIN_TOKEN_TTL = 30L;

    /* ---------- 商品缓存 ---------- */
    public static final String CACHE_PRODUCT_KEY = "cache:product:";
    public static final Long CACHE_PRODUCT_TTL = 10L;
    public static final Long CACHE_NULL_TTL = 2L;
    public static final String CACHE_CATEGORY_LIST_KEY = "cache:category:list";
    public static final Long CACHE_CATEGORY_TTL = 60L;

    /* ---------- 下单锁 ---------- */
    public static final String LOCK_ORDER_KEY = "lock:order:";

    /* ---------- 订单延迟队列 ---------- */
    public static final String DELAY_QUEUE_ORDER_KEY = "delay:queue:order";

    /* ---------- 社交 ---------- */
    public static final String FOLLOW_KEY = "follow:";
    public static final String FANS_KEY = "fans:";
    public static final String FEED_KEY = "feed:";
    public static final String LIKE_PRODUCT_KEY = "like:product:";
    public static final String RANK_HOT_PRODUCT_KEY = "rank:hot:product";

    /* ---------- 秒杀 ---------- */
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String SECKILL_BUYERS_KEY = "seckill:buyers:";

    /* ---------- 全局ID ---------- */
    public static final String ID_ORDER_KEY = "id:order:";
}
