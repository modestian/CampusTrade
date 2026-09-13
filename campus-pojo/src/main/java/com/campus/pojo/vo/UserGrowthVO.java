package com.campus.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户增长趋势统计
 */
@Data
public class UserGrowthVO implements Serializable {

    /** 日期列表 */
    private List<String> dateList;

    /** 每日新增用户数 */
    private List<Integer> newUserList;
}
