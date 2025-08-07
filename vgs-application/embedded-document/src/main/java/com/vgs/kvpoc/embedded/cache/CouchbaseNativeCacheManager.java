package com.vgs.kvpoc.embedded.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vgs.kvpoc.embedded.service.CouchbaseConnectionManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ====================================================================
 * VGS KV POC - Embedded Document Pattern: Couchbase Native Cache Manager
 * ====================================================================
 * 
 * WHAT THIS CLASS DOES:
 * This cache manager implements Spring's CacheManager interface using Couchbase
 * as the backing store. It provides high-performance caching with metrics tracking.
 * 
 * KEY FEATURES:
 * - Uses Couchbase collections for caching
 * - Tracks cache hit/miss metrics
 * - Manages cache instances for different cache names
 * - Provides performance statistics
 */
@Slf4j
public class CouchbaseNativeCacheManager implements CacheManager {

    private final CouchbaseConnectionManager connectionManager;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();
    
    // Metrics
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Timer cacheGetTimer;
    private final Timer cachePutTimer;

    public CouchbaseNativeCacheManager(CouchbaseConnectionManager connectionManager, 
                                     MeterRegistry meterRegistry) {
        this.connectionManager = connectionManager;
        this.meterRegistry = meterRegistry;
        this.objectMapper = new ObjectMapper();
        
        // Initialize metrics
        this.cacheHits = Counter.builder("vgs.cache.hits")
                .description("Cache hits")
                .register(meterRegistry);
                
        this.cacheMisses = Counter.builder("vgs.cache.misses")
                .description("Cache misses")
                .register(meterRegistry);
                
        this.cacheGetTimer = Timer.builder("vgs.cache.get.latency")
                .description("Cache get latency")
                .register(meterRegistry);
                
        this.cachePutTimer = Timer.builder("vgs.cache.put.latency")
                .description("Cache put latency")
                .register(meterRegistry);
                
        log.info("Initialized Couchbase Native Cache Manager");
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, cacheName -> {
            log.info("Creating cache: {}", cacheName);
            return new CouchbaseNativeCache(cacheName, connectionManager, meterRegistry, objectMapper,
                                          cacheHits, cacheMisses, cacheGetTimer, cachePutTimer);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return cacheMap.keySet();
    }

    /**
     * Get the current cache hit ratio as a percentage
     * 
     * @return Cache hit ratio (0-100%)
     */
    public double getCacheHitRatio() {
        double hits = cacheHits.count();
        double misses = cacheMisses.count();
        double total = hits + misses;
        return total > 0 ? (hits / total) * 100.0 : 0.0;
    }
}