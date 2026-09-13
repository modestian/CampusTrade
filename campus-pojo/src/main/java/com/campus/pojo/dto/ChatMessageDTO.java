package com.campus.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * WebSocket 私聊消息（客户端 -> 服务端）
 */
@Data
public class ChatMessageDTO implements Serializable {

    /** 接收方用户 ID */
    private Long receiverId;

    /** 消息内容 */
    private String content;
}
