package com.campus.service;

import com.campus.common.result.PageResult;
import com.campus.pojo.dto.LoginDTO;
import com.campus.pojo.dto.PasswordEditDTO;
import com.campus.pojo.dto.RegisterDTO;
import com.campus.pojo.dto.UserUpdateDTO;
import com.campus.pojo.vo.UserLoginVO;
import com.campus.pojo.vo.UserVO;

/**
 * 学生用户服务
 */
public interface UserService {

    /** 注册（注册成功直接登录） */
    UserLoginVO register(RegisterDTO registerDTO);

    /** 登录 */
    UserLoginVO login(LoginDTO loginDTO);

    /** 登出 */
    void logout(String token);

    /** 当前登录用户信息 */
    UserVO getMe();

    /** 修改个人资料 */
    void updateProfile(UserUpdateDTO userUpdateDTO);

    /** 修改密码 */
    void editPassword(PasswordEditDTO passwordEditDTO);

    /* ---------- 管理端 ---------- */

    /** 学生分页（管理端） */
    PageResult<UserVO> pageUsers(String keyword, Integer status, int page, int size);

    /** 学生详情（管理端） */
    UserVO getUserDetail(Long id);

    /** 封禁 / 解封（管理端） */
    void updateStatus(Long id, Integer status);
}
