package com.campus.controller.admin;

import com.campus.common.result.PageResult;
import com.campus.common.result.Result;
import com.campus.pojo.vo.UserVO;
import com.campus.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 管理端：学生管理
 */
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    @Resource
    private UserService userService;

    /** 学生分页 */
    @GetMapping
    public Result<PageResult<UserVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(userService.pageUsers(keyword, status, page, pageSize));
    }

    /** 学生详情 */
    @GetMapping("/{id}")
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.success(userService.getUserDetail(id));
    }

    /** 封禁 / 解封（1 封禁 / 0 正常） */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.success();
    }
}
