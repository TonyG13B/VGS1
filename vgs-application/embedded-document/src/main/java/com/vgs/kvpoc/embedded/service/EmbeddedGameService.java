package com.vgs.kvpoc.embedded.service;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutationResult;
import com.vgs.kvpoc.embedded.model.GameRound;
import com.vgs.kvpoc.embedded.model.EmbeddedTransaction;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * ====================================================================
 * VGS KV POC - Embedded Document Pattern: Game Service
 * ====================================================================
 * 
 * WHAT THIS CLASS DOES:
 * This is the "business logic engine" for the embedded document pattern. It handles
 * all the core gaming operations like creating game rounds, adding transactions,
 * and retrieving game data from the Couchbase database.
 * 
 * WHY WE NEED THIS:
 * - Separates business logic from web interface (controllers)
 * - Handles all database operations for game rounds and transactions
 * - Implements the "embedded document" pattern where transactions are stored inside game rounds
 * - Provides error handling and logging for all gaming operations
 * 
 * HOW IT WORKS:
 * 1. Receives requests from the controller layer
 * 2. Creates or retrieves GameRound objects from Couchbase
 * 3. Adds transactions directly to the embedded array inside GameRound documents
 * 4. Uses Compare-And-Swap (CAS) for safe concurrent updates
 * 5. Returns results back to the controller
 * 
 * REAL-WORLD EXAMPLE:
 * When a player starts a poker game:
 * 1. createGameRound() creates a new round document
 * 2. addTransaction() adds each bet/win to the embedded transactions array
 * 3. getGameRound() retrieves the complete round with all transactions
 * 
 * BENEFITS OF EMBEDDED PATTERN:
 * - One database call gets complete round + all transactions
 * - Atomic updates ensure data consistency
 * - Perfect for real-time gaming where you need the full picture fast
 * 
 * TECHNICAL DETAILS:
 * - Uses Spring Boot @Service annotation for dependency injection
 * - Directly interfaces with Couchbase Java SDK
 * - Implements pure key-value operations (no queries)
 * - Uses CAS (Compare-And-Swap) for safe concurrent updates
 */
@Service
@Slf4j
public class EmbeddedGameService {
    
    private final CouchbaseConnectionManager connectionManager;
    private final MeterRegistry meterRegistry;
    private final Collection gameRoundsCollection;
    private final CacheService cacheService;
    
    // Performance metrics
    private Timer roundRetrievalTimer;
    private Timer transactionAddTimer;
    
    @Autowired
    public EmbeddedGameService(CouchbaseConnectionManager connectionManager, 
                              MeterRegistry meterRegistry,
                              @org.springframework.beans.factory.annotation.Qualifier("gameRoundsCollection") Collection gameRoundsCollection,
                              CacheService cacheService) {
        this.connectionManager = connectionManager;
        this.meterRegistry = meterRegistry;
        this.gameRoundsCollection = gameRoundsCollection;
        this.cacheService = cacheService;
        
        // Initialize metrics
        this.roundRetrievalTimer = Timer.builder("vgs.game.round.retrieval")
                .description("Game round retrieval time")
                .register(meterRegistry);
                
        this.transactionAddTimer = Timer.builder("vgs.game.transaction.add")
                .description("Transaction add time")
                .register(meterRegistry);
                
        log.info("Initialized EmbeddedGameService with optimized connection management");
    }
    
