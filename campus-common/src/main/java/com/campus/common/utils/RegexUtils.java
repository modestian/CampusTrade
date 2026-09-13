package com.campus.common.utils;

import java.util.regex.Pattern;

/**
 * 正则校验工具
 */
public class RegexUtils {

    /** 学号：6~12 位数字 */
    public static final Pattern USERNAME_PATTERN = Pattern.compile("^\\d{6,12}$");

    /** 密码：6~20 位 */
    public static final Pattern PASSWORD_PATTERN = Pattern.compile("^.{6,20}$");

    /** 手机号 */
    public static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /** 色块 hex 值 */
    public static final Pattern COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    public static boolean isUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    public static boolean isPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isColor(String color) {
        return color != null && COLOR_PATTERN.matcher(color).matches();
    }
}
