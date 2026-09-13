package com.campus.common.context;

/**
 * 当前登录人上下文（ThreadLocal）
 * 由拦截器在请求开始时写入，请求结束后清理。
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Long> ADMIN_HOLDER = new ThreadLocal<>();

    /* ---------- 学生端 ---------- */

    public static void setUserId(Long userId) {
        USER_HOLDER.set(userId);
    }

    public static Long getUserId() {
        return USER_HOLDER.get();
    }

    public static void removeUserId() {
        USER_HOLDER.remove();
    }

    /* ---------- 管理端 ---------- */

    public static void setAdminId(Long adminId) {
        ADMIN_HOLDER.set(adminId);
    }

    public static Long getAdminId() {
        return ADMIN_HOLDER.get();
    }

    public static void removeAdminId() {
        ADMIN_HOLDER.remove();
    }
}
