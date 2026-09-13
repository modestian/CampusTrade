package com.campus.common.exception;

import com.campus.common.constant.MessageConstant;

/**
 * 账号被封禁异常
 */
public class AccountLockedException extends BaseException {

    public AccountLockedException() {
        super(MessageConstant.ACCOUNT_LOCKED);
    }
}
