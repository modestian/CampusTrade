package com.campus.service;

import com.campus.pojo.entity.Category;

import java.util.List;

/**
 * 商品分类服务
 */
public interface CategoryService {

    /** 学生端：启用分类列表（走缓存） */
    List<Category> listEnabled();

    /** 管理端：全部分类 */
    List<Category> listAll();

    /** 管理端：新增 */
    void save(Category category);

    /** 管理端：编辑 */
    void update(Category category);

    /** 管理端：启用 / 禁用 */
    void updateStatus(Long id, Integer status);

    /** 管理端：删除（分类下有商品时禁止） */
    void delete(Long id);
}
