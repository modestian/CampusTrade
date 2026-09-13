package com.campus.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 修改个人资料请求
 */
@Data
public class UserUpdateDTO implements Serializable {

    /** 昵称 */
    private String nickname;

    /** 头像色块 */
    private String avatarColor;

    /** 手机号 */
    private String phone;

    /** 常用校区/楼栋 */
    private String campus;
}
