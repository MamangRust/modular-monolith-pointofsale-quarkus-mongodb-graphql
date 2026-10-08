package com.sanedge.cashier.seed;

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
 * Seeds the merchant context ({@code pos_merchant}): cashiers.
 * Order = 25 (after merchants).
 *
 * <p>Reads the first merchant from the merchant context and the first user from
 * the identity context, then writes a single cashier idempotently (upsert on
 * the {@code (merchantId, name)} pair).</p>
 */
public class CashierSeeder implements Seeder {

    private static final String NAME = "Cashier One";

    @Override
    public String domain() {
        return "cashier";
    }

    @Override
    public int order() {
        return 25;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        Logger log = ctx.log();
        ReactiveMongoDatabase merchant = ctx.db("merchant");
        ReactiveMongoDatabase identity = ctx.db("identity");
        ReactiveMongoCollection<Document> merchants = merchant.getCollection("merchants", Document.class);
        ReactiveMongoCollection<Document> users = identity.getCollection("users", Document.class);
        ReactiveMongoCollection<Document> cashiers = merchant.getCollection("cashiers", Document.class);

        return merchants.find().collect().first()
                .onItem().ifNull().continueWith(() -> null)
                .chain(merchantDoc -> users.find().collect().first()
                        .onItem().ifNull().continueWith(() -> null)
                        .chain(userDoc -> {
                            ObjectId merchantId = merchantDoc != null ? merchantDoc.getObjectId("_id") : null;
                            ObjectId userId = userDoc != null ? userDoc.getObjectId("_id") : null;
                            Date now = Date.from(Instant.now());
                            return cashiers.updateOne(
                                    Filters.and(Filters.eq("merchantId", merchantId), Filters.eq("name", NAME)),
                                    Updates.combine(
                                            Updates.setOnInsert("merchantId", merchantId),
                                            Updates.setOnInsert("userId", userId),
                                            Updates.setOnInsert("name", NAME),
                                            Updates.setOnInsert("createdAt", now),
                                            Updates.setOnInsert("updatedAt", now),
                                            Updates.setOnInsert("deletedAt", null)),
                                    new UpdateOptions().upsert(true)).replaceWithVoid();
                        }))
                .invoke(() -> log.info("[seeder]   Cashiers seeded: 1"));
    }
}
