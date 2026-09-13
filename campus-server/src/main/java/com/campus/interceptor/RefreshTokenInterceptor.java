package com.campus.interceptor;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.campus.common.constant.JwtClaimsConstant;
import com.campus.common.constant.RedisConstants;
import com.campus.common.context.UserContext;
import com.campus.common.exception.AccountLockedException;
import com.campus.common.properties.JwtProperties;
import com.campus.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * 登录态刷新拦截器（第一道，拦截全部路径）
 * 仿黑马点评 RefreshTokenInterceptor：解析令牌 -> 校验 Redis 登录态 -> 写入 ThreadLocal -> 滑动续期
 */
@Slf4j
@Component
public class RefreshTokenInterceptor implements HandlerInterceptor {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        /* ---------- 学生端 token ---------- */
        String token = request.getHeader(jwtProperties.getStudentTokenName());
        if (StrUtil.isNotBlank(token)) {
            refreshStudentToken(token);
        }

        /* ---------- 管理端 admin-token ---------- */
        String adminToken = request.getHeader(jwtProperties.getAdminTokenName());
        if (StrUtil.isNotBlank(adminToken)) {
            refreshAdminToken(adminToken);
        }

        return true;
    }

    private void refreshStudentToken(String token) {
        try {
            Claims claims = JwtUtil.parseJwt(jwtProperties.getStudentSecretKey(), token);
            String key = RedisConstants.LOGIN_TOKEN_KEY + token;
            String userJson = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(userJson)) {
                JSONObject obj = JSON.parseObject(userJson);
                if (obj.getInteger("status") != null && obj.getInteger("status") == 1) {
                    throw new AccountLockedException();
                }
                UserContext.setUserId(obj.getLong("id"));
                // 滑动续期
                stringRedisTemplate.expire(key, RedisConstants.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
            }
        } catch (AccountLockedException e) {
            throw e;
        } catch (Exception e) {
            // 令牌无效或已过期：交由登录拦截器决定是否放行
            log.debug("学生端令牌解析失败：{}", e.getMessage());
        }
    }

    private void refreshAdminToken(String adminToken) {
        try {
            Claims claims = JwtUtil.parseJwt(jwtProperties.getAdminSecretKey(), adminToken);
            String key = RedisConstants.LOGIN_ADMIN_KEY + adminToken;
            String adminJson = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(adminJson)) {
                JSONObject obj = JSON.parseObject(adminJson);
                if (obj.getInteger("status") != null && obj.getInteger("status") == 0) {
                    throw new AccountLockedException();
                }
                UserContext.setAdminId(obj.getLong("id"));
                stringRedisTemplate.expire(key, RedisConstants.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
            }
        } catch (AccountLockedException e) {
            throw e;
        } catch (Exception e) {
            log.debug("管理端令牌解析失败：{}", e.getMessage());
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.removeUserId();
        UserContext.removeAdminId();
    }
}
