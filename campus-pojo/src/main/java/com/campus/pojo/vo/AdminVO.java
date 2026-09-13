package com.campus.pojo.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员信息
 */
@Data
public class AdminVO implements Serializable {

    private Long id;

    private String username;

    private String name;

    private String avatarColor;

    private Integer status;
}
