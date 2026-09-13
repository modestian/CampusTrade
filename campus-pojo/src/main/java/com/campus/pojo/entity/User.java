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
 * 学生用户（买卖一体账号）
 */
@Data
@TableName("tb_user")
public class User implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学号（登录账号） */
    private String username;

    /** MD5 加盐密码 */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像色块 hex 值 */
    private String avatarColor;

    /** 手机号 */
    private String phone;

    /** 常用校区/楼栋 */
    private String campus;

    /** 信用分（初始 100） */
    private Integer credit;

    /** 0 正常 / 1 封禁 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
