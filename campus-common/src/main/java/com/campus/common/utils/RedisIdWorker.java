package com.campus.common.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 全局唯一 ID 生成器（仿黑马点评 RedisIdWorker）
 * 基于 Redis INCR 按天分片，保证全局唯一、时间有序。
 */
@Component
public class RedisIdWorker {

    /** 2024-01-01 00:00:00 作为时间戳起点 */
    private static final long BEGIN_TIMESTAMP = 1704067200L;

    private static final int COUNT_BITS = 32;

    /** 订单日期自增键前缀 */
    private static final String ORDER_SEQ_PREFIX = "icr:order:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成全局唯一数字 ID：时间戳（秒）左移 32 位 + 当日自增序列
     */
    public long nextId(String keyPrefix) {
        long nowSecond = System.currentTimeMillis() / 1000;
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
        return timestamp << COUNT_BITS | count;
    }

    /**
     * 生成订单号：yyyyMMdd + 12 位当日自增序列（共 20 位，时间有序，便于排查）
     */
    public String nextOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = stringRedisTemplate.opsForValue().increment(ORDER_SEQ_PREFIX + date);
        return date + String.format("%012d", seq);
    }
}
