package com.campus.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 评价请求
 */
@Data
public class ReviewDTO implements Serializable {

    /** 订单 ID */
    private Long orderId;

    /** 评分 1~5 */
    private Integer score;

    /** 评价内容 */
    private String content;

    /** 是否匿名 0/1 */
    private Integer isAnonymous;
}
