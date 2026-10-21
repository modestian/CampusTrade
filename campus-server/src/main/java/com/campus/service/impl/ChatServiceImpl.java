package com.campus.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.constant.MessageConstant;
import com.campus.common.enumeration.WsMessageType;
import com.campus.common.exception.BusinessException;
import com.campus.common.result.PageResult;
import com.campus.mapper.ChatMessageMapper;
import com.campus.mapper.UserMapper;
import com.campus.pojo.entity.ChatMessage;
import com.campus.pojo.entity.User;
import com.campus.pojo.vo.ChatSessionVO;
import com.campus.service.ChatService;
import com.campus.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 私聊服务实现（WebSocket 实时收发 + 落库）
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    /** 私聊内容长度上限（与 tb_chat_message.content VARCHAR(512) 对齐，超长将被拒绝而非静默丢失） */
    private static final int MAX_CONTENT_LENGTH = 512;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    @Transactional
    public void sendMessage(Long fromUserId, Long receiverId, String content) {
        if (fromUserId == null || receiverId == null || StrUtil.isBlank(content)) {
            return;
        }
        // 长度校验：超长会超出列宽导致落库失败，前端只回显本地而对方收不到，必须显式拒绝并回执
        if (content.length() > MAX_CONTENT_LENGTH) {
            log.warn("私聊消息超长被拒绝：from={}, length={}", fromUserId, content.length());
            WebSocketServer.sendToUser(fromUserId, WsMessageType.NOTICE.name(),
                    "消息发送失败：内容超过 " + MAX_CONTENT_LENGTH + " 字上限");
            return;
        }
        // 不允许给自己发消息
        if (fromUserId.equals(receiverId)) {
            return;
        }
        User receiver = userMapper.selectById(receiverId);
        if (receiver == null) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setFromUserId(fromUserId);
        message.setToUserId(receiverId);
        message.setContent(content);
        message.setIsRead(0);
        chatMessageMapper.insert(message);

        // 实时推送
        JSONObject data = new JSONObject();
        data.put("fromUserId", fromUserId);
        data.put("toUserId", receiverId);
        data.put("content", content);
        data.put("createTime", LocalDateTime.now().toString().replace("T", " "));
        WebSocketServer.sendToUser(receiverId, WsMessageType.CHAT.name(), data);
    }

    @Override
    @Transactional
    public PageResult<ChatMessage> history(Long userId, Long otherUserId, int page, int size) {
        // 读取即标记已读（对方发给我的）
        ChatMessage readUpdate = new ChatMessage();
        readUpdate.setIsRead(1);
        chatMessageMapper.update(readUpdate, new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getFromUserId, otherUserId)
                .eq(ChatMessage::getToUserId, userId)
                .eq(ChatMessage::getIsRead, 0));

        Page<ChatMessage> mpPage = new Page<>(page, size);
        chatMessageMapper.selectPage(mpPage, new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w.eq(ChatMessage::getFromUserId, userId).eq(ChatMessage::getToUserId, otherUserId))
                .or(w -> w.eq(ChatMessage::getFromUserId, otherUserId).eq(ChatMessage::getToUserId, userId))
                .orderByDesc(ChatMessage::getCreateTime));
        return new PageResult<>(mpPage.getTotal(), mpPage.getPages(), mpPage.getRecords());
    }

    @Override
    public List<ChatSessionVO> sessions(Long userId) {
        // 取最近 200 条相关消息，Java 分组出会话
        List<ChatMessage> recent = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w.eq(ChatMessage::getFromUserId, userId).or().eq(ChatMessage::getToUserId, userId))
                .orderByDesc(ChatMessage::getCreateTime)
                .last("LIMIT 200"));

        Map<Long, ChatMessage> lastByUser = new LinkedHashMap<>();
        Map<Long, Integer> unreadCount = new HashMap<>();
        for (ChatMessage message : recent) {
            Long other = message.getFromUserId().equals(userId)
                    ? message.getToUserId() : message.getFromUserId();
            lastByUser.putIfAbsent(other, message);
            if (message.getToUserId().equals(userId) && Integer.valueOf(0).equals(message.getIsRead())) {
                unreadCount.merge(other, 1, Integer::sum);
            }
        }

        List<ChatSessionVO> sessions = new ArrayList<>();
        lastByUser.forEach((otherId, message) -> {
            User other = userMapper.selectById(otherId);
            ChatSessionVO vo = new ChatSessionVO();
            vo.setUserId(otherId);
            vo.setNickname(other != null ? other.getNickname() : "未知用户");
            vo.setAvatarColor(other != null ? other.getAvatarColor() : "#DFE6E9");
            vo.setLastMessage(message.getContent());
            vo.setUnreadCount(unreadCount.getOrDefault(otherId, 0));
            vo.setLastTime(message.getCreateTime());
            sessions.add(vo);
        });
        return sessions;
    }
}
