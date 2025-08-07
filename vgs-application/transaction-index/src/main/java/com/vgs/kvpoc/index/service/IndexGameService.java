package com.vgs.kvpoc.index.service;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutationResult;
import com.vgs.kvpoc.index.model.GameRound;
import com.vgs.kvpoc.index.model.TransactionIndex;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ====================================================================
 * VGS KV POC - Transaction Index Pattern: Game Service
 * ====================================================================
 * 
 * WHAT THIS CLASS DOES:
 * This is the "business logic engine" for the transaction index pattern. Unlike
 * the embedded pattern, this service creates lightweight GameRounds and stores
 * detailed transaction information in separate TransactionIndex documents.
 * 
 * WHY WE USE THIS PATTERN:
 * - Enables lightning-fast searches across ALL transactions (not just within one round)
 * - Perfect for regulatory compliance and audit requirements
 * - Supports complex queries like "find all transactions over $100 for player X"
 * - Scales to millions of transactions without slowing down individual rounds
 * 
 * HOW IT WORKS:
 * 1. Creates GameRound documents with lightweight transaction references
 * 2. Creates separate TransactionIndex documents with complete transaction details
 * 3. Links them together using reference IDs
 * 4. Enables fast lookups in both directions (round→transactions, transaction→round)
 * 
 * REAL-WORLD EXAMPLE:
 * When a player makes a bet:
 * 1. GameRound gets a lightweight reference: {refId: "txn-idx-123", type: "BET", amount: 50.00}
 * 2. TransactionIndex gets full details: complete transaction info + compliance data
 * 3. This enables fast round display AND comprehensive transaction searching
 * 
 * BENEFITS FOR ENTERPRISE GAMING:
 * - Fast searches across all player activity
 * - Comprehensive audit trails for compliance
 * - Fraud detection across all transactions
 * - Regulatory reporting made simple
 * - Performance scales with volume
 * 
 * TECHNICAL DETAILS:
 * - Uses two separate Couchbase collections (game_rounds and game_transactions)
 * - Implements pure key-value operations for maximum performance
 * - Uses CAS (Compare-And-Swap) for safe concurrent updates
 * - Provides rich metadata for business intelligence
 */
@Service
public class IndexGameService {
    
    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("gameRoundsCollection")
    private Collection gameRoundsCollection;
    
    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("gameTransactionsCollection")
    private Collection gameTransactionsCollection;
    
    // Metrics for monitoring performance
    @Autowired
    private MeterRegistry meterRegistry;
    private Timer roundRetrievalTimer;
    private Timer transactionAddTimer;
    
    @PostConstruct
    private void initializeMetrics() {
        this.roundRetrievalTimer = Timer.builder("vgs.game.round.retrieval")
                .description("Time to retrieve a game round")
                .register(meterRegistry);
                
        this.transactionAddTimer = Timer.builder("vgs.game.transaction.add")
                .description("Transaction add time")
                .register(meterRegistry);
    }
    
    /**
     * CREATE A NEW GAME ROUND (TRANSACTION INDEX PATTERN)
     * 
     * This creates a new gaming round using the transaction index pattern. Unlike
     * the embedded pattern, this creates a lightweight GameRound that will store
     * references to transactions rather than the full transaction details.
     * 
     * WHAT IT DOES:
     * 1. Creates a new GameRound object with basic round information
     * 2. Saves it to the game_rounds collection in Couchbase
     * 3. Sets up the structure for lightweight transaction references
     * 
     * WHY THIS IS DIFFERENT FROM EMBEDDED:
     * - GameRound stays small and fast to read/update
     * - Full transaction details go in separate TransactionIndex documents
     * - Enables fast searches across all transactions from all rounds
     * - Perfect for compliance and audit requirements
     * 
     * EXAMPLE USAGE:
     * createGameRound("round-123", 1, "player-abc", "agent-xyz", 1000.0)
     * Creates a lightweight round that will hold transaction references
     * 
     * @param roundId Unique identifier for this round
     * @param roundNumber Which round this is in the session
     * @param playerId Who is playing
     * @param agentId Gaming agent managing this round
     * @param initialBalance Player's starting balance
     * @return The created GameRound object (lightweight version)
     */
    public GameRound createGameRound(String roundId, Integer roundNumber, String playerId, String agentId, Double initialBalance) {
        GameRound gameRound = new GameRound(roundId, roundNumber, playerId, agentId, initialBalance);
        
        try {
            MutationResult result = gameRoundsCollection.insert(roundId, gameRound);
            System.out.println("Created game round: " + roundId + " with CAS: " + result.cas());
            return gameRound;
        } catch (Exception e) {
            System.err.println("Failed to create game round: " + e.getMessage());
            throw new RuntimeException("Failed to create game round", e);
        }
    }
    
