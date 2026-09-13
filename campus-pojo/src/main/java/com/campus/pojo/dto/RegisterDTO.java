package com.campus.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 注册请求
 */
@Data
public class RegisterDTO implements Serializable {

    /** 学号（6~12 位数字） */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 密码（6~20 位） */
    private String password;

    /** 头像色块（可选，不填则随机） */
    private String avatarColor;
}
