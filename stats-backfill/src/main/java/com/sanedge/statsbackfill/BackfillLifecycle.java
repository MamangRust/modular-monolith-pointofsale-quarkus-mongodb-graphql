package com.sanedge.statsbackfill;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.clickhouse.ClickHouseClient;

import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * One-shot backfill of historical OLTP data into ClickHouse.
 *
 * <p>After the MongoDB migration the transactional data lives in the
 * {@code pos_sales} context: the {@code orders} and {@code transactions}
 * collections (ids are ObjectId, exposed as 24-char hex strings). The default
 * Mongo client of this app points at that context (see application.properties).
 *
 * <p>Rows are written directly to the same ClickHouse tables the stats-writer
 * populates ({@code pos.order_daily} / {@code pos.transaction_daily}), so the
 * stats-reader sees them identically to live events.
 */
@ApplicationScoped
public class BackfillLifecycle {

    private static final Logger log = LoggerFactory.getLogger(BackfillLifecycle.class);
    private static final DateTimeFormatter CH_DATETIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Inject
    ClickHouseClient clickhouse;

    @Inject
    ReactiveMongoClient mongoClient;

    @ConfigProperty(name = "quarkus.mongodb.database", defaultValue = "pos_sales")
    String databaseName;

    @ConfigProperty(name = "backfill.domains", defaultValue = "order,transaction")
    String domainsConfig;

    @ConfigProperty(name = "backfill.from-date", defaultValue = "2024-01-01")
    String fromDate;

    void onStart(@Observes StartupEvent ev) {
        log.info("=== Stats Backfill starting ===");
        Set<String> domains = Arrays.stream(domainsConfig.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());

        backfillOrders(domains)
                .chain(() -> backfillTransactions(domains))
                .invoke(() -> {
                    log.info("=== Stats Backfill completed ===");
                    Quarkus.asyncExit(0);
                })
                .onFailure().invoke(err -> {
                    log.error("Backfill FAILED", err);
                    Quarkus.asyncExit(1);
                })
                .subscribe().with(item -> {}, failure -> {});
    }

    private Instant fromInstant() {
        return LocalDate.parse(fromDate).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Uni<Void> backfillOrders(Set<String> domains) {
        if (!domains.contains("order")) {
            return Uni.createFrom().voidItem();
        }
        ReactiveMongoDatabase database = mongoClient.getDatabase(databaseName);
        Document filter = new Document("deletedAt", null)
                .append("createdAt", new Document("$gte", fromInstant()));

        AtomicInteger count = new AtomicInteger(0);
        return database.getCollection("orders", Document.class).find(filter).collect().asList()
                .chain(docs -> {
                    if (docs.isEmpty()) {
                        return Uni.createFrom().voidItem();
                    }
                    List<Map<String, Object>> rows = new ArrayList<>();
                    for (Document row : docs) {
                        String orderId = hex(row.get("_id"));
                        String eventId = "backfill:order:" + orderId;
                        String occurredAt = chDateTime(toLocalDateTime(row.get("createdAt")));
                        Map<String, Object> r = new HashMap<>();
                        r.put("event_id", eventId);
                        r.put("occurred_at", occurredAt);
                        r.put("order_id", orderId);
                        r.put("merchant_id", hex(row.get("merchantId")));
                        r.put("cashier_id", row.get("cashierId") != null ? hex(row.get("cashierId")) : null);
                        r.put("status", "completed");
                        r.put("total_amount", number(row.get("totalPrice")));
                        r.put("event_version", System.currentTimeMillis());
                        rows.add(r);
                        count.incrementAndGet();
                    }
                    return Uni.createFrom().completionStage(
                            clickhouse.insert("pos.order_daily", rows));
                })
                .invoke(() -> log.info("Backfilled {} order records", count.get()));
    }

    private Uni<Void> backfillTransactions(Set<String> domains) {
        if (!domains.contains("transaction")) {
            return Uni.createFrom().voidItem();
        }
        ReactiveMongoDatabase database = mongoClient.getDatabase(databaseName);
        Document filter = new Document("deletedAt", null)
                .append("createdAt", new Document("$gte", fromInstant()));

        AtomicInteger count = new AtomicInteger(0);
        return database.getCollection("transactions", Document.class).find(filter).collect().asList()
                .chain(docs -> {
                    if (docs.isEmpty()) {
                        return Uni.createFrom().voidItem();
                    }
                    List<Map<String, Object>> rows = new ArrayList<>();
                    for (Document row : docs) {
                        String transactionId = hex(row.get("_id"));
                        String eventId = "backfill:transaction:" + transactionId;
                        String occurredAt = chDateTime(toLocalDateTime(row.get("createdAt")));
                        Map<String, Object> r = new HashMap<>();
                        r.put("event_id", eventId);
                        r.put("occurred_at", occurredAt);
                        r.put("transaction_id", transactionId);
                        r.put("order_id", String.valueOf(row.get("orderId")));
                        r.put("merchant_id", hex(row.get("merchantId")));
                        r.put("cashier_id", null);
                        r.put("payment_method", String.valueOf(row.get("paymentMethod")));
                        r.put("status", String.valueOf(row.get("status")));
                        r.put("amount", number(row.get("amount")));
                        r.put("event_version", System.currentTimeMillis());
                        rows.add(r);
                        count.incrementAndGet();
                    }
                    return Uni.createFrom().completionStage(
                            clickhouse.insert("pos.transaction_daily", rows));
                })
                .invoke(() -> log.info("Backfilled {} transaction records", count.get()));
    }

    private static String hex(Object value) {
        if (value instanceof ObjectId objectId) {
            return objectId.toHexString();
        }
        return value == null ? "" : value.toString();
    }

    private static Object number(Object value) {
        return value == null ? 0 : value;
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof java.util.Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
        }
        if (value instanceof Instant instant) {
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        }
        return null;
    }

    private static String chDateTime(LocalDateTime value) {
        return value == null ? null : value.format(CH_DATETIME);
    }
}
