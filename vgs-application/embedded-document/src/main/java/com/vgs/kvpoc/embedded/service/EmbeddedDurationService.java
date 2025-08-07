package com.vgs.kvpoc.embedded.service;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.core.error.CasMismatchException;
import com.couchbase.client.core.error.DocumentNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DURATION-BASED BENCHMARK SERVICE
 * 
 * This service implements duration-based benchmarking where clients
 * continuously process transactions for a specified time period.
 * This provides better insights into sustained performance.
 */
@Service
public class EmbeddedDurationService {

    @Autowired
    private Collection gameRoundsCollection;
    
    @Autowired
    private EmbeddedGameService embeddedGameService;

    /**
     * DURATION-BASED CLIENT SIMULATION
     * 
     * Runs a single client for the specified duration, continuously processing
     * transactions and collecting detailed performance metrics.
     * 
     * @param baseDealId Base identifier for this benchmark run
     * @param clientId Unique client identifier
     * @param durationSeconds How long to run the test
     * @return Comprehensive performance metrics
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
                
                // Process transaction using instrumented service (WRITE operation)
                Map<String, Object> result = processTransactionUsingInstrumentedService(roundId, transactionType, amount);
                
                // PERFORM READ OPERATION after write to verify data persistence using instrumented service
                // This ensures we see READ activity in Capella during benchmarks and metrics are collected
                boolean readSuccess = true;
                try {
                    embeddedGameService.getGameRound(roundId);
                    // Successfully read the round data using instrumented service
                } catch (Exception readException) {
                    // READ operation failed - this should count as an error
                    readSuccess = false;
                    result.put("success", false);
                    result.put("error", "READ_OPERATION_FAILED: " + readException.getMessage());
                }
                
                // Calculate total response time for BOTH read and write operations
                long responseTime = System.currentTimeMillis() - txnStartTime;
                responseTimes.add(responseTime);
                
                if ((Boolean) result.get("success")) {
                    successfulTransactions.incrementAndGet();
                } else {
                    failedTransactions.incrementAndGet();
                }
                
                // Track conflicts and retries
                totalConflicts.addAndGet((Integer) result.getOrDefault("retryCount", 0));
                totalRetries.addAndGet((Integer) result.getOrDefault("retryCount", 0));
                
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
        
        // Conflict metrics
        clientResult.put("conflicts_resolved", totalConflicts.get());
        clientResult.put("total_retries", totalRetries.get());
        
        return clientResult;
    }
    
    /**
     * PROCESS SINGLE TRANSACTION
     * 
     * Core transaction processing with CAS-based conflict resolution
     */
    private Map<String, Object> processTransaction(String roundId, String transactionType, Double amount) {
        Map<String, Object> result = new HashMap<>();
        
        // ========================================================================
        // TEMPORARY FAILURE INJECTION FOR TESTING - REMOVE IN PRODUCTION
        // This forces realistic failure rates under high concurrency for benchmarking
        // Random 5% failure rate to demonstrate PARTIAL (ERRORS) status
        // ========================================================================
        /*
        if (Math.random() < 0.001) { // 0.1% random failure rate
            result.put("success", false);
            result.put("error", "Simulated CAS conflict failure for testing (REMOVE IN PRODUCTION)");
            result.put("retryCount", 3);
            result.put("timedOut", false);
            result.put("operationDurationMs", 45L);
            return result;
        }
        */
        // ========================================================================
        // END TEMPORARY FAILURE INJECTION - REMOVE ABOVE BLOCK IN PRODUCTION
        // ========================================================================
        
        int maxRetries = 5; // Allow up to 5 retries to handle CAS conflicts under high concurrency
        int retryCount = 0;
        long operationStartTime = System.currentTimeMillis();
        long operationTimeoutMs = 50; // 50ms timeout aligned with 20ms client requirement (2.5x buffer)
        
        while (retryCount <= maxRetries && (System.currentTimeMillis() - operationStartTime) < operationTimeoutMs) {
            try {
                String transactionId = "TXN_" + roundId + "_" + System.currentTimeMillis() + "_" + 
                                     ThreadLocalRandom.current().nextInt(1000);
                
                GetResult existingRound = null;
                long currentCas = 0;
                
                try {
                    existingRound = gameRoundsCollection.get(roundId);
                    currentCas = existingRound.cas();
                } catch (DocumentNotFoundException e) {
                    // Round doesn't exist, we'll create it
                }
                
                JsonObject roundDoc;
                
                if (existingRound != null) {
                    // Update existing round
                    roundDoc = existingRound.contentAsObject();
                    JsonArray transactions = roundDoc.getArray("transactions");
                    if (transactions == null) {
                        transactions = JsonArray.create();
                    }
                    
                    JsonObject newTransaction = JsonObject.create()
                        .put("id", transactionId)
                        .put("type", transactionType)
                        .put("amount", amount)
                        .put("timestamp", System.currentTimeMillis());
                    
                    transactions.add(newTransaction);
                    roundDoc.put("transactions", transactions);
                    roundDoc.put("lastUpdated", System.currentTimeMillis());
                    
                    gameRoundsCollection.replace(roundId, roundDoc, 
                        com.couchbase.client.java.kv.ReplaceOptions.replaceOptions().cas(currentCas));
                } else {
                    // Create new round
                    roundDoc = JsonObject.create()
                        .put("id", roundId)
                        .put("gameType", "DURATION_BENCHMARK")
                        .put("status", "ACTIVE")
                        .put("createdAt", System.currentTimeMillis())
                        .put("lastUpdated", System.currentTimeMillis());
                    
                    JsonArray transactions = JsonArray.create();
                    JsonObject newTransaction = JsonObject.create()
                        .put("id", transactionId)
                        .put("type", transactionType)
                        .put("amount", amount)
                        .put("timestamp", System.currentTimeMillis());
                    
                    transactions.add(newTransaction);
                    roundDoc.put("transactions", transactions);
                    
                    gameRoundsCollection.insert(roundId, roundDoc);
                }
                
                result.put("success", true);
                result.put("transactionId", transactionId);
                result.put("retryCount", retryCount);
                return result;
                
            } catch (CasMismatchException e) {
                retryCount++;
                if (retryCount <= maxRetries) {
                    try {
                        Thread.sleep(retryCount * 2);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                result.put("success", false);
                result.put("error", e.getMessage());
                result.put("retryCount", retryCount);
                return result;
            }
        }
        
        // Determine failure reason
        boolean timedOut = (System.currentTimeMillis() - operationStartTime) >= operationTimeoutMs;
        result.put("success", false);
        result.put("error", timedOut ? "Operation timeout after " + operationTimeoutMs + "ms (exceeds 20ms client requirement)" : "Max retries exceeded");
        result.put("retryCount", retryCount);
        result.put("timedOut", timedOut);
        result.put("operationDurationMs", System.currentTimeMillis() - operationStartTime);
        return result;
    }
    
    /**
     * Process transaction using the instrumented EmbeddedGameService
     * This ensures that all VGS metrics are properly collected
     */
    private Map<String, Object> processTransactionUsingInstrumentedService(String roundId, String transactionType, Double amount) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String transactionId = "TXN_" + roundId + "_" + System.currentTimeMillis() + "_" +
                                 ThreadLocalRandom.current().nextInt(1000);
            
            // Use the instrumented service which will update VGS metrics
            Map<String, Object> serviceResult = embeddedGameService.processTransactionFirst(
                roundId, transactionId, transactionType, amount, "benchmark_player", "benchmark_agent"
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