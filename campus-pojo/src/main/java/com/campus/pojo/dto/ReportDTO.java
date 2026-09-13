package com.campus.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 举报请求
 */
@Data
public class ReportDTO implements Serializable {

    /** 被举报商品 ID */
    private Long productId;

    /** 举报原因 */
    private String reason;

    /** 补充说明 */
    private String description;
}
