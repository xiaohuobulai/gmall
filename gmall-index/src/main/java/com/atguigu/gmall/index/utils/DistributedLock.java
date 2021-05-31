package com.atguigu.gmall.index.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class DistributedLock {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Timer timer;

    public Boolean tryLock(String lockName, String uuid, Integer expire) {
        String script = "if (redis.call('exists', KEYS[1]) == 0 or redis.call('hexists', KEYS[1], ARGV[1]) == 1) then " +
                "   redis.call('hincrby', KEYS[1], ARGV[1], 1) " +
                "   redis.call('expire', KEYS[1], ARGV[2]) " +
                "   return 1 " +
                "else " +
                "   return 0 " +
                "end";
        if (!this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid, expire.toString())) {
            try {
                Thread.sleep(100);
                tryLock(lockName, uuid, expire);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 开启定时任务子线程自动续期
        renewExpire(expire, lockName, uuid);
        return true;

    }

    public void unLock(String lockName, Integer uuid) {
        String script = "if (redis.call('hexists', KEYS[1], ARGV[1]) == 0) then " +
                "    return nil " +
                "elseif (redis.call('hincrby', KEYS[1], ARGV[1], -1) == 0) then " +
                "    redis.call('del', KEYS[1]) " +
                "else " +
                "   return 0 " +
                "end";
        Long execute = this.redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(lockName), uuid);
        if (execute == null) {
            log.error("要解的锁不存在，或者在尝试解别人的锁，锁的名称是:{},uuid是:{}" + lockName, uuid);
        } else if (execute == 1) {
            // 解锁成功，取消定时任务
            timer.cancel();
        }
    }

    private void renewExpire(Integer expire, String lockName, String uuid) {
        String script = "if(redis.call('hexists',KEYS[1],ARGV[1]) == 1) then " +
                            "return redis.call('expire',KEYS[1],ARGV[2]) " +
                        "else " +
                            "return 0 " +
                        "end";
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid, expire.toString());
            }
        }, expire * 1000 / 3, expire * 1000 / 3);
    }

//    public static void main(String[] args) {// 延迟五秒执行，每十秒执行一次
//        System.out.println("定时任务初始化时间：" + System.currentTimeMillis());
//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                System.out.println("这是jdk的定时器任务：" + System.currentTimeMillis());
//            }
//        }, 5000, 10000);
//    }

}
