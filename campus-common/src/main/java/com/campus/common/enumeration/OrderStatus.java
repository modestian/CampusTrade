package com.campus.common.enumeration;

import lombok.Getter;

/**
 * 订单状态枚举
 * 1 待支付 / 2 待发货 / 3 待收货 / 4 已完成 / 5 已取消 / 6 退款中 / 7 已退款
 */
@Getter
public enum OrderStatus {

    PENDING_PAYMENT(1, "待支付"),
    PENDING_DELIVERY(2, "待发货"),
    PENDING_RECEIVE(3, "待收货"),
    COMPLETED(4, "已完成"),
    CANCELED(5, "已取消"),
    REFUNDING(6, "退款中"),
    REFUNDED(7, "已退款");

    private final Integer value;
    private final String desc;

    OrderStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static String descOf(Integer value) {
        for (OrderStatus status : values()) {
            if (status.value.equals(value)) {
                return status.desc;
            }
        }
        return "未知状态";
    }
}
