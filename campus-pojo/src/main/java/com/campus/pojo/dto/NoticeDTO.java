package com.campus.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 公告发布 / 编辑请求（管理端）
 */
@Data
public class NoticeDTO implements Serializable {

    /** 公告标题 */
    private String title;

    /** 公告内容 */
    private String content;
}
