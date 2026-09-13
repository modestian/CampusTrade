package com.campus.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学生用户信息（脱敏）
 */
@Data
public class UserVO implements Serializable {

    private Long id;

    /** 学号 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像色块 */
    private String avatarColor;

    /** 手机号 */
    private String phone;

    /** 常用校区 */
    private String campus;

    /** 信用分 */
    private Integer credit;

    /** 0 正常 / 1 封禁 */
    private Integer status;

    private LocalDateTime createTime;
}
