package com.campus.controller.admin;

import com.campus.common.result.PageResult;
import com.campus.common.result.Result;
import com.campus.pojo.dto.AuditDTO;
import com.campus.pojo.vo.ProductDetailVO;
import com.campus.pojo.vo.ProductVO;
import com.campus.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 管理端：商品审核与管理
 */
@RestController
@RequestMapping("/admin/products")
public class AdminProductController {

    @Resource
    private ProductService productService;

    /** 商品分页（任意状态） */
    @GetMapping
    public Result<PageResult<ProductVO>> page(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(productService.adminPageProducts(status, keyword, categoryId, page, pageSize));
    }

    /** 商品详情 */
    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.success(productService.adminDetail(id));
    }

    /** 审核（通过 / 驳回） */
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody AuditDTO auditDTO) {
        productService.audit(id, auditDTO);
        return Result.success();
    }

    /** 违规下架 */
    @PutMapping("/{id}/offline")
    public Result<Void> offline(@PathVariable Long id) {
        productService.offline(id);
        return Result.success();
    }

    /** 恢复上架 */
    @PutMapping("/{id}/restore")
    public Result<Void> restore(@PathVariable Long id) {
        productService.restore(id);
        return Result.success();
    }
}
