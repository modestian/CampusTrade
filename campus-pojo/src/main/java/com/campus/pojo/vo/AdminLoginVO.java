package com.campus.pojo.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员登录响应
 */
@Data
public class AdminLoginVO implements Serializable {

    /** JWT 令牌 */
    private String token;

    /** 管理员信息 */
    private AdminVO admin;
}
