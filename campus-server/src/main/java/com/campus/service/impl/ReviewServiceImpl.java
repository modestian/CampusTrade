package com.campus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.constant.MessageConstant;
import com.campus.common.context.UserContext;
import com.campus.common.enumeration.OrderStatus;
import com.campus.common.exception.BusinessException;
import com.campus.common.exception.NoAuthException;
import com.campus.common.result.PageResult;
import com.campus.mapper.OrderMapper;
import com.campus.mapper.ProductMapper;
import com.campus.mapper.ReviewMapper;
import com.campus.mapper.UserMapper;
import com.campus.pojo.dto.ReviewDTO;
import com.campus.pojo.entity.Product;
import com.campus.pojo.entity.ProductOrder;
import com.campus.pojo.entity.Review;
import com.campus.pojo.entity.User;
import com.campus.pojo.vo.ReviewVO;
import com.campus.service.ReviewService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评价服务实现（一单一评）
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    @Resource
    private ReviewMapper reviewMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public void submit(ReviewDTO reviewDTO) {
        if (reviewDTO.getScore() == null || reviewDTO.getScore() < 1 || reviewDTO.getScore() > 5) {
            throw new BusinessException("评分需为 1~5 星");
        }
        ProductOrder order = orderMapper.selectById(reviewDTO.getOrderId());
        if (order == null) {
            throw new BusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!order.getBuyerId().equals(UserContext.getUserId())) {
            throw new NoAuthException(MessageConstant.ORDER_NO_AUTH);
        }
        if (!OrderStatus.COMPLETED.getValue().equals(order.getStatus())) {
            throw new BusinessException(MessageConstant.REVIEW_ONLY_COMPLETED);
        }
        Integer exists = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, order.getId()));
        if (exists > 0) {
            throw new BusinessException(MessageConstant.REVIEW_EXISTS);
        }
        Review review = new Review();
        review.setOrderId(order.getId());
        review.setProductId(order.getProductId());
        review.setReviewerId(order.getBuyerId());
        review.setSellerId(order.getSellerId());
        review.setScore(reviewDTO.getScore());
        review.setContent(reviewDTO.getContent());
        review.setIsAnonymous(reviewDTO.getIsAnonymous() == null ? 0 : reviewDTO.getIsAnonymous());
        reviewMapper.insert(review);
    }

    @Override
    public PageResult<ReviewVO> productReviews(Long productId, int page, int size) {
        Page<Review> mpPage = new Page<>(page, size);
        reviewMapper.selectPage(mpPage, new LambdaQueryWrapper<Review>()
                .eq(Review::getProductId, productId)
                .orderByDesc(Review::getCreateTime));
        List<ReviewVO> records = mpPage.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(mpPage.getTotal(), mpPage.getPages(), records);
    }

    private ReviewVO toVO(Review review) {
        ReviewVO vo = new ReviewVO();
        BeanUtils.copyProperties(review, vo);
        if (review.getIsAnonymous() != null && review.getIsAnonymous() == 1) {
            vo.setReviewerNickname("匿名同学");
            vo.setReviewerAvatarColor("#DFE6E9");
        } else {
            User reviewer = userMapper.selectById(review.getReviewerId());
            if (reviewer != null) {
                vo.setReviewerNickname(reviewer.getNickname());
                vo.setReviewerAvatarColor(reviewer.getAvatarColor());
            }
        }
        Product product = productMapper.selectById(review.getProductId());
        if (product != null) {
            vo.setProductTitle(product.getTitle());
        }
        return vo;
    }
}
