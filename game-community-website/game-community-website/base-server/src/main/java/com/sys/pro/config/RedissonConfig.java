package com.sys.pro.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Redisson 客户端配置。
 * 复用 Spring Redis 地址，并启用锁看门狗，避免业务执行时间超过预估 TTL 时锁被提前释放。
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host:127.0.0.1}")
    private String host;

    @Value("${spring.redis.port:6379}")
    private int port;

    @Value("${spring.redis.password:}")
    private String password;

    @Value("${spring.redis.database:0}")
    private int database;

    @Value("${spring.redis.timeout:3000ms}")
    private Duration timeout;

    @Value("${app.redisson.lock-watchdog-timeout:30000}")
    private long lockWatchdogTimeout;

    /**
     * 创建 Redisson 客户端，提供带看门狗的分布式锁能力。
     * @return Redis 缓存在该步骤产出的业务结果。
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.setLockWatchdogTimeout(Math.max(10000L, lockWatchdogTimeout));

        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setConnectTimeout((int) timeout.toMillis())
                .setTimeout((int) timeout.toMillis());
        if (StringUtils.hasText(password)) {
            singleServerConfig.setPassword(password);
        }
        return Redisson.create(config);
    }
}
