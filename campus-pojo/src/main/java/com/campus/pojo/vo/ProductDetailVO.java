package com.campus.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 商品详情
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductDetailVO extends ProductVO implements Serializable {

    /** 商品描述 */
    private String description;

    /** 当前用户是否已收藏 */
    private Boolean collected = false;

    /** 当前用户是否已点赞 */
    private Boolean liked = false;

    /** 当前用户是否已关注卖家 */
    private Boolean followed = false;

    /** 卖家信用分 */
    private Integer sellerCredit;

    /** 卖家已售出数量 */
    private Integer sellerSoldCount;
}
