package com.campus.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 举报处理请求（管理端）
 */
@Data
public class ReportHandleDTO implements Serializable {

    /** offline 下架商品 / reject 驳回举报 */
    private String action;

    /** 处理备注 */
    private String remark;
}
