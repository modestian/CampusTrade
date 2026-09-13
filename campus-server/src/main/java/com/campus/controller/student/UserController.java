package com.campus.controller.student;

import com.campus.common.result.Result;
import com.campus.pojo.dto.LoginDTO;
import com.campus.pojo.dto.PasswordEditDTO;
import com.campus.pojo.dto.RegisterDTO;
import com.campus.pojo.dto.UserUpdateDTO;
import com.campus.pojo.vo.UserLoginVO;
import com.campus.pojo.vo.UserVO;
import com.campus.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 学生端：认证与个人中心
 */
@RestController
@RequestMapping("/student")
public class UserController {

    @Resource
    private UserService userService;

    /** 注册（注册成功直接登录） */
    @PostMapping("/register")
    public Result<UserLoginVO> register(@RequestBody RegisterDTO registerDTO) {
        return Result.success(userService.register(registerDTO));
    }

    /** 登录 */
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody LoginDTO loginDTO) {
        return Result.success(userService.login(loginDTO));
    }

    /** 登出 */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "token", required = false) String token) {
        userService.logout(token);
        return Result.success();
    }

    /** 当前用户信息 */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(userService.getMe());
    }

    /** 修改资料 */
    @PutMapping("/me")
    public Result<Void> updateProfile(@RequestBody UserUpdateDTO userUpdateDTO) {
        userService.updateProfile(userUpdateDTO);
        return Result.success();
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Void> editPassword(@RequestBody PasswordEditDTO passwordEditDTO) {
        userService.editPassword(passwordEditDTO);
        return Result.success();
    }
}
