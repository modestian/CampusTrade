package com.campus.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 下单请求
 */
@Data
public class OrderSubmitDTO implements Serializable {

    /** 客户端为一次下单生成的幂等键，网络重试必须复用同一个值 */
    private String requestId;

    /** 商品 ID */
    private Long productId;

    /** 交付方式：1 校内面交 / 2 快递代取 */
    private Integer deliveryType;

    /** 买家留言 */
    private String remark;
}