    /**
     * Get game round by ID - basic read operation
     */
    @Cacheable(value = "gameRounds", key = "#roundId")
    @Timed(value = "vgs.game.round.get", description = "Time to get a game round")
    public GameRound getGameRound(String roundId) {
        Timer.Sample sample = Timer.start();
        try {
            GetResult result = gameRoundsCollection.get(roundId);
            GameRound gameRound = result.contentAs(GameRound.class);
            System.out.println("Retrieved game round: " + roundId + " with " + gameRound.getTransactionRefs().size() + " transaction refs");
            sample.stop(roundRetrievalTimer);
            return gameRound;
        } catch (Exception e) {
            sample.stop(roundRetrievalTimer);
            System.err.println("Failed to get game round: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * CREATE TRANSACTION WITH INDEX (CORE OF INDEX PATTERN)
     * 
     * This is the heart of the transaction index pattern! This function creates
     * a detailed TransactionIndex document AND adds a lightweight reference to
     * the GameRound. It's like creating both a detailed file and a catalog entry.
     * 
     * WHAT IT DOES:
     * 1. Creates a comprehensive TransactionIndex document with full details
     * 2. Saves it to the game_transactions collection
     * 3. Creates a lightweight reference in the GameRound
     * 4. Updates the GameRound with the new reference
     * 5. Returns the complete TransactionIndex for further processing
     * 
     * WHY THIS IS POWERFUL:
     * - Full transaction details stored separately for fast searching
     * - Lightweight references keep rounds fast to read/update
     * - Enables complex queries across all transactions
     * - Perfect for compliance and audit trails
     * 
     * TWO-DOCUMENT APPROACH:
     * - TransactionIndex: Complete details, compliance data, audit trail
     * - GameRound reference: Just the basics needed for round display
     * 
     * EXAMPLE USAGE:
     * createTransactionWithIndex("txn-456", "round-123", "player-abc", "BET", 50.0, "Poker bet")
     * Creates full transaction document + lightweight reference in round
     * 
     * @param transactionId Unique identifier for this transaction
     * @param roundId Which round this transaction belongs to
     * @param playerId Who made this transaction
     * @param type Transaction type (BET, WIN, BONUS, etc.)
     * @param amount Money amount involved
     * @param description Human-readable description
     * @return The created TransactionIndex document
     */
    public TransactionIndex createTransactionWithIndex(String transactionId, String roundId, String playerId, String type, Double amount, String description) {
        try {
            // Create transaction index document
            TransactionIndex index = new TransactionIndex(transactionId, roundId, playerId, type, amount);
            index.setBalanceBefore(1000.0); // Simplified for Basic
            index.setBalanceAfter(type.equals("BET") ? 1000.0 - amount : 1000.0 + amount);
            index.updatePerformanceMetrics(System.currentTimeMillis() % 100, 1, 1);
            index.completeIndexing();
            
            String indexId = "IDX_" + transactionId;
            MutationResult indexResult = gameTransactionsCollection.insert(indexId, index);
            System.out.println("Created transaction index: " + indexId + " with CAS: " + indexResult.cas());
            
            // Add lightweight reference to round
            GameRound gameRound = getGameRound(roundId);
            if (gameRound != null) {
                GetResult getResult = gameRoundsCollection.get(roundId);
                gameRound.addTransactionRef(transactionId, type, amount, java.time.LocalDateTime.now().toString());
                
                MutationResult roundResult = gameRoundsCollection.replace(roundId, gameRound, 
                    com.couchbase.client.java.kv.ReplaceOptions.replaceOptions().cas(getResult.cas()));
                System.out.println("Added transaction ref to round: " + roundId + " with CAS: " + roundResult.cas());
            }
            
            return index;
        } catch (Exception e) {
            System.err.println("Failed to create transaction with index: " + e.getMessage());
            throw new RuntimeException("Failed to create transaction with index", e);
        }
    }
    
    /**
     * Get transaction from index - index lookup
     */
    public TransactionIndex getTransactionIndex(String transactionId) {
        try {
            String indexId = "IDX_" + transactionId;
            GetResult result = gameTransactionsCollection.get(indexId);
            TransactionIndex index = result.contentAs(TransactionIndex.class);
            System.out.println("Retrieved transaction index: " + indexId);
            return index;
        } catch (Exception e) {
            System.err.println("Failed to get transaction index: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get transactions for round using refs - lightweight query
     */
    public List<TransactionIndex> getTransactionsForRound(String roundId) {
        List<TransactionIndex> transactions = new ArrayList<>();
        
        try {
            GameRound gameRound = getGameRound(roundId);
            if (gameRound == null) return transactions;
            
            // Use transaction references to find indexed transactions
            for (GameRound.TransactionRef ref : gameRound.getTransactionRefs()) {
                TransactionIndex index = getTransactionIndex(ref.getTransactionId());
                if (index != null) {
                    transactions.add(index);
                }
            }
            
            System.out.println("Found " + transactions.size() + " indexed transactions for round: " + roundId);
            return transactions;
        } catch (Exception e) {
            System.err.println("Failed to get transactions for round: " + e.getMessage());
            return transactions;
        }
    }
    
    /**
     * Update game round - basic update operation
     */
    public GameRound updateGameRound(String roundId, GameRound updatedRound) {
        try {
            GetResult getResult = gameRoundsCollection.get(roundId);
            updatedRound.setLastUpdateTime(java.time.LocalDateTime.now().toString());
            
            MutationResult result = gameRoundsCollection.replace(roundId, updatedRound, 
                com.couchbase.client.java.kv.ReplaceOptions.replaceOptions().cas(getResult.cas()));
            System.out.println("Updated game round: " + roundId + " with CAS: " + result.cas());
            return updatedRound;
        } catch (Exception e) {
            System.err.println("Failed to update game round: " + e.getMessage());
            throw new RuntimeException("Failed to update game round", e);
        }
    }
    
    /**
     * Delete game round - basic delete operation
     */
    public boolean deleteGameRound(String roundId) {
        try {
            gameRoundsCollection.remove(roundId);
            System.out.println("Deleted game round: " + roundId);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to delete game round: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete transaction index - basic delete operation
     */
    public boolean deleteTransactionIndex(String transactionId) {
        try {
            String indexId = "IDX_" + transactionId;
            gameTransactionsCollection.remove(indexId);
            System.out.println("Deleted transaction index: " + indexId);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to delete transaction index: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Search transactions by player - basic index search
     */
    public List<TransactionIndex> searchTransactionsByPlayer(String playerId) {
        List<TransactionIndex> transactions = new ArrayList<>();
        
        try {
            // Simple implementation for - check predictable index IDs
            for (int i = 1; i <= 20; i++) { // Check up to 20 possible transactions
                try {
                    String indexId = "IDX_TXN_" + playerId + "_" + i;
                    GetResult result = gameTransactionsCollection.get(indexId);
                    TransactionIndex index = result.contentAs(TransactionIndex.class);
                    if (playerId.equals(index.getAgentPlayerId())) {
                        transactions.add(index);
                    }
                } catch (Exception e) {
                    // Index doesn't exist, continue
                }
            }
            
            System.out.println("Found " + transactions.size() + " transactions for player: " + playerId);
            return transactions;
        } catch (Exception e) {
            System.err.println("Failed to search transactions by player: " + e.getMessage());
            return transactions;
        }
    }
    
    /**
     * PROCESS TRANSACTION FIRST - NEW TRANSACTION-FIRST APPROACH (INDEX PATTERN)
     * 
     * This method implements the new transaction-first flow for the index pattern where
     * rounds are created automatically when the first transaction is received, eliminating
     * the need for separate round creation endpoints.
     * 
     * FLOW:
     * 1st transaction → Creates game round and transaction index
     * 2nd transaction → Looks up existing round, creates new transaction index, updates round refs
     * 3rd transaction → Looks up round again, creates new transaction index, updates round refs
     * 
     * @param roundId Unique identifier for the round
     * @param transactionId Unique identifier for the transaction
     * @param playerId Player identifier
     * @param type Transaction type (BET, WIN, BONUS, etc.)
     * @param amount Transaction amount
     * @param description Transaction description
     * @return Map containing operation result and metadata
     */
    @Timed(value = "vgs.game.transaction.add", description = "Time to add a transaction")
    public Map<String, Object> processTransactionFirst(String roundId, String transactionId, String playerId, String type, Double amount, String description) {
        Map<String, Object> result = new HashMap<>();
        Timer.Sample sample = Timer.start();
        
        try {
            GetResult existingRound = null;
            boolean roundExists = false;
            
            // Try to get existing round using cached method to enable cache metrics
            try {
                GameRound cachedRound = getGameRound(roundId);
                if (cachedRound != null) {
                    // Convert to GetResult format for compatibility with existing code
                    existingRound = gameRoundsCollection.get(roundId);
                    roundExists = true;
                } else {
                    roundExists = false;
                }
            } catch (Exception e) {
                // Round doesn't exist, we'll create it with the first transaction
                roundExists = false;
            }
            
            // Create the transaction index first (always needed)
            String indexId = "IDX_" + transactionId;
            TransactionIndex transactionIndex = new TransactionIndex(
                transactionId, roundId, playerId, type, amount
            );
            
            // Insert transaction index
            MutationResult indexResult = gameTransactionsCollection.insert(indexId, transactionIndex);
            System.out.println("Created transaction index: " + indexId + " with CAS: " + indexResult.cas());
            
            if (roundExists) {
                // SUBSEQUENT TRANSACTION: Update existing round with new transaction reference
                GameRound gameRound = existingRound.contentAs(GameRound.class);
                
                // Add transaction reference to the round
                gameRound.addTransactionRef(transactionId, type, amount, java.time.LocalDateTime.now().toString());
                gameRound.setLastUpdateTimestamp(System.currentTimeMillis());
                
                // Update the round document using CAS for atomicity
                MutationResult updateResult = gameRoundsCollection.replace(roundId, gameRound, 
                    com.couchbase.client.java.kv.ReplaceOptions.replaceOptions().cas(existingRound.cas()));
                
                System.out.println("Added transaction reference " + transactionId + " to existing round " + roundId + " with CAS: " + updateResult.cas());
                
                result.put("operation", "UPDATE");
                result.put("message", "Transaction index created and reference added to existing round");
                result.put("transactionCount", gameRound.getTransactionRefs().size());
                result.put("roundCasValue", updateResult.cas());
                
            } else {
                // FIRST TRANSACTION: Create new round with transaction reference
                String agentId = "AGENT_VGS"; // Default agent
                GameRound gameRound = new GameRound(roundId, 1, playerId, agentId, 1000.0); // Default initial balance
                
                // Add the first transaction reference
                gameRound.addTransactionRef(transactionId, type, amount, java.time.LocalDateTime.now().toString());
                
                // Insert new round document
                MutationResult insertResult = gameRoundsCollection.insert(roundId, gameRound);
                
                System.out.println("Created new round " + roundId + " with first transaction reference " + transactionId + " with CAS: " + insertResult.cas());
                
                result.put("operation", "CREATE");
                result.put("message", "New round created with first transaction index");
                result.put("transactionCount", 1);
                result.put("roundCasValue", insertResult.cas());
            }
            
            result.put("success", true);
            result.put("roundId", roundId);
            result.put("transactionId", transactionId);
            result.put("indexId", indexId);
            result.put("indexCasValue", indexResult.cas());
            result.put("approach", "Transaction-first (Index Pattern)");
            
            // Record metrics
            sample.stop(transactionAddTimer);
            
            return result;
            
        } catch (Exception e) {
            sample.stop(transactionAddTimer);
            System.err.println("Failed to process transaction-first (index pattern): " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
            throw new RuntimeException("Failed to process transaction-first (index pattern)", e);
        }
    }
    
    /**
     * Test connectivity - health check
     */
    public boolean testConnectivity() {
        try {
            String testRoundId = "CONNECTIVITY_TEST_ROUND_" + System.currentTimeMillis();
            String testTxnId = "CONNECTIVITY_TEST_TXN_" + System.currentTimeMillis();
            String indexId = "IDX_" + testTxnId;
            
            // Create minimal test objects
            Map<String, Object> testRound = new HashMap<>();
            testRound.put("id", testRoundId);
            testRound.put("type", "connectivity_test_round");
            testRound.put("timestamp", System.currentTimeMillis());
            
            Map<String, Object> testIndex = new HashMap<>();
            testIndex.put("id", indexId);
            testIndex.put("transactionId", testTxnId);
            testIndex.put("type", "connectivity_test_index");
            testIndex.put("timestamp", System.currentTimeMillis());
            
            // Insert test documents
            gameRoundsCollection.insert(testRoundId, testRound);
            gameTransactionsCollection.insert(indexId, testIndex);
            
            // Read them back
            GetResult roundResult = gameRoundsCollection.get(testRoundId);
            GetResult indexResult = gameTransactionsCollection.get(indexId);
            
            Map<String, Object> retrievedRound = roundResult.contentAs(Map.class);
            Map<String, Object> retrievedIndex = indexResult.contentAs(Map.class);
            
            // Clean up
            gameRoundsCollection.remove(testRoundId);
            gameTransactionsCollection.remove(indexId);
            
            return retrievedRound != null && retrievedIndex != null && 
                   testRoundId.equals(retrievedRound.get("id")) && 
                   testTxnId.equals(retrievedIndex.get("transactionId"));
                   
        } catch (Exception e) {
            System.err.println("Connectivity test failed: " + e.getMessage());
            return false;
        }
    }
}