package com.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.pojo.entity.Report;
import com.campus.pojo.vo.ReportVO;
import org.apache.ibatis.annotations.Param;

/**
 * 举报 Mapper（含联表查询）
 */
public interface ReportMapper extends BaseMapper<Report> {

    /** 举报分页（联查举报人、商品） */
    IPage<ReportVO> pageReports(IPage<ReportVO> page, @Param("status") Integer status);
}
