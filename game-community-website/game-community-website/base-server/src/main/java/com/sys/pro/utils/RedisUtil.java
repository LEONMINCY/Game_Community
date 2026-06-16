package com.sys.pro.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Redis工具类
 */
@Component
@RequiredArgsConstructor
public class RedisUtil {



    private final RedisTemplate<String, Object> redisTemplate;



    /**
     * 完成Redis 缓存中的 set 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     */
    public void set(String key, Object value) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(key, value);
    }

    /**
     * 完成Redis 缓存中的 set 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @param timeout timeout 字段，来源于当前接口入参或内部调用上下文。
     */
    public void set(String key, Object value, long timeout) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(key, value, timeout);
    }

    /**
     * 完成Redis 缓存中的 exists 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示Redis 缓存当前状态满足业务判断。
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 读取Redis 缓存的  数据，供页面展示或后续业务判断。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存在该步骤产出的业务结果。
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 按主键删除Redis 缓存记录，并让业务层同步处理关联状态。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 完成Redis 缓存中的 deleteBatch 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param keys keys 字段，来源于当前接口入参或内部调用上下文。
     */
    public void deleteBatch(List<String> keys) {
        redisTemplate.delete(keys);
    }

    /**
     * 完成Redis 缓存中的 hGet 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @param field field 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存在该步骤产出的业务结果。
     */
    public Object hGet(String key, String field) {
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();
        return ops.get(key, field);
    }

    /**
     * 完成Redis 缓存中的 hGetAll 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存聚合数据，键名与前端展示字段保持一致。
     */
    public Map<String, Object> hGetAll(String key) {
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();
        return ops.entries(key);
    }

    /**
     * 完成Redis 缓存中的 hSet 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @param field field 字段，来源于当前接口入参或内部调用上下文。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     */
    public void hSet(String key, String field, Object value) {
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();
        ops.put(key, field, value);
    }

    /**
     * 完成Redis 缓存中的 hSet 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @param field field 字段，来源于当前接口入参或内部调用上下文。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @param timeout timeout 字段，来源于当前接口入参或内部调用上下文。
     */
    public void hSet(String key, String field, Object value, long timeout) {
        hSet(key, field, value);
        redisTemplate.expire(key, timeout, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * 完成Redis 缓存中的 hDel 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @param fields fields 字段，来源于当前接口入参或内部调用上下文。
     */
    public void hDel(String key, Object... fields) {
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        ops.delete(key, fields);
    }

    /**
     * 完成Redis 缓存中的 incr 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存统计值或主键结果。
     */
    public Long incr(String key) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        return ops.increment(key);
    }

    /**
     * 完成Redis 缓存中的 decr 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存统计值或主键结果。
     */
    public Long decr(String key) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        return ops.decrement(key);
    }

    /**
     * 完成Redis 缓存中的 lPush 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存统计值或主键结果。
     */
    public Long lPush(String key, Object value) {
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        return ops.leftPush(key, value);
    }

    /**
     * 完成Redis 缓存中的 rPush 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存统计值或主键结果。
     */
    public Long rPush(String key, Object value) {
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        return ops.rightPush(key, value);
    }

    /**
     * 完成Redis 缓存中的 lRange 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @param start start 字段，来源于当前接口入参或内部调用上下文。
     * @param end end 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存列表数据。
     */
    public List<Object> lRange(String key, long start, long end) {
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        return ops.range(key, start, end);
    }

    /**
     * 完成Redis 缓存中的 lPop 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存在该步骤产出的业务结果。
     */
    public Object lPop(String key) {
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        return ops.leftPop(key);
    }

    /**
     * 完成Redis 缓存中的 rPop 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param key key 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存在该步骤产出的业务结果。
     */
    public Object rPop(String key) {
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        return ops.rightPop(key);
    }
}
