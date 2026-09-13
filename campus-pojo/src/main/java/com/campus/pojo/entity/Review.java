package com.campus.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单评价（一单一评）
 */
@Data
@TableName("tb_review")
public class Review implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单 ID（唯一） */
    private Long orderId;

    /** 商品 ID */
    private Long productId;

    /** 评价人（买家）ID */
    private Long reviewerId;

    /** 被评价卖家 ID */
    private Long sellerId;

    /** 评分 1~5 星 */
    private Integer score;

    /** 评价内容 */
    private String content;

    /** 0 实名 / 1 匿名 */
    private Integer isAnonymous;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
