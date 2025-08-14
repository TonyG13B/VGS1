
package com.vgs.kvpoc.embedded.health;

import com.couchbase.client.java.Cluster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Custom Health Indicator for Couchbase
 * 
 * This class provides a simple health check for the Couchbase connection.
 * Since we're having issues with the Spring Boot Actuator health classes,
 * this is a simplified version that can be used for basic health monitoring.
 */
@Component
public class CouchbaseHealthIndicator {

    private final Cluster cluster;

    @Autowired
    public CouchbaseHealthIndicator(Cluster cluster) {
        this.cluster = cluster;
    }

    /**
     * Check if Couchbase is healthy
     * @return true if healthy, false otherwise
     */
    public boolean isHealthy() {
        try {
            // Simple ping to check if cluster is responsive
            cluster.ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get health status as string
     * @return "UP" if healthy, "DOWN" if not
     */
    public String getHealthStatus() {
        return isHealthy() ? "UP" : "DOWN";
    }
}
