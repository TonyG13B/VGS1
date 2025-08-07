package com.vgs.kvpoc.index.controller;

import com.vgs.kvpoc.index.model.GameRound;
import com.vgs.kvpoc.index.model.TransactionIndex;
import com.vgs.kvpoc.index.service.IndexGameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VGS KV POC - Transaction Index Pattern: Game Controller
 * Basic REST API operations for gaming transaction management with index pattern
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class IndexGameController {
    
    @Autowired
    private IndexGameService gameService;
    
    /**
     * Health check endpoint - basic connectivity verification
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        
        boolean isConnected = gameService.testConnectivity();
        response.put("status", isConnected ? "UP" : "DOWN");
        response.put("pattern", "Transaction Index");
        response.put("database", "Couchbase Capella");
        response.put("ssl", "Enabled");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * DEPRECATED: Round creation endpoint removed
     * 
     * Rounds are now created automatically when the first transaction is received.
     * Use POST /api/transactions instead to process transactions.
     * 
     * This endpoint returns a 410 Gone status to indicate the resource is no longer available.
     */
    @PostMapping("/rounds")
    public ResponseEntity<Map<String, Object>> createRoundDeprecated() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Round creation endpoint has been removed");
        response.put("message", "Rounds are now created automatically when the first transaction is received");
        response.put("alternative", "Use POST /api/transactions to process transactions");
        response.put("pattern", "Transaction-first approach");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(410).body(response); // 410 Gone
    }
    
    /**
     * Get game round - basic read operation
     */
    @GetMapping("/rounds/{roundId}")
    public ResponseEntity<GameRound> getRound(@PathVariable String roundId) {
        GameRound gameRound = gameService.getGameRound(roundId);
        if (gameRound != null) {
            return ResponseEntity.ok(gameRound);
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Process Transaction - Transaction-first approach (Index Pattern)
     * 
     * This endpoint implements the new transaction-first flow for the index pattern:
     * 1st transaction → Creates game round and transaction index
     * 2nd transaction → Looks up existing round, creates new transaction index, updates round refs
     * 3rd transaction → Looks up round again, creates new transaction index, updates round refs
     * 
     * Replaces the old two-step process of creating rounds then adding transactions.
     */
    @PostMapping("/transactions")
    public ResponseEntity<Map<String, Object>> processTransaction(@RequestBody Map<String, Object> request) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Extract transaction details from request
            String roundId = (String) request.get("roundId");
            String transactionId = (String) request.get("transactionId");
            String type = (String) request.get("type");
            Double amount = Double.valueOf(request.get("amount").toString());
            String playerId = (String) request.getOrDefault("playerId", "PLAYER_" + System.currentTimeMillis());
            String description = (String) request.getOrDefault("description", "Transaction-first processing");
            
            if (roundId == null || transactionId == null || type == null || amount == null) {
                result.put("success", false);
                result.put("error", "Missing required fields: roundId, transactionId, type, amount");
                return ResponseEntity.badRequest().body(result);
            }
            
            // Process transaction using transaction-first approach
            Map<String, Object> transactionResult = gameService.processTransactionFirst(roundId, transactionId, playerId, type, amount, description);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Success response
            result.put("success", true);
            result.put("message", "Transaction processed successfully");
            result.put("transaction_id", transactionId);
            result.put("round_id", roundId);
            result.put("operation", transactionResult.get("operation")); // CREATE or UPDATE
            result.put("execution_time_ms", executionTime);
            result.put("pattern", "Transaction-first approach (Index Pattern)");
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("execution_time_ms", executionTime);
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * Get transaction index by ID - index lookup
     */
    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<TransactionIndex> getTransactionIndex(@PathVariable String transactionId) {
        TransactionIndex index = gameService.getTransactionIndex(transactionId);
        if (index != null) {
            return ResponseEntity.ok(index);
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Get all transactions for a round - reference-based query
     */
    @GetMapping("/rounds/{roundId}/transactions")
    public ResponseEntity<List<TransactionIndex>> getTransactionsForRound(@PathVariable String roundId) {
        List<TransactionIndex> transactions = gameService.getTransactionsForRound(roundId);
        return ResponseEntity.ok(transactions);
    }
    
    /**
     * Search transactions by player - index search
     */
    @GetMapping("/transactions/player/{playerId}")
    public ResponseEntity<List<TransactionIndex>> searchTransactionsByPlayer(@PathVariable String playerId) {
        List<TransactionIndex> transactions = gameService.searchTransactionsByPlayer(playerId);
        return ResponseEntity.ok(transactions);
    }
    

    /**
     * Update game round - basic update operation
     */
    @PutMapping("/rounds/{roundId}")
    public ResponseEntity<GameRound> updateRound(@PathVariable String roundId, @RequestBody GameRound gameRound) {
        try {
            GameRound updated = gameService.updateGameRound(roundId, gameRound);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Delete game round - basic delete operation
     */
    @DeleteMapping("/rounds/{roundId}")
    public ResponseEntity<Map<String, Object>> deleteRound(@PathVariable String roundId) {
        Map<String, Object> response = new HashMap<>();
        
        boolean deleted = gameService.deleteGameRound(roundId);
        response.put("deleted", deleted);
        response.put("roundId", roundId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete transaction index - basic delete operation
     */
    @DeleteMapping("/transactions/{transactionId}")
    public ResponseEntity<Map<String, Object>> deleteTransactionIndex(@PathVariable String transactionId) {
        Map<String, Object> response = new HashMap<>();
        
        boolean deleted = gameService.deleteTransactionIndex(transactionId);
        response.put("deleted", deleted);
        response.put("transactionId", transactionId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get round with indexed transactions - combined view
     */
    @GetMapping("/rounds/{roundId}/full")
    public ResponseEntity<Map<String, Object>> getRoundWithIndexedTransactions(@PathVariable String roundId) {
        GameRound gameRound = gameService.getGameRound(roundId);
        if (gameRound == null) {
            return ResponseEntity.notFound().build();
        }
        
        List<TransactionIndex> transactions = gameService.getTransactionsForRound(roundId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("round", gameRound);
        response.put("transactionRefs", gameRound.getTransactionRefs());
        response.put("indexedTransactions", transactions);
        response.put("transactionCount", transactions.size());
        response.put("complianceInfo", gameRound.getComplianceInfo());
        response.put("riskAssessment", gameRound.getRiskAssessment());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get pattern info - Updated for transaction-first approach
     */
    @GetMapping("/pattern-info")
    public ResponseEntity<Map<String, Object>> getPatternInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("pattern", "Transaction Index (Transaction-First)");
        info.put("description", "GameRound with lightweight refs + separate TransactionIndex for fast lookups using transaction-first approach");
        info.put("approach", "Transaction-first: Rounds created automatically with first transaction");
        info.put("flow", new String[]{
            "1st transaction → Creates game round and transaction index",
            "2nd transaction → Looks up existing round, creates new transaction index, updates round refs",
            "3rd transaction → Looks up round again, creates new transaction index, updates round refs"
        });
        info.put("benefits", new String[]{"Fast lookups", "Audit trail", "Compliance support", "Fraud detection", "Simplified API"});
        info.put("tradeOffs", new String[]{"Increased complexity", "Storage overhead", "Index maintenance"});
        info.put("useCase", "High-volume systems with audit and compliance requirements");
        info.put("port", 5300);
        info.put("endpoints", new String[]{
            "POST /api/transactions - Process transaction (creates round and index if needed)",
            "GET /api/rounds/{roundId} - Get round data",
            "GET /api/transactions/{transactionId} - Get transaction index",
            "POST /api/rounds - DEPRECATED (returns 410 Gone)"
        });
        info.put("deprecated_endpoints", new String[]{
            "POST /api/rounds - Use POST /api/transactions instead"
        });
        
        return ResponseEntity.ok(info);
    }
}