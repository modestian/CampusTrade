package com.campus.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求（学生端 / 管理端共用）
 */
@Data
public class LoginDTO implements Serializable {

    /** 学号 / 管理员账号 */
    private String username;

    /** 密码 */
    private String password;
}
