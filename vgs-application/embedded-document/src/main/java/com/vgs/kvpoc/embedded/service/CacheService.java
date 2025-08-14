package com.vgs.kvpoc.embedded.service;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.cluster.Cluster;
import com.couchbase.client.java.kv.GetResult;
import com.vgs.kvpoc.embedded.model.GameRound;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Separate cache service to handle cached operations
 * This avoids Spring AOP proxy issues with self-injection
 */
@Service
@Slf4j
public class CacheService {

    private final Collection gameRoundsCollection;
    private final Cluster cluster;

    private final io.micrometer.core.instrument.Timer cacheAccessTimer;
    private final Counter cacheHits;
    private final Counter cacheMisses;


    public CacheService(Collection gameRoundsCollection, MeterRegistry meterRegistry, Cluster cluster) {
        this.gameRoundsCollection = gameRoundsCollection;
        this.cluster = cluster;
        // Initialize the Micrometer cache
        // The Cache class is not directly available, using SimpleCache as an example if needed.
        // However, the current implementation focuses on Counter and Timer metrics.

        // Register metrics
        this.cacheHits = Counter.builder("cache.hits")
                .description("Number of cache hits")
                .register(meterRegistry);
        this.cacheMisses = Counter.builder("cache.misses")
                .description("Number of cache misses")
                .register(meterRegistry);
        this.cacheAccessTimer = Timer.builder("cache.access")
                .description("Cache access time")
                .register(meterRegistry);
    }

    @Cacheable(value = "gameRounds", key = "#roundId")
    public GameRound getGameRound(String roundId) {
        try {
            // The previous line with `cache.get(roundId)` is removed as `io.micrometer.core.instrument.Cache` is not used.
            // This method now relies on Spring's @Cacheable and custom cache logic.

            // Fetch from Couchbase if not in cache (handled by Spring Cache or custom cache)
            // If Spring Cache misses, this logic will be executed.
            // The following lines are meant to simulate cache interaction for metrics.

            // Placeholder for cache interaction logic that would increment metrics
            // In a real scenario, if using a cache library, you'd get from there first.
            // For this example, we directly fetch and assume a cache miss if not found via Spring Cache.

            // Fetch from Couchbase
            GetResult result = gameRoundsCollection.get(roundId);
            GameRound gameRound = result.contentAs(GameRound.class);

            // Simulate cache put and increment hits if it were a hit
            // For demonstration, we'll just increment misses here if we were to assume a miss.
            // If Spring Cache handles the hit, this method won't even be called.
            // Thus, any execution here implies a cache miss.
            cacheMisses.increment(); // Increment miss count as Spring Cache missed this entry.

            // Note: The original code had a `cache.put(roundId, gameRound, Duration.ofMinutes(5));`
            // This is now implicitly handled by Spring Cache's configuration.
            // If a custom cache was to be used alongside Spring Cache, that logic would be here.

            return gameRound;
        } catch (Exception e) {
            log.debug("Round not found or error accessing cache/DB: {}", roundId, e);
            return null;
        }
    }

    // --- Custom Cache Implementation (for demonstration, usually use Spring Cache or Micrometer Cache) ---

    private final ConcurrentMap<String, CacheEntry> customCache = new ConcurrentHashMap<>();
    private final Duration defaultTtl = Duration.ofMinutes(5);

    public void put(String key, Object value) {
        put(key, value, defaultTtl);
    }

    public void put(String key, Object value, Duration ttl) {
        CacheEntry entry = new CacheEntry(value, LocalDateTime.now().plus(ttl));
        customCache.put(key, entry);
        log.debug("Put in custom cache: {}", key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        CacheEntry entry = customCache.get(key);
        if (entry == null || entry.isExpired()) {
            customCache.remove(key);
            log.debug("Cache miss or expired in custom cache: {}", key);
            return null;
        }
        log.debug("Cache hit in custom cache: {}", key);
        return (T) entry.getValue();
    }

    public void evict(String key) {
        customCache.remove(key);
        log.debug("Evicted from custom cache: {}", key);
    }

    public void clear() {
        customCache.clear();
        log.debug("Cleared custom cache");
    }

    // Cleanup expired entries periodically
    public void cleanup() {
        customCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        log.debug("Cleaned up expired entries from custom cache");
    }

    private static class CacheEntry {
        private final Object value;
        private final LocalDateTime expiry;

        public CacheEntry(Object value, LocalDateTime expiry) {
            this.value = value;
            this.expiry = expiry;
        }

        public Object getValue() {
            return value;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiry);
        }
    }
}