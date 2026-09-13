package com.campus.service;

import com.campus.pojo.vo.OrderStatsVO;
import com.campus.pojo.vo.TopProductVO;
import com.campus.pojo.vo.TurnoverVO;
import com.campus.pojo.vo.UserGrowthVO;

import java.util.List;

/**
 * 统计服务（管理端数据报表）
 */
public interface StatisticsService {

    /** 成交额趋势 */
    TurnoverVO turnover(String begin, String end);

    /** 订单量趋势 */
    OrderStatsVO orderStats(String begin, String end);

    /** 用户增长趋势 */
    UserGrowthVO userGrowth(String begin, String end);

    /** 热销商品 Top10 */
    List<TopProductVO> topProducts();
}
