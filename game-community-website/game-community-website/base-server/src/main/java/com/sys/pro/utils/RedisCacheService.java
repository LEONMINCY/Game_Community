package com.sys.pro.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Cache-Aside helper with delayed double delete.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisDistributedLock redisDistributedLock;
    private final ScheduledExecutorService delayedDeleteExecutor = Executors.newScheduledThreadPool(2, r -> {
        Thread thread = new Thread(r, "redis-cache-delayed-delete");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * 读取Redis 缓存的 OrLoad 数据，供页面展示或后续业务判断。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @param ttlSeconds ttlSeconds 字段，来源于当前接口入参或内部调用上下文。
     * @param loader loader 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存在该步骤产出的业务结果。
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, long ttlSeconds, Supplier<T> loader) {
        T cached = readCache(key);
        if (cached != null) {
            return cached;
        }

        T value = loader.get();
        writeCache(key, value, ttlSeconds);
        return value;
    }

    /**
     * 读取Redis 缓存的 OrLoadWithLock 数据，供页面展示或后续业务判断。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @param lockKey 分布式锁键，用来隔离同一业务资源的并发操作。
     * @param lockTtl lockTtl 字段，来源于当前接口入参或内部调用上下文。
     * @param waitMillis waitMillis 字段，来源于当前接口入参或内部调用上下文。
     * @param ttlSeconds ttlSeconds 字段，来源于当前接口入参或内部调用上下文。
     * @param loader loader 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存在该步骤产出的业务结果。
     */
    public <T> T getOrLoadWithLock(String key,
                                   String lockKey,
                                   Duration lockTtl,
                                   long waitMillis,
                                   long ttlSeconds,
                                   Supplier<T> loader) {
        T cached = readCache(key);
        if (cached != null) {
            return cached;
        }

        // Use a short Redis lock to prevent cache breakdown on hot aggregate queries.
        String token;
        try {
            token = redisDistributedLock.tryLock(lockKey, lockTtl);
        } catch (Exception e) {
            log.warn("Acquire redis cache lock failed, fallback to loader, key={}, lockKey={}", key, lockKey, e);
            return loader.get();
        }
        if (token != null) {
            try {
                cached = readCache(key);
                if (cached != null) {
                    return cached;
                }
                T value = loader.get();
                writeCache(key, value, ttlSeconds);
                return value;
            } finally {
                redisDistributedLock.unlock(lockKey, token);
            }
        }

        T warmed = waitForCache(key, waitMillis);
        if (warmed != null) {
            return warmed;
        }
        return loader.get();
    }

    /**
     * 读取Redis 缓存的  数据，供页面展示或后续业务判断。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存在该步骤产出的业务结果。
     */
    public <T> T get(String key) {
        return readCache(key);
    }

    /**
     * 完成Redis 缓存中的 readCache 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存在该步骤产出的业务结果。
     */
    @SuppressWarnings("unchecked")
    private <T> T readCache(String key) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return (T) cached;
            }
        } catch (Exception e) {
            log.warn("Read redis cache failed, key={}, removed bad cache, reason={}", key, e.getMessage());
            deleteKeys(key);
        }
        return null;
    }

    /**
     * 完成Redis 缓存中的 writeCache 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @param ttlSeconds ttlSeconds 字段，来源于当前接口入参或内部调用上下文。
     */
    private <T> void writeCache(String key, T value, long ttlSeconds) {
        if (value != null) {
            try {
                long ttlWithJitter = ttlSeconds + ThreadLocalRandom.current().nextLong(1, 31);
                redisTemplate.opsForValue().set(key, toCacheSafeValue(value), ttlWithJitter, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Write redis cache failed, key={}", key, e);
            }
        }
    }

    /**
     * 转换Redis 缓存字段格式，便于后续计算或接口返回。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存在该步骤产出的业务结果。
     */
    private Object toCacheSafeValue(Object value) {
        if (value instanceof Map) {
            Map<?, ?> source = (Map<?, ?>) value;
            Map<String, Object> target = new LinkedHashMap<>(source.size());
            source.forEach((key, item) -> target.put(String.valueOf(key), toCacheSafeValue(item)));
            return target;
        }
        if (value instanceof Collection) {
            Collection<?> source = (Collection<?>) value;
            ArrayList<Object> target = new ArrayList<>(source.size());
            source.forEach(item -> target.add(toCacheSafeValue(item)));
            return target;
        }
        if (value != null && value.getClass().isArray()) {
            int length = Array.getLength(value);
            ArrayList<Object> target = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                target.add(toCacheSafeValue(Array.get(value, index)));
            }
            return target;
        }
        return value;
    }

    /**
     * 完成Redis 缓存中的 waitForCache 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @param waitMillis waitMillis 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存在该步骤产出的业务结果。
     */
    private <T> T waitForCache(String key, long waitMillis) {
        long deadline = System.currentTimeMillis() + Math.max(waitMillis, 0);
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            T cached = readCache(key);
            if (cached != null) {
                return cached;
            }
        }
        return null;
    }

    /**
     * 完成Redis 缓存中的 delayedDoubleDelete 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param keys keys 字段，来源于当前接口入参或内部调用上下文。
     */
    public void delayedDoubleDelete(String... keys) {
        delayedDeleteExecutor.execute(() -> deleteKeys(keys));
        delayedDeleteExecutor.schedule(() -> deleteKeys(keys), 800, TimeUnit.MILLISECONDS);
    }

    /**
     * 完成Redis 缓存中的 delayedDoubleDeleteByPattern 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param patterns patterns 字段，来源于当前接口入参或内部调用上下文。
     */
    public void delayedDoubleDeleteByPattern(String... patterns) {
        delayedDeleteExecutor.execute(() -> deletePatterns(patterns));
        delayedDeleteExecutor.schedule(() -> deletePatterns(patterns), 800, TimeUnit.MILLISECONDS);
    }

    /**
     * 关闭操作日志线程池，保证应用停止时不遗留后台线程。
     */
    @PreDestroy
    public void shutdown() {
        delayedDeleteExecutor.shutdown();
    }

    /**
     * 完成Redis 缓存中的 deleteKeys 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param keys keys 字段，来源于当前接口入参或内部调用上下文。
     */
    private void deleteKeys(String... keys) {
        Arrays.stream(keys)
                .filter(key -> key != null && !key.trim().isEmpty())
                .forEach(key -> {
                    try {
                        redisTemplate.delete(key);
                    } catch (Exception e) {
                        log.warn("Delete redis cache failed, key={}", key, e);
                    }
                });
    }

    /**
     * 完成Redis 缓存中的 deletePatterns 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param patterns patterns 字段，来源于当前接口入参或内部调用上下文。
     */
    private void deletePatterns(String... patterns) {
        Arrays.stream(patterns)
                .filter(pattern -> pattern != null && !pattern.trim().isEmpty())
                .forEach(pattern -> {
                    try {
                        Set<String> keys = scanKeys(pattern);
                        if (keys != null && !keys.isEmpty()) {
                            redisTemplate.delete(keys);
                        }
                    } catch (Exception e) {
                        log.warn("Delete redis cache by pattern failed, pattern={}", pattern, e);
                    }
                });
    }

    /**
     * 完成Redis 缓存中的 scanKeys 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param pattern pattern 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存在该步骤产出的业务结果。
     */
    private Set<String> scanKeys(String pattern) {
        return redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keys = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(500).build();
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return keys;
        });
    }
}
