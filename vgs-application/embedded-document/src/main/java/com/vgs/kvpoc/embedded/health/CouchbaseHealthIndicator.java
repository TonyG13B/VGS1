
package com.vgs.kvpoc.embedded.health;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.diagnostics.PingResult;
import com.couchbase.client.java.diagnostics.ServiceType;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Component
public class CouchbaseHealthIndicator implements HealthIndicator {

    private final Cluster cluster;

    public CouchbaseHealthIndicator(Cluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public Health health() {
        try {
            // Ping the cluster with a short timeout
            PingResult pingResult = cluster.ping(
                Duration.ofSeconds(5),
                Set.of(ServiceType.KV, ServiceType.QUERY)
            );

            // Check if all services are available
            boolean isHealthy = pingResult.endpoints().values().stream()
                .allMatch(endpoints -> endpoints.values().stream()
                    .allMatch(endpoint -> endpoint.state() == com.couchbase.client.java.diagnostics.EndpointPingState.OK));

            if (isHealthy) {
                return Health.up()
                    .withDetail("cluster", "connected")
                    .withDetail("services", "kv,query")
                    .withDetail("pattern", "Embedded Document")
                    .build();
            } else {
                return Health.down()
                    .withDetail("cluster", "degraded")
                    .withDetail("reason", "Some endpoints are not responding")
                    .build();
            }

        } catch (Exception e) {
            return Health.down()
                .withDetail("cluster", "disconnected")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
