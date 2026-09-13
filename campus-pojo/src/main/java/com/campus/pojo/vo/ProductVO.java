package com.campus.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表项
 */
@Data
public class ProductVO implements Serializable {

    private Long id;

    private String title;

    /** 封面色块 */
    private String coverColor;

    private BigDecimal price;

    private BigDecimal originalPrice;

    /** 成色 1~5 */
    private Integer conditionLevel;

    /** 成色文案（如"几乎全新"） */
    private String conditionText;

    /** 交付方式 1/2 */
    private Integer deliveryType;

    /** 面交地点 */
    private String campus;

    private Integer viewCount;

    private Integer collectCount;

    private Integer likeCount;

    /** 0 待审核 / 1 在售 / 2 交易中 / 3 已售出 / 4 已下架 / 5 已驳回 */
    private Integer status;

    /** 状态文案 */
    private String statusText;

    private Long categoryId;

    private String categoryName;

    private Long sellerId;

    private String sellerNickname;

    private String sellerAvatarColor;

    /** 审核意见（我的商品列表展示驳回原因） */
    private String auditRemark;

    private LocalDateTime createTime;
}
