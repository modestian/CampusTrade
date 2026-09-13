package com.campus.controller.admin;

import com.campus.common.result.Result;
import com.campus.pojo.dto.LoginDTO;
import com.campus.pojo.vo.AdminLoginVO;
import com.campus.pojo.vo.AdminVO;
import com.campus.pojo.vo.WorkspaceVO;
import com.campus.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 管理端：认证与工作台
 */
@RestController
@RequestMapping("/admin")
public class AdminAuthController {

    @Resource
    private AdminService adminService;

    /** 管理员登录 */
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@RequestBody LoginDTO loginDTO) {
        return Result.success(adminService.login(loginDTO));
    }

    /** 登出 */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "admin-token", required = false) String token) {
        adminService.logout(token);
        return Result.success();
    }

    /** 当前管理员 */
    @GetMapping("/me")
    public Result<AdminVO> me() {
        return Result.success(adminService.getMe());
    }

    /** 工作台数据概览 */
    @GetMapping("/workspace")
    public Result<WorkspaceVO> workspace() {
        return Result.success(adminService.workspace());
    }
}
