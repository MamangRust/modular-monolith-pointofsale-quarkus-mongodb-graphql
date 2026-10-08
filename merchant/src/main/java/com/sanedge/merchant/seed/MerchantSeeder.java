package com.sanedge.merchant.seed;

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
import java.util.UUID;

/**
 * Seeds the merchant context ({@code pos_merchant}): merchants.
 * Order = 20.
 *
 * <p>Reads the first user from the identity context ({@code pos_identity.users})
 * to reference as the merchant owner, then writes idempotently (upsert on the
 * natural key {@code apiKey}).</p>
 */
public class MerchantSeeder implements Seeder {

    private static final String NAME = "Merchant One";
    private static final String DESCRIPTION = "First merchant store";
    private static final String API_KEY = "pos-merchant-one-apikey";

    @Override
    public String domain() {
        return "merchant";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        Logger log = ctx.log();
        ReactiveMongoDatabase identity = ctx.db("identity");
        ReactiveMongoDatabase merchant = ctx.db("merchant");
        ReactiveMongoCollection<Document> users = identity.getCollection("users", Document.class);
        ReactiveMongoCollection<Document> merchants = merchant.getCollection("merchants", Document.class);

        return users.find().collect().first()
                .onItem().ifNull().continueWith(() -> null)
                .chain(userDoc -> {
                    ObjectId userId = userDoc != null ? userDoc.getObjectId("_id") : null;
                    Date now = Date.from(Instant.now());
                    return merchants.updateOne(
                            Filters.eq("apiKey", API_KEY),
                            Updates.combine(
                                    Updates.setOnInsert("merchantNo", UUID.randomUUID()),
                                    Updates.setOnInsert("name", NAME),
                                    Updates.setOnInsert("apiKey", API_KEY),
                                    Updates.setOnInsert("userId", userId),
                                    Updates.setOnInsert("status", "ACTIVE"),
                                    Updates.setOnInsert("createdAt", now),
                                    Updates.setOnInsert("updatedAt", now),
                                    Updates.setOnInsert("deletedAt", null)),
                            new UpdateOptions().upsert(true)).replaceWithVoid();
                })
                .invoke(() -> log.info("[seeder]   Merchants seeded: 1"));
    }
}
