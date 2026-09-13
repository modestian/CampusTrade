package com.campus.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单信息（买卖双方视角通用）
 */
@Data
public class OrderVO implements Serializable {

    private Long id;

    private String orderNo;

    private BigDecimal amount;

    /** 订单状态 1~7 */
    private Integer status;

    /** 状态文案 */
    private String statusText;

    /** 交付方式 */
    private Integer deliveryType;

    private String remark;

    private String cancelReason;

    private String refundReason;

    private String refundRejectReason;

    /* ---------- 商品快照信息 ---------- */
    private Long productId;

    private String productTitle;

    private String productCoverColor;

    /* ---------- 买家信息 ---------- */
    private Long buyerId;

    private String buyerNickname;

    private String buyerAvatarColor;

    /* ---------- 卖家信息 ---------- */
    private Long sellerId;

    private String sellerNickname;

    private String sellerAvatarColor;

    /** 是否已评价 */
    private Boolean reviewed = false;

    private LocalDateTime payTime;

    private LocalDateTime deliverTime;

    private LocalDateTime receiveTime;

    private LocalDateTime cancelTime;

    private LocalDateTime createTime;
}
