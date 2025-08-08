package com.vgs.kvpoc.embedded.config;

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
 * VGS KV POC - Embedded Document Pattern: Couchbase Configuration
 * ====================================================================
 * 
 * WHAT THIS CLASS DOES:
 * This is the "connection setup" for the embedded document pattern. It handles
 * all the technical details of connecting to the Couchbase Capella cloud database
 * securely and efficiently.
 * 
 * WHY WE NEED THIS:
 * - Sets up secure SSL/TLS connections to the cloud database
 * - Configures timeouts and performance settings
 * - Creates the database "collections" (like tables) that we'll use
 * - Handles authentication with username/password
 * - Makes database connections available to other parts of the application
 * 
 * HOW IT WORKS:
 * 1. Reads database connection details from configuration files
 * 2. Creates a secure connection environment with SSL enabled
 * 3. Connects to the Couchbase Capella cluster using credentials
 * 4. Opens the specific database bucket we'll use
 * 5. Creates references to the collections (game_rounds, game_transactions)
 * 6. Makes these connections available to service classes
 * 
 * SECURITY FEATURES:
 * - SSL/TLS encryption for all database communications
 * - Secure credential management through Spring configuration
 * - Proper timeout handling to prevent hanging connections
 * - Connection pooling for efficient resource usage
 * 
 * COLLECTIONS CREATED:
 * - game_rounds: Stores GameRound documents with embedded transactions
 * - game_transactions: Available for utility operations (though embedded pattern doesn't use it heavily)
 * 
 * TECHNICAL DETAILS:
 * - Uses Spring Boot @Configuration for dependency injection
 * - Creates @Bean objects that can be injected into other classes
 * - Configures Couchbase Java SDK with optimal settings
 * - Implements proper connection lifecycle management
 */
@Configuration
public class SimpleCouchbaseConfig {
    
    @Value("${spring.couchbase.connection-string:couchbase://localhost}")
    private String connectionString;
    
    @Value("${spring.couchbase.username:Administrator}")
    private String username;
    
    @Value("${spring.couchbase.password:password}")
    private String password;
    
    @Value("${spring.couchbase.bucket-name:vgs-gaming}")
    private String bucketName;
    
    /**
     * CREATE SECURE CLUSTER ENVIRONMENT
     * 
     * This function sets up the secure connection environment for connecting to
     * Couchbase Capella in the cloud. It's like configuring the security settings
     * and timeouts for a safe, reliable connection.
     * 
     * WHAT IT DOES:
     * - Enables SSL/TLS encryption for all database communications
     * - Sets timeout values to prevent hanging connections
     * - Configures security policies for cloud connectivity
     * 
     * SECURITY FEATURES:
     * - SSL/TLS encryption ensures all data is encrypted in transit
     * - Proper timeout handling prevents resource leaks
     * - Optimized for cloud database connections
     * 
     * @return ClusterEnvironment configured for secure cloud connectivity
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
     * CONNECT TO COUCHBASE CAPELLA CLUSTER
     * 
     * This function establishes the actual connection to the Couchbase Capella
     * database in the cloud using the provided credentials and secure environment.
     * 
     * WHAT IT DOES:
     * - Connects to the Couchbase Capella cluster using the connection string
     * - Authenticates with the provided username and password
     * - Uses the secure environment configured above
     * 
     * @param clusterEnvironment The secure environment configuration
     * @return Connected Cluster object for database operations
     */
    @Bean
    public Cluster couchbaseCluster(ClusterEnvironment clusterEnvironment) {
        // Connect to the Couchbase Capella cluster with authentication
        return Cluster.connect(connectionString, 
            com.couchbase.client.java.ClusterOptions.clusterOptions(username, password)
                .environment(clusterEnvironment));
    }
    
    /**
     * OPEN DATABASE BUCKET
     * 
     * This function opens the specific database bucket that contains our gaming data.
     * A bucket is like a database within the Couchbase cluster.
     * 
     * @param cluster The connected Couchbase cluster
     * @return Opened bucket for gaming data operations
     */
    @Bean
    public Bucket couchbaseBucket(Cluster cluster) {
        // Open the bucket containing our gaming data
        return cluster.bucket(bucketName);
    }
    
    /**
     * CREATE GAME ROUNDS COLLECTION
     * 
     * This function creates a reference to the "game_rounds" collection within the bucket.
     * This is where we store GameRound documents with embedded transactions.
     * 
     * @param bucket The opened database bucket
     * @return Collection reference for game rounds operations
     */
    @Bean("gameRoundsCollection")
    public Collection gameRoundsCollection(Bucket bucket) {
        // Access the game_rounds collection in the default scope
        return bucket.scope("_default").collection("game_rounds");
    }
    
    /**
     * CREATE GAME TRANSACTIONS COLLECTION
     * 
     * This function creates a reference to the "game_transactions" collection.
     * While the embedded pattern stores transactions inside rounds, this collection
     * is available for utility operations and benchmarking.
     * 
     * @param bucket The opened database bucket
     * @return Collection reference for game transactions operations
     */
    @Bean("gameTransactionsCollection") 
    public Collection gameTransactionsCollection(Bucket bucket) {
        // Access the game_transactions collection in the default scope
        return bucket.scope("_default").collection("game_transactions");
    }
}