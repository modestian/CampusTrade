package com.campus.common.constant;

/**
 * 提示消息常量
 */
public class MessageConstant {

    /* ---------- 账号 ---------- */
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_LOCKED = "账号已被封禁，请联系管理员";
    public static final String USERNAME_EXISTS = "该学号已被注册";
    public static final String USERNAME_FORMAT_ERROR = "学号格式不正确（6~12位数字）";
    public static final String PASSWORD_FORMAT_ERROR = "密码长度需为 6~20 位";
    public static final String OLD_PASSWORD_ERROR = "原密码错误";
    public static final String PHONE_FORMAT_ERROR = "手机号格式不正确";

    /* ---------- 登录 ---------- */
    public static final String NOT_LOGIN = "未登录或登录已过期";
    public static final String NO_AUTH = "无权限执行该操作";

    /* ---------- 商品 ---------- */
    public static final String PRODUCT_NOT_FOUND = "商品不存在或已被删除";
    public static final String PRODUCT_NOT_ON_SALE = "商品当前不在售";
    public static final String PRODUCT_SOLD_OUT = "手慢了，商品已被买走";
    public static final String CANNOT_BUY_OWN = "不能购买自己发布的商品";
    public static final String CANNOT_OPERATE_OTHERS_PRODUCT = "只能操作自己发布的商品";
    public static final String PRODUCT_HAS_ORDER = "商品已有订单关联，无法删除";
    public static final String CATEGORY_HAS_PRODUCTS = "该分类下存在商品，无法删除";

    /* ---------- 订单 ---------- */
    public static final String ORDER_NOT_FOUND = "订单不存在";
    public static final String ORDER_NO_AUTH = "无权操作该订单";
    public static final String ORDER_STATUS_ERROR = "当前订单状态不允许该操作";
    public static final String PAY_TIMEOUT = "订单已超时，请重新下单";

    /* ---------- 评价 ---------- */
    public static final String REVIEW_EXISTS = "该订单已评价过";
    public static final String REVIEW_ONLY_COMPLETED = "订单完成后才能评价";

    /* ---------- 通用 ---------- */
    public static final String UNKNOWN_ERROR = "系统繁忙，请稍后再试";
    public static final String OPERATION_SUCCESS = "操作成功";
}
