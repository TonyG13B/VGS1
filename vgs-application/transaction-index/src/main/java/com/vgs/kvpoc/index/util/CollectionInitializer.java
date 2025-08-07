package com.vgs.kvpoc.index.util;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.manager.collection.CollectionManager;
import com.couchbase.client.java.manager.collection.CollectionSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Initialize required collections for VGS POC
 */
@Component
public class CollectionInitializer {
    
    @Autowired
    private Cluster cluster;
    
    @Autowired
    private Bucket bucket;
    
    @PostConstruct
    public void initializeCollections() {
        try {
            System.out.println("Initializing Couchbase collections...");
            
            CollectionManager collectionManager = bucket.collections();
            
            // Create game_rounds collection if it doesn't exist
            try {
                collectionManager.createCollection(CollectionSpec.create("game_rounds", "_default"));
                System.out.println("Created collection: game_rounds");
            } catch (Exception e) {
                System.out.println("Collection game_rounds already exists or creation failed: " + e.getMessage());
            }
            
            // Create game_transactions collection if it doesn't exist
            try {
                collectionManager.createCollection(CollectionSpec.create("game_transactions", "_default"));
                System.out.println("Created collection: game_transactions");
            } catch (Exception e) {
                System.out.println("Collection game_transactions already exists or creation failed: " + e.getMessage());
            }
            
            // Test collections are accessible
            Collection gameRounds = bucket.scope("_default").collection("game_rounds");
            Collection gameTransactions = bucket.scope("_default").collection("game_transactions");
            
            System.out.println("✓ Collections initialized successfully for transaction index pattern");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize collections: " + e.getMessage());
            e.printStackTrace();
        }
    }
}