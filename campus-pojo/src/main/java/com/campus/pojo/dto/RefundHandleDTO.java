package com.campus.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 退款处理请求（卖家）
 */
@Data
public class RefundHandleDTO implements Serializable {

    /** true 同意退款 / false 拒绝 */
    private Boolean agree;

    /** 拒绝理由（拒绝时必填） */
    private String rejectReason;
}
