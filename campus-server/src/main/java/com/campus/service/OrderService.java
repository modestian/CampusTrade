package com.campus.service;

import com.campus.common.result.PageResult;
import com.campus.pojo.dto.OrderSubmitDTO;
import com.campus.pojo.dto.RefundHandleDTO;
import com.campus.pojo.vo.OrderVO;

/**
 * 订单服务
 */
public interface OrderService {

    /* ---------- 交易主流程 ---------- */

    /** 下单（分布式锁 + 商品状态 CAS 防超卖） */
    OrderVO submit(OrderSubmitDTO orderSubmitDTO);

    /** 模拟支付 */
    void pay(Long id);

    /** 取消订单（买家） */
    void cancel(Long id);

    /** 发货（卖家） */
    void deliver(Long id);

    /** 确认收货（买家） */
    void receive(Long id);

    /** 催发货（买家，WebSocket 提醒卖家） */
    void urge(Long id);

    /** 申请退款（买家） */
    void applyRefund(Long id, String reason);

    /** 处理退款（卖家） */
    void handleRefund(Long id, RefundHandleDTO refundHandleDTO);

    /** 订单详情 */
    OrderVO getDetail(Long id);

    /* ---------- 列表 ---------- */

    /** 我买到的 */
    PageResult<OrderVO> myBought(Integer status, int page, int size);

    /** 我卖出的 */
    PageResult<OrderVO> mySold(Integer status, int page, int size);

    /* ---------- 定时任务 ---------- */

    /** 超时未支付订单取消（Spring Task 调用，商品回滚为在售） */
    void cancelTimeoutOrder(Long orderId);

    /** 自动确认收货（发货满 N 天，返回处理数量） */
    int autoReceiveOrders();

    /* ---------- 管理端 ---------- */

    /** 订单分页（管理端） */
    PageResult<OrderVO> adminPageOrders(Integer status, String keyword, String beginTime, String endTime, int page, int size);

    /** 订单详情（管理端） */
    OrderVO adminGetDetail(Long id);

    /** 关闭纠纷订单（管理端，商品回滚为在售） */
    void adminClose(Long id, String reason);
}
