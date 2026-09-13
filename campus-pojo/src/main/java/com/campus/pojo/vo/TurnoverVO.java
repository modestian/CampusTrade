package com.campus.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 成交额趋势统计
 */
@Data
public class TurnoverVO implements Serializable {

    /** 日期列表（yyyy-MM-dd） */
    private List<String> dateList;

    /** 每日成交额 */
    private List<BigDecimal> turnoverList;
}
