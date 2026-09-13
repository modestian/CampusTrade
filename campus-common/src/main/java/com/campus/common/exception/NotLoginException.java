package com.campus.common.exception;

import com.campus.common.constant.MessageConstant;

/**
 * 未登录异常（登录态缺失或过期）
 */
public class NotLoginException extends BaseException {

    public NotLoginException() {
        super(MessageConstant.NOT_LOGIN);
    }
}
