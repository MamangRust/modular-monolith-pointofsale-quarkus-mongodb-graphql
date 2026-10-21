package com.sanedge.order.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.sanedge.order.entity.Outbox;
import com.sanedge.order.entity.OutboxStatus;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Reactive store for the order module outbox.
 *
 * <p>Claiming is a conditional update guarded by an {@code _id} + {@code status}
 * + lease filter, so concurrent application instances never publish the same row
 * twice. {@code claimToken} pins every later transition to the claim that won,
 * which stops an instance whose lease expired from overwriting the row after
 * its predecessor already moved it on.
 */
@ApplicationScoped
public class OutboxRepository implements ReactivePanacheMongoRepository<Outbox> {

    @ConfigProperty(name = "outbox.publisher.lease-seconds", defaultValue = "300")
    long leaseSeconds;

    /** Rows waiting to be published (used for the backlog gauge). */
    public Uni<Long> countPending() {
        return count("{ 'status': ?1 }", OutboxStatus.PENDING);
    }

    public Uni<List<Outbox>> findPending(int limit) {
        Instant now = Instant.now();
        Instant leaseCutoff = now.minusSeconds(Math.max(1, leaseSeconds));
        return find("{ 'status': ?1, '$and': [ "
                + "{ '$or': [ { 'nextAttemptAt': null }, { 'nextAttemptAt': { '$lte': ?2 } } ] }, "
                + "{ '$or': [ { 'claimedAt': null }, { 'claimedAt': { '$lt': ?3 } } ] } ] }",
                Sort.ascending("createdAt"), OutboxStatus.PENDING, now, leaseCutoff)
                .page(0, limit)
                .list();
    }

    /**
     * Atomically takes ownership of {@code event}. Returns {@code null} when
     * another instance won the race, in which case the caller must skip it.
     */
    public Uni<Outbox> claim(Outbox event) {
        Instant now = Instant.now();
        Instant leaseCutoff = now.minusSeconds(Math.max(1, leaseSeconds));
        String claimToken = UUID.randomUUID().toString();
        return update("{ '$set': { 'claimedAt': ?1, 'claimToken': ?2 }, '$inc': { 'attempts': 1 } }",
                now, claimToken)
                .where("{ '_id': ?1, 'status': ?2, '$and': [ "
                        + "{ '$or': [ { 'claimedAt': null }, { 'claimedAt': { '$lt': ?3 } } ] }, "
                        + "{ '$or': [ { 'nextAttemptAt': null }, { 'nextAttemptAt': { '$lte': ?4 } } ] } ] }",
                        event.id, OutboxStatus.PENDING, leaseCutoff, now)
                .chain(updated -> updated > 0 ? findById(event.id) : Uni.<Outbox>createFrom().nullItem());
    }

    public Uni<Void> markProcessed(Outbox event) {
        return update("{ '$set': { 'status': ?1, 'claimedAt': null, 'processedAt': ?2, 'lastError': null } }",
                OutboxStatus.PROCESSED, Instant.now())
                .where("{ '_id': ?1, 'status': ?2, 'claimToken': ?3 }",
                        event.id, OutboxStatus.PENDING, event.getClaimToken())
                .replaceWithVoid();
    }

    /**
     * Records a failed attempt. The row stays PENDING with a backoff delay
     * until {@code maxAttempts} is reached, after which it becomes FAILED
     * (dead letter) so a poison message cannot block the queue forever.
     */
    public Uni<Void> markFailed(Outbox event, String error, int maxAttempts) {
        int attempts = event.getAttempts() != null ? event.getAttempts() : 0;
        boolean deadLetter = attempts >= maxAttempts;
        Instant now = Instant.now();
        return update("{ '$set': { 'status': ?1, 'claimedAt': null, 'lastError': ?2, "
                + "'nextAttemptAt': ?3, 'processedAt': ?4 } }",
                deadLetter ? OutboxStatus.FAILED : OutboxStatus.PENDING,
                error,
                deadLetter ? null : now.plusSeconds(backoffSeconds(attempts)),
                deadLetter ? now : null)
                .where("{ '_id': ?1, 'status': ?2, 'claimToken': ?3 }",
                        event.id, OutboxStatus.PENDING, event.getClaimToken())
                .replaceWithVoid();
    }

    /** Exponential backoff capped at five minutes. */
    private static long backoffSeconds(int attempts) {
        return Math.min(300, 1L << Math.min(8, Math.max(0, attempts - 1)));
    }
}
