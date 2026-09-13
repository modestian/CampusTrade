package com.campus.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 热销商品 Top 统计
 */
@Data
public class TopProductVO implements Serializable {

    private Long productId;

    private String title;

    private String coverColor;

    /** 成交量 */
    private Integer orderCount;

    /** 成交额 */
    private BigDecimal turnover;
}
