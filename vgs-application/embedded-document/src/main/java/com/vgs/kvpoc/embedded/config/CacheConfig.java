package com.vgs.kvpoc.embedded.config;

import com.vgs.kvpoc.embedded.cache.CouchbaseNativeCacheManager;
import com.vgs.kvpoc.embedded.service.CouchbaseConnectionManager;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * ====================================================================
 * VGS KV POC - Embedded Document Pattern: Cache Configuration
 * ====================================================================
 * 
 * WHAT THIS CLASS DOES:
 * This configuration class sets up the caching system for the embedded document pattern.
 * It enables Spring's caching support and configures a Couchbase-native cache manager
 * for optimal performance.
 * 
 * KEY FEATURES:
 * - Enables Spring's @Cacheable, @CacheEvict, etc. annotations
 * - Configures a Couchbase-native cache implementation
 * - Integrates with metrics for cache performance monitoring
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Autowired
    private CouchbaseConnectionManager connectionManager;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    public void customizeMetricsCommonTags(MeterRegistry registry) {
        String instance = java.util.Optional.ofNullable(System.getenv("HOSTNAME")).orElse("unknown");
        registry.config().commonTags(
            "application", "vgs-embedded",
            "pattern", "EmbeddedDocument",
            "instance", instance
        );
    }

    /**
     * Creates a Couchbase-native cache manager that uses Couchbase collections
     * for caching frequently accessed data.
     * 
     * @return Configured cache manager for the application
     */
    @Bean
    @Primary
    public CacheManager couchbaseCacheManager() {
        return new CouchbaseNativeCacheManager(connectionManager, meterRegistry);
    }
}