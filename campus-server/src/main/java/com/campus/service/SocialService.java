package com.campus.service;

import com.campus.common.result.PageResult;
import com.campus.pojo.vo.ProductVO;
import com.campus.pojo.vo.UserVO;

import java.util.List;
import java.util.Map;

/**
 * 社交服务：收藏 / 点赞 / 关注 / Feed 流
 */
public interface SocialService {

    /* ---------- 收藏 ---------- */

    void collect(Long productId);

    void uncollect(Long productId);

    PageResult<ProductVO> myCollects(int page, int size);

    /* ---------- 点赞 ---------- */

    void like(Long productId);

    void unlike(Long productId);

    /* ---------- 关注 ---------- */

    void follow(Long userId);

    void unfollow(Long userId);

    PageResult<UserVO> myFollows(int page, int size);

    List<UserVO> commonFollows(Long userId);

    /* ---------- Feed 流 ---------- */

    /** 关注动态流（推模式，滚动分页：lastTime + offset），返回 {products, minTime, offset} */
    Map<String, Object> feed(Long lastTime, Integer offset);

    /* ---------- 内部协作 ---------- */

    /** 商品审核通过后推送给粉丝（供 ProductService 调用） */
    void pushToFans(Long sellerId, Long productId);

    /** 判断是否已收藏 */
    boolean isCollected(Long userId, Long productId);

    /** 判断是否已点赞 */
    boolean isLiked(Long userId, Long productId);

    /** 判断是否已关注 */
    boolean isFollowed(Long userId, Long followUserId);
}
