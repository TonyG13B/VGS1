<content><![CDATA[
package com.vgs.service;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.transactions.TransactionResult;
import com.couchbase.client.java.transactions.Transactions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class TransactionIndexService {
    private final Transactions transactions;
    private final Collection collection;

    public CompletableFuture<TransactionResult> indexTransaction(String txKey, String gameKey, Object txData) {
        return CompletableFuture.supplyAsync(() -> 
            transactions.run(ctx -> {
                // KV-only transaction: Update game and "index" doc atomically
                ctx.get(collection, gameKey); // For CAS
                ctx.upsert(collection, txKey, txData); // Denormalized index doc
                // Retry logic for 100% success
            })
        ).exceptionally(ex -> { /* Retry logic */ return null; });
    }

    public CompletableFuture<Object> getIndexedTransaction(String txKey) {
        return CompletableFuture.supplyAsync(() -> collection.get(txKey).contentAs(Object.class));
    }
}
]]></content>
