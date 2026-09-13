package com.campus.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 卖家主页
 */
@Data
public class SellerHomeVO implements Serializable {

    /** 卖家基本信息 */
    private UserVO user;

    /** 在售商品数 */
    private Integer onSaleCount;

    /** 已售出数量 */
    private Integer soldCount;

    /** 收到评价数 */
    private Integer reviewCount;

    /** 平均评分 */
    private BigDecimal avgScore;

    /** 在售商品列表 */
    private List<ProductVO> onSaleProducts;

    /** 最新评价 */
    private List<ReviewVO> latestReviews;

    /** 当前用户是否已关注 */
    private Boolean followed = false;
}
