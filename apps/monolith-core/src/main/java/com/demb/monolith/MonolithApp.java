package com.demb.monolith;

import com.demb.monolith.auth.IdentityFederation;
import com.demb.monolith.db.MongoBridge;
import com.demb.monolith.db.SqlStore;
import com.demb.monolith.service.*;
import spark.Request;
import spark.Response;

import java.util.*;

import static spark.Spark.*;

/**
 * DEMb Monolith Core - Java 8 / SparkJava
 * Legacy God-object style (acquired ShopForge core, 2012+).
 */
public class MonolithApp {
    private static final IdentityFederation FED = new IdentityFederation();

    public static void main(String[] args) {
        port(Config.PORT);

        final SqlStore sql = new SqlStore(Config.SQL_DSN);
        final MongoBridge mongo = new MongoBridge(Config.MONGO_URI);
        final FeatureFlags flags = new FeatureFlags();
        final IntegrationsFacade integrations = new IntegrationsFacade();
        final OrderService orderSvc = new OrderService(sql, mongo, integrations, flags);
        final CustomerService custSvc = new CustomerService(sql, mongo);
        final ProductService prodSvc = new ProductService(sql);
        final CatalogHandler catalog = new CatalogHandler(sql, prodSvc);
        final ReportEngine reports = new ReportEngine(sql, mongo, integrations);
        final PromoEngine promoSvc = new PromoEngine();
        final SupportTickets tickets = new SupportTickets();
        final ReturnsService returnsSvc = new ReturnsService(sql, integrations);
        final ShippingService shippingSvc = new ShippingService(sql);
        final OrderWorkflow workflow = new OrderWorkflow(true);
        final PricingEngine pricing = new PricingEngine();
        final MerchandisingService merch = new MerchandisingService(prodSvc);

        before((req, res) -> {
            String region = I18nRegions.resolveRegion(req.headers("X-Region"), req.queryParams("country"));
            req.attribute("region", region);
            req.attribute("requestId", req.headers("X-Request-Id") != null
                    ? req.headers("X-Request-Id") : "m-" + System.currentTimeMillis());
            res.type("application/json");
        });

        after((req, res) -> {
            res.header("X-Platform", "dem-monolith-java8");
            res.header("X-Region", attr(req, "region"));
        });

        get("/health", (req, res) -> json(mapOf(
                "status", "ok",
                "service", "monolith-core",
                "runtime", "java8",
                "ts", new Date().toInstant().toString())));

        get("/api/integrations", (req, res) -> json(integrations.listConnectors()));

        post("/api/integrations/:name/retry", (req, res) -> json(integrations.healConnector(req.params("name"))));

        get("/api/v1/orders", (req, res) -> {
            require(req, res, "orders:read");
            int limit = req.queryParams("limit") != null ? Integer.parseInt(req.queryParams("limit")) : 50;
            return json(mapOf(
                    "orders", orderSvc.listOrders(req.queryParams("status"), limit, region(req)),
                    "region", region(req)));
        });

        post("/api/v1/orders", (req, res) -> {
            require(req, res, "orders:write");
            Map<String, Object> body = Jsons.map(req.body());
            body = I18nRegions.applyTaxRules(body, region(req));
            if (body.containsKey("email")) {
                body.put("email", UtilsDup.normalizeEmail(String.valueOf(body.get("email"))));
            }
            if (body.containsKey("Email")) {
                body.put("Email", UtilsMisc.NormalizeEmail(body.get("Email")));
            }
            try {
                res.status(201);
                return json(orderSvc.createOrder(body, region(req), attr(req, "requestId")));
            } catch (Exception e) {
                res.status(400);
                return json(mapOf("error", e.getMessage(), "code", "ORDER_CREATE_FAIL"));
            }
        });

        get("/api/v1/orders/:id", (req, res) -> {
            require(req, res, "orders:read");
            Map<String, Object> order = orderSvc.getOrder(Integer.parseInt(req.params("id")));
            if (order == null) {
                res.status(404);
                return "{\"error\":\"not found\"}";
            }
            return json(order);
        });

        post("/api/v1/orders/:id/cancel", (req, res) -> {
            require(req, res, "orders:write");
            Map<String, Object> body = Jsons.map(req.body());
            return json(orderSvc.cancelOrder(Integer.parseInt(req.params("id")),
                    body.get("reason") != null ? String.valueOf(body.get("reason")) : null));
        });

        get("/api/v1/customers", (req, res) -> {
            require(req, res, "customers:read");
            return json(mapOf(
                    "customers", custSvc.search(req.queryParams("q") != null ? req.queryParams("q") : ""),
                    "source", "sql+mongo-merge"));
        });

        post("/api/v1/customers", (req, res) -> {
            require(req, res, "customers:write");
            Map<String, Object> body = Jsons.map(req.body());
            boolean vip = Boolean.TRUE.equals(body.get("vip"))
                    || Boolean.TRUE.equals(body.get("isVIP"))
                    || Boolean.TRUE.equals(body.get("Is_Vip"));
            res.status(201);
            return json(custSvc.create(body, vip));
        });

        get("/api/v1/customers/:id", (req, res) -> {
            require(req, res, "customers:read");
            Map<String, Object> cust = custSvc.get(req.params("id"));
            if (cust == null) {
                res.status(404);
                return "{\"error\":\"not_found\"}";
            }
            return json(cust);
        });

        get("/api/v1/products", (req, res) ->
                json(mapOf("products", prodSvc.listAll(region(req)))));

        get("/api/v1/catalog/search", (req, res) ->
                json(catalog.search(req.queryParams("q") != null ? req.queryParams("q") : "", region(req))));

        post("/api/v1/internal/inventory-hook", (req, res) ->
                json(orderSvc.applyInventoryEvent(Jsons.map(req.body()))));

        post("/api/v1/internal/billing-sync", (req, res) ->
                json(orderSvc.applyBillingSync(Jsons.map(req.body()))));

        get("/api/v1/admin/flags", (req, res) -> {
            require(req, res, "admin");
            return json(flags.dump());
        });

        get("/api/v1/admin/federation/whoami", (req, res) ->
                json(FED.introspect(req.headers("Authorization"))));

        get("/api/v1/reports/sales-by-region", (req, res) -> {
            require(req, res, "orders:read");
            return json(reports.salesByRegion(30));
        });

        get("/api/v1/reports/vip-vs-standard", (req, res) -> {
            require(req, res, "customers:read");
            return json(reports.vipVsStandard());
        });

        get("/api/v1/reports/sla", (req, res) -> {
            require(req, res, "admin");
            return json(reports.slaBurn());
        });

        post("/api/v1/promos/apply", (req, res) -> {
            require(req, res, "orders:read");
            Map<String, Object> body = Jsons.map(req.body());
            List<String> skus = new ArrayList<String>();
            if (body.get("skus") instanceof List) {
                for (Object s : (List<?>) body.get("skus")) skus.add(String.valueOf(s));
            }
            double sub = body.get("subtotal") instanceof Number ? ((Number) body.get("subtotal")).doubleValue() : 0;
            return json(promoSvc.apply(body.get("code") != null ? String.valueOf(body.get("code")) : null,
                    sub, region(req), skus));
        });

        post("/api/v1/support/tickets", (req, res) -> {
            require(req, res, "admin");
            Map<String, Object> body = Jsons.map(req.body());
            res.status(201);
            return json(tickets.create(
                    str(body, "subject", "untitled"),
                    str(body, "priority", "P3"),
                    str(body, "country", "US"),
                    str(body, "body", "")));
        });

        get("/api/v1/support/tickets", (req, res) -> {
            require(req, res, "admin");
            return json(mapOf("tickets", tickets.listOpen(req.queryParams("pod"))));
        });

        post("/api/v1/returns", (req, res) -> {
            require(req, res, "orders:write");
            Map<String, Object> body = Jsons.map(req.body());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> lines = body.get("lines") instanceof List
                    ? (List<Map<String, Object>>) body.get("lines") : new ArrayList<Map<String, Object>>();
            res.status(201);
            return json(returnsSvc.createRma(((Number) body.get("order_id")).intValue(),
                    str(body, "reason", "unspecified"), lines));
        });

        post("/api/v1/returns/:id/approve", (req, res) -> {
            require(req, res, "orders:write");
            return json(returnsSvc.approve(req.params("id")));
        });

        post("/api/v1/shipping", (req, res) -> {
            require(req, res, "orders:write");
            Map<String, Object> body = Jsons.map(req.body());
            res.status(201);
            return json(shippingSvc.createShipment(((Number) body.get("order_id")).intValue(), region(req)));
        });

        get("/api/v1/shipping/:tracking", (req, res) -> {
            require(req, res, "orders:read");
            return json(shippingSvc.track(req.params("tracking")));
        });

        post("/api/v1/orders/:id/transition", (req, res) -> {
            require(req, res, "orders:write");
            Map<String, Object> body = Jsons.map(req.body());
            Map<String, Object> order = sql.getOrder(Integer.parseInt(req.params("id")));
            if (order == null) {
                res.status(404);
                return "{\"error\":\"not found\"}";
            }
            try {
                Map<String, Object> updated = workflow.transition(order, String.valueOf(body.get("status")),
                        str(body, "actor", "api"));
                sql.updateOrder(Integer.parseInt(req.params("id")), updated);
                return json(updated);
            } catch (Exception e) {
                res.status(400);
                return json(mapOf("error", e.getMessage()));
            }
        });

        post("/api/v1/pricing/quote", (req, res) -> {
            require(req, res, "orders:read");
            Map<String, Object> body = Jsons.map(req.body());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> lines = body.get("lines") instanceof List
                    ? (List<Map<String, Object>>) body.get("lines") : new ArrayList<Map<String, Object>>();
            return json(pricing.quote(lines, region(req)));
        });

        get("/api/v1/merchandising", (req, res) ->
                json(mapOf("products", merch.listEnriched(region(req)))));

        get("/api/v1/admin/audit", (req, res) -> {
            require(req, res, "admin");
            return json(mapOf("events", AuditTrail.INSTANCE.query(null, 100)));
        });

        System.out.println("monolith-core (Java 8) on port " + Config.PORT);
    }

