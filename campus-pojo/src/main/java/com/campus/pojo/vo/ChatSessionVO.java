package com.campus.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天会话项
 */
@Data
public class ChatSessionVO implements Serializable {

    /** 对方用户 ID */
    private Long userId;

    /** 对方昵称 */
    private String nickname;

    /** 对方头像色块 */
    private String avatarColor;

    /** 最后一条消息 */
    private String lastMessage;

    /** 未读消息数 */
    private Integer unreadCount;

    /** 最后消息时间 */
    private LocalDateTime lastTime;
}
