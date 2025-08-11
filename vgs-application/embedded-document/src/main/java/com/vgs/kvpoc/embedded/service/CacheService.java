package com.vgs.kvpoc.embedded.service;

import com.couchbase.client.java.Collection;
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

    private final io.micrometer.core.instrument.Cache<String, Object> cache; // Changed to io.micrometer.core.instrument.Cache
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Timer cacheAccessTimer;


    public CacheService(Collection gameRoundsCollection, MeterRegistry meterRegistry) {
        this.gameRoundsCollection = gameRoundsCollection;
        // Initialize the Micrometer cache
        this.cache = io.micrometer.core.instrument.SimpleCache.builder()
                .maximumSize(1000) // Example: Max 1000 entries
                .build();

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
            // Use Micrometer cache
            GameRound gameRound = (GameRound) cache.get(roundId);
            if (gameRound != null) {
                cacheHits.increment();
                return gameRound;
            } else {
                cacheMisses.increment();
                // Fetch from Couchbase if not in cache
                GetResult result = gameRoundsCollection.get(roundId);
                gameRound = result.contentAs(GameRound.class);
                // Put into Micrometer cache with a TTL
                cache.put(roundId, gameRound, Duration.ofMinutes(5)); // Example TTL
                return gameRound;
            }
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