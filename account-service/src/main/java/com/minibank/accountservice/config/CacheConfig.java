package com.minibank.accountservice.config;

import com.minibank.accountservice.entity.Account;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching   // accende @Cacheable / @CacheEvict
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                          @Value("${app.cache.accounts-ttl:5m}") Duration accountsTtl) {

        RedisCacheConfiguration accountsConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(accountsTtl)              // TTL: scade da sola dopo X minuti
                .disableCachingNullValues()         // non salvare "null"
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new JacksonJsonRedisSerializer<>(Account.class)));  // salva in JSON

        return RedisCacheManager.builder(connectionFactory)
                .withCacheConfiguration("accounts", accountsConfig)
                .build();
    }
}