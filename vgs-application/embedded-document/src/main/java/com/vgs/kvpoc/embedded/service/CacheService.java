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