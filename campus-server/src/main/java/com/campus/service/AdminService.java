package com.campus.service;

import com.campus.pojo.dto.LoginDTO;
import com.campus.pojo.vo.AdminLoginVO;
import com.campus.pojo.vo.AdminVO;
import com.campus.pojo.vo.WorkspaceVO;

/**
 * 管理员服务
 */
public interface AdminService {

    /** 管理员登录 */
    AdminLoginVO login(LoginDTO loginDTO);

    /** 登出 */
    void logout(String token);

    /** 当前管理员信息 */
    AdminVO getMe();

    /** 工作台数据概览 */
    WorkspaceVO workspace();
}
