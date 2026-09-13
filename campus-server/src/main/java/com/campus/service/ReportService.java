package com.campus.service;

import com.campus.common.result.PageResult;
import com.campus.pojo.dto.ReportDTO;
import com.campus.pojo.dto.ReportHandleDTO;
import com.campus.pojo.vo.ReportVO;

/**
 * 举报服务
 */
public interface ReportService {

    /** 提交举报（学生端） */
    void submit(ReportDTO reportDTO);

    /** 举报分页（管理端） */
    PageResult<ReportVO> pageReports(Integer status, int page, int size);

    /** 处理举报（管理端）：offline 下架商品 / reject 驳回举报 */
    void handle(Long id, ReportHandleDTO reportHandleDTO);
}
