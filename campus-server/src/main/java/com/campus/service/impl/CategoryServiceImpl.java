package com.campus.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.constant.MessageConstant;
import com.campus.common.constant.RedisConstants;
import com.campus.common.exception.BusinessException;
import com.campus.mapper.CategoryMapper;
import com.campus.mapper.ProductMapper;
import com.campus.pojo.entity.Category;
import com.campus.pojo.entity.Product;
import com.campus.service.CategoryService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商品分类服务实现（读多写少 -> CacheClient 式缓存）
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<Category> listEnabled() {
        String key = RedisConstants.CACHE_CATEGORY_LIST_KEY;
        String cacheJson = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(cacheJson)) {
            return JSON.parseArray(cacheJson, Category.class);
        }
        List<Category> list = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort));
        stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(list),
                RedisConstants.CACHE_CATEGORY_TTL, TimeUnit.MINUTES);
        return list;
    }

    @Override
    public List<Category> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort));
    }

    @Override
    public void save(Category category) {
        if (StrUtil.isBlank(category.getName())) {
            throw new BusinessException("分类名称不能为空");
        }
        Integer exists = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getName, category.getName()));
        if (exists > 0) {
            throw new BusinessException("分类已存在");
        }
        if (category.getSort() == null) {
            category.setSort(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        categoryMapper.insert(category);
        clearCache();
    }

    @Override
    public void update(Category category) {
        categoryMapper.updateById(category);
        clearCache();
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Category update = new Category();
        update.setId(id);
        update.setStatus(status);
        categoryMapper.updateById(update);
        clearCache();
    }

    @Override
    public void delete(Long id) {
        Integer productCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, id));
        if (productCount > 0) {
            throw new BusinessException(MessageConstant.CATEGORY_HAS_PRODUCTS);
        }
        categoryMapper.deleteById(id);
        clearCache();
    }

    private void clearCache() {
        stringRedisTemplate.delete(RedisConstants.CACHE_CATEGORY_LIST_KEY);
    }
}
