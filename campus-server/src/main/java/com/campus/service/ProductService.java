package com.campus.service;

import com.campus.common.result.PageResult;
import com.campus.pojo.dto.AuditDTO;
import com.campus.pojo.dto.ProductDTO;
import com.campus.pojo.vo.ProductDetailVO;
import com.campus.pojo.vo.ProductVO;
import com.campus.pojo.vo.SellerHomeVO;

import java.util.List;

/**
 * 商品服务
 */
public interface ProductService {

    /* ---------- 买家侧 ---------- */

    /** 商品分页（在售商品，支持分类/关键词/排序） */
    PageResult<ProductVO> pageProducts(Long categoryId, String keyword, String sort, int page, int size);

    /** 商品详情（缓存 + 浏览量 + 个性化标记） */
    ProductDetailVO getDetail(Long id);

    /** 热门商品榜 Top10（ZSet） */
    List<ProductVO> hotProducts();

    /** 卖家主页 */
    SellerHomeVO sellerHome(Long sellerId);

    /* ---------- 卖家侧 ---------- */

    /** 发布商品（进入待审核） */
    void publish(ProductDTO productDTO);

    /** 我的商品分页 */
    PageResult<ProductVO> myProducts(Integer status, int page, int size);

    /** 编辑商品（驳回商品编辑后重新送审） */
    void edit(Long id, ProductDTO productDTO);

    /** 上/下架（在售 <-> 已下架） */
    void updateStatus(Long id, Integer status);

    /** 删除商品（仅无订单关联的可删除） */
    void delete(Long id);

    /* ---------- 管理端 ---------- */

    /** 商品分页（管理端，任意状态） */
    PageResult<ProductVO> adminPageProducts(Integer status, String keyword, Long categoryId, int page, int size);

    /** 商品详情（管理端，不做状态过滤） */
    ProductDetailVO adminDetail(Long id);

    /** 审核（通过 / 驳回，结果 WebSocket 通知卖家） */
    void audit(Long id, AuditDTO auditDTO);

    /** 违规下架 */
    void offline(Long id);

    /** 恢复上架 */
    void restore(Long id);
}
