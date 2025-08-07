<content><![CDATA[
package com.vgs.service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.kv.MutateInSpec;
import com.couchbase.client.java.kv.MutationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class EmbeddedDocumentService {
    private final Collection collection;

    public CompletableFuture<MutationResult> upsertEmbedded(String key, GameDocument doc) {
        // Size check for optimization
        if (doc.getTransactions().size() > 100) throw new IllegalStateException("Document too large");
        return CompletableFuture.supplyAsync(() -> collection.upsert(key, doc));
    }

    public CompletableFuture<GameDocument> getEmbedded(String key) {
        return CompletableFuture.supplyAsync(() -> collection.get(key).contentAs(GameDocument.class));
    }

    public CompletableFuture<MutationResult> addTransaction(String key, Map<String, Object> tx) {
        return CompletableFuture.supplyAsync(() -> collection.mutateIn(key, ArrayList.of(MutateInSpec.arrayAppend("transactions", tx))));
    }
}
]]></content>
