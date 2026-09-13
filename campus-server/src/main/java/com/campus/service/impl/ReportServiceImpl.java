package com.campus.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.constant.MessageConstant;
import com.campus.common.context.UserContext;
import com.campus.common.enumeration.ProductStatus;
import com.campus.common.enumeration.WsMessageType;
import com.campus.common.exception.BusinessException;
import com.campus.common.result.PageResult;
import com.campus.mapper.ProductMapper;
import com.campus.mapper.ReportMapper;
import com.campus.pojo.dto.ReportDTO;
import com.campus.pojo.dto.ReportHandleDTO;
import com.campus.pojo.entity.Product;
import com.campus.pojo.entity.Report;
import com.campus.pojo.vo.ReportVO;
import com.campus.service.ReportService;
import com.campus.websocket.WebSocketServer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 举报服务实现
 */
@Service
public class ReportServiceImpl implements ReportService {

    @Resource
    private ReportMapper reportMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void submit(ReportDTO reportDTO) {
        Product product = productMapper.selectById(reportDTO.getProductId());
        if (product == null) {
            throw new BusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        if (product.getSellerId().equals(UserContext.getUserId())) {
            throw new BusinessException("不能举报自己发布的商品");
        }
        Report report = new Report();
        report.setReporterId(UserContext.getUserId());
        report.setProductId(reportDTO.getProductId());
        report.setReason(reportDTO.getReason());
        report.setDescription(reportDTO.getDescription());
        report.setStatus(0);
        reportMapper.insert(report);
    }

    @Override
    public PageResult<ReportVO> pageReports(Integer status, int page, int size) {
        Page<ReportVO> mpPage = new Page<>(page, size);
        IPage<ReportVO> result = reportMapper.pageReports(mpPage, status);
        result.getRecords().forEach(vo -> {
            String[] texts = {"待处理", "已处理", "已驳回"};
            if (vo.getStatus() != null && vo.getStatus() >= 0 && vo.getStatus() <= 2) {
                vo.setStatusText(texts[vo.getStatus()]);
            }
        });
        return new PageResult<>(result.getTotal(), result.getPages(), result.getRecords());
    }

    @Override
    @Transactional
    public void handle(Long id, ReportHandleDTO dto) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException("举报记录不存在");
        }
        if (!Integer.valueOf(0).equals(report.getStatus())) {
            throw new BusinessException("该举报已处理过");
        }
        Report update = new Report();
        update.setId(id);
        update.setHandleRemark(dto.getRemark());
        if ("offline".equals(dto.getAction())) {
            // 下架商品
            Product product = productMapper.selectById(report.getProductId());
            if (product != null) {
                Product productUpdate = new Product();
                productUpdate.setId(product.getId());
                productUpdate.setStatus(ProductStatus.REJECTED.getValue());
                productUpdate.setAuditRemark("被举报违规，平台下架");
                productMapper.updateById(productUpdate);
                stringRedisTemplate.delete(com.campus.common.constant.RedisConstants.CACHE_PRODUCT_KEY + product.getId());
                WebSocketServer.sendToUser(product.getSellerId(), WsMessageType.AUDIT_RESULT.name(),
                        "商品《" + product.getTitle() + "》被举报违规，已被平台下架");
            }
            update.setStatus(1);
        } else if ("reject".equals(dto.getAction())) {
            update.setStatus(2);
        } else {
            throw new BusinessException("非法处理动作");
        }
        reportMapper.updateById(update);
    }
}
