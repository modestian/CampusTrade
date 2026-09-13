package com.campus.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单
 * 状态：1 待支付 / 2 待发货 / 3 待收货 / 4 已完成 / 5 已取消 / 6 退款中 / 7 已退款
 */
@Data
@TableName("tb_order")
public class ProductOrder implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号（RedisIdWorker 生成） */
    private String orderNo;

    /** 商品 ID */
    private Long productId;

    /** 买家 ID */
    private Long buyerId;

    /** 卖家 ID（冗余，便于卖家侧查询） */
    private Long sellerId;

    /** 成交金额（下单时快照） */
    private BigDecimal amount;

    /** 订单状态 */
    private Integer status;

    /** 交付方式快照 */
    private Integer deliveryType;

    /** 买家留言 */
    private String remark;

    /** 取消原因（超时/买家取消/管理端关闭） */
    private String cancelReason;

    /** 退款申请原因 */
    private String refundReason;

    /** 卖家拒绝退款理由 */
    private String refundRejectReason;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 发货时间 */
    private LocalDateTime deliverTime;

    /** 收货时间 */
    private LocalDateTime receiveTime;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
