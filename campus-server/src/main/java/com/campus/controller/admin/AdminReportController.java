package com.campus.controller.admin;

import com.campus.common.result.PageResult;
import com.campus.common.result.Result;
import com.campus.pojo.dto.ReportHandleDTO;
import com.campus.pojo.vo.ReportVO;
import com.campus.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 管理端：举报处理
 */
@RestController
@RequestMapping("/admin/reports")
public class AdminReportController {

    @Resource
    private ReportService reportService;

    /** 举报分页 */
    @GetMapping
    public Result<PageResult<ReportVO>> page(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(reportService.pageReports(status, page, pageSize));
    }

    /** 处理举报（offline 下架商品 / reject 驳回举报） */
    @PutMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestBody ReportHandleDTO reportHandleDTO) {
        reportService.handle(id, reportHandleDTO);
        return Result.success();
    }
}
