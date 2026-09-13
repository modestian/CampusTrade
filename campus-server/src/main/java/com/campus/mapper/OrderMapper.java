package com.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.pojo.entity.ProductOrder;
import com.campus.pojo.vo.OrderVO;
import org.apache.ibatis.annotations.Param;

/**
 * 订单 Mapper（含联表查询）
 */
public interface OrderMapper extends BaseMapper<ProductOrder> {

    /**
     * 订单分页（联查商品/买家/卖家），买卖双方与管理端共用
     */
    IPage<OrderVO> pageOrders(IPage<OrderVO> page,
                              @Param("buyerId") Long buyerId,
                              @Param("sellerId") Long sellerId,
                              @Param("status") Integer status,
                              @Param("keyword") String keyword,
                              @Param("beginTime") String beginTime,
                              @Param("endTime") String endTime);
}
