package com.vgs.kvpoc.index.service;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.core.error.CasMismatchException;
import com.couchbase.client.core.error.DocumentNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TRANSACTION INDEX DURATION-BASED BENCHMARK SERVICE
 * 
 * This service implements duration-based benchmarking for the Transaction Index pattern,
 * where clients continuously process transactions for a specified time period while
 * maintaining separate index documents for fast lookups.
 */
@Service
public class IndexDurationService {

    @Autowired
    private Collection gameRoundsCollection;
    
    @Autowired
    private Collection gameTransactionsCollection;
    
    @Autowired
    private IndexGameService indexGameService;

    /**
     * DURATION-BASED CLIENT SIMULATION WITH INDEX MAINTENANCE
     * 
     * Runs a single client for the specified duration, continuously processing
     * transactions with dual-document atomic operations and collecting detailed
     * performance metrics.
     */
    public Map<String, Object> simulateDurationClient(String baseDealId, int clientId, int durationSeconds) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationSeconds * 1000L);
        
        Map<String, Object> clientResult = new HashMap<>();
        List<Long> responseTimes = new ArrayList<>();
        AtomicLong transactionCount = new AtomicLong(0);
        AtomicLong successfulTransactions = new AtomicLong(0);
        AtomicLong failedTransactions = new AtomicLong(0);
        AtomicLong totalConflicts = new AtomicLong(0);
        AtomicLong totalRetries = new AtomicLong(0);
        AtomicLong indexConflicts = new AtomicLong(0);
        AtomicLong roundCount = new AtomicLong(0);
        
        // Transaction types for realistic simulation
        String[] transactionTypes = {"BET", "WIN", "BONUS", "REFUND", "CASHOUT"};
        
        // Gaming compliance: 3-5 transactions per round
        int currentRoundTransactions = 0;
        int transactionsPerRound = ThreadLocalRandom.current().nextInt(3, 6);
        long currentRoundNumber = 0;
        
        while (System.currentTimeMillis() < endTime) {
            try {
                long txnStartTime = System.currentTimeMillis();
                
                // Start new round when current round is complete
                if (currentRoundTransactions == 0) {
                    currentRoundNumber++;
                    transactionsPerRound = ThreadLocalRandom.current().nextInt(3, 6);
                    roundCount.incrementAndGet();
                }
                
                // Generate realistic transaction data
                String transactionType = transactionTypes[ThreadLocalRandom.current().nextInt(transactionTypes.length)];
                double amount = ThreadLocalRandom.current().nextDouble(1.0, 1000.0);
                String roundId = baseDealId + "_client_" + clientId + "_round_" + currentRoundNumber;
                
                // Process transaction with index (WRITE operations)
                Map<String, Object> result = processTransactionUsingInstrumentedService(roundId, transactionType, amount);
                
                // PERFORM READ OPERATIONS after write to verify data persistence
                // This ensures we see READ activity in Capella during benchmarks
                boolean readSuccess = true;
                int readOperationsCompleted = 0;
                try {
                    indexGameService.getGameRound(roundId);
                    readOperationsCompleted++;
                    
                    // Also read from transaction index if transaction was successful
                    if ((Boolean) result.get("success")) {
                        String transactionId = (String) result.get("transactionId");
                        if (transactionId != null) {
                            GetResult txnReadResult = gameTransactionsCollection.get(transactionId);
                            readOperationsCompleted++;
                        }
                    }
                } catch (Exception readException) {
                    // READ operation failed - this should count as an error
                    readSuccess = false;
                    result.put("success", false);
                    result.put("error", "READ_OPERATION_FAILED: " + readException.getMessage());
                    result.put("read_operations_completed", readOperationsCompleted);
                }
                
                // Calculate total response time for ALL operations (write + read)
                long responseTime = System.currentTimeMillis() - txnStartTime;
                responseTimes.add(responseTime);
                
                if ((Boolean) result.get("success")) {
                    successfulTransactions.incrementAndGet();
                } else {
                    failedTransactions.incrementAndGet();
                }
                
                // Track conflicts and retries for both documents
                totalConflicts.addAndGet((Integer) result.getOrDefault("roundRetryCount", 0));
                indexConflicts.addAndGet((Integer) result.getOrDefault("indexRetryCount", 0));
                totalRetries.addAndGet((Integer) result.getOrDefault("totalRetries", 0));
                
                transactionCount.incrementAndGet();
                currentRoundTransactions++;
                
                // Complete round when we reach the target transaction count
                if (currentRoundTransactions >= transactionsPerRound) {
                    currentRoundTransactions = 0;
                }
                
                // Removed artificial delay for accurate performance measurement
                // Thread.sleep(1);
                
            } catch (Exception e) {
                failedTransactions.incrementAndGet();
                transactionCount.incrementAndGet();
            }
        }
        
        long actualDuration = System.currentTimeMillis() - startTime;
        
        // Calculate comprehensive metrics
        clientResult.put("client_id", clientId);
        clientResult.put("actual_duration_ms", actualDuration);
        clientResult.put("total_rounds", roundCount.get());
        clientResult.put("total_transactions", transactionCount.get());
        clientResult.put("successful_transactions", successfulTransactions.get());
        clientResult.put("failed_transactions", failedTransactions.get());
        clientResult.put("success_rate_percent", 
            transactionCount.get() > 0 ? (double) successfulTransactions.get() / transactionCount.get() * 100.0 : 0.0);
        
        // Response time metrics
        if (!responseTimes.isEmpty()) {
            Collections.sort(responseTimes);
            clientResult.put("min_response_time_ms", responseTimes.get(0));
            clientResult.put("max_response_time_ms", responseTimes.get(responseTimes.size() - 1));
            clientResult.put("average_response_time_ms", 
                responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0));
            
            // Enhanced Percentiles for comprehensive latency analysis
            int p95Index = (int) (responseTimes.size() * 0.95);
            int p99Index = (int) (responseTimes.size() * 0.99);
            int p99_5Index = (int) (responseTimes.size() * 0.995);
            int p99_9Index = (int) (responseTimes.size() * 0.999);
            
            clientResult.put("p95_response_time_ms", responseTimes.get(Math.min(p95Index, responseTimes.size() - 1)));
            clientResult.put("p99_response_time_ms", responseTimes.get(Math.min(p99Index, responseTimes.size() - 1)));
            clientResult.put("p99_5_response_time_ms", responseTimes.get(Math.min(p99_5Index, responseTimes.size() - 1)));
            clientResult.put("p99_9_response_time_ms", responseTimes.get(Math.min(p99_9Index, responseTimes.size() - 1)));
        }
        
        // Throughput metrics
        double actualDurationSeconds = actualDuration / 1000.0;
        clientResult.put("transactions_per_second", 
            actualDurationSeconds > 0 ? transactionCount.get() / actualDurationSeconds : 0.0);
        
        // Index-specific conflict metrics
        clientResult.put("conflicts_resolved", totalConflicts.get());
        clientResult.put("index_conflicts_resolved", indexConflicts.get());
        clientResult.put("total_retries", totalRetries.get());
        
        return clientResult;
    }
    
    /**
     * PROCESS TRANSACTION WITH INDEX MAINTENANCE
     * 
     * Core transaction processing with dual-document CAS operations
     */
    private Map<String, Object> processTransactionWithIndex(String roundId, String transactionType, Double amount) {
        Map<String, Object> result = new HashMap<>();
        
        // ========================================================================
        // TEMPORARY FAILURE INJECTION FOR TESTING - REMOVE IN PRODUCTION
        // This forces realistic failure rates under high concurrency for benchmarking
        // Random 8% failure rate (higher for dual-document complexity)
        // ========================================================================
        /*
        if (Math.random() < 0.001) { // 0.1% random failure rate for dual-document operations
            result.put("success", false);
            result.put("error", "Simulated dual-document CAS conflict for testing (REMOVE IN PRODUCTION)");
            result.put("roundRetryCount", 2);
            result.put("indexRetryCount", 1);
            result.put("totalRetries", 3);
            result.put("timedOut", false);
            result.put("operationDurationMs", 48L);
            return result;
        }
        */
        // ========================================================================
        // END TEMPORARY FAILURE INJECTION - REMOVE ABOVE BLOCK IN PRODUCTION
        // ========================================================================
        
        int maxRetries = 5; // Allow up to 5 retries to handle CAS conflicts under high concurrency
        int roundRetryCount = 0;
        int indexRetryCount = 0;
        long operationStartTime = System.currentTimeMillis();
        long operationTimeoutMs = 50; // 50ms timeout aligned with 20ms client requirement (2.5x buffer)
        
        while (roundRetryCount <= maxRetries && indexRetryCount <= maxRetries && 
               (System.currentTimeMillis() - operationStartTime) < operationTimeoutMs) {
            try {
                String transactionId = "TXN_" + roundId + "_" + System.currentTimeMillis() + "_" + 
                                     ThreadLocalRandom.current().nextInt(1000);
                String indexId = "IDX_" + roundId;
                
                // Step 1: Handle Round Document
                GetResult existingRound = null;
                long roundCas = 0;
                
                try {
                    existingRound = gameRoundsCollection.get(roundId);
                    roundCas = existingRound.cas();
                } catch (DocumentNotFoundException e) {
                    // Round doesn't exist, we'll create it
                }
                
                JsonObject roundDoc;
                if (existingRound != null) {
                    // Update existing round
                    roundDoc = existingRound.contentAsObject();
                    roundDoc.put("lastUpdated", System.currentTimeMillis());
                    roundDoc.put("transactionCount", roundDoc.getInt("transactionCount") + 1);
                    
                    gameRoundsCollection.replace(roundId, roundDoc, 
                        com.couchbase.client.java.kv.ReplaceOptions.replaceOptions().cas(roundCas));
                } else {
                    // Create new round
                    roundDoc = JsonObject.create()
                        .put("id", roundId)
                        .put("gameType", "DURATION_INDEX_BENCHMARK")
                        .put("status", "ACTIVE")
                        .put("createdAt", System.currentTimeMillis())
                        .put("lastUpdated", System.currentTimeMillis())
                        .put("transactionCount", 1);
                    
                    gameRoundsCollection.insert(roundId, roundDoc);
                }
                
                // Step 2: Handle Index Document
                GetResult existingIndex = null;
                long indexCas = 0;
                
                try {
                    existingIndex = gameTransactionsCollection.get(indexId);
                    indexCas = existingIndex.cas();
                } catch (DocumentNotFoundException e) {
                    // Index doesn't exist, we'll create it
                }
                
                JsonObject indexDoc;
                if (existingIndex != null) {
                    // Update existing index
                    indexDoc = existingIndex.contentAsObject();
                    indexDoc.put("lastTransactionId", transactionId);
                    indexDoc.put("lastUpdated", System.currentTimeMillis());
                    indexDoc.put("transactionCount", indexDoc.getInt("transactionCount") + 1);
                    
                    gameTransactionsCollection.replace(indexId, indexDoc, 
                        com.couchbase.client.java.kv.ReplaceOptions.replaceOptions().cas(indexCas));
                } else {
                    // Create new index
                    indexDoc = JsonObject.create()
                        .put("roundId", roundId)
                        .put("indexType", "TRANSACTION_INDEX")
                        .put("lastTransactionId", transactionId)
                        .put("lastTransactionType", transactionType)
                        .put("lastAmount", amount)
                        .put("createdAt", System.currentTimeMillis())
                        .put("lastUpdated", System.currentTimeMillis())
                        .put("transactionCount", 1);
                    
                    gameTransactionsCollection.insert(indexId, indexDoc);
                }
                
                result.put("success", true);
                result.put("transactionId", indexId);  // Return index ID for read operations
                result.put("actualTransactionId", transactionId);
                result.put("roundRetryCount", roundRetryCount);
                result.put("indexRetryCount", indexRetryCount);
                result.put("totalRetries", roundRetryCount + indexRetryCount);
                return result;
                
            } catch (CasMismatchException e) {
                if (e.getMessage().contains(roundId)) {
                    roundRetryCount++;
                } else {
                    indexRetryCount++;
                }
                
                if (roundRetryCount <= maxRetries && indexRetryCount <= maxRetries) {
                    try {
                        Thread.sleep((roundRetryCount + indexRetryCount) * 2);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                result.put("success", false);
                result.put("error", e.getMessage());
                result.put("roundRetryCount", roundRetryCount);
                result.put("indexRetryCount", indexRetryCount);
                result.put("totalRetries", roundRetryCount + indexRetryCount);
                return result;
            }
        }
        
        // Determine failure reason
        boolean timedOut = (System.currentTimeMillis() - operationStartTime) >= operationTimeoutMs;
        result.put("success", false);
        result.put("error", timedOut ? "Dual-document operation timeout after " + operationTimeoutMs + "ms (exceeds 20ms client requirement)" : "Max retries exceeded for dual-document operation");
        result.put("roundRetryCount", roundRetryCount);
        result.put("indexRetryCount", indexRetryCount);
        result.put("totalRetries", roundRetryCount + indexRetryCount);
        result.put("timedOut", timedOut);
        result.put("operationDurationMs", System.currentTimeMillis() - operationStartTime);
        return result;
    }
    
    /**
     * Process transaction using the instrumented IndexGameService
     * This ensures that all VGS metrics are properly collected
     */
    private Map<String, Object> processTransactionUsingInstrumentedService(String roundId, String transactionType, Double amount) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String transactionId = "TXN_" + roundId + "_" + System.currentTimeMillis() + "_" +
                                 ThreadLocalRandom.current().nextInt(1000);
            
            // Use the instrumented service which will update VGS metrics
            Map<String, Object> serviceResult = indexGameService.processTransactionFirst(
                roundId, transactionId, "benchmark_player", transactionType, amount, "Gaming transaction"
            );
            
            // Convert service result to expected format
            result.put("success", serviceResult.get("success"));
            result.put("transactionId", transactionId);
            result.put("retryCount", 0); // The service handles retries internally
            
            if (serviceResult.containsKey("error")) {
                result.put("error", serviceResult.get("error"));
            }
            
            return result;
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("retryCount", 0);
            return result;
        }
    }
}