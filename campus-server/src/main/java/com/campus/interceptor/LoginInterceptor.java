package com.campus.interceptor;

import com.campus.common.context.UserContext;
import com.campus.common.exception.NotLoginException;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录校验拦截器（第二道，拦截受保护路径）
 * 学生端校验 ThreadLocal 中的 userId，管理端校验 adminId
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String uri = request.getRequestURI();
        if (uri.startsWith("/admin")) {
            if (UserContext.getAdminId() == null) {
                throw new NotLoginException();
            }
        } else {
            if (UserContext.getUserId() == null) {
                throw new NotLoginException();
            }
        }
        return true;
    }
}
