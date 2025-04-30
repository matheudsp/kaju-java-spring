package com.valedosol.kaju.config.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        // Create different cache instances with different configurations
        CaffeineCache plansCache = new CaffeineCache("plans", 
            Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .initialCapacity(10)
                .maximumSize(50)
                .recordStats()
                .build());
        
        CaffeineCache accountsCache = new CaffeineCache("accounts", 
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .initialCapacity(100)
                .maximumSize(500)
                .recordStats()
                .build());
        
        CaffeineCache targetsCache = new CaffeineCache("targets", 
            Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .initialCapacity(20)
                .maximumSize(100)
                .recordStats()
                .build());
        
        // Register all cache instances
        cacheManager.setCaches(Arrays.asList(plansCache, accountsCache, targetsCache));
        
        return cacheManager;
    }
}