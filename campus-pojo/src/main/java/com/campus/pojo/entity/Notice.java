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
 * 平台公告
 */
@Data
@TableName("tb_notice")
public class Notice implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布管理员 ID */
    private Long adminId;

    /** 公告标题 */
    private String title;

    /** 公告内容 */
    private String content;

    /** 1 展示 / 0 隐藏（伪删除） */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
