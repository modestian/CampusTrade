package com.campus.controller.student;

import com.campus.common.result.PageResult;
import com.campus.common.result.Result;
import com.campus.pojo.dto.ProductDTO;
import com.campus.pojo.entity.Category;
import com.campus.pojo.vo.ProductDetailVO;
import com.campus.pojo.vo.ProductVO;
import com.campus.pojo.vo.ReviewVO;
import com.campus.pojo.vo.SellerHomeVO;
import com.campus.pojo.vo.UserVO;
import com.campus.service.CategoryService;
import com.campus.service.ProductService;
import com.campus.service.ReviewService;
import com.campus.service.SocialService;
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
 * 学生端：商品浏览（买家侧）与商品管理（卖家侧）
 */
@RestController
@RequestMapping("/student")
public class ProductController {

    @Resource
    private ProductService productService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private ReviewService reviewService;

    @Resource
    private SocialService socialService;

    /* ---------- 买家侧（公开浏览） ---------- */

    /** 分类列表 */
    @GetMapping("/categories")
    public Result<List<Category>> categories() {
        return Result.success(categoryService.listEnabled());
    }

    /** 商品分页 */
    @GetMapping("/products")
    public Result<PageResult<ProductVO>> pageProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(productService.pageProducts(categoryId, keyword, sort, page, pageSize));
    }

    /** 热门商品榜 */
    @GetMapping("/products/hot")
    public Result<List<ProductVO>> hotProducts() {
        return Result.success(productService.hotProducts());
    }

    /** 商品详情 */
    @GetMapping("/products/{id}")
    public Result<ProductDetailVO> productDetail(@PathVariable Long id) {
        return Result.success(productService.getDetail(id));
    }

    /** 商品评价分页 */
    @GetMapping("/products/{id}/reviews")
    public Result<PageResult<ReviewVO>> productReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(reviewService.productReviews(id, page, pageSize));
    }

    /** 卖家主页 */
    @GetMapping("/users/{id}/home")
    public Result<SellerHomeVO> sellerHome(@PathVariable Long id) {
        return Result.success(productService.sellerHome(id));
    }

    /** 与某用户的共同关注 */
    @GetMapping("/users/{id}/common-follows")
    public Result<List<UserVO>> commonFollows(@PathVariable Long id) {
        return Result.success(socialService.commonFollows(id));
    }

    /* ---------- 卖家侧 ---------- */

    /** 发布商品 */
    @PostMapping("/products")
    public Result<Void> publish(@RequestBody ProductDTO productDTO) {
        productService.publish(productDTO);
        return Result.success();
    }

    /** 我的商品分页 */
    @GetMapping("/my/products")
    public Result<PageResult<ProductVO>> myProducts(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(productService.myProducts(status, page, pageSize));
    }

    /** 编辑商品 */
    @PutMapping("/my/products/{id}")
    public Result<Void> edit(@PathVariable Long id, @RequestBody ProductDTO productDTO) {
        productService.edit(id, productDTO);
        return Result.success();
    }

    /** 上下架（1 上架 / 4 下架） */
    @PutMapping("/my/products/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        productService.updateStatus(id, status);
        return Result.success();
    }

    /** 删除商品 */
    @DeleteMapping("/my/products/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success();
    }
}
