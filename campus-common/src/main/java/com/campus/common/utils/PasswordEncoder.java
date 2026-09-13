package com.campus.common.utils;

import cn.hutool.crypto.digest.DigestUtil;

/**
 * 密码加密器（MD5 加盐）
 */
public class PasswordEncoder {

    private static final String SALT = "campus_trade";

    /** 加密 */
    public static String encode(String rawPassword) {
        return DigestUtil.md5Hex(rawPassword + SALT);
    }

    /** 校验 */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encode(rawPassword).equals(encodedPassword);
    }
}
