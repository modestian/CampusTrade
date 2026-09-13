package com.campus.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 管理端工作台数据概览
 */
@Data
public class WorkspaceVO implements Serializable {

    /** 今日新增用户 */
    private Integer todayNewUsers;

    /** 今日新增订单 */
    private Integer todayOrders;

    /** 今日成交额 */
    private BigDecimal todayTurnover;

    /** 用户总数 */
    private Integer totalUsers;

    /** 商品总数 */
    private Integer totalProducts;

    /** 订单总数 */
    private Integer totalOrders;

    /** 待审核商品数 */
    private Integer pendingAuditCount;

    /** 待处理举报数 */
    private Integer pendingReportCount;
}
