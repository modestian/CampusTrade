package com.campus.config;

import com.campus.interceptor.LoginInterceptor;
import com.campus.interceptor.RefreshTokenInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * Web MVC 配置：拦截器注册 + 前端静态资源映射（前后端分离，前端独立目录 campus-web）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private RefreshTokenInterceptor refreshTokenInterceptor;

    @Resource
    private LoginInterceptor loginInterceptor;

    /** 前端目录（前后端分离：campus-web 独立于 Maven 工程） */
    @Value("${campus.web.base-dir:../campus-web}")
    private String webBaseDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 第一道：登录态刷新（全部路径）
        registry.addInterceptor(refreshTokenInterceptor)
                .order(0)
                .addPathPatterns("/**");

        // 第二道：学生端登录校验（浏览类接口白名单放行）
        registry.addInterceptor(loginInterceptor)
                .order(1)
                .addPathPatterns("/student/**")
                .excludePathPatterns(
                        "/student/register",
                        "/student/login",
                        "/student/categories",
                        "/student/products",
                        "/student/products/*",
                        "/student/products/*/reviews",
                        "/student/users/*/home",
                        "/student/notices"
                );

        // 第二道：管理端登录校验
        registry.addInterceptor(loginInterceptor)
                .order(1)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 开发期统一协商缓存：no-cache 时浏览器每次向服务器验证（Last-Modified/304），
        // 前端文件更新后普通刷新即可生效，无需强制清缓存。
        CacheControl noCache = CacheControl.noCache();
        // 学生端页面：/student/**  ->  campus-web/student/
        registry.addResourceHandler("/student/**")
                .addResourceLocations("file:" + webBaseDir + "/student/")
                .setCacheControl(noCache);
        // 管理端页面：/admin/**（注意与 /admin 接口区分：接口前缀为 /admin/xxx，页面为 /admin/page/xxx）
        registry.addResourceHandler("/admin-ui/**")
                .addResourceLocations("file:" + webBaseDir + "/admin/")
                .setCacheControl(noCache);
        // 共享静态资源（vue、axios 等）
        registry.addResourceHandler("/campus-static/**")
                .addResourceLocations("file:" + webBaseDir + "/static/")
                .setCacheControl(noCache);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
