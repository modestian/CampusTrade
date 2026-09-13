package com.campus.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 商品审核请求
 */
@Data
public class AuditDTO implements Serializable {

    /** true 通过 / false 驳回 */
    private Boolean pass;

    /** 审核意见（驳回必填） */
    private String remark;
}
