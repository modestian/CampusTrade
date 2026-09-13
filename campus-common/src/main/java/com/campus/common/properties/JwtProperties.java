package com.campus.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 双端配置（学生端 / 管理端）
 */
@Component
@ConfigurationProperties(prefix = "campus.jwt")
@Data
public class JwtProperties {

    /* ---------- 学生端 ---------- */
    private String studentSecretKey;
    private long studentTtl;
    private String studentTokenName;

    /* ---------- 管理端 ---------- */
    private String adminSecretKey;
    private long adminTtl;
    private String adminTokenName;
}
