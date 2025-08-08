package com.vgs.kvpoc.embedded.controller;

import com.vgs.kvpoc.embedded.service.EmbeddedBenchmarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * ====================================================================
 * VGS KV POC - Embedded Document Pattern: Benchmark Controller
 * ====================================================================
 * 
 * WHAT THIS CLASS DOES:
 * This is the "web interface" for the Embedded Document Pattern benchmarking system.
 * It receives requests from the dashboard (or other clients) and runs performance tests
 * on the embedded document pattern to see how fast it can process gaming transactions.
 * 
 * WHY WE NEED THIS:
 * - Provides a way for external tools (like the Python dashboard) to trigger tests
 * - Converts web requests into actual database benchmark operations
 * - Returns performance results in a format that's easy to analyze
 * - Handles errors gracefully so the system doesn't crash during testing
 * 
 * HOW IT WORKS:
 * 1. Dashboard sends HTTP request to run a benchmark
 * 2. This controller receives the request and validates parameters
 * 3. Calls the BenchmarkService to run actual database operations
 * 4. Collects performance metrics (speed, success rate, errors)
 * 5. Returns results as JSON for the dashboard to display
 * 
 * REAL-WORLD EXAMPLE:
 * Dashboard: "Test embedded pattern for 30 seconds"
 * Controller: "Starting benchmark..." → calls service → collects results
 * Returns: {"pattern": "Embedded", "operations": 1500, "avgTime": "12ms", "success": true}
 * 
 * BENEFITS FOR TESTING:
 * - Easy to trigger tests from any client (dashboard, scripts, etc.)
 * - Provides standardized benchmark results
 * - Handles multiple test types (connectivity, performance, operations)
 * - Error handling prevents system crashes during testing
 * 
 * TECHNICAL DETAILS:
 * - Uses Spring Boot @RestController for web API endpoints
 * - CORS is configured centrally via CorsConfig
 * - Returns JSON responses that can be easily parsed
 */
@RestController
@RequestMapping("/api/benchmark")
public class EmbeddedBenchmarkController {
    
    @Autowired
    private EmbeddedBenchmarkService benchmarkService;
    
    /**
     * RUN EMBEDDED DOCUMENT PATTERN BENCHMARK
     * 
     * This is the main function that runs performance tests on the embedded document pattern.
     * Think of it like running a stress test on a car engine to see how fast it can go.
     * 
     * WHAT IT DOES:
     * 1. Receives a request to run a benchmark test
     * 2. Calls the service layer to perform actual database operations
     * 3. Measures performance metrics like speed and success rate
     * 4. Returns detailed results for analysis
     * 
     * HOW THE TEST WORKS:
     * - Creates many fake game rounds with embedded transactions
     * - Measures how long each database operation takes
     * - Counts successful vs failed operations
     * - Calculates operations per second and average response time
     * 
     * EXAMPLE USAGE:
     * POST /api/benchmark/embedded?durationSeconds=60
     * This would run the test for 60 seconds and return performance metrics
     * 
     * @param durationSeconds How long to run the test (default is 30 seconds)
     * @return JSON response with performance metrics and test results
     */
    @PostMapping("/embedded")
    public ResponseEntity<Map<String, Object>> runEmbeddedBenchmark(
            @RequestParam(defaultValue = "30") int durationSeconds) {
        
        try {
            // Call the actual benchmark service that performs real database operations
            // This is where the magic happens - real transactions with Couchbase
            Map<String, Object> results = benchmarkService.runBenchmark(durationSeconds);
            
            // Return successful results as HTTP 200 OK with JSON data
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            // If anything goes wrong during the benchmark, handle it gracefully
            // Log the error for debugging but don't crash the server
            System.err.println("Benchmark failed: " + e.getMessage());
            e.printStackTrace();
            
            // Create a standardized error response that the dashboard can understand
            Map<String, Object> errorResults = new HashMap<>();
            errorResults.put("success", false);                    // Clearly mark as failed
            errorResults.put("status", "FAILED");                  // Status for easy checking
            errorResults.put("pattern", "Embedded Document");      // Which pattern failed
            errorResults.put("testType", "embedded_benchmark");    // Type of test that failed
            errorResults.put("error", e.getMessage());             // Human-readable error message
            errorResults.put("timestamp", System.currentTimeMillis()); // When the error occurred
            
            // Return HTTP 500 (Internal Server Error) with error details
            return ResponseEntity.status(500).body(errorResults);
        }
    }
    
    /**
     * TEST CONNECTIVITY FOR EMBEDDED PATTERN
     * 
     * This function checks if the embedded document pattern can connect to
     * the Couchbase database successfully. It's like checking if your phone
     * can connect to WiFi before trying to browse the internet.
     * 
     * WHAT IT DOES:
     * - Tests database connection to Couchbase Capella
     * - Verifies SSL/TLS security is working
     * - Checks if required collections exist
     * - Returns connection status and any error details
     * 
     * WHY THIS IS USEFUL:
     * - Quick way to verify system is properly configured
     * - Helps diagnose connection problems
     * - Confirms database credentials are working
     * 
     * @return JSON response with connectivity status and details
     */
    @GetMapping("/connectivity")
    public ResponseEntity<Map<String, Object>> testConnectivity() {
        // Call the service to test database connectivity
        Map<String, Object> results = benchmarkService.runConnectivityTest();
        // Return the results as JSON
        return ResponseEntity.ok(results);
    }
    
    /**
     * TEST EMBEDDED DOCUMENT OPERATIONS
     * 
     * This function runs a quick test of basic embedded document operations
     * to make sure the pattern is working correctly. It's like doing a quick
     * engine check before a long road trip.
     * 
     * WHAT IT TESTS:
     * - Creating a GameRound with embedded transactions
     * - Reading the round back from the database
     * - Updating the round with new transactions
     * - Deleting test data when finished
     * 
     * WHY THIS IS IMPORTANT:
     * - Verifies all CRUD operations work (Create, Read, Update, Delete)
     * - Tests the core embedded document functionality
     * - Helps identify any data model issues
     * 
     * @return JSON response with operation test results
     */
    @PostMapping("/test-embedded")
    public ResponseEntity<Map<String, Object>> testEmbeddedOperations() {
        // Call the service to test basic embedded document operations
        Map<String, Object> results = benchmarkService.testEmbeddedOperations();
        // Return the results as JSON
        return ResponseEntity.ok(results);
    }
}