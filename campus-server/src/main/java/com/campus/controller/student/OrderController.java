package com.campus.controller.student;

import com.campus.common.result.PageResult;
import com.campus.common.result.Result;
import com.campus.pojo.dto.OrderSubmitDTO;
import com.campus.pojo.dto.RefundHandleDTO;
import com.campus.pojo.dto.ReviewDTO;
import com.campus.pojo.vo.OrderVO;
import com.campus.service.OrderService;
import com.campus.service.ReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 学生端：订单（买家侧 + 卖家侧）
 */
@RestController
@RequestMapping("/student")
public class OrderController {

    @Resource
    private OrderService orderService;

    @Resource
    private ReviewService reviewService;

    /* ---------- 买家操作 ---------- */

    /** 下单（返回订单信息） */
    @PostMapping("/orders")
    public Result<OrderVO> submit(@RequestBody OrderSubmitDTO orderSubmitDTO) {
        return Result.success(orderService.submit(orderSubmitDTO));
    }

    /** 模拟支付 */
    @PutMapping("/orders/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        orderService.pay(id);
        return Result.success();
    }

    /** 取消订单 */
    @PutMapping("/orders/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return Result.success();
    }

    /** 确认收货 */
    @PutMapping("/orders/{id}/receive")
    public Result<Void> receive(@PathVariable Long id) {
        orderService.receive(id);
        return Result.success();
    }

    /** 催发货 */
    @PutMapping("/orders/{id}/urge")
    public Result<Void> urge(@PathVariable Long id) {
        orderService.urge(id);
        return Result.success();
    }

    /** 申请退款 */
    @PostMapping("/orders/{id}/refund")
    public Result<Void> applyRefund(@PathVariable Long id, @RequestParam(required = false) String reason) {
        orderService.applyRefund(id, reason);
        return Result.success();
    }

    /** 评价订单 */
    @PostMapping("/orders/{id}/review")
    public Result<Void> review(@PathVariable Long id, @RequestBody ReviewDTO reviewDTO) {
        reviewDTO.setOrderId(id);
        reviewService.submit(reviewDTO);
        return Result.success();
    }

    /* ---------- 卖家操作 ---------- */

    /** 发货 */
    @PutMapping("/orders/{id}/deliver")
    public Result<Void> deliver(@PathVariable Long id) {
        orderService.deliver(id);
        return Result.success();
    }

    /** 处理退款申请 */
    @PutMapping("/orders/{id}/refund/handle")
    public Result<Void> handleRefund(@PathVariable Long id, @RequestBody RefundHandleDTO refundHandleDTO) {
        orderService.handleRefund(id, refundHandleDTO);
        return Result.success();
    }

    /* ---------- 列表与详情 ---------- */

    /** 我买到的 */
    @GetMapping("/my/orders/bought")
    public Result<PageResult<OrderVO>> myBought(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(orderService.myBought(status, page, pageSize));
    }

    /** 我卖出的 */
    @GetMapping("/my/orders/sold")
    public Result<PageResult<OrderVO>> mySold(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(orderService.mySold(status, page, pageSize));
    }

    /** 订单详情 */
    @GetMapping("/orders/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.getDetail(id));
    }
}
