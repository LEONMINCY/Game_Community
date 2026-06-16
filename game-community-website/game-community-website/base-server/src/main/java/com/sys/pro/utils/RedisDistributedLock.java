package com.sys.pro.utils;

import com.sys.pro.web.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redisson based distributed lock.
 * 不指定 leaseTime 时 Redisson 会启用 watchdog 自动续期，避免订单支付、购物车结算等较长流程锁提前释放。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDistributedLock {

    private static final String LOCK_PREFIX = "gc:lock:";

    private final RedissonClient redissonClient;

    /**
     * 完成Redis 缓存中的 tryLock 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @param ttl ttl 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存处理后的文本结果。
     */
    public String tryLock(String key, Duration ttl) {
        String lockKey = normalizeKey(key);
        try {
            RLock lock = redissonClient.getLock(lockKey);
            // 保留 ttl 参数兼容旧调用；这里不传 leaseTime，确保 Redisson watchdog 能自动续期。
            return lock.tryLock() ? lockKey : null;
        } catch (Exception e) {
            log.error("Redis lock acquire failed, key={}", lockKey, e);
            /**
             * 完成Redis 缓存中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return Redis 缓存在该步骤产出的业务结果。
             */
            throw new ServiceException(600, "系统繁忙，请稍后再试");
        }
    }

    /**
     * 完成Redis 缓存中的 unlock 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @param token 锁令牌，用于校验分布式锁是否由当前线程持有。
     */
    public void unlock(String key, String token) {
        if (token == null) {
            return;
        }
        String lockKey = normalizeKey(key);
        try {
            if (!lockKey.equals(token)) {
                log.warn("Redis lock token mismatch, key={}", lockKey);
                return;
            }
            RLock lock = redissonClient.getLock(lockKey);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            } else {
                log.warn("Redis lock is not held by current thread, key={}", lockKey);
            }
        } catch (Exception e) {
            log.warn("Redis lock release failed, key={}", lockKey, e);
        }
    }

    /**
     * 完成Redis 缓存中的 normalizeKey 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存处理后的文本结果。
     */
    private String normalizeKey(String key) {
        return key.startsWith(LOCK_PREFIX) ? key : LOCK_PREFIX + key;
    }
}