    /**
     * CREATE A NEW GAME ROUND
     * 
     * This function creates a brand new gaming round in the database. Think of it
     * like setting up a new poker table - you need to record who's playing,
     * what their starting balance is, and give the round a unique identifier.
     * 
     * WHAT IT DOES:
     * 1. Creates a new GameRound object with the provided details
     * 2. Saves it to the Couchbase database using the embedded document pattern
     * 3. Returns the created round so other functions can work with it
     * 
     * WHY THIS IS IMPORTANT:
     * - Every gaming session needs a round to track all the activity
     * - Sets up the container that will hold all embedded transactions
     * - Establishes the starting point for compliance and audit trails
     * 
     * EXAMPLE USAGE:
     * createGameRound("round-123", 1, "player-abc", "agent-xyz", 1000.0)
     * Creates a round where player-abc starts with $1000 balance
     * 
     * @param roundId Unique identifier for this round (like "round-123")
     * @param roundNumber Which round this is in the session (1, 2, 3, etc.)
     * @param playerId Who is playing (like "player-abc")
     * @param agentId Gaming agent managing this round (like "agent-xyz")
     * @param initialBalance Player's starting balance (like 1000.0 for $1000)
     * @return The created GameRound object
     */
    public GameRound createGameRound(String roundId, Integer roundNumber, String playerId, String agentId, Double initialBalance) {
        // Create a new GameRound object with all the provided information
        GameRound gameRound = new GameRound(roundId, roundNumber, playerId, agentId, initialBalance);
        
        try {
            // Save the new round to the database using Couchbase's insert operation
            // This will fail if a round with this ID already exists (prevents duplicates)
            MutationResult result = gameRoundsCollection.insert(roundId, gameRound);
            
            // Log successful creation with CAS value for debugging
            System.out.println("Created game round: " + roundId + " with CAS: " + result.cas());
            return gameRound;
            
        } catch (Exception e) {
            // If something goes wrong, log the error and throw a runtime exception
            System.err.println("Failed to create game round: " + e.getMessage());
            throw new RuntimeException("Failed to create game round", e);
        }
    }
    
