package com.campus.mapper;

import com.campus.pojo.vo.TopProductVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 统计 Mapper（管理端报表）
 */
public interface StatisticsMapper {

    /** 每日成交额（状态 2/3/4：已支付未退款） */
    List<Map<String, Object>> selectDailyTurnover(@Param("begin") String begin, @Param("end") String end);

    /** 每日订单量与有效订单量 */
    List<Map<String, Object>> selectDailyOrderStats(@Param("begin") String begin, @Param("end") String end);

    /** 每日新增用户 */
    List<Map<String, Object>> selectDailyNewUsers(@Param("begin") String begin, @Param("end") String end);

    /** 热销商品 Top（按成交量） */
    List<TopProductVO> selectTopProducts(@Param("limit") int limit);
}
