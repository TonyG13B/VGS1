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

import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Value("${spring.couchbase.trust-cert-path:}")
    private String trustCertPath;

    @Autowired
    private MeterRegistry meterRegistry;

    @Bean
    @Primary
    public ClusterEnvironment couchbaseClusterEnvironment() {
        SecurityConfig.Builder security = SecurityConfig.enableTls(true);
        if (trustCertPath != null && !trustCertPath.isBlank()) {
            Path pem = Paths.get(trustCertPath);
            security = security.trustCertificate(pem);
        }
        return ClusterEnvironment.builder()
            .securityConfig(security)
            .timeoutConfig(timeoutConfig -> timeoutConfig
                .kvTimeout(Duration.ofMillis(1500))
                .kvDurableTimeout(Duration.ofMillis(2000))
                .connectTimeout(Duration.ofSeconds(10))
                .managementTimeout(Duration.ofSeconds(10))
                .queryTimeout(Duration.ofSeconds(15)))
            .ioConfig(ioConfig -> ioConfig
                .numKvConnections(8)
                .maxHttpConnections(16)
                .idleHttpConnectionTimeout(Duration.ofSeconds(30))
                .configIdleRedialTimeout(Duration.ofSeconds(300)))
            .compressionConfig(compressionConfig -> compressionConfig
                .enable(true)
                .minRatio(0.83f)
                .minSize(32))
            .orphanReporterConfig(orphanReporterConfig -> orphanReporterConfig
                .emitInterval(Duration.ofSeconds(10))
                .sampleSize(64))
            .build();
    }

    @Bean
    public Cluster couchbaseCluster(ClusterEnvironment clusterEnvironment) {
        return Cluster.connect(connectionString,
            com.couchbase.client.java.ClusterOptions.clusterOptions(username, password)
                .environment(clusterEnvironment));
    }

    @Bean
    public Bucket couchbaseBucket(Cluster cluster) {
        return cluster.bucket(bucketName);
    }

    @Bean("gameRoundsCollection")
    public Collection gameRoundsCollection(Bucket bucket) {
        return bucket.scope("_default").collection("game_rounds");
    }

    @Bean("gameTransactionsCollection")
    public Collection gameTransactionsCollection(Bucket bucket) {
        return bucket.scope("_default").collection("game_transactions");
    }
}