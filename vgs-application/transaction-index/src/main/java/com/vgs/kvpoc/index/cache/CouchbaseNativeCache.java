package com.vgs.kvpoc.index.cache;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.UpsertOptions;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vgs.kvpoc.index.service.CouchbaseConnectionManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * ====================================================================
 * VGS KV POC - Transaction Index Pattern: Couchbase Native Cache
 * ====================================================================
 * 
 * WHAT THIS CLASS DOES:
 * This class implements Spring's Cache interface using Couchbase as the backing store.
 * It provides high-performance caching with metrics tracking and TTL support.
 * 
 * KEY FEATURES:
 * - Uses Couchbase collections for caching
 * - Supports TTL (time-to-live) for cached items
 * - Tracks performance metrics
 * - Handles serialization/deserialization of cached objects
 */
@Slf4j
public class CouchbaseNativeCache implements Cache {

    private final String name;
    private final Collection cacheCollection;
    private final ObjectMapper objectMapper;
    private final Duration defaultTtl = Duration.ofMinutes(5);
    
    // Metrics
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Timer cacheGetTimer;
    private final Timer cachePutTimer;

    public CouchbaseNativeCache(String name, 
                              CouchbaseConnectionManager connectionManager,
                              MeterRegistry meterRegistry,
                              ObjectMapper objectMapper,
                              Counter cacheHits,
                              Counter cacheMisses,
                              Timer cacheGetTimer,
                              Timer cachePutTimer) {
        this.name = name;
        this.objectMapper = objectMapper;
        this.cacheHits = cacheHits;
        this.cacheMisses = cacheMisses;
        this.cacheGetTimer = cacheGetTimer;
        this.cachePutTimer = cachePutTimer;
        
        // Use a dedicated cache collection or fallback to default
        Collection tempCollection;
        try {
            tempCollection = connectionManager.getCollection("cache");
            log.info("Using 'cache' collection for cache: {}", name);
        } catch (Exception e) {
            log.warn("Cache collection not available, using game_rounds collection for cache: {}", name);
            tempCollection = connectionManager.getCollection("game_rounds");
        }
        this.cacheCollection = tempCollection;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return cacheCollection;
    }

    @Override
    public ValueWrapper get(Object key) {
        String cacheKey = createCacheKey(key);
        Timer.Sample sample = Timer.start();
        
        try {
            GetResult result = cacheCollection.get(cacheKey);
            cacheHits.increment();
            sample.stop(cacheGetTimer);
            
            String json = result.contentAs(String.class);
            CacheEntry entry = objectMapper.readValue(json, CacheEntry.class);
            
            return new SimpleValueWrapper(entry.getValue());
        } catch (DocumentNotFoundException e) {
            cacheMisses.increment();
            sample.stop(cacheGetTimer);
            return null;
        } catch (Exception e) {
            cacheMisses.increment();
            sample.stop(cacheGetTimer);
            log.error("Error getting from cache {}: {}", name, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper wrapper = get(key);
        return wrapper != null ? (T) wrapper.get() : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            return (T) wrapper.get();
        }
        
        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        if (value == null) {
            return;
        }
        
        String cacheKey = createCacheKey(key);
        Timer.Sample sample = Timer.start();
        
        try {
            CacheEntry entry = new CacheEntry(value);
            String json = objectMapper.writeValueAsString(entry);
            
            cacheCollection.upsert(
                cacheKey, 
                json,
                UpsertOptions.upsertOptions().expiry(defaultTtl)
            );
            
            sample.stop(cachePutTimer);
            log.debug("Cached value for key: {} in cache: {}", key, name);
        } catch (JsonProcessingException e) {
            sample.stop(cachePutTimer);
            log.error("Error serializing cache value for key: {} in cache: {}", key, name, e);
        } catch (Exception e) {
            sample.stop(cachePutTimer);
            log.error("Error putting to cache {} for key {}: {}", name, key, e.getMessage());
        }
    }

    @Override
    public void evict(Object key) {
        String cacheKey = createCacheKey(key);
        try {
            cacheCollection.remove(cacheKey);
            log.debug("Evicted key: {} from cache: {}", key, name);
        } catch (Exception e) {
            log.error("Error evicting from cache {} for key {}: {}", name, key, e.getMessage());
        }
    }

    @Override
    public void clear() {
        log.info("Clear operation not supported for Couchbase Native Cache: {}", name);
    }

    private String createCacheKey(Object key) {
        return name + ":" + key.toString();
    }

    /**
     * Cache entry wrapper to store metadata along with the value
     */
    private static class CacheEntry {
        private Object value;
        private long timestamp;

        public CacheEntry() {
            // For Jackson deserialization
        }

        public CacheEntry(Object value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}