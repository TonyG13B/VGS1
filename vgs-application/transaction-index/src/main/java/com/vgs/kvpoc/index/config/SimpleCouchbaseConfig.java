package com.vgs.kvpoc.index.config;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.core.env.SecurityConfig;
import com.couchbase.client.core.env.TimeoutConfig;
import com.couchbase.client.core.env.IoConfig;
import com.couchbase.client.core.env.CompressionConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * ====================================================================
 * VGS KV POC - Transaction Index Pattern: Couchbase Configuration
 * ====================================================================
 * 
 * WHAT THIS CLASS DOES:
 * This is the "connection setup" for the transaction index pattern. It handles
 * all the technical details of connecting to the Couchbase Capella cloud database
 * securely and efficiently, specifically optimized for the transaction index approach.
 * 
 * WHY WE NEED THIS:
 * - Sets up secure SSL/TLS connections to the cloud database
 * - Configures timeouts and performance settings for index operations
 * - Creates the database "collections" (like separate tables) that we'll use
 * - Handles authentication with username/password
 * - Makes database connections available to other parts of the application
 * 
 * HOW IT WORKS:
 * 1. Reads database connection details from configuration files
 * 2. Creates a secure connection environment with SSL enabled
 * 3. Connects to the Couchbase Capella cluster using credentials
 * 4. Opens the specific database bucket we'll use for transaction indexing
 * 5. Creates references to TWO collections (game_rounds and game_transactions)
 * 6. Makes these connections available to service classes
 * 
 * DIFFERENCE FROM EMBEDDED PATTERN:
 * - This configuration is optimized for the transaction index pattern
 * - Uses the same secure connection setup but emphasizes TWO collections
 * - game_rounds: Stores lightweight GameRound documents with references
 * - game_transactions: Stores detailed TransactionIndex documents
 * 
 * COLLECTIONS CREATED:
 * - game_rounds: Stores GameRound documents with lightweight transaction references
 * - game_transactions: Stores TransactionIndex documents with comprehensive transaction details
 * 
 * SECURITY FEATURES:
 * - SSL/TLS encryption for all database communications
 * - Secure credential management through Spring configuration
 * - Proper timeout handling to prevent hanging connections
 * - Connection pooling for efficient resource usage
 * 
 * TECHNICAL DETAILS:
 * - Uses Spring Boot @Configuration for dependency injection
 * - Creates @Bean objects that can be injected into other classes
 * - Configures Couchbase Java SDK with optimal settings for index operations
 * - Implements proper connection lifecycle management
 */
@Configuration
public class SimpleCouchbaseConfig {
    
    @Value("${couchbase.connection-string}")
    private String connectionString;
    
    @Value("${couchbase.username}")
    private String username;
    
    @Value("${couchbase.password}")
    private String password;
    
    @Value("${couchbase.bucket-name}")
    private String bucketName;
    
    /**
     * CREATE SECURE CLUSTER ENVIRONMENT FOR TRANSACTION INDEX PATTERN
     * 
     * This function sets up the secure connection environment for the transaction
     * index pattern, optimized for handling both lightweight rounds and detailed
     * transaction index documents.
     * 
     * @return ClusterEnvironment configured for transaction index operations
     */
    @Autowired
    private MeterRegistry meterRegistry;

    @Bean
    @Primary
    public ClusterEnvironment couchbaseClusterEnvironment() {
        return ClusterEnvironment.builder()
            // Enable SSL/TLS encryption for secure cloud connectivity
            .securityConfig(SecurityConfig.enableTls(true))
            // Optimized timeouts for t3.xlarge performance
            .timeoutConfig(timeoutConfig -> timeoutConfig
                .kvTimeout(Duration.ofMillis(1500))      // Reduced for faster operations
                .kvDurableTimeout(Duration.ofMillis(2000)) // Reduced for faster writes  
                .connectTimeout(Duration.ofSeconds(10))   // 10 seconds for initial connection
                .managementTimeout(Duration.ofSeconds(10))
                .queryTimeout(Duration.ofSeconds(15)))    // 15 seconds for query operations
            // Optimize I/O configuration for t3.xlarge (4 vCPUs, up to 5 Gbps network)
            .ioConfig(ioConfig -> ioConfig
                .numKvConnections(8)                     // Increased from 4 for better throughput
                .maxHttpConnections(16)                  // 4x vCPUs for HTTP
                .idleHttpConnectionTimeout(Duration.ofSeconds(30))
                .configIdleRedialTimeout(Duration.ofSeconds(300)))
            // Enable compression for better network utilization
            .compressionConfig(compressionConfig -> compressionConfig
                .enable(true)
                .minRatio(0.83f)
                .minSize(32))
            // Connection pool optimization
            .orphanReporterConfig(orphanReporterConfig -> orphanReporterConfig
                .emitInterval(Duration.ofSeconds(10))
                .sampleSize(64))
            // Metrics integration for monitoring
            // Removed metrics integration due to compatibility issues
            .build();
    }
    
    /**
     * CONNECT TO COUCHBASE CAPELLA CLUSTER FOR TRANSACTION INDEX PATTERN
     * 
     * Establishes the connection to the Couchbase Capella database specifically
     * for the transaction index pattern operations.
     * 
     * @param clusterEnvironment The secure environment configuration
     * @return Connected Cluster object for transaction index operations
     */
    @Bean
    public Cluster couchbaseCluster(ClusterEnvironment clusterEnvironment) {
        // Connect to the Couchbase Capella cluster with authentication
        return Cluster.connect(connectionString, 
            com.couchbase.client.java.ClusterOptions.clusterOptions(username, password)
                .environment(clusterEnvironment));
    }
    
    /**
     * OPEN TRANSACTION INDEX BUCKET
     * 
     * Opens the specific database bucket that contains our transaction index data.
     * This bucket will contain both lightweight rounds and detailed transaction indexes.
     * 
     * @param cluster The connected Couchbase cluster
     * @return Opened bucket for transaction index operations
     */
    @Bean
    public Bucket couchbaseBucket(Cluster cluster) {
        // Open the bucket containing our transaction index data
        return cluster.bucket(bucketName);
    }
    
    /**
     * CREATE GAME ROUNDS COLLECTION (FOR LIGHTWEIGHT ROUNDS)
     * 
     * Creates a reference to the "game_rounds" collection for the transaction index pattern.
     * This collection stores lightweight GameRound documents with transaction references.
     * 
     * @param bucket The opened database bucket
     * @return Collection reference for lightweight game rounds
     */
    @Bean("gameRoundsCollection")
    public Collection gameRoundsCollection(Bucket bucket) {
        // Access the game_rounds collection for lightweight round documents
        return bucket.scope("_default").collection("game_rounds");
    }
    
    /**
     * CREATE GAME TRANSACTIONS COLLECTION (FOR DETAILED INDEXES)
     * 
     * Creates a reference to the "game_transactions" collection for the transaction index pattern.
     * This collection stores detailed TransactionIndex documents with comprehensive transaction data.
     * 
     * @param bucket The opened database bucket
     * @return Collection reference for detailed transaction indexes
     */
    @Bean("gameTransactionsCollection") 
    public Collection gameTransactionsCollection(Bucket bucket) {
        // Access the game_transactions collection for detailed transaction indexes
        return bucket.scope("_default").collection("game_transactions");
    }
}