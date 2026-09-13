package com.campus.service;

import com.campus.common.result.PageResult;
import com.campus.pojo.dto.ReviewDTO;
import com.campus.pojo.vo.ReviewVO;

/**
 * 评价服务
 */
public interface ReviewService {

    /** 提交评价（订单已完成 + 一单一评） */
    void submit(ReviewDTO reviewDTO);

    /** 商品评价分页 */
    PageResult<ReviewVO> productReviews(Long productId, int page, int size);
}
