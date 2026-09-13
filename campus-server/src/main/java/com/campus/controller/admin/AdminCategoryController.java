package com.campus.controller.admin;

import com.campus.common.result.Result;
import com.campus.pojo.entity.Category;
import com.campus.service.CategoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 管理端：分类管理
 */
@RestController
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    @Resource
    private CategoryService categoryService;

    /** 全部分类（含禁用） */
    @GetMapping
    public Result<List<Category>> list() {
        return Result.success(categoryService.listAll());
    }

    /** 新增分类 */
    @PostMapping
    public Result<Void> save(@RequestBody Category category) {
        categoryService.save(category);
        return Result.success();
    }

    /** 编辑分类 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        categoryService.update(category);
        return Result.success();
    }

    /** 启用 / 禁用 */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        categoryService.updateStatus(id, status);
        return Result.success();
    }

    /** 删除分类 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
