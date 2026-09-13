package com.campus.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 举报信息（管理端列表）
 */
@Data
public class ReportVO implements Serializable {

    private Long id;

    private Long reporterId;

    /** 举报人昵称 */
    private String reporterNickname;

    private Long productId;

    /** 被举报商品标题 */
    private String productTitle;

    /** 被举报商品封面 */
    private String productCoverColor;

    private String reason;

    private String description;

    /** 0 待处理 / 1 已处理 / 2 已驳回 */
    private Integer status;

    /** 状态文案 */
    private String statusText;

    private String handleRemark;

    private LocalDateTime createTime;
}
