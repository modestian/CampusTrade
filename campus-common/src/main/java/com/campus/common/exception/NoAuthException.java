package com.campus.common.exception;

import com.campus.common.constant.MessageConstant;

/**
 * 无权限异常（越权操作）
 */
public class NoAuthException extends BaseException {

    public NoAuthException() {
        super(MessageConstant.NO_AUTH);
    }

    public NoAuthException(String msg) {
        super(msg);
    }
}
