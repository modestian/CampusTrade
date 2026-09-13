package com.campus.controller.admin;

import com.campus.common.result.Result;
import com.campus.pojo.vo.OrderStatsVO;
import com.campus.pojo.vo.TopProductVO;
import com.campus.pojo.vo.TurnoverVO;
import com.campus.pojo.vo.UserGrowthVO;
import com.campus.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 管理端：数据统计
 */
@RestController
@RequestMapping("/admin/statistics")
public class AdminStatisticsController {

    @Resource
    private StatisticsService statisticsService;

    /** 成交额趋势 */
    @GetMapping("/turnover")
    public Result<TurnoverVO> turnover(@RequestParam String begin, @RequestParam String end) {
        return Result.success(statisticsService.turnover(begin, end));
    }

    /** 订单量趋势 */
    @GetMapping("/orders")
    public Result<OrderStatsVO> orderStats(@RequestParam String begin, @RequestParam String end) {
        return Result.success(statisticsService.orderStats(begin, end));
    }

    /** 用户增长趋势 */
    @GetMapping("/users")
    public Result<UserGrowthVO> userGrowth(@RequestParam String begin, @RequestParam String end) {
        return Result.success(statisticsService.userGrowth(begin, end));
    }

    /** 热销商品 Top10 */
    @GetMapping("/top10")
    public Result<List<TopProductVO>> top10() {
        return Result.success(statisticsService.topProducts());
    }
}
