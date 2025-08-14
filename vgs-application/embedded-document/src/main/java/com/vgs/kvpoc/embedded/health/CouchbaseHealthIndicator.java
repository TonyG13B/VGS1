
package com.vgs.kvpoc.embedded.health;

import com.couchbase.client.java.Cluster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CouchbaseHealthIndicator implements HealthIndicator {
    
    private final Cluster cluster;
    
    @Autowired
    public CouchbaseHealthIndicator(Cluster cluster) {
        this.cluster = cluster;
    }
    
    @Override
    public Health health() {
        try {
            // Simple ping to test connectivity
            var result = cluster.ping();
            
            boolean isHealthy = result.endpoints().values().stream()
                .anyMatch(endpoints -> endpoints.values().stream()
                    .anyMatch(endpoint -> endpoint.state().name().equals("CONNECTED")));
            
            if (isHealthy) {
                return Health.up()
                    .withDetail("cluster", "connected")
                    .withDetail("endpoints", result.endpoints().size())
                    .build();
            } else {
                return Health.down()
                    .withDetail("cluster", "disconnected")
                    .withDetail("reason", "No healthy endpoints found")
                    .build();
            }
            
        } catch (Exception e) {
            return Health.down()
                .withDetail("cluster", "error")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
