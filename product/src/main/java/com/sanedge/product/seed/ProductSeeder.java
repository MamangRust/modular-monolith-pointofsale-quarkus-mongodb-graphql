package com.sanedge.product.seed;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds the catalog context ({@code pos_catalog}): products.
 * Order = 35.
 *
 * <p>References the first merchant (merchant context) as the owner and resolves
 * each product's category by {@code slugCategory}. Idempotent: upserts on the
 * natural key {@code slugProduct}.</p>
 */
public class ProductSeeder implements Seeder {

    /** {merchantId placeholder, categorySlug, name, description, price, stock, slug} */
    private static final String[][] PRODUCTS = {
            {"electronics", "Smartphone X1", "Latest smartphone", "5000000", "50", "smartphone-x1"},
            {"electronics", "Laptop Pro", "Professional laptop", "15000000", "20", "laptop-pro"},
            {"clothing", "T-Shirt Basic", "Cotton t-shirt", "150000", "100", "tshirt-basic"},
            {"food-beverage", "Coffee Beans 1kg", "Premium coffee beans", "120000", "200", "coffee-beans-1kg"},
            {"home-garden", "Garden Hose 10m", "Durable garden hose", "250000", "30", "garden-hose-10m"},
            {"sports", "Yoga Mat", "Non-slip yoga mat", "200000", "40", "yoga-mat"},
            {"books", "Java Programming", "Java programming book", "180000", "25", "java-programming"},
            {"toys", "Building Blocks", "Creative building blocks", "95000", "60", "building-blocks"},
            {"health", "Vitamin C 1000mg", "Immune support supplement", "75000", "80", "vitamin-c-1000mg"},
            {"automotive", "Car Air Freshener", "Long-lasting air freshener", "35000", "150", "car-air-freshener"}
    };

    @Override
    public String domain() {
        return "catalog";
    }

    @Override
    public int order() {
        return 35;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        Logger log = ctx.log();
        ReactiveMongoDatabase catalog = ctx.db("catalog");
        ReactiveMongoDatabase merchant = ctx.db("merchant");
        ReactiveMongoCollection<Document> products = catalog.getCollection("products", Document.class);
        ReactiveMongoCollection<Document> categories = catalog.getCollection("categories", Document.class);
        ReactiveMongoCollection<Document> merchants = merchant.getCollection("merchants", Document.class);

        return merchants.find().collect().first()
                .onItem().ifNull().continueWith(() -> new Document("_id", new ObjectId()))
                .chain(merchantDoc -> {
                    ObjectId merchantId = merchantDoc.getObjectId("_id");
                    return categories.find().collect().asList()
                            .chain(categoryDocs -> {
                                Map<String, ObjectId> categoryMap = new HashMap<>();
                                for (Document c : categoryDocs) {
                                    categoryMap.put(c.getString("slugCategory"), c.getObjectId("_id"));
                                }
                                return seedProducts(products, merchantId, categoryMap, log);
                            });
                })
                .replaceWithVoid();
    }

    private Uni<Void> seedProducts(ReactiveMongoCollection<Document> products,
                                   ObjectId merchantId,
                                   Map<String, ObjectId> categoryMap,
                                   Logger log) {
        Uni<Void> chain = Uni.createFrom().voidItem();
        for (String[] p : PRODUCTS) {
            String slugCategory = p[0];
            String name = p[1];
            String description = p[2];
            int price = Integer.parseInt(p[3]);
            int stock = Integer.parseInt(p[4]);
            String slug = p[5];
            ObjectId categoryId = categoryMap.getOrDefault(slugCategory, new ObjectId());

            chain = chain.chain(() -> {
                Date now = Date.from(Instant.now());
                return products.updateOne(
                        Filters.eq("slugProduct", slug),
                        Updates.combine(
                                Updates.setOnInsert("merchantId", merchantId),
                                Updates.setOnInsert("categoryId", categoryId),
                                Updates.setOnInsert("name", name),
                                Updates.setOnInsert("description", description),
                                Updates.setOnInsert("price", price),
                                Updates.setOnInsert("countInStock", stock),
                                Updates.setOnInsert("slugProduct", slug),
                                Updates.setOnInsert("createdAt", now),
                                Updates.setOnInsert("updatedAt", now),
                                Updates.setOnInsert("deletedAt", null)),
                        new UpdateOptions().upsert(true))
                        .replaceWithVoid();
            });
        }
        return chain.invoke(() -> log.infof("[seeder]   Products seeded: %d", PRODUCTS.length));
    }
}
