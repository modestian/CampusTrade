package com.campus.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 订单量趋势统计
 */
@Data
public class OrderStatsVO implements Serializable {

    /** 日期列表 */
    private List<String> dateList;

    /** 每日订单总数 */
    private List<Integer> orderCountList;

    /** 每日有效订单数（已完成/待发货/待收货） */
    private List<Integer> validOrderCountList;
}
