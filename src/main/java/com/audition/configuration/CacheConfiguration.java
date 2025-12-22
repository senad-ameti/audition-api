package com.audition.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Production cache configuration using Caffeine. Controlled by 'audition.cache.enabled'.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "audition.cache.enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfiguration {

    @Value("${audition.cache.ttl.minutes:15}")
    private transient int cacheTtlMinutes;

    @Value("${audition.cache.max.size:1000}")
    private transient int cacheMaxSize;

    /**
     * Creates a Caffeine-based CacheManager bean with specified caches and TTL.
     *
     * @return the configured CacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        final CaffeineCacheManager cacheManager = new CaffeineCacheManager("posts", "postsById", "comments",
            "postWithComments");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(cacheTtlMinutes))
            .maximumSize(cacheMaxSize));
        return cacheManager;
    }
}
