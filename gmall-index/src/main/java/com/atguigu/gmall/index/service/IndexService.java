package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    public static final String KEY_PREFIX = "index:category:";

    public List<CategoryEntity> queryLevel1Categories() {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategories(0L);
        return listResponseVo.getData();
    }

    public List<CategoryEntity> queryLevel2CategoriesWithSubsByPid(Long pid) {
        // 1.查询缓存，如果缓存中有则直接返回
        String categoriesCache = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(categoriesCache)) {
            List<CategoryEntity> categoryEntities = JSON.parseArray(categoriesCache, CategoryEntity.class);
            return categoryEntities;
        }

        // 如果缓存中没有则去查询数据库，并放入缓存
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryLevel2CatesWithSubsPid(pid);
        List<CategoryEntity> data = listResponseVo.getData();
        if (CollectionUtils.isEmpty(data)) {
            this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(data), 5, TimeUnit.MINUTES);
        } else {
            this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(data), 30 + new Random(10).nextInt(), TimeUnit.DAYS);
        }
        return data;
    }


}
