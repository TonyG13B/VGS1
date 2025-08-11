package com.vgs.kvpoc.embedded.service;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;
import com.vgs.kvpoc.embedded.model.GameRound;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Separate cache service to handle cached operations
 * This avoids Spring AOP proxy issues with self-injection
 */
@Service
@Slf4j
public class CacheService {
    
    private final Collection gameRoundsCollection;
    
    public CacheService(Collection gameRoundsCollection) {
        this.gameRoundsCollection = gameRoundsCollection;
    }
    
    @Cacheable(value = "gameRounds", key = "#roundId")
    public GameRound getGameRound(String roundId) {
        try {
            GetResult result = gameRoundsCollection.get(roundId);
            return result.contentAs(GameRound.class);
        } catch (Exception e) {
            log.debug("Round not found in cache: {}", roundId);
            return null;
        }
    }
}
package com.vgs.kvpoc.embedded.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.time.LocalDateTime;
import java.time.Duration;

@Service
public class CacheService {
    
    private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Duration defaultTtl = Duration.ofMinutes(5);
    
    public void put(String key, Object value) {
        put(key, value, defaultTtl);
    }
    
    public void put(String key, Object value, Duration ttl) {
        CacheEntry entry = new CacheEntry(value, LocalDateTime.now().plus(ttl));
        cache.put(key, entry);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        CacheEntry entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return (T) entry.getValue();
    }
    
    public void evict(String key) {
        cache.remove(key);
    }
    
    public void clear() {
        cache.clear();
    }
    
    // Cleanup expired entries periodically
    public void cleanup() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
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
