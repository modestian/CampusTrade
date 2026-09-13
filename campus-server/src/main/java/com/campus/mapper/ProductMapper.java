package com.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.pojo.entity.Product;
import com.campus.pojo.vo.ProductDetailVO;
import com.campus.pojo.vo.ProductVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品 Mapper（含联表查询）
 */
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 商品分页查询（联查分类名、卖家信息）
     * 支持：买家浏览（status=1）、我的商品（sellerId）、管理端（status 任意 + keyword）
     */
    IPage<ProductVO> pageProducts(IPage<ProductVO> page,
                                  @Param("categoryId") Long categoryId,
                                  @Param("keyword") String keyword,
                                  @Param("sellerId") Long sellerId,
                                  @Param("status") Integer status,
                                  @Param("sort") String sort);

    /** 商品详情（联查分类名、卖家信息） */
    ProductDetailVO selectProductDetail(@Param("id") Long id);

    /** 按 ID 批量查询商品（Feed 流、热门榜使用） */
    List<ProductVO> selectByIds(@Param("ids") List<Long> ids);

    /** 卖家主页：在售商品 */
    List<ProductVO> selectSellerOnSaleProducts(@Param("sellerId") Long sellerId, @Param("limit") int limit);

    /**
     * 商品状态 CAS（防超卖核心）：在售(1) -> 交易中(2)
     * @return 影响行数，0 表示已被买走
     */
    int casOnSaleToTrading(@Param("id") Long id);

    /**
     * 商品状态回滚：交易中(2) -> 在售(1)
     */
    int casTradingToOnSale(@Param("id") Long id);

    /**
     * 商品完成交易：交易中(2) -> 已售出(3)
     */
    int casTradingToSold(@Param("id") Long id);
}
