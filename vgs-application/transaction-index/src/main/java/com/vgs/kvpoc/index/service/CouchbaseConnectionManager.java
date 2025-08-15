package com.vgs.kvpoc.index.service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.Scope;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ====================================================================
 * VGS KV POC - Transaction Index Pattern: Couchbase Connection Manager
 * ====================================================================
 * 
 * WHAT THIS CLASS DOES:
 * This service manages Couchbase connections, providing optimized access to collections
 * with connection pooling, metrics, and error handling. It implements the recommendations
 * from the VGS Optimization Implementation Guide.
 * 
 * KEY FEATURES:
 * - Connection pooling and caching for better performance
 * - Metrics collection for monitoring connection usage
 * - Error handling and logging for database operations
 * - Automatic resource cleanup on application shutdown
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "couchbase.enabled", havingValue = "true", matchIfMissing = true)
public class CouchbaseConnectionManager {

    private final Cluster cluster;
    private final String bucketName;
    private final MeterRegistry meterRegistry;
    
    private Bucket defaultBucket;
    private final ConcurrentHashMap<String, Collection> collectionCache = new ConcurrentHashMap<>();
    
    // Metrics
    private Counter connectionRequests;
    private Counter connectionErrors;
    private Timer connectionLatency;
    private AtomicInteger activeConnections = new AtomicInteger(0);

    @Autowired
    public CouchbaseConnectionManager(Cluster cluster, 
                                    @Value("${couchbase.bucket-name:transaction_index}") String bucketName,
                                    MeterRegistry meterRegistry) {
        this.cluster = cluster;
        this.bucketName = bucketName;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void initialize() {
        try {
            // Initialize metrics
            connectionRequests = Counter.builder("vgs.couchbase.connection.requests")
                    .description("Total connection requests")
                    .register(meterRegistry);
                    
            connectionErrors = Counter.builder("vgs.couchbase.connection.errors")
                    .description("Connection errors")
                    .register(meterRegistry);
                    
            connectionLatency = Timer.builder("vgs.couchbase.connection.latency")
                    .description("Connection latency")
                    .register(meterRegistry);

            // Wait for cluster to be ready
            cluster.waitUntilReady(Duration.ofSeconds(10));
            
            // Initialize default bucket
            defaultBucket = cluster.bucket(bucketName);
            defaultBucket.waitUntilReady(Duration.ofSeconds(5));
            
            // Register active connections gauge
            meterRegistry.gauge("vgs.couchbase.connections.active", activeConnections);
            
            log.info("✅ Optimized Couchbase cluster connection established for bucket: {}", bucketName);
            
        } catch (Exception e) {
            connectionErrors.increment();
            log.error("❌ Failed to initialize Couchbase connection: ", e);
            throw new RuntimeException("Couchbase initialization failed", e);
        }
    }

    @Timed(value = "vgs.couchbase.collection.access", description = "Time to access collection")
    public Collection getCollection(String collectionName) {
        return getCollection("_default", collectionName);
    }

    @Timed(value = "vgs.couchbase.collection.access", description = "Time to access collection")
    public Collection getCollection(String scopeName, String collectionName) {
        connectionRequests.increment();
        
        String cacheKey = scopeName + "." + collectionName;
        
        return collectionCache.computeIfAbsent(cacheKey, key -> {
            try {
                activeConnections.incrementAndGet();
                Scope scope = defaultBucket.scope(scopeName);
                Collection collection = scope.collection(collectionName);
                
                log.debug("Created new collection reference: {}", cacheKey);
                return collection;
                
            } catch (Exception e) {
                connectionErrors.increment();
                activeConnections.decrementAndGet();
                log.error("Failed to get collection {}: ", cacheKey, e);
                throw new RuntimeException("Failed to access collection: " + cacheKey, e);
            }
        });
    }

    public Bucket getDefaultBucket() {
        return defaultBucket;
    }

    public Cluster getCluster() {
        return cluster;
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (cluster != null) {
                cluster.disconnect();
                log.info("🔌 Couchbase cluster disconnected");
            }
        } catch (Exception e) {
            log.error("Error during Couchbase cleanup: ", e);
        }
    }
}