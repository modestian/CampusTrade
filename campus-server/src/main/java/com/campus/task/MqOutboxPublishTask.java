package com.campus.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.config.RabbitMqConstants;
import com.campus.mapper.MqOutboxMapper;
import com.campus.mq.ReliableRabbitPublisher;
import com.campus.pojo.entity.MqOutbox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/** 扫描未发送 Outbox 事件，通过 Publisher Confirm 可靠投递 RabbitMQ。 */
@Slf4j
@Component
@ConditionalOnProperty(name = "campus.order.timeout-strategy", havingValue = "rabbitmq", matchIfMissing = true)
public class MqOutboxPublishTask {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_ERROR_LENGTH = 500;

    @Resource
    private MqOutboxMapper outboxMapper;

    @Resource
    private ReliableRabbitPublisher publisher;

    @Value("${campus.mq.outbox.retention-days:7}")
    private long retentionDays;

    @Scheduled(fixedDelayString = "${campus.mq.outbox.publish-interval-ms:5000}", initialDelay = 5000)
    public void publishPendingEvents() {
        LocalDateTime now = LocalDateTime.now();
        List<MqOutbox> events = outboxMapper.selectList(new LambdaQueryWrapper<MqOutbox>()
                .eq(MqOutbox::getStatus, MqOutbox.STATUS_PENDING)
                .and(query -> query.isNull(MqOutbox::getNextRetryTime)
                        .or().le(MqOutbox::getNextRetryTime, now))
                .orderByAsc(MqOutbox::getId)
                .last("LIMIT " + BATCH_SIZE));

        for (MqOutbox event : events) {
            publishOne(event);
        }
    }

    /** 已发送事件只保留有限时间，避免 Outbox 表无限增长。 */
    @Scheduled(cron = "${campus.mq.outbox.cleanup-cron:0 0 3 * * ?}")
    public void cleanupSentEvents() {
        int deleted = outboxMapper.delete(new LambdaQueryWrapper<MqOutbox>()
                .eq(MqOutbox::getStatus, MqOutbox.STATUS_SENT)
                .le(MqOutbox::getSentTime, LocalDateTime.now().minusDays(retentionDays)));
        if (deleted > 0) {
            log.info("已清理过期 Outbox 事件：{} 条", deleted);
        }
    }

    private void publishOne(MqOutbox event) {
        try {
            if (!RabbitMqConstants.EVENT_ORDER_TIMEOUT.equals(event.getEventType())) {
                throw new IllegalArgumentException("不支持的 Outbox 事件类型: " + event.getEventType());
            }
            publisher.publish(RabbitMqConstants.ORDER_TIMEOUT_DELAY_ROUTING_KEY,
                    event.getAggregateId().toString(), event.getEventId(), 0);

            MqOutbox update = new MqOutbox();
            update.setStatus(MqOutbox.STATUS_SENT);
            update.setSentTime(LocalDateTime.now());
            update.setErrorMessage(null);
            outboxMapper.update(update, new LambdaUpdateWrapper<MqOutbox>()
                    .eq(MqOutbox::getId, event.getId())
                    .eq(MqOutbox::getStatus, MqOutbox.STATUS_PENDING));
        } catch (Exception e) {
            int retryCount = event.getRetryCount() == null ? 1 : event.getRetryCount() + 1;
            MqOutbox update = new MqOutbox();
            update.setRetryCount(retryCount);
            update.setNextRetryTime(LocalDateTime.now().plusSeconds(retryDelaySeconds(retryCount)));
            update.setErrorMessage(truncate(e.getMessage()));
            outboxMapper.update(update, new LambdaUpdateWrapper<MqOutbox>()
                    .eq(MqOutbox::getId, event.getId())
                    .eq(MqOutbox::getStatus, MqOutbox.STATUS_PENDING));
            log.error("Outbox 消息发送失败，将重试：eventId={}, retryCount={}",
                    event.getEventId(), retryCount, e);
        }
    }

    private long retryDelaySeconds(int retryCount) {
        return Math.min(300L, 5L * (1L << Math.min(retryCount - 1, 6)));
    }

    private String truncate(String message) {
        if (message == null) {
            return "unknown error";
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
