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
 * 关注关系（Redis Set 加速查询，数据库持久化）
 */
@Data
@TableName("tb_follow")
public class Follow implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关注者（粉丝）ID */
    private Long userId;

    /** 被关注者 ID */
    private Long followUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
