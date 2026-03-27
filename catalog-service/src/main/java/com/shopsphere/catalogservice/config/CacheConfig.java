package com.shopsphere.catalogservice.config;

<<<<<<< HEAD
=======
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
>>>>>>> 0a1129c (Complete Project)
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
<<<<<<< HEAD
=======
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper());
>>>>>>> 0a1129c (Complete Project)
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
<<<<<<< HEAD
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
=======
                        .fromSerializer(serializer))
>>>>>>> 0a1129c (Complete Project)
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        cacheConfigurations.put("products", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("product", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("featuredProducts", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("productsByCategory", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("categories", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("category", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
<<<<<<< HEAD
=======

    private ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        return objectMapper;
    }
>>>>>>> 0a1129c (Complete Project)
}
