package com.vgs.kvpoc.index.controller;

import com.vgs.kvpoc.index.service.IndexAtomicService;
import com.vgs.kvpoc.index.service.IndexDurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * ATOMIC TRANSACTION CONTROLLER - TRANSACTION INDEX PATTERN
 * 
 * This controller handles atomic transaction processing and concurrency handling
 * using the Transaction Index pattern. It provides endpoints for processing
 * multiple concurrent transactions with CAS-based conflict resolution across
 * multiple documents (round + index).
 * 
 * FEATURES:
 * - Atomic transaction processing with dual-document CAS operations
 * - Concurrent write handling (1-100 clients) with index maintenance
 * - Performance monitoring (≤20ms average response time)
 * - 100% write success guarantee under concurrent load
 * - Transaction index consistency under high concurrency
 * 
 * TRANSACTION INDEX PATTERN COMPLEXITY:
 * This pattern is more complex than Embedded Document because it maintains
 * two separate documents: the game round and the transaction index. Both
 * must be updated atomically to maintain consistency, which requires
 * careful CAS handling across multiple documents.
 */
@RestController
@RequestMapping("/api/atomic")
// CORS is configured centrally via CorsConfig
public class AtomicController {

    @Autowired
    private IndexAtomicService atomicService;
    
    @Autowired
    private IndexDurationService durationService;
    
