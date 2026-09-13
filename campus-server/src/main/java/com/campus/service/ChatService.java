package com.campus.service;

import com.campus.common.result.PageResult;
import com.campus.pojo.entity.ChatMessage;
import com.campus.pojo.vo.ChatSessionVO;

import java.util.List;

/**
 * 私聊服务
 */
public interface ChatService {

    /** 发送消息（WebSocket 调用：落库 + 实时推送） */
    void sendMessage(Long fromUserId, Long receiverId, String content);

    /** 与某用户的历史聊天记录（分页，读取时标记已读） */
    PageResult<ChatMessage> history(Long userId, Long otherUserId, int page, int size);

    /** 我的会话列表（含未读数） */
    List<ChatSessionVO> sessions(Long userId);
}
