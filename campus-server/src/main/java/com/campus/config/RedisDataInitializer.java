package com.campus.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.constant.RedisConstants;
import com.campus.common.enumeration.ProductStatus;
import com.campus.mapper.FollowMapper;
import com.campus.mapper.ProductLikeMapper;
import com.campus.mapper.ProductMapper;
import com.campus.pojo.entity.Follow;
import com.campus.pojo.entity.Product;
import com.campus.pojo.entity.ProductLike;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * Redis 数据预热（应用启动时从数据库重建社交类缓存）
 * 说明：社交数据以 MySQL 为权威源、Redis 为加速层；
 * 启动时全量预热，保证种子数据或缓存丢失后功能依然可用（SADD/ZADD 幂等）。
 */
@Slf4j
@Component
public class RedisDataInitializer implements ApplicationRunner {

    @Resource
    private FollowMapper followMapper;

    @Resource
    private ProductLikeMapper productLikeMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        // 1. 关注 / 粉丝集合
        List<Follow> follows = followMapper.selectList(null);
        for (Follow follow : follows) {
            stringRedisTemplate.opsForSet().add(
                    RedisConstants.FOLLOW_KEY + follow.getUserId(), follow.getFollowUserId().toString());
            stringRedisTemplate.opsForSet().add(
                    RedisConstants.FANS_KEY + follow.getFollowUserId(), follow.getUserId().toString());
        }

        // 2. 商品点赞集合
        List<ProductLike> likes = productLikeMapper.selectList(null);
        for (ProductLike like : likes) {
            stringRedisTemplate.opsForSet().add(
                    RedisConstants.LIKE_PRODUCT_KEY + like.getProductId(), like.getUserId().toString());
        }

        // 3. 热门榜初始化（仅当排行榜为空时，以浏览量作为初始热度）
        Long rankSize = stringRedisTemplate.opsForZSet().size(RedisConstants.RANK_HOT_PRODUCT_KEY);
        if (rankSize == null || rankSize == 0) {
            List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                    .eq(Product::getStatus, ProductStatus.ON_SALE.getValue()));
            for (Product product : products) {
                if (product.getViewCount() != null && product.getViewCount() > 0) {
                    stringRedisTemplate.opsForZSet().add(
                            RedisConstants.RANK_HOT_PRODUCT_KEY, product.getId().toString(), product.getViewCount());
                }
            }
        }

        log.info("Redis 数据预热完成：关注 {} 条、点赞 {} 条", follows.size(), likes.size());
    }
}
