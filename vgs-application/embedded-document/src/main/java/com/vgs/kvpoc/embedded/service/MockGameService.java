package com.vgs.kvpoc.embedded.service;

import com.vgs.kvpoc.embedded.model.GameRound;
import com.vgs.kvpoc.embedded.model.EmbeddedTransaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

@Service
@ConditionalOnProperty(name = "couchbase.enabled", havingValue = "false")
public class MockGameService {

    private final ConcurrentHashMap<String, GameRound> gameRounds = new ConcurrentHashMap<>();
    private final AtomicLong roundIdCounter = new AtomicLong(1);
    private final AtomicLong transactionIdCounter = new AtomicLong(1);
    private final Random random = new Random();

    public GameRound createGameRound(String playerId, double betAmount) {
        String roundId = "ROUND_" + roundIdCounter.getAndIncrement();
        
        GameRound round = new GameRound(roundId, 1, playerId, "VENDOR_001", betAmount);
        
        // Create some mock transactions
        List<EmbeddedTransaction> transactions = new ArrayList<>();
        for (int i = 0; i < random.nextInt(5) + 1; i++) {
            EmbeddedTransaction transaction = new EmbeddedTransaction();
            transaction.setId("TXN_" + transactionIdCounter.getAndIncrement());
            transaction.setType("GAME_ACTION");
            transaction.setAmount(random.nextDouble() * 100);
            transaction.setCreateTime(System.currentTimeMillis());
            transaction.setStatus("COMPLETED");
            transactions.add(transaction);
        }
        round.setTransactions(transactions);
        
        gameRounds.put(roundId, round);
        return round;
    }

    public GameRound getGameRound(String roundId) {
        return gameRounds.get(roundId);
    }

    public List<GameRound> getPlayerRounds(String playerId) {
        return gameRounds.values().stream()
                .filter(round -> playerId.equals(round.getAgentPlayerId()))
                .toList();
    }

    public GameRound updateGameRound(String roundId, String status) {
        GameRound round = gameRounds.get(roundId);
        if (round != null) {
            round.setRoundStatus(status);
            round.setEndTimestamp(System.currentTimeMillis());
        }
        return round;
    }

    public boolean deleteGameRound(String roundId) {
        return gameRounds.remove(roundId) != null;
    }

    public long getTotalRounds() {
        return gameRounds.size();
    }
}
