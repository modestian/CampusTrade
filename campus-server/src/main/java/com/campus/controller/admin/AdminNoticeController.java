package com.campus.controller.admin;

import com.campus.common.result.PageResult;
import com.campus.common.result.Result;
import com.campus.pojo.dto.NoticeDTO;
import com.campus.pojo.entity.Notice;
import com.campus.service.NoticeService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 管理端：公告管理
 */
@RestController
@RequestMapping("/admin/notices")
public class AdminNoticeController {

    @Resource
    private NoticeService noticeService;

    /** 公告分页（全部） */
    @GetMapping
    public Result<PageResult<Notice>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(noticeService.adminPage(page, pageSize));
    }

    /** 发布公告（WebSocket 广播） */
    @PostMapping
    public Result<Void> save(@RequestBody NoticeDTO noticeDTO) {
        noticeService.save(noticeDTO);
        return Result.success();
    }

    /** 编辑公告 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody NoticeDTO noticeDTO) {
        noticeService.update(id, noticeDTO);
        return Result.success();
    }

    /** 展示 / 隐藏 */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        noticeService.updateStatus(id, status);
        return Result.success();
    }

    /** 删除公告 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.success();
    }
}
