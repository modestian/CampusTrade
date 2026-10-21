package com.campus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.constant.MessageConstant;
import com.campus.common.constant.RedisConstants;
import com.campus.common.context.UserContext;
import com.campus.common.exception.BusinessException;
import com.campus.common.result.PageResult;
import com.campus.mapper.CollectMapper;
import com.campus.mapper.FollowMapper;
import com.campus.mapper.ProductLikeMapper;
import com.campus.mapper.ProductMapper;
import com.campus.mapper.UserMapper;
import com.campus.pojo.entity.Collect;
import com.campus.pojo.entity.Follow;
import com.campus.pojo.entity.Product;
import com.campus.pojo.entity.ProductLike;
import com.campus.pojo.entity.User;
import com.campus.pojo.vo.ProductVO;
import com.campus.pojo.vo.UserVO;
import com.campus.service.SocialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 社交服务实现
 * 收藏/关注：Redis Set + MySQL 双写；点赞：Redis Set 实时统计；Feed：推模式 ZSet 滚动分页
 */
@Slf4j
@Service
public class SocialServiceImpl implements SocialService {

    @Resource
    private CollectMapper collectMapper;

    @Resource
    private ProductLikeMapper productLikeMapper;

    @Resource
    private FollowMapper followMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** Feed 时间线保留上限：写扩散时裁剪最旧记录，避免 ZSet 无限增长 */
    private static final long FEED_MAX_SIZE = 500;

    /* ==================== 收藏 ==================== */

