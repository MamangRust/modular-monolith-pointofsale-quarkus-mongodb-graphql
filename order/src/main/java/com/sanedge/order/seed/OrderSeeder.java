package com.sanedge.order.seed;

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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Seeds the sales context ({@code pos_sales}): orders + order_items.
 * Order = 40. Uses fixed createdAt dates so stats are deterministic.
 *
 * <p>References the first merchant and first cashier (merchant context) and the
 * seeded products (catalog context). Idempotent: upserts on the synthetic
 * {@code _seedKey} field.</p>
 */
public class OrderSeeder implements Seeder {

    /** {totalPrice, createdAt} — merchant/cashier resolved at runtime. */
    private static final String[][] ORDERS = {
            {"5150000", "2024-01-05 10:00:00"},
            {"15000000", "2024-01-10 14:30:00"},
            {"150000", "2024-01-15 09:15:00"},
            {"120000", "2024-01-20 16:45:00"},
            {"200000", "2024-01-25 11:20:00"}
    };

    /** {productIndex, quantity, price} per order (productIndex into catalog). */
    private static final String[][] ITEMS = {
            {"0", "1", "5000000"},
            {"2", "1", "150000"},
            {"1", "1", "15000000"},
            {"3", "1", "150000"},
            {"5", "1", "120000"},
            {"4", "1", "200000"}
    };

    @Override
    public String domain() {
        return "order";
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        Logger log = ctx.log();
        ReactiveMongoDatabase sales = ctx.db("sales");
        ReactiveMongoDatabase merchant = ctx.db("merchant");
        ReactiveMongoDatabase catalog = ctx.db("catalog");
        ReactiveMongoCollection<Document> orders = sales.getCollection("orders", Document.class);
        ReactiveMongoCollection<Document> orderItems = sales.getCollection("order_items", Document.class);
        ReactiveMongoCollection<Document> merchants = merchant.getCollection("merchants", Document.class);
        ReactiveMongoCollection<Document> cashiers = merchant.getCollection("cashiers", Document.class);
        ReactiveMongoCollection<Document> products = catalog.getCollection("products", Document.class);

        return merchants.find().collect().first()
                .onItem().ifNull().continueWith(() -> new Document("_id", new ObjectId()))
                .chain(merchantDoc -> cashiers.find().collect().first()
                        .onItem().ifNull().continueWith(() -> new Document("_id", new ObjectId()))
                        .chain(cashierDoc -> products.find().collect().asList()
                                .chain(productDocs -> {
                                    ObjectId merchantId = merchantDoc.getObjectId("_id");
                                    ObjectId cashierId = cashierDoc.getObjectId("_id");
                                    return seedOrders(orders, orderItems, merchantId, cashierId, productDocs, log);
                                })));
    }

    private Uni<Void> seedOrders(ReactiveMongoCollection<Document> orders,
                                 ReactiveMongoCollection<Document> orderItems,
                                 ObjectId merchantId,
                                 ObjectId cashierId,
                                 List<Document> productDocs,
                                 Logger log) {
        List<ObjectId> orderIds = new ArrayList<>();
        Uni<Void> chain = Uni.createFrom().voidItem();

        for (int i = 0; i < ORDERS.length; i++) {
            String[] o = ORDERS[i];
            ObjectId orderId = new ObjectId();
            orderIds.add(orderId);
            long totalPrice = Long.parseLong(o[0]);
            Date createdAt = Date.from(LocalDateTime.parse(o[1].replace(' ', 'T')).toInstant(ZoneOffset.UTC));
            String seedKey = "order-" + i;
            chain = chain.chain(() -> orders.updateOne(
                    Filters.eq("_seedKey", seedKey),
                    Updates.combine(
                            Updates.setOnInsert("_seedKey", seedKey),
                            Updates.setOnInsert("_id", orderId),
                            Updates.setOnInsert("merchantId", merchantId),
                            Updates.setOnInsert("cashierId", cashierId),
                            Updates.setOnInsert("totalPrice", totalPrice),
                            Updates.setOnInsert("createdAt", createdAt),
                            Updates.setOnInsert("updatedAt", createdAt),
                            Updates.setOnInsert("deletedAt", null)),
                    new UpdateOptions().upsert(true)).replaceWithVoid());
        }

        chain = chain.invoke(() -> log.infof("[seeder]   Orders seeded: %d", ORDERS.length));

        int itemCount = 0;
        for (int i = 0; i < ITEMS.length; i++) {
            String[] it = ITEMS[i];
            int productIndex = Integer.parseInt(it[0]);
            int quantity = Integer.parseInt(it[1]);
            int price = Integer.parseInt(it[2]);
            ObjectId orderId = orderIds.get(i % orderIds.size());
            ObjectId productId = productDocs.isEmpty()
                    ? new ObjectId()
                    : productDocs.get(productIndex % productDocs.size()).getObjectId("_id");
            Date now = Date.from(Instant.now());
            String seedKey = "order-item-" + i;
            itemCount++;
            chain = chain.chain(() -> orderItems.updateOne(
                    Filters.eq("_seedKey", seedKey),
                    Updates.combine(
                            Updates.setOnInsert("_seedKey", seedKey),
                            Updates.setOnInsert("orderId", orderId),
                            Updates.setOnInsert("productId", productId),
                            Updates.setOnInsert("quantity", quantity),
                            Updates.setOnInsert("price", price),
                            Updates.setOnInsert("createdAt", now),
                            Updates.setOnInsert("updatedAt", now),
                            Updates.setOnInsert("deletedAt", null)),
                    new UpdateOptions().upsert(true)).replaceWithVoid());
        }

        final int seededItems = itemCount;
        return chain.invoke(() -> log.infof("[seeder]   Order items seeded: %d", seededItems));
    }
}