    private static void require(Request req, Response res, String... scopes) {
        if ("1".equals(req.headers("X-Legacy-Bypass"))) {
            return;
        }
        Map<String, Object> info = FED.introspect(req.headers("Authorization"));
        if (!Boolean.TRUE.equals(info.get("active"))) {
            halt(401, "{\"error\":\"unauthorized\",\"hint\":\"use Bearer dev-admin\"}");
        }
        @SuppressWarnings("unchecked")
        Set<String> have = new HashSet<String>((List<String>) info.get("scopes"));
        if (scopes != null && scopes.length > 0 && !have.contains("admin")) {
            for (String s : scopes) {
                if (!have.contains(s)) {
                    halt(403, "{\"error\":\"forbidden\",\"need\":\"" + s + "\"}");
                }
            }
        }
    }

    private static String region(Request req) {
        return attr(req, "region");
    }

    /** Avoid String.valueOf(req.attribute(...)) - Spark's generic attribute() can bind to valueOf(char[]). */
    private static String attr(Request req, String key) {
        Object v = req.attribute(key);
        return v == null ? "" : v.toString();
    }

    private static String json(Object o) {
        return Jsons.toJson(o);
    }

    private static String str(Map<String, Object> m, String k, String def) {
        return m.get(k) != null ? String.valueOf(m.get(k)) : def;
    }

    private static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }
}

