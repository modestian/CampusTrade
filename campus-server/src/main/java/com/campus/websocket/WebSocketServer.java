package com.campus.websocket;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.campus.common.constant.JwtClaimsConstant;
import com.campus.common.constant.RedisConstants;
import com.campus.common.properties.JwtProperties;
import com.campus.common.utils.JwtUtil;
import com.campus.service.ChatService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务：订单实时提醒 + 私聊
 * 连接地址：ws://host:port/ws/{token}
 */
@Slf4j
@Component
@ServerEndpoint("/ws/{token}")
public class WebSocketServer {

    /** 在线会话表：用户 ID -> Session */
    private static final Map<Long, Session> ONLINE_SESSIONS = new ConcurrentHashMap<>();

    /* ---------- 静态注入的依赖（@ServerEndpoint 实例由容器创建，无法直接注入实例字段） ---------- */
    private static StringRedisTemplate stringRedisTemplate;
    private static JwtProperties jwtProperties;
    private static ChatService chatService;

    @Autowired
    public void setStringRedisTemplate(StringRedisTemplate template) {
        WebSocketServer.stringRedisTemplate = template;
    }

    @Autowired
    public void setJwtProperties(JwtProperties properties) {
        WebSocketServer.jwtProperties = properties;
    }

    @Autowired
    public void setChatService(ChatService service) {
        WebSocketServer.chatService = service;
    }

    /** 当前连接的用户 ID */
    private Long userId;

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        try {
            Claims claims = JwtUtil.parseJwt(jwtProperties.getStudentSecretKey(), token);
            String key = RedisConstants.LOGIN_TOKEN_KEY + token;
            String userJson = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isBlank(userJson)) {
                session.close();
                return;
            }
            JSONObject obj = JSON.parseObject(userJson);
            this.userId = obj.getLong("id");
            // 单会话策略：同一用户新连接覆盖旧连接（旧连接不再接收推送）
            ONLINE_SESSIONS.put(this.userId, session);
            log.info("WebSocket 连接建立：用户 {}", this.userId);
        } catch (Exception e) {
            log.warn("WebSocket 鉴权失败：{}", e.getMessage());
            try {
                session.close();
            } catch (Exception ignored) {
            }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            JSONObject obj = JSON.parseObject(message);
            String type = obj.getString("type");
            if ("CHAT".equals(type)) {
                Long receiverId = obj.getLong("receiverId");
                String content = obj.getString("content");
                chatService.sendMessage(this.userId, receiverId, content);
            }
        } catch (Exception e) {
            log.warn("WebSocket 消息处理失败：{}", e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {
        if (userId != null) {
            // 精确删除：仅当在线表中登记的仍是本连接时才移除，
            // 避免多端登录时旧连接关闭触发 remove(userId) 误删新连接
            ONLINE_SESSIONS.remove(userId, session);
            log.info("WebSocket 连接关闭：用户 {}", userId);
        }
    }

    /* ---------- 对外推送方法（供业务层调用） ---------- */

    /** 推送消息给指定用户（离线自动丢弃） */
    public static void sendToUser(Long userId, String type, Object data) {
        Session session = ONLINE_SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            JSONObject msg = new JSONObject();
            msg.put("type", type);
            msg.put("data", data);
            String text = msg.toJSONString();
            synchronized (session) {
                // 异步发送并检查发送结果（失败仅记日志，不影响业务主流程）
                session.getAsyncRemote().sendText(text, result -> {
                    if (!result.isOK()) {
                        log.warn("WebSocket 推送失败：userId={}, type={}", userId, type);
                    }
                });
            }
        }
    }

    /** 广播消息给所有在线用户 */
    public static void broadcast(String type, Object data) {
        ONLINE_SESSIONS.values().forEach(session -> {
            if (session.isOpen()) {
                JSONObject msg = new JSONObject();
                msg.put("type", type);
                msg.put("data", data);
                String text = msg.toJSONString();
                synchronized (session) {
                    session.getAsyncRemote().sendText(text, result -> {
                        if (!result.isOK()) {
                            log.warn("WebSocket 广播失败：type={}", type);
                        }
                    });
                }
            }
        });
    }
}
