package com.campus.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码请求
 */
@Data
public class PasswordEditDTO implements Serializable {

    /** 原密码 */
    private String oldPassword;

    /** 新密码 */
    private String newPassword;
}
