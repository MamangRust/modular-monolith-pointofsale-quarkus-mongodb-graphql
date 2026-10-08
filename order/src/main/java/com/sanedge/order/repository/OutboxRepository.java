package com.sanedge.order.repository;

import java.time.Instant;
import java.util.List;

import com.sanedge.order.entity.Outbox;
import com.sanedge.order.entity.OutboxStatus;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

@ApplicationScoped
public class OutboxRepository implements ReactivePanacheMongoRepository<Outbox> {

    public Uni<List<Outbox>> findPending(int limit) {
        return find("{ 'status': ?1 }", Sort.ascending("createdAt"), OutboxStatus.PENDING)
                .page(io.quarkus.panache.common.Page.ofSize(limit))
                .list();
    }

    public Uni<Void> markProcessed(ObjectId id) {
        return findById(id)
                .chain(outbox -> {
                    if (outbox == null) {
                        return Uni.createFrom().voidItem();
                    }
                    outbox.setStatus(OutboxStatus.PROCESSED);
                    outbox.setProcessedAt(Instant.now());
                    outbox.setLastError(null);
                    return outbox.update().replaceWithVoid();
                });
    }

    public Uni<Void> markFailed(ObjectId id, String error, int maxAttempts) {
        return findById(id)
                .chain(outbox -> {
                    if (outbox == null) {
                        return Uni.createFrom().voidItem();
                    }
                    int attempts = outbox.getAttempts() + 1;
                    outbox.setAttempts(attempts);
                    outbox.setLastError(error);
                    if (attempts >= maxAttempts) {
                        outbox.setStatus(OutboxStatus.FAILED);
                        outbox.setProcessedAt(Instant.now());
                    }
                    return outbox.update().replaceWithVoid();
                });
    }
}
