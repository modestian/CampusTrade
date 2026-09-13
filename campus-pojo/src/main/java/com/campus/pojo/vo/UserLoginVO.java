package com.campus.pojo.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 学生登录响应
 */
@Data
public class UserLoginVO implements Serializable {

    /** JWT 令牌 */
    private String token;

    /** 用户信息 */
    private UserVO user;
}
