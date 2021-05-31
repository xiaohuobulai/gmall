package com.atguigu.gmall.index;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class GmallIndexApplicationTests {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    void contextLoads() {
        this.redisTemplate.opsForValue().set("test", "张三");
        System.out.println(redisTemplate.opsForValue().get("test"));
    }

}
