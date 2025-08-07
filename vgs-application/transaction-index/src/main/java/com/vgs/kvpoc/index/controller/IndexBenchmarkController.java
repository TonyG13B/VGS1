package com.vgs.kvpoc.index.controller;

import com.vgs.kvpoc.index.service.IndexBenchmarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * VGS KV POC - Transaction Index Pattern: Benchmark Controller
 * complexity level - basic benchmark endpoints for index pattern
 */
@RestController
@RequestMapping("/api/benchmark")
@CrossOrigin(origins = "*")
public class IndexBenchmarkController {
    
    @Autowired
    private IndexBenchmarkService benchmarkService;
    
    /**
     * Run transaction index pattern benchmark
     */
    @PostMapping("/index")
    public ResponseEntity<Map<String, Object>> runIndexBenchmark(
            @RequestParam(defaultValue = "30") int durationSeconds) {
        
        try {
            // Call actual benchmark service that performs real database operations
            Map<String, Object> results = benchmarkService.runBenchmark(durationSeconds);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            // Log error but return a failure response instead of crashing
            System.err.println("Benchmark failed: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResults = new HashMap<>();
            errorResults.put("success", false);
            errorResults.put("status", "FAILED");
            errorResults.put("pattern", "Transaction Index");
            errorResults.put("testType", "index_benchmark");
            errorResults.put("error", e.getMessage());
            errorResults.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(errorResults);
        }
    }
    
    /**
     * Test connectivity for transaction index pattern
     */
    @GetMapping("/connectivity")
    public ResponseEntity<Map<String, Object>> testConnectivity() {
        Map<String, Object> results = benchmarkService.runConnectivityTest();
        return ResponseEntity.ok(results);
    }
    
    /**
     * Test transaction index operations
     */
    @PostMapping("/test-index")
    public ResponseEntity<Map<String, Object>> testIndexOperations() {
        Map<String, Object> results = benchmarkService.testIndexOperations();
        return ResponseEntity.ok(results);
    }
}