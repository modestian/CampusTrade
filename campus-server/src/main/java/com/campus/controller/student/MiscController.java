package com.campus.controller.student;

import com.campus.common.result.PageResult;
import com.campus.common.result.Result;
import com.campus.pojo.dto.ReportDTO;
import com.campus.pojo.entity.ChatMessage;
import com.campus.pojo.entity.Notice;
import com.campus.pojo.vo.ChatSessionVO;
import com.campus.service.ChatService;
import com.campus.service.NoticeService;
import com.campus.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 学生端：举报、公告与私聊（WebSocket 之外的历史记录与会话列表）
 */
@RestController
@RequestMapping("/student")
public class MiscController {

    @Resource
    private ReportService reportService;

    @Resource
    private NoticeService noticeService;

    @Resource
    private ChatService chatService;

    /** 举报商品 */
    @PostMapping("/reports")
    public Result<Void> report(@RequestBody ReportDTO reportDTO) {
        reportService.submit(reportDTO);
        return Result.success();
    }

    /** 公告列表（公开） */
    @GetMapping("/notices")
    public Result<PageResult<Notice>> notices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(noticeService.listEnabled(page, pageSize));
    }

    /** 我的会话列表 */
    @GetMapping("/chat/sessions")
    public Result<List<ChatSessionVO>> sessions() {
        return Result.success(chatService.sessions(com.campus.common.context.UserContext.getUserId()));
    }

    /** 与某用户的历史聊天记录（读取时自动标记已读） */
    @GetMapping("/chat/history/{userId}")
    public Result<PageResult<ChatMessage>> history(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(chatService.history(
                com.campus.common.context.UserContext.getUserId(), userId, page, pageSize));
    }
}
