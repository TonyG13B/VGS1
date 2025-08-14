
package com.vgs.kvpoc.embedded.config;

import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.core.env.TimeoutConfig;
import com.couchbase.client.core.env.IoConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;

@Configuration
public class CouchbaseConnectionPoolConfig {

    @Value("${couchbase.pool.min-endpoints:4}")
    private int minEndpoints;
    
    @Value("${couchbase.pool.max-endpoints:12}")
    private int maxEndpoints;
    
    @Value("${couchbase.timeout.connect:10s}")
    private Duration connectTimeout;
    
    @Value("${couchbase.timeout.kv:2500ms}")
    private Duration kvTimeout;

    @Bean
    public ClusterEnvironment couchbaseClusterEnvironment() {
        return ClusterEnvironment.builder()
            .timeoutConfig(TimeoutConfig.builder()
                .connectTimeout(connectTimeout)
                .kvTimeout(kvTimeout)
                .queryTimeout(Duration.ofSeconds(30))
                .build())
            .ioConfig(IoConfig.builder()
                .numKvConnections(minEndpoints)
                .maxHttpConnections(maxEndpoints)
                .idleHttpConnectionTimeout(Duration.ofSeconds(30))
                .build())
            .build();
    }
}
