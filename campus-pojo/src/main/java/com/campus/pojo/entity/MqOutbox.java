package com.campus.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 可靠消息 Outbox：业务事务只写数据库，后台任务在提交后投递 RabbitMQ。
 */
@Data
@TableName("tb_mq_outbox")
public class MqOutbox implements Serializable {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_SENT = 1;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 全局事件 ID，同时作为 MQ messageId，便于排查重复消息 */
    private String eventId;

    /** 事件类型，例如 ORDER_TIMEOUT */
    private String eventType;

    /** 聚合根 ID，本事件中为订单 ID */
    private Long aggregateId;

    /** 0待发送，1已发送 */
    private Integer status;

    private Integer retryCount;

    private LocalDateTime nextRetryTime;

    private LocalDateTime sentTime;

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
