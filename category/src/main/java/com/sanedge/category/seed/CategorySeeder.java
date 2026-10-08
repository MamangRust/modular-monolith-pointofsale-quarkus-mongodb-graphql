package com.sanedge.category.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.smallrye.mutiny.Uni;
import org.bson.Document;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Seeds the catalog context ({@code pos_catalog}): categories.
 * Order = 30.
 *
 * <p>Idempotent: upserts on the natural key {@code slugCategory}.</p>
 */
public class CategorySeeder implements Seeder {

    private static final List<String[]> CATEGORIES = List.of(
            new String[]{"Electronics", "Electronic devices and gadgets", "electronics"},
            new String[]{"Clothing", "Apparel and accessories", "clothing"},
            new String[]{"Food & Beverage", "Edible items and drinks", "food-beverage"},
            new String[]{"Home & Garden", "Home improvement and garden supplies", "home-garden"},
            new String[]{"Sports", "Sports equipment and accessories", "sports"},
            new String[]{"Books", "Books and publications", "books"},
            new String[]{"Toys", "Children's toys and games", "toys"},
            new String[]{"Health", "Health and wellness products", "health"},
            new String[]{"Automotive", "Car parts and accessories", "automotive"},
            new String[]{"Beauty", "Beauty and personal care", "beauty"}
    );

    @Override
    public String domain() {
        return "catalog";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        Logger log = ctx.log();
        ReactiveMongoDatabase database = ctx.db("catalog");
        ReactiveMongoCollection<Document> collection = database.getCollection("categories", Document.class);

        Uni<Void> chain = Uni.createFrom().voidItem();
        for (String[] cat : CATEGORIES) {
            String name = cat[0];
            String description = cat[1];
            String slug = cat[2];
            chain = chain.chain(() -> {
                Date now = Date.from(Instant.now());
                return collection.updateOne(
                        Filters.eq("slugCategory", slug),
                        Updates.combine(
                                Updates.setOnInsert("name", name),
                                Updates.setOnInsert("description", description),
                                Updates.setOnInsert("slugCategory", slug),
                                Updates.setOnInsert("createdAt", now),
                                Updates.setOnInsert("updatedAt", now),
                                Updates.setOnInsert("deletedAt", null)),
                        new UpdateOptions().upsert(true))
                        .replaceWithVoid();
            });
        }

        return chain.invoke(() -> log.infof("[seeder]   Categories seeded: %d", CATEGORIES.size()));
    }
}