    @Override
    @Transactional
    public void collect(Long productId) {
        Long userId = UserContext.getUserId();
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        Integer exists = collectMapper.selectCount(new LambdaQueryWrapper<Collect>()
                .eq(Collect::getUserId, userId)
                .eq(Collect::getProductId, productId));
        if (exists > 0) {
            return;
        }
        Collect collect = new Collect();
        collect.setUserId(userId);
        collect.setProductId(productId);
        collectMapper.insert(collect);
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .setSql("collect_count = collect_count + 1")
                .eq(Product::getId, productId));
        incrementHot(productId, 3);
        stringRedisTemplate.delete(RedisConstants.CACHE_PRODUCT_KEY + productId);
    }

    @Override
    @Transactional
    public void uncollect(Long productId) {
        Long userId = UserContext.getUserId();
        int deleted = collectMapper.delete(new LambdaQueryWrapper<Collect>()
                .eq(Collect::getUserId, userId)
                .eq(Collect::getProductId, productId));
        if (deleted > 0) {
            productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .setSql("collect_count = GREATEST(collect_count - 1, 0)")
                    .eq(Product::getId, productId));
            stringRedisTemplate.delete(RedisConstants.CACHE_PRODUCT_KEY + productId);
        }
    }

    @Override
    public PageResult<ProductVO> myCollects(int page, int size) {
        Page<Collect> mpPage = new Page<>(page, size);
        collectMapper.selectPage(mpPage, new LambdaQueryWrapper<Collect>()
                .eq(Collect::getUserId, UserContext.getUserId())
                .orderByDesc(Collect::getCreateTime));
        List<Long> productIds = mpPage.getRecords().stream()
                .map(Collect::getProductId).collect(Collectors.toList());
        List<ProductVO> products = new ArrayList<>();
        if (!productIds.isEmpty()) {
            products = productMapper.selectByIds(productIds);
        }
        return new PageResult<>(mpPage.getTotal(), mpPage.getPages(), products);
    }

    /* ==================== 点赞 ==================== */

    @Override
    @Transactional
    public void like(Long productId) {
        Long userId = UserContext.getUserId();
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        String key = RedisConstants.LIKE_PRODUCT_KEY + productId;
        // 原子 SADD：返回 0 说明已点赞（含并发重复请求），幂等返回
        Long added = stringRedisTemplate.opsForSet().add(key, userId.toString());
        if (added == null || added == 0) {
            return;
        }
        try {
            ProductLike like = new ProductLike();
            like.setUserId(userId);
            like.setProductId(productId);
            productLikeMapper.insert(like);
        } catch (Exception e) {
            // 唯一键冲突（如 Redis 点赞集合丢失后的重复点赞）：
            // 撤销本次 Redis 标记并按未点赞处理，避免计数虚增
            stringRedisTemplate.opsForSet().remove(key, userId.toString());
            log.warn("点赞失败（记录已存在）：userId={}, productId={}", userId, productId);
            return;
        }
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .setSql("like_count = like_count + 1")
                .eq(Product::getId, productId));
        incrementHot(productId, 2);
        stringRedisTemplate.delete(RedisConstants.CACHE_PRODUCT_KEY + productId);
    }

    @Override
    @Transactional
    public void unlike(Long productId) {
        Long userId = UserContext.getUserId();
        String key = RedisConstants.LIKE_PRODUCT_KEY + productId;
        // 数据库为权威：先删记录，成功后再清理 Redis 标记与计数
        int deleted = productLikeMapper.delete(new LambdaQueryWrapper<ProductLike>()
                .eq(ProductLike::getUserId, userId)
                .eq(ProductLike::getProductId, productId));
        // 无论 DB 是否有记录都清理 Redis 标记（自愈遗留的不一致标记）
        stringRedisTemplate.opsForSet().remove(key, userId.toString());
        if (deleted > 0) {
            productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .setSql("like_count = GREATEST(like_count - 1, 0)")
                    .eq(Product::getId, productId));
            stringRedisTemplate.delete(RedisConstants.CACHE_PRODUCT_KEY + productId);
        }
    }

    /* ==================== 关注 ==================== */

    @Override
    @Transactional
    public void follow(Long userId) {
        Long me = UserContext.getUserId();
        if (me.equals(userId)) {
            throw new BusinessException("不能关注自己");
        }
        User target = userMapper.selectById(userId);
        if (target == null) {
            throw new BusinessException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        Integer exists = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, me)
                .eq(Follow::getFollowUserId, userId));
        if (exists > 0) {
            return;
        }
        Follow follow = new Follow();
        follow.setUserId(me);
        follow.setFollowUserId(userId);
        followMapper.insert(follow);
        // Redis 双写
        stringRedisTemplate.opsForSet().add(RedisConstants.FOLLOW_KEY + me, userId.toString());
        stringRedisTemplate.opsForSet().add(RedisConstants.FANS_KEY + userId, me.toString());
    }

    @Override
    @Transactional
    public void unfollow(Long userId) {
        Long me = UserContext.getUserId();
        int deleted = followMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, me)
                .eq(Follow::getFollowUserId, userId));
        if (deleted > 0) {
            stringRedisTemplate.opsForSet().remove(RedisConstants.FOLLOW_KEY + me, userId.toString());
            stringRedisTemplate.opsForSet().remove(RedisConstants.FANS_KEY + userId, me.toString());
        }
    }

    @Override
    public PageResult<UserVO> myFollows(int page, int size) {
        Page<Follow> mpPage = new Page<>(page, size);
        followMapper.selectPage(mpPage, new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, UserContext.getUserId())
                .orderByDesc(Follow::getCreateTime));
        List<Long> userIds = mpPage.getRecords().stream()
                .map(Follow::getFollowUserId).collect(Collectors.toList());
        List<UserVO> users = new ArrayList<>();
        if (!userIds.isEmpty()) {
            users = userMapper.selectBatchIds(userIds).stream().map(u -> {
                UserVO vo = new UserVO();
                BeanUtils.copyProperties(u, vo);
                return vo;
            }).collect(Collectors.toList());
        }
        return new PageResult<>(mpPage.getTotal(), mpPage.getPages(), users);
    }

    @Override
    public List<UserVO> commonFollows(Long userId) {
        Long me = UserContext.getUserId();
        Set<String> intersection = stringRedisTemplate.opsForSet()
                .intersect(RedisConstants.FOLLOW_KEY + me, RedisConstants.FOLLOW_KEY + userId);
        if (intersection == null || intersection.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = intersection.stream().map(Long::valueOf).collect(Collectors.toList());
        return userMapper.selectBatchIds(ids).stream().map(u -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(u, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    /* ==================== Feed 流 ==================== */

    @Override
    public Map<String, Object> feed(Long lastTime, Integer offset) {
        Long userId = UserContext.getUserId();
        String key = RedisConstants.FEED_KEY + userId;
        long max = (lastTime == null || lastTime <= 0) ? System.currentTimeMillis() : lastTime;
        int skip = (offset == null || offset < 0) ? 0 : offset;

        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, skip, 2);
        Map<String, Object> result = new HashMap<>();
        if (tuples == null || tuples.isEmpty()) {
            result.put("products", Collections.emptyList());
            result.put("minTime", 0L);
            result.put("offset", 0);
            return result;
        }

        // 滚动分页：计算下一次的 minTime 与 offset
        long minTime = 0L;
        int nextOffset = 1;
        LinkedHashSet<Long> idSet = new LinkedHashSet<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() != null) {
                idSet.add(Long.valueOf(tuple.getValue()));
            }
            if (tuple.getScore() != null) {
                minTime = tuple.getScore().longValue();
            }
        }
        // 统计与最小时间相同的记录数（用于下次跳过重复 score）
        Long sameCount = stringRedisTemplate.opsForZSet().count(key, minTime, minTime);
        nextOffset = sameCount == null ? 1 : sameCount.intValue();

        List<ProductVO> products = productMapper.selectByIds(new ArrayList<>(idSet));
        // 过滤非在售商品，并保持 feed 顺序
        List<ProductVO> ordered = new ArrayList<>();
        for (Long id : idSet) {
            products.stream()
                    .filter(p -> p.getId().equals(id) && Integer.valueOf(1).equals(p.getStatus()))
                    .findFirst().ifPresent(ordered::add);
        }

        result.put("products", ordered);
        result.put("minTime", minTime);
        result.put("offset", nextOffset);
        return result;
    }

    @Override
    public void pushToFans(Long sellerId, Long productId) {
        // 以数据库为准查询粉丝（Redis Set 仅作加速，避免缓存丢失导致推送失败）
        List<Follow> follows = followMapper.selectList(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowUserId, sellerId));
        if (follows.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Follow follow : follows) {
            String feedKey = RedisConstants.FEED_KEY + follow.getUserId();
            stringRedisTemplate.opsForZSet().add(feedKey, productId.toString(), now);
            // 时间线裁剪：只保留最近 FEED_MAX_SIZE 条，避免写扩散下 ZSet 无限增长
            stringRedisTemplate.opsForZSet().removeRange(feedKey, 0, -(FEED_MAX_SIZE + 1));
        }
    }

    /* ==================== 状态判断 ==================== */

    @Override
    public boolean isCollected(Long userId, Long productId) {
        return collectMapper.selectCount(new LambdaQueryWrapper<Collect>()
                .eq(Collect::getUserId, userId)
                .eq(Collect::getProductId, productId)) > 0;
    }

    @Override
    public boolean isLiked(Long userId, Long productId) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet()
                .isMember(RedisConstants.LIKE_PRODUCT_KEY + productId, userId.toString()));
    }

    @Override
    public boolean isFollowed(Long userId, Long followUserId) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet()
                .isMember(RedisConstants.FOLLOW_KEY + userId, followUserId.toString()));
    }

    /* ==================== 私有方法 ==================== */

    /** 热度计分（浏览量 +1 / 点赞 +2 / 收藏 +3，ZSet 支持热门榜） */
    private void incrementHot(Long productId, double delta) {
        try {
            stringRedisTemplate.opsForZSet().incrementScore(
                    RedisConstants.RANK_HOT_PRODUCT_KEY, productId.toString(), delta);
        } catch (Exception e) {
            log.warn("热度计分失败：{}", e.getMessage());
        }
    }
}
