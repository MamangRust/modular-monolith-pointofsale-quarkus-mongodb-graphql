package com.sanedge.user.seed;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Seeds the identity context: roles, users, and user_roles junction.
 * Order = 10 (runs first).
 *
 * <p>Idempotent: roles upsert on {@code roleName}, users upsert on
 * {@code username}, and user_roles upserts on the {@code (userId, roleId)}
 * pair.</p>
 */
public class UserSeeder implements Seeder {

    private static final List<String> ROLES = List.of("admin", "cashier", "staff");

    private static final String[][] USERS = {
            {"Admin", "User", "admin", "admin@example.com"},
            {"Cashier", "One", "cashier1", "cashier1@example.com"},
            {"Staff", "One", "staff1", "staff1@example.com"}
    };

    private static final Map<String, String> USER_ROLE = Map.of(
            "admin", "admin",
            "cashier1", "cashier",
            "staff1", "staff");

    @Override
    public String domain() {
        return "identity";
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        Logger log = ctx.log();
        ReactiveMongoDatabase identity = ctx.db("identity");
        ReactiveMongoCollection<Document> roles = identity.getCollection("roles", Document.class);
        ReactiveMongoCollection<Document> users = identity.getCollection("users", Document.class);
        ReactiveMongoCollection<Document> userRoles = identity.getCollection("user_roles", Document.class);

        return seedRoles(roles, log)
                .chain(() -> seedUsers(users, ctx.passwordHasher(), log))
                .chain(() -> seedUserRoles(roles, users, userRoles, log))
                .replaceWithVoid();
    }

    private Uni<Void> seedRoles(ReactiveMongoCollection<Document> roles, Logger log) {
        Date now = Date.from(Instant.now());
        Uni<Void> chain = Uni.createFrom().voidItem();
        for (String role : ROLES) {
            chain = chain.chain(() -> roles.updateOne(
                    Filters.eq("roleName", role),
                    Updates.combine(
                            Updates.setOnInsert("roleName", role),
                            Updates.setOnInsert("createdAt", now),
                            Updates.setOnInsert("updatedAt", now),
                            Updates.setOnInsert("deletedAt", null)),
                    new UpdateOptions().upsert(true)).replaceWithVoid());
        }
        return chain.invoke(() -> log.infof("[seeder]   Roles seeded: %d", ROLES.size()));
    }

    private Uni<Void> seedUsers(ReactiveMongoCollection<Document> users,
                                Function<String, String> passwordHasher,
                                Logger log) {
        Date now = Date.from(Instant.now());
        String hashed = passwordHasher.apply("password123");
        Uni<Void> chain = Uni.createFrom().voidItem();
        for (String[] u : USERS) {
            chain = chain.chain(() -> users.updateOne(
                    Filters.eq("username", u[2]),
                    Updates.combine(
                            Updates.setOnInsert("username", u[2]),
                            Updates.setOnInsert("firstname", u[0]),
                            Updates.setOnInsert("lastname", u[1]),
                            Updates.setOnInsert("email", u[3]),
                            Updates.setOnInsert("password", hashed),
                            Updates.setOnInsert("createdAt", now),
                            Updates.setOnInsert("updatedAt", now),
                            Updates.setOnInsert("deletedAt", null)),
                    new UpdateOptions().upsert(true)).replaceWithVoid());
        }
        return chain.invoke(() -> log.infof("[seeder]   Users seeded: %d", USERS.length));
    }

    private Uni<Void> seedUserRoles(ReactiveMongoCollection<Document> roles,
                                    ReactiveMongoCollection<Document> users,
                                    ReactiveMongoCollection<Document> userRoles,
                                    Logger log) {
        List<String> roleNames = new ArrayList<>(USER_ROLE.values());
        List<String> userNames = new ArrayList<>(USER_ROLE.keySet());
        return roles.find(Filters.in("roleName", roleNames)).collect().asList()
                .chain(roleDocs -> {
                    Map<String, ObjectId> roleIds = new java.util.HashMap<>();
                    for (Document role : roleDocs) {
                        roleIds.put(role.getString("roleName"), role.getObjectId("_id"));
                    }
                    return users.find(Filters.in("username", userNames)).collect().asList()
                            .chain(userDocs -> {
                                Map<String, ObjectId> userIds = new java.util.HashMap<>();
                                for (Document user : userDocs) {
                                    userIds.put(user.getString("username"), user.getObjectId("_id"));
                                }
                                Uni<Void> chain = Uni.createFrom().voidItem();
                                int assigned = 0;
                                for (Map.Entry<String, String> entry : USER_ROLE.entrySet()) {
                                    ObjectId userId = userIds.get(entry.getKey());
                                    ObjectId roleId = roleIds.get(entry.getValue());
                                    if (userId == null || roleId == null) {
                                        continue;
                                    }
                                    final ObjectId uid = userId;
                                    final ObjectId rid = roleId;
                                    chain = chain.chain(() -> userRoles.updateOne(
                                            Filters.and(Filters.eq("userId", uid), Filters.eq("roleId", rid)),
                                            Updates.combine(
                                                    Updates.setOnInsert("userId", uid),
                                                    Updates.setOnInsert("roleId", rid)),
                                            new UpdateOptions().upsert(true)).replaceWithVoid())
                                            .chain(() -> users.updateOne(
                                                    Filters.eq("_id", uid),
                                                    Updates.set("roleIds", List.of(rid)))
                                                    .replaceWithVoid());
                                    assigned++;
                                }
                                final int count = assigned;
                                return chain.invoke(() -> log.infof("[seeder]   User roles assigned: %d", count));
                            });
                });
    }
}
