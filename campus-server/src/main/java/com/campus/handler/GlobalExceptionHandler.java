package com.campus.handler;

import com.campus.common.constant.MessageConstant;
import com.campus.common.exception.AccountLockedException;
import com.campus.common.exception.BaseException;
import com.campus.common.exception.NoAuthException;
import com.campus.common.exception.NotLoginException;
import com.campus.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 未登录：401 */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLogin(NotLoginException ex) {
        return Result.error(401, ex.getMessage());
    }

    /** 无权限 / 封禁：403 */
    @ExceptionHandler({NoAuthException.class, AccountLockedException.class})
    public Result<Void> handleNoAuth(BaseException ex) {
        return Result.error(403, ex.getMessage());
    }

    /** 通用业务异常：code = 0 */
    @ExceptionHandler(BaseException.class)
    public Result<Void> handleBusiness(BaseException ex) {
        return Result.error(ex.getMessage());
    }

    /** 唯一键冲突 */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result<Void> handleDuplicateKey(SQLIntegrityConstraintViolationException ex) {
        log.warn("唯一键冲突：{}", ex.getMessage());
        return Result.error("数据已存在，请勿重复操作");
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception ex) {
        log.error("系统异常", ex);
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }
}