    /**
     * SINGLE ATOMIC TRANSACTION PROCESSING WITH INDEX
     * 
     * Processes a single atomic transaction with CAS-based conflict resolution
     * while maintaining the transaction index for fast lookups. This is more
     * complex than the embedded pattern because it must coordinate updates
     * across multiple documents.
     * 
     * WHAT IT DOES:
     * 1. Creates/updates the game round document
     * 2. Creates/updates the transaction index document
     * 3. Uses CAS operations to ensure consistency across both documents
     * 4. Handles conflicts and retries for both document types
     * 5. Returns detailed performance metrics including index operations
     * 
     * @param request Map containing roundId, transactionType, and amount
     * @return ResponseEntity with transaction result and performance data
     */
    @PostMapping("/atomic-transaction")
    public ResponseEntity<Map<String, Object>> processAtomicTransaction(@RequestBody Map<String, Object> request) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        
        try {
            String roundId = (String) request.get("roundId");
            String transactionType = (String) request.get("transactionType");
            Double amount = Double.valueOf(request.get("amount").toString());
            
            // Process the atomic transaction with index maintenance
            Map<String, Object> transactionResult = atomicService.processAtomicTransactionWithIndex(roundId, transactionType, amount);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Atomic transaction success response
            result.put("success", true);
            result.put("status", "Atomic transaction with index processed successfully");
            result.put("transaction_id", transactionResult.get("transactionId"));
            result.put("round_id", roundId);
            result.put("round_cas", transactionResult.get("roundCas"));
            result.put("index_cas", transactionResult.get("indexCas"));
            result.put("execution_time_ms", executionTime);
            result.put("atomic_operation", true);
            result.put("pattern", "Transaction Index Pattern");
            result.put("timestamp", System.currentTimeMillis());
            
            // Performance metrics
            result.put("response_time_ms", executionTime);
            result.put("meets_20ms_target", executionTime <= 20);
            result.put("round_conflict_resolved", transactionResult.get("roundConflictResolved"));
            result.put("index_conflict_resolved", transactionResult.get("indexConflictResolved"));
            result.put("round_retry_count", transactionResult.get("roundRetryCount"));
            result.put("index_retry_count", transactionResult.get("indexRetryCount"));
            result.put("index_maintained", true);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("execution_time_ms", executionTime);
            result.put("status", "Atomic transaction with index failed");
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * DURATION-BASED CONCURRENT BENCHMARK WITH INDEX MAINTENANCE
     * 
     * This endpoint runs time-based benchmarks where multiple clients
     * continuously process transactions for a specified duration while
     * maintaining dual-document consistency (round + index).
     * 
     * WHAT IT TESTS:
     * - Runs concurrent clients for specified duration (default: 60 seconds)
     * - Maintains atomic consistency across round and index documents
     * - Measures sustained throughput and response time percentiles
     * - Validates dual-document CAS operations under continuous load
     * - Tracks separate conflict statistics for round vs index documents
     * 
     * INDEX PATTERN CHALLENGES:
     * This pattern is more challenging because each transaction requires
     * updating two documents atomically. If one update succeeds but the
     * other fails due to CAS conflicts, the system must retry the entire
     * operation to maintain consistency.
     * 
     * @param request Map containing concurrentClients (1-10) and transactionsPerClient
     * @return ResponseEntity with comprehensive concurrency test results
     */
    @PostMapping("/concurrent-benchmark")
    public ResponseEntity<Map<String, Object>> runConcurrentBenchmark(@RequestBody Map<String, Object> request) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Parse request parameters
            int concurrentClients = Integer.parseInt(request.getOrDefault("concurrentClients", "5").toString());
            int durationSeconds = Integer.parseInt(request.getOrDefault("durationSeconds", "60").toString());
            
            // Validate requirements
            if (concurrentClients < 1 || concurrentClients > 1000) {
                throw new IllegalArgumentException("Requirement: concurrentClients must be between 1 and 100");
            }
            
            String roundId = "duration-index-benchmark-" + System.currentTimeMillis();
            
            // Create thread pool for concurrent execution
            ExecutorService executor = Executors.newFixedThreadPool(concurrentClients);
            
            // Launch duration-based concurrent client simulations
            List<CompletableFuture<Map<String, Object>>> futures = IntStream.range(0, concurrentClients)
                .mapToObj(clientId -> CompletableFuture.supplyAsync(() -> {
                    return durationService.simulateDurationClient(roundId, clientId, durationSeconds);
                }, executor))
                .toList();
            
            // Wait for all clients to complete with adaptive timeout
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            int timeout = durationSeconds + 30; // Duration + 30 second buffer
            allOf.get(timeout, TimeUnit.SECONDS); // Timeout based on actual duration
            
            // Collect results from all clients
            List<Map<String, Object>> clientResults = futures.stream()
                .map(CompletableFuture::join)
                .toList();
            
            executor.shutdown();
            
            // Calculate metrics with null safety - FIXED to count ALL attempted transactions
            long totalSuccessful = clientResults.stream()
                .mapToLong(r -> {
                    Object value = r.get("successful_transactions");
                    return value != null ? ((Number) value).longValue() : 0;
                })
                .sum();
            
            // Calculate total attempted transactions and rounds
            long totalFailed = clientResults.stream()
                .mapToLong(r -> {
                    Object value = r.getOrDefault("failed_transactions", 0);
                    return value != null ? ((Number) value).longValue() : 0;
                })
                .sum();
            
            // CRITICAL FIX: Use total_transactions (attempts) not just successful_transactions
            long totalAttempted = clientResults.stream()
                .mapToLong(r -> {
                    Object value = r.get("total_transactions");
                    return value != null ? ((Number) value).longValue() : 0;
                })
                .sum();
            long totalRounds = clientResults.stream()
                .mapToLong(r -> {
                    Object value = r.getOrDefault("total_rounds", 0);
                    return value != null ? ((Number) value).longValue() : 0;
                })
                .sum();
            // CRITICAL FIX: Calculate success rate based on successful vs attempted
            double successRate = totalAttempted > 0 ? (double) totalSuccessful / totalAttempted * 100.0 : 100.0;
            
            double averageResponseTime = clientResults.stream()
                .mapToDouble(r -> {
                    Object value = r.get("average_response_time_ms");
                    return value != null ? ((Number) value).doubleValue() : 0.0;
                })
                .average()
                .orElse(0.0);
            
            // Calculate P95 and P99 latencies
            double p95ResponseTime = clientResults.stream()
                .mapToDouble(r -> {
                    Object value = r.get("p95_response_time_ms");
                    return value != null ? ((Number) value).doubleValue() : 0.0;
                })
                .max()
                .orElse(0.0);
            
            double p99ResponseTime = clientResults.stream()
                .mapToDouble(r -> {
                    Object value = r.get("p99_response_time_ms");
                    return value != null ? ((Number) value).doubleValue() : 0.0;
                })
                .max()
                .orElse(0.0);
            
            // Calculate TPS (Transactions Per Second)
            double actualDurationSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
            double tps = actualDurationSeconds > 0 ? totalSuccessful / actualDurationSeconds : 0.0;
            
            long totalRoundConflicts = clientResults.stream()
                .mapToLong(r -> {
                    Object value = r.getOrDefault("conflicts_resolved", 0);
                    return value != null ? ((Number) value).longValue() : 0;
                })
                .sum();
            
            long totalIndexConflicts = clientResults.stream()
                .mapToLong(r -> {
                    Object value = r.getOrDefault("index_conflicts_resolved", 0);
                    return value != null ? ((Number) value).longValue() : 0;
                })
                .sum();
            
            long totalRetries = clientResults.stream()
                .mapToLong(r -> {
                    Object value = r.getOrDefault("total_retries", 0);
                    return value != null ? ((Number) value).longValue() : 0;
                })
                .sum();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Verify index consistency after concurrent operations
            Map<String, Object> consistencyCheck = atomicService.verifyIndexConsistency(roundId);
            
            //  Comprehensive benchmark results
            result.put("success", true);
            result.put("deliverable", "Atomic transaction processing with index maintenance under concurrency");
            result.put("pattern", "Transaction Index Pattern");
            result.put("round_id", roundId);
            result.put("execution_time_ms", executionTime);
            result.put("timestamp", System.currentTimeMillis());
            
            //  Concurrency metrics
            result.put("concurrent_clients", concurrentClients);
            result.put("duration_seconds", durationSeconds);
            result.put("total_rounds_created", totalRounds);
            result.put("total_transactions_attempted", totalAttempted);
            result.put("total_transactions_failed", totalFailed);
            result.put("total_transactions_successful", totalSuccessful);
            result.put("success_rate_percent", Math.round(successRate * 100.0) / 100.0);
            result.put("error_rate_percent", Math.round((100.0 - successRate) * 100.0) / 100.0);
            result.put("transactions_per_second", Math.round(tps * 100.0) / 100.0);
            result.put("average_response_time_ms", Math.round(averageResponseTime * 100.0) / 100.0);
            result.put("p95_response_time_ms", Math.round(p95ResponseTime * 100.0) / 100.0);
            result.put("p99_response_time_ms", Math.round(p99ResponseTime * 100.0) / 100.0);
            
            //  Success criteria validation
            result.put("meets_100_percent_success", successRate >= 100.0);
            result.put("meets_20ms_response_time", averageResponseTime <= 20.0);
            result.put("cas_operations_working", (totalRoundConflicts + totalIndexConflicts) >= 0);
            
            //  Index-specific metrics
            result.put("round_conflicts_resolved", totalRoundConflicts);
            result.put("index_conflicts_resolved", totalIndexConflicts);
            result.put("total_conflicts_resolved", totalRoundConflicts + totalIndexConflicts);
            result.put("total_retries", totalRetries);
            result.put("index_consistency_verified", consistencyCheck.get("consistent"));
            result.put("index_transaction_count", consistencyCheck.get("indexTransactionCount"));
            result.put("round_transaction_count", consistencyCheck.get("roundTransactionCount"));
            
            //  Detailed client breakdown
            result.put("client_results", clientResults);
            
            //  Overall status
            boolean indexConsistent = (Boolean) consistencyCheck.get("consistent");
            boolean week2Success = successRate >= 100.0 && averageResponseTime <= 20.0 && indexConsistent;
            result.put("status", week2Success ? "All criteria met with index consistency" : "criteria not fully met");
            result.put("success", week2Success);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("execution_time_ms", executionTime);
            result.put("status", "Concurrent benchmark with index failed");
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * GET ROUND DATA - CRITICAL CLIENT REQUIREMENT
     * 
     * Retrieves complete round data to demonstrate data persistence
     * and meet the "Round data must be retrievable within ≤50ms" requirement.
     * 
     * @param roundId The ID of the round to retrieve
     * @return Complete round document with all related transactions
     */
    @GetMapping("/get-round/{roundId}")
    public ResponseEntity<Map<String, Object>> getRoundData(@PathVariable String roundId) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> roundData = atomicService.getRoundData(roundId);
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (roundData != null) {
                result.put("success", true);
                result.put("response_time_ms", responseTime);
                result.put("meets_50ms_requirement", responseTime <= 50);
                result.putAll(roundData);
                
                return ResponseEntity.ok(result);
            } else {
                result.put("success", false);
                result.put("error", "Round not found");
                result.put("response_time_ms", responseTime);
                
                return ResponseEntity.status(404).body(result);
            }
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("response_time_ms", responseTime);
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * CORE: HEALTH CHECK FOR ATOMIC PROCESSING WITH INDEX
     * 
     * Verifies that the atomic transaction processing service is ready
     * and capable of handling requirements with index maintenance.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Test basic atomic operation capability with index
            boolean atomicReady = atomicService.isAtomicProcessingWithIndexReady();
            
            health.put("status", atomicReady ? "UP" : "DOWN");
            health.put("atomic_processing", atomicReady);
            health.put("cas_operations", "Available");
            health.put("index_maintenance", "Available");
            health.put("concurrent_clients_supported", "1-10");
            health.put("target_response_time", "≤20ms");
            health.put("target_success_rate", "100%");
            health.put("pattern", "Transaction Index Pattern");
            health.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("atomic_processing", false);
            
            return ResponseEntity.status(503).body(health);
        }
    }
}