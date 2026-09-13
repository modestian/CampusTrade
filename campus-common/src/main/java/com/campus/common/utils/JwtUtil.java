package com.campus.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 */
public class JwtUtil {

    /**
     * 生成 JWT
     *
     * @param secretKey 签名密钥
     * @param ttlMillis 过期毫秒数
     * @param claims    自定义载荷
     */
    public static String createJwt(String secretKey, long ttlMillis, Map<String, Object> claims) {
        long expMillis = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMillis);
        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(exp)
                .signWith(SignatureAlgorithm.HS256, secretKey.getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    /**
     * 解析 JWT，签名不合法或已过期时抛出异常
     */
    public static Claims parseJwt(String secretKey, String token) {
        return Jwts.parser()
                .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token)
                .getBody();
    }
}
