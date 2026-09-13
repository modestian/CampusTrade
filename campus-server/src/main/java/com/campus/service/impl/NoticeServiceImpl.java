package com.campus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.context.UserContext;
import com.campus.common.enumeration.WsMessageType;
import com.campus.common.exception.BusinessException;
import com.campus.common.result.PageResult;
import com.campus.mapper.NoticeMapper;
import com.campus.pojo.dto.NoticeDTO;
import com.campus.pojo.entity.Notice;
import com.campus.service.NoticeService;
import com.campus.websocket.WebSocketServer;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 公告服务实现（发布时 WebSocket 广播）
 */
@Service
public class NoticeServiceImpl implements NoticeService {

    @Resource
    private NoticeMapper noticeMapper;

    @Override
    public PageResult<Notice> listEnabled(int page, int size) {
        Page<Notice> mpPage = new Page<>(page, size);
        noticeMapper.selectPage(mpPage, new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, 1)
                .orderByDesc(Notice::getCreateTime));
        return new PageResult<>(mpPage.getTotal(), mpPage.getPages(), mpPage.getRecords());
    }

    @Override
    public PageResult<Notice> adminPage(int page, int size) {
        Page<Notice> mpPage = new Page<>(page, size);
        noticeMapper.selectPage(mpPage, new LambdaQueryWrapper<Notice>()
                .orderByDesc(Notice::getCreateTime));
        return new PageResult<>(mpPage.getTotal(), mpPage.getPages(), mpPage.getRecords());
    }

    @Override
    public void save(NoticeDTO noticeDTO) {
        Notice notice = new Notice();
        notice.setAdminId(UserContext.getAdminId());
        notice.setTitle(noticeDTO.getTitle());
        notice.setContent(noticeDTO.getContent());
        notice.setStatus(1);
        noticeMapper.insert(notice);
        // WebSocket 广播公告
        WebSocketServer.broadcast(WsMessageType.NOTICE.name(),
                "【平台公告】" + noticeDTO.getTitle() + "：" + noticeDTO.getContent());
    }

    @Override
    public void update(Long id, NoticeDTO noticeDTO) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException("公告不存在");
        }
        notice.setTitle(noticeDTO.getTitle());
        notice.setContent(noticeDTO.getContent());
        noticeMapper.updateById(notice);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException("公告不存在");
        }
        notice.setStatus(status);
        noticeMapper.updateById(notice);
    }

    @Override
    public void delete(Long id) {
        noticeMapper.deleteById(id);
    }
}
