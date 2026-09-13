package com.campus.common.enumeration;

/**
 * WebSocket 推送事件类型
 */
public enum WsMessageType {
    /** 买家创建订单通知卖家 */
    NEW_ORDER,
    /** 买家支付成功通知卖家 */
    ORDER_PAID,
    /** 订单取消（双向） */
    ORDER_CANCELED,
    /** 卖家发货通知买家 */
    ORDER_DELIVERED,
    /** 买家确认收货通知卖家 */
    ORDER_RECEIVED,
    /** 买家催发货通知卖家 */
    URGE,
    /** 买家申请退款通知卖家 */
    REFUND_APPLY,
    /** 退款处理结果通知买家 */
    REFUND_RESULT,
    /** 商品审核结果通知卖家 */
    AUDIT_RESULT,
    /** 私聊消息 */
    CHAT,
    /** 平台公告广播 */
    NOTICE
}
