package com.campus.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品（默认单件，先到先得）
 */
@Data
@TableName("tb_product")
public class Product implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 卖家用户 ID */
    private Long sellerId;

    /** 分类 ID */
    private Long categoryId;

    /** 商品标题 */
    private String title;

    /** 商品描述 */
    private String description;

    /** 封面色块 hex 值（替代图片） */
    private String coverColor;

    /** 出售价 */
    private BigDecimal price;

    /** 购入原价 */
    private BigDecimal originalPrice;

    /** 成色：1 全新 / 2 几乎全新 / 3 轻微使用痕迹 / 4 明显使用痕迹 / 5 磨损明显 */
    private Integer conditionLevel;

    /** 交付方式：1 校内面交 / 2 快递代取 */
    private Integer deliveryType;

    /** 面交地点 */
    private String campus;

    /** 浏览量 */
    private Integer viewCount;

    /** 收藏数 */
    private Integer collectCount;

    /** 点赞数 */
    private Integer likeCount;

    /** 0 待审核 / 1 在售 / 2 交易中 / 3 已售出 / 4 已下架 / 5 已驳回 */
    private Integer status;

    /** 审核意见（驳回原因） */
    private String auditRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