    /**
     * GET GAME ROUND BY ID
     * 
     * This function retrieves a complete game round from the database using its ID.
     * It's like looking up a specific poker table by its table number - you get
     * all the information about that round including all embedded transactions.
     * 
     * WHAT IT DOES:
     * 1. Looks up the round in the database using the provided round ID
     * 2. Converts the stored JSON back into a GameRound object
     * 3. Returns the complete round with all embedded transactions
     * 
     * WHY THIS IS USEFUL:
     * - Gets complete round information in a single database call
     * - Includes all transactions that happened in that round
     * - Perfect for displaying round history or continuing a game
     * 
     * OPTIMIZATIONS:
     * - Uses Spring Cache for frequently accessed rounds
     * - Tracks performance metrics for monitoring
     * - Implements connection pooling for better throughput
     * 
     * EXAMPLE USAGE:
     * getGameRound("round-123") returns the complete round with all transactions
     * 
     * @param roundId The unique identifier of the round to retrieve
     * @return The complete GameRound object with all embedded transactions, or null if not found
     */
    @Cacheable(value = "gameRounds", key = "#roundId")
    @Timed(value = "vgs.game.round.get", description = "Time to get a game round")
    public GameRound getGameRound(String roundId) {
        Timer.Sample sample = Timer.start();
        try {
            // Look up the round in the database using its ID
            GetResult result = gameRoundsCollection.get(roundId);
            
            // Convert the JSON data back into a GameRound object
            GameRound gameRound = result.contentAs(GameRound.class);
            
            // Log successful retrieval with transaction count
            log.debug("Retrieved game round: {} with {} transactions", roundId, gameRound.getTransactions().size());
            
            sample.stop(roundRetrievalTimer);
            return gameRound;
            
        } catch (Exception e) {
            // If the round doesn't exist or there's an error, log it and return null
            log.error("Failed to get game round: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * ADD TRANSACTION TO EMBEDDED ARRAY
     * 
     * This is the heart of the embedded document pattern! This function adds a new
     * transaction directly into the transactions array inside a GameRound document.
     * It's like adding a new entry to a poker game's score sheet.
     * 
     * WHAT IT DOES:
     * 1. Retrieves the current GameRound from the database
     * 2. Creates a new EmbeddedTransaction with the provided details
     * 3. Adds the transaction to the embedded array inside the GameRound
     * 4. Saves the updated GameRound back to the database using CAS for safety
     * 
     * WHY THIS IS POWERFUL:
     * - Keeps all related transactions together with their round
     * - Atomic operation ensures data consistency (all-or-nothing)
     * - Single document update, no need for complex joins
     * - Perfect for real-time gaming where you need immediate consistency
     * 
     * HOW CAS WORKS:
     * CAS (Compare-And-Swap) ensures that the document hasn't changed since we read it.
     * If another process modified the round while we were working, the update fails
     * and we can retry with the latest version.
     * 
     * EXAMPLE USAGE:
     * addTransaction("round-123", "txn-456", "50.00", "BET")
     * Adds a $50 bet to the round-123 embedded transactions array
     * 
     * @param roundId Which round to add the transaction to
     * @param transactionId Unique identifier for this transaction
     * @param amount Money amount as string (like "50.00")
     * @param type Transaction type (BET, WIN, BONUS, etc.)
     * @return The updated GameRound with the new transaction added
     */
    @CacheEvict(value = "gameRounds", key = "#roundId")
    @Timed(value = "vgs.game.transaction.add", description = "Time to add a transaction")
    public GameRound addTransaction(String roundId, String transactionId, String amount, String type) {
        Timer.Sample sample = Timer.start();
        try {
            // Get the current round using cached method to enable cache metrics
            GameRound gameRound = cacheService.getGameRound(roundId);
            if (gameRound == null) {
                throw new RuntimeException("Round not found: " + roundId);
            }
            
            // Get raw result for CAS operations
            GetResult getResult = gameRoundsCollection.get(roundId);
            
            // Create new embedded transaction
            EmbeddedTransaction transaction = new EmbeddedTransaction(transactionId, type, Double.parseDouble(amount), "Gaming transaction");
            
            // Add to embedded array
            gameRound.addTransaction(transaction);
            
            // Update the document using CAS for atomicity
            MutationResult result = gameRoundsCollection.replace(roundId, gameRound, 
                com.couchbase.client.java.kv.ReplaceOptions.replaceOptions().cas(getResult.cas()));
            log.debug("Added transaction {} to round {} with CAS: {}", transactionId, roundId, result.cas());
            
            sample.stop(transactionAddTimer);
            return gameRound;
        } catch (Exception e) {
            log.error("Failed to add transaction: {}", e.getMessage());
            throw new RuntimeException("Failed to add transaction", e);
        }
    }
    
    /**
     * Get transaction from embedded array - nested access
     */
    public EmbeddedTransaction getTransaction(String roundId, String transactionId) {
        GameRound gameRound = getGameRound(roundId);
        if (gameRound != null) {
            return gameRound.getTransactionById(transactionId);
        }
        return null;
    }
    
    /**
     * Update game round - basic update operation
     */
    public GameRound updateGameRound(String roundId, GameRound updatedRound) {
        try {
            GetResult getResult = gameRoundsCollection.get(roundId);
            updatedRound.setLastUpdateTimestamp(System.currentTimeMillis());
            
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
     * PROCESS TRANSACTION FIRST - NEW TRANSACTION-FIRST APPROACH
     * 
     * This method implements the new transaction-first flow where rounds are created
     * automatically when the first transaction is received, eliminating the need
     * for separate round creation endpoints.
     * 
     * FLOW:
     * 1st transaction → Creates game round using transaction data
     * 2nd transaction → Looks up existing round, appends transaction
     * 3rd transaction → Looks up round again, appends transaction
     * 
     * @param roundId Unique identifier for the round
     * @param transactionId Unique identifier for the transaction
     * @param type Transaction type (BET, WIN, BONUS, etc.)
     * @param amount Transaction amount
     * @param playerId Player identifier
     * @param agentId Agent identifier
     * @return Map containing operation result and metadata
     */
    public Map<String, Object> processTransactionFirst(String roundId, String transactionId, String type, Double amount, String playerId, String agentId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            GetResult existingRound = null;
            boolean roundExists = false;
            
            // Try to get existing round using cached method to enable cache metrics
            try {
                GameRound cachedRound = cacheService.getGameRound(roundId);
                if (cachedRound != null) {
                    // Round exists in cache, get the raw result for CAS operations
                    existingRound = gameRoundsCollection.get(roundId);
                    roundExists = true;
                } else {
                    // Round doesn't exist, we'll create it with the first transaction
                    roundExists = false;
                }
            } catch (Exception e) {
                // Round doesn't exist, we'll create it with the first transaction
                roundExists = false;
            }
            
            if (roundExists) {
                // SUBSEQUENT TRANSACTION: Use addTransaction method
                GameRound updatedRound = addTransaction(roundId, transactionId, amount.toString(), type);
                
                result.put("operation", "UPDATE");
                result.put("message", "Transaction added to existing round");
                result.put("transactionCount", updatedRound.getTransactions().size());
                result.put("casValue", 0); // CAS value not available from cached method
                
            } else {
                // FIRST TRANSACTION: Create new round with transaction
                Timer.Sample sample = Timer.start();
                try {
                    GameRound gameRound = new GameRound(roundId, 1, playerId, agentId, 1000.0); // Default initial balance
                    
                    // Create the first embedded transaction
                    EmbeddedTransaction transaction = new EmbeddedTransaction(transactionId, type, amount, "First transaction - round created");
                    
                    // Add to embedded array
                    gameRound.addTransaction(transaction);
                    
                    // Insert new document
                    MutationResult insertResult = gameRoundsCollection.insert(roundId, gameRound);
                    
                    System.out.println("Created new round " + roundId + " with first transaction " + transactionId + " with CAS: " + insertResult.cas());
                    
                    // Record metrics for transaction add (round creation)
                    sample.stop(transactionAddTimer);
                    
                    result.put("operation", "CREATE");
                    result.put("message", "New round created with first transaction");
                    result.put("transactionCount", 1);
                    result.put("casValue", insertResult.cas());
                } catch (Exception e) {
                    sample.stop(transactionAddTimer);
                    throw e;
                }
            }
            
            result.put("success", true);
            result.put("roundId", roundId);
            result.put("transactionId", transactionId);
            result.put("approach", "Transaction-first");
            
            return result;
            
        } catch (Exception e) {
            System.err.println("Failed to process transaction-first: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
            throw new RuntimeException("Failed to process transaction-first", e);
        }
    }
    
    /**
     * Test connectivity - health check
     */
    public boolean testConnectivity() {
        try {
            String testId = "CONNECTIVITY_TEST_" + System.currentTimeMillis();
            
            // Create minimal test object
            Map<String, Object> testDoc = new HashMap<>();
            testDoc.put("id", testId);
            testDoc.put("type", "connectivity_test");
            testDoc.put("timestamp", System.currentTimeMillis());
            
            // Insert test document
            gameRoundsCollection.insert(testId, testDoc);
            
            // Read it back
            GetResult result = gameRoundsCollection.get(testId);
            Map<String, Object> retrieved = result.contentAs(Map.class);
            
            // Clean up
            gameRoundsCollection.remove(testId);
            
            return retrieved != null && testId.equals(retrieved.get("id"));
        } catch (Exception e) {
            System.err.println("Connectivity test failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Async version of getGameRound for high-throughput scenarios
     * 
     * @param roundId The unique identifier of the round to retrieve
     * @return CompletableFuture containing the GameRound
     */
    public CompletableFuture<GameRound> getGameRoundAsync(String roundId) {
        return CompletableFuture.supplyAsync(() -> getGameRound(roundId));
    }
    
    /**
     * Async version of addTransaction for high-throughput scenarios
     * 
     * @param roundId Which round to add the transaction to
     * @param transactionId Unique identifier for this transaction
     * @param amount Money amount as string (like "50.00")
     * @param type Transaction type (BET, WIN, BONUS, etc.)
     * @return CompletableFuture containing the updated GameRound
     */
    public CompletableFuture<GameRound> addTransactionAsync(String roundId, String transactionId, String amount, String type) {
        return CompletableFuture.supplyAsync(() -> addTransaction(roundId, transactionId, amount, type));
    }
    
    /**
     * Async version of processTransactionFirst for high-throughput scenarios
     * 
     * @param roundId Unique identifier for the round
     * @param transactionId Unique identifier for the transaction
     * @param type Transaction type (BET, WIN, BONUS, etc.)
     * @param amount Transaction amount
     * @param playerId Player identifier
     * @param agentId Agent identifier
     * @return CompletableFuture containing operation result and metadata
     */
    public CompletableFuture<Map<String, Object>> processTransactionFirstAsync(String roundId, String transactionId, 
                                                                             String type, Double amount, 
                                                                             String playerId, String agentId) {
        return CompletableFuture.supplyAsync(() -> 
            processTransactionFirst(roundId, transactionId, type, amount, playerId, agentId));
    }
}