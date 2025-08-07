package com.vgs.kvpoc.index.config;

import com.vgs.kvpoc.index.service.CouchbaseConnectionManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
@EnableScheduling
public class CouchbaseMetricsConfig {

    private final MeterRegistry meterRegistry;
    private final CouchbaseConnectionManager connectionManager;
    
    // Metrics
    private Timer kvOperationTimer;
    private Timer networkLatencyTimer;
    
    // Atomic counters for connection pool metrics
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong maxConnections = new AtomicLong(100); // Default max

    @Autowired
    public CouchbaseMetricsConfig(MeterRegistry meterRegistry, 
                                 CouchbaseConnectionManager connectionManager) {
        this.meterRegistry = meterRegistry;
        this.connectionManager = connectionManager;
    }

    @PostConstruct
    public void initializeMetrics() {
        // KV Operation metrics
        this.kvOperationTimer = Timer.builder("couchbase.kv.ops.latency")
            .description("Couchbase KV operation latency")
            .register(meterRegistry);

        // Connection pool metrics
        Gauge.builder("couchbase.connection.pool.active", this, CouchbaseMetricsConfig::getActiveConnectionCount)
            .description("Active connections in pool")
            .register(meterRegistry);
            
        Gauge.builder("couchbase.connection.pool.max", this, CouchbaseMetricsConfig::getMaxConnectionCount)
            .description("Maximum connections in pool")
            .register(meterRegistry);

        // Network latency timer
        this.networkLatencyTimer = Timer.builder("couchbase.network.latency")
            .description("Network latency to Couchbase Capella")
            .register(meterRegistry);
    }

    // Scheduled task to update connection pool metrics
    @Scheduled(fixedRate = 10000) // Every 10 seconds
    public void updateConnectionPoolMetrics() {
        try {
            // Simulate connection pool monitoring
            // In a real implementation, you would get these from the Couchbase SDK
            long currentActive = estimateActiveConnections();
            activeConnections.set(currentActive);
            
            // Update max connections based on configuration
            maxConnections.set(100); // This should come from your connection pool config
            
        } catch (Exception e) {
            // Log error but don't fail
            System.err.println("Failed to update connection pool metrics: " + e.getMessage());
        }
    }

    // Scheduled task to measure network latency
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void measureNetworkLatency() {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // Perform a simple ping operation to Couchbase
            // This is a lightweight operation to measure network latency
            connectionManager.getCluster().ping();
            
            sample.stop(networkLatencyTimer);
            
        } catch (Exception e) {
            sample.stop(networkLatencyTimer);
            System.err.println("Failed to measure network latency: " + e.getMessage());
        }
    }

    // Method to record KV operation metrics (call this from your service methods)
    public void recordKvOperation(String operation, long durationMs, boolean success) {
        kvOperationTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        Counter.builder("couchbase.kv.ops.total")
            .tag("operation", operation)
            .tag("status", success ? "success" : "error")
            .register(meterRegistry)
            .increment();
    }

    // Helper methods for connection pool metrics
    private double getActiveConnectionCount() {
        return activeConnections.get();
    }

    private double getMaxConnectionCount() {
        return maxConnections.get();
    }

    private long estimateActiveConnections() {
        // This is a simplified estimation
        // In a real implementation, you would get this from the Couchbase SDK
        try {
            // You could use cluster diagnostics or connection pool statistics
            return Math.min(50, Math.max(1, System.currentTimeMillis() % 20));
        } catch (Exception e) {
            return 1; // Default fallback
        }
    }
}