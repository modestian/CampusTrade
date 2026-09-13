package com.campus.service.impl;

import com.campus.mapper.StatisticsMapper;
import com.campus.pojo.vo.OrderStatsVO;
import com.campus.pojo.vo.TopProductVO;
import com.campus.pojo.vo.TurnoverVO;
import com.campus.pojo.vo.UserGrowthVO;
import com.campus.service.StatisticsService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计服务实现（按天补全日期，图表数据连续）
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private StatisticsMapper statisticsMapper;

    @Override
    public TurnoverVO turnover(String begin, String end) {
        List<String> dateList = buildDateList(begin, end);
        Map<String, BigDecimal> map = new HashMap<>();
        List<Map<String, Object>> rows = statisticsMapper.selectDailyTurnover(
                begin + " 00:00:00", end + " 23:59:59");
        for (Map<String, Object> row : rows) {
            map.put(String.valueOf(row.get("date")), toBigDecimal(row.get("turnover")));
        }
        List<BigDecimal> turnoverList = new ArrayList<>();
        for (String date : dateList) {
            turnoverList.add(map.getOrDefault(date, BigDecimal.ZERO));
        }
        TurnoverVO vo = new TurnoverVO();
        vo.setDateList(dateList);
        vo.setTurnoverList(turnoverList);
        return vo;
    }

    @Override
    public OrderStatsVO orderStats(String begin, String end) {
        List<String> dateList = buildDateList(begin, end);
        Map<String, Integer> totalMap = new HashMap<>();
        Map<String, Integer> validMap = new HashMap<>();
        List<Map<String, Object>> rows = statisticsMapper.selectDailyOrderStats(
                begin + " 00:00:00", end + " 23:59:59");
        for (Map<String, Object> row : rows) {
            String date = String.valueOf(row.get("date"));
            totalMap.put(date, toInt(row.get("orderCount")));
            validMap.put(date, toInt(row.get("validOrderCount")));
        }
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        for (String date : dateList) {
            orderCountList.add(totalMap.getOrDefault(date, 0));
            validOrderCountList.add(validMap.getOrDefault(date, 0));
        }
        OrderStatsVO vo = new OrderStatsVO();
        vo.setDateList(dateList);
        vo.setOrderCountList(orderCountList);
        vo.setValidOrderCountList(validOrderCountList);
        return vo;
    }

    @Override
    public UserGrowthVO userGrowth(String begin, String end) {
        List<String> dateList = buildDateList(begin, end);
        Map<String, Integer> map = new HashMap<>();
        List<Map<String, Object>> rows = statisticsMapper.selectDailyNewUsers(
                begin + " 00:00:00", end + " 23:59:59");
        for (Map<String, Object> row : rows) {
            map.put(String.valueOf(row.get("date")), toInt(row.get("newUsers")));
        }
        List<Integer> newUserList = new ArrayList<>();
        for (String date : dateList) {
            newUserList.add(map.getOrDefault(date, 0));
        }
        UserGrowthVO vo = new UserGrowthVO();
        vo.setDateList(dateList);
        vo.setNewUserList(newUserList);
        return vo;
    }

    @Override
    public List<TopProductVO> topProducts() {
        return statisticsMapper.selectTopProducts(10);
    }

    /* ---------- 私有方法 ---------- */

    private List<String> buildDateList(String begin, String end) {
        List<String> dates = new ArrayList<>();
        LocalDate b = LocalDate.parse(begin, DAY_FORMAT);
        LocalDate e = LocalDate.parse(end, DAY_FORMAT);
        while (!b.isAfter(e)) {
            dates.add(b.format(DAY_FORMAT));
            b = b.plusDays(1);
        }
        return dates;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }

    private Integer toInt(Object value) {
        if (value == null) {
            return 0;
        }
        return Integer.valueOf(value.toString());
    }
}
