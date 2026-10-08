package com.sanedge.transaction.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.smallrye.mutiny.Uni;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Date;

/**
 * Seeds the sales context ({@code pos_sales}): transactions (one per order).
 * Order = 50. Idempotent: upserts on the synthetic {@code _seedKey} field.
 *
 * <p>Each transaction references the order it was created from (by String
 * orderId = the order's {@code _id} hex) and the order's merchant.</p>
 */
public class TransactionSeeder implements Seeder {

    private static final String[][] TXNS = {
            {"cash", "5200000", "50000"},
            {"card", "15000000", "0"},
            {"cash", "150000", "0"},
            {"cash", "130000", "10000"},
            {"card", "200000", "0"}
    };

    @Override
    public String domain() {
        return "transaction";
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        Logger log = ctx.log();
        ReactiveMongoDatabase sales = ctx.db("sales");
        ReactiveMongoCollection<Document> orders = sales.getCollection("orders", Document.class);
        ReactiveMongoCollection<Document> transactions = sales.getCollection("transactions", Document.class);

        return orders.find().collect().asList()
                .chain(orderDocs -> {
                    Uni<Void> chain = Uni.createFrom().voidItem();
                    for (int i = 0; i < TXNS.length; i++) {
                        String[] t = TXNS[i];
                        final int idx = i;
                        Document order = orderDocs.stream()
                                .filter(d -> ("order-" + idx).equals(d.getString("_seedKey")))
                                .findFirst()
                                .orElse(null);
                        if (order == null) {
                            continue;
                        }
                        ObjectId orderObjectId = order.getObjectId("_id");
                        ObjectId merchantId = order.getObjectId("merchantId");
                        String orderId = orderObjectId != null ? orderObjectId.toHexString() : null;
                        String method = t[0];
                        int amount = Integer.parseInt(t[1]);
                        int change = Integer.parseInt(t[2]);
                        Date createdAt = order.getDate("createdAt") != null
                                ? order.getDate("createdAt") : Date.from(Instant.now());
                        String seedKey = "transaction-" + i;
                        chain = chain.chain(() -> transactions.updateOne(
                                Filters.eq("_seedKey", seedKey),
                                Updates.combine(
                                        Updates.setOnInsert("_seedKey", seedKey),
                                        Updates.setOnInsert("orderId", orderId),
                                        Updates.setOnInsert("merchantId", merchantId),
                                        Updates.setOnInsert("paymentMethod", method),
                                        Updates.setOnInsert("amount", amount),
                                        Updates.setOnInsert("changeAmount", change),
                                        Updates.setOnInsert("status", "SUCCESS"),
                                        Updates.setOnInsert("createdAt", createdAt),
                                        Updates.setOnInsert("updatedAt", createdAt),
                                        Updates.setOnInsert("deletedAt", null)),
                                new UpdateOptions().upsert(true)).replaceWithVoid());
                    }
                    return chain;
                })
                .invoke(() -> log.info("[seeder]   Transactions seeded"));
    }
}
