package com.campus.common.enumeration;

import lombok.Getter;

/**
 * 商品状态枚举
 * 0 待审核 / 1 在售 / 2 交易中 / 3 已售出 / 4 已下架 / 5 已驳回
 */
@Getter
public enum ProductStatus {

    PENDING_AUDIT(0, "待审核"),
    ON_SALE(1, "在售"),
    TRADING(2, "交易中"),
    SOLD(3, "已售出"),
    OFF_SHELF(4, "已下架"),
    REJECTED(5, "已驳回");

    private final Integer value;
    private final String desc;

    ProductStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static String descOf(Integer value) {
        for (ProductStatus status : values()) {
            if (status.value.equals(value)) {
                return status.desc;
            }
        }
        return "未知状态";
    }
}
