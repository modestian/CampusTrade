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
 * 举报（商品举报与处理）
 */
@Data
@TableName("tb_report")
public class Report implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 举报人 ID */
    private Long reporterId;

    /** 被举报商品 ID */
    private Long productId;

    /** 举报原因 */
    private String reason;

    /** 补充说明 */
    private String description;

    /** 0 待处理 / 1 已处理（下架）/ 2 已驳回 */
    private Integer status;

    /** 处理备注 */
    private String handleRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
