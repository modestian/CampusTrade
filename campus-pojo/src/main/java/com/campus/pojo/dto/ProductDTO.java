package com.campus.pojo.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品发布 / 编辑请求
 */
@Data
public class ProductDTO implements Serializable {

    /** 商品标题 */
    private String title;

    /** 商品描述 */
    private String description;

    /** 分类 ID */
    private Long categoryId;

    /** 封面色块 hex 值 */
    private String coverColor;

    /** 出售价 */
    private BigDecimal price;

    /** 购入原价 */
    private BigDecimal originalPrice;

    /** 成色 1~5 */
    private Integer conditionLevel;

    /** 交付方式：1 校内面交 / 2 快递代取 */
    private Integer deliveryType;

    /** 面交地点 */
    private String campus;
}
