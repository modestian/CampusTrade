package com.campus.controller.admin;

import com.campus.common.result.PageResult;
import com.campus.common.result.Result;
import com.campus.pojo.vo.OrderVO;
import com.campus.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 管理端：订单管理
 */
@RestController
@RequestMapping("/admin/orders")
public class AdminOrderController {

    @Resource
    private OrderService orderService;

    /** 订单分页 */
    @GetMapping
    public Result<PageResult<OrderVO>> page(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(orderService.adminPageOrders(status, keyword, beginTime, endTime, page, pageSize));
    }

    /** 订单详情 */
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.adminGetDetail(id));
    }

    /** 关闭纠纷订单（商品回滚为在售） */
    @PutMapping("/{id}/close")
    public Result<Void> close(@PathVariable Long id, @RequestParam(required = false) String reason) {
        orderService.adminClose(id, reason);
        return Result.success();
    }
}
