
package com.vgs.kvpoc.embedded.health;

import com.couchbase.client.java.Cluster;
import org.springframework.stereotype.Component;

@Component
public class CouchbaseHealthIndicator {

    private final Cluster cluster;

    public CouchbaseHealthIndicator(Cluster cluster) {
        this.cluster = cluster;
    }

    public boolean isHealthy() {
        try {
            // Simple connectivity test - try to access the cluster
            cluster.buckets().getAllBuckets();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
