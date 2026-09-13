package com.campus.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评价信息
 */
@Data
public class ReviewVO implements Serializable {

    private Long id;

    /** 评分 1~5 */
    private Integer score;

    private String content;

    /** 是否匿名 */
    private Integer isAnonymous;

    /** 评价人昵称（匿名时为"匿名同学"） */
    private String reviewerNickname;

    /** 评价人头像色块 */
    private String reviewerAvatarColor;

    private Long productId;

    private String productTitle;

    private LocalDateTime createTime;
}
