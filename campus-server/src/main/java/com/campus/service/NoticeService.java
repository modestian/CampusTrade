package com.campus.service;

import com.campus.common.result.PageResult;
import com.campus.pojo.dto.NoticeDTO;
import com.campus.pojo.entity.Notice;

/**
 * 公告服务
 */
public interface NoticeService {

    /** 学生端：公告分页（仅展示中） */
    PageResult<Notice> listEnabled(int page, int size);

    /** 管理端：公告分页（全部） */
    PageResult<Notice> adminPage(int page, int size);

    /** 发布公告（WebSocket 广播） */
    void save(NoticeDTO noticeDTO);

    /** 编辑公告 */
    void update(Long id, NoticeDTO noticeDTO);

    /** 展示 / 隐藏 */
    void updateStatus(Long id, Integer status);

    /** 删除公告 */
    void delete(Long id);
}
