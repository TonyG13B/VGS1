<content><![CDATA[
package com.vgs.config;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.env.ClusterEnvironment;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CouchbaseConfig {
    @Bean
    public Cluster cluster(MeterRegistry registry) {
        ClusterEnvironment env = ClusterEnvironment.builder()
            .kvTimeout(java.time.Duration.ofMillis(50))
            .build();
        Cluster cluster = Cluster.connect("your-capella-connection-string", "username", "password");
        // Add Micrometer for metrics
        // env.observability().meterRegistry(registry);
        return cluster;
    }
}
]]></content>
