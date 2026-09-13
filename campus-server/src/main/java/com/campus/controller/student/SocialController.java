package com.campus.controller.student;

import com.campus.common.result.PageResult;
import com.campus.common.result.Result;
import com.campus.pojo.vo.ProductVO;
import com.campus.pojo.vo.UserVO;
import com.campus.service.SocialService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 学生端：社交互动（收藏 / 点赞 / 关注 / Feed 流）
 */
@RestController
@RequestMapping("/student")
public class SocialController {

    @Resource
    private SocialService socialService;

    /* ---------- 收藏 ---------- */

    @PostMapping("/collects/{productId}")
    public Result<Void> collect(@PathVariable Long productId) {
        socialService.collect(productId);
        return Result.success();
    }

    @DeleteMapping("/collects/{productId}")
    public Result<Void> uncollect(@PathVariable Long productId) {
        socialService.uncollect(productId);
        return Result.success();
    }

    @GetMapping("/my/collects")
    public Result<PageResult<ProductVO>> myCollects(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(socialService.myCollects(page, pageSize));
    }

    /* ---------- 点赞 ---------- */

    @PostMapping("/likes/{productId}")
    public Result<Void> like(@PathVariable Long productId) {
        socialService.like(productId);
        return Result.success();
    }

    @DeleteMapping("/likes/{productId}")
    public Result<Void> unlike(@PathVariable Long productId) {
        socialService.unlike(productId);
        return Result.success();
    }

    /* ---------- 关注 ---------- */

    @PostMapping("/follows/{userId}")
    public Result<Void> follow(@PathVariable Long userId) {
        socialService.follow(userId);
        return Result.success();
    }

    @DeleteMapping("/follows/{userId}")
    public Result<Void> unfollow(@PathVariable Long userId) {
        socialService.unfollow(userId);
        return Result.success();
    }

    @GetMapping("/my/follows")
    public Result<PageResult<UserVO>> myFollows(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(socialService.myFollows(page, pageSize));
    }

    /* ---------- Feed 流 ---------- */

    /** 关注动态流（滚动分页：lastTime + offset） */
    @GetMapping("/feed")
    public Result<Map<String, Object>> feed(
            @RequestParam(required = false) Long lastTime,
            @RequestParam(required = false) Integer offset) {
        return Result.success(socialService.feed(lastTime, offset));
    }
}
