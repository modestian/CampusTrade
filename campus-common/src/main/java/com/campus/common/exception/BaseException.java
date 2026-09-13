package com.campus.common.exception;

/**
 * 业务异常基类
 */
public class BaseException extends RuntimeException {

    public BaseException(String msg) {
        super(msg);
    }
}
