package com.demb.monolith.service;

import com.demb.monolith.Config;
import com.demb.monolith.FeatureFlags;
import com.demb.monolith.HttpClient;
import com.demb.monolith.IntegrationsFacade;
import com.demb.monolith.db.MongoBridge;
import com.demb.monolith.db.SqlStore;

import java.util.*;

/** Order service — tightly coupled to inventory, billing, notify over HTTP. */
public class OrderService {
    private final SqlStore sql;
    private final MongoBridge mongo;
    private final IntegrationsFacade integrations;
    private final FeatureFlags flags;

    public OrderService(SqlStore sql, MongoBridge mongo, IntegrationsFacade integrations, FeatureFlags flags) {
        this.sql = sql;
        this.mongo = mongo;
        this.integrations = integrations;
        this.flags = flags;
    }

    public List<Map<String, Object>> listOrders(String status, int limit, String region) {
        List<Map<String, Object>> rows = sql.listOrders(status, limit);
        for (Map<String, Object> r : rows) {
            String sku = "SKU-100";
            Object lines = r.get("lines");
            if (lines instanceof List && !((List<?>) lines).isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> first = (Map<String, Object>) ((List<?>) lines).get(0);
                if (first.get("sku") != null) {
                    sku = String.valueOf(first.get("sku"));
                }
            }
            r.put("inventory_snapshot", HttpClient.httpJson("GET",
                    Config.INVENTORY_BASE_URL + "/api/stock/check?sku=" + sku, null));
            r.put("region_view", region);
        }
        return rows;
    }

    public Map<String, Object> getOrder(int orderId) {
        Map<String, Object> row = sql.getOrder(orderId);
        if (row == null) {
            return mongo.findShadow(orderId);
        }
        return row;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createOrder(Map<String, Object> body, String region, String requestId) {
        Object linesInObj = body.containsKey("lines") ? body.get("lines") : body.get("Items");
        List<?> linesIn = linesInObj instanceof List ? (List<?>) linesInObj : Collections.emptyList();
        List<Map<String, Object>> lines = new ArrayList<Map<String, Object>>();
        for (Object itemObj : linesIn) {
            Map<String, Object> item = (Map<String, Object>) itemObj;
            String sku = firstStr(item, "sku", "SKU", "product_id");
            int qty = (int) num(item, "qty", "quantity", 1);
            double price = num(item, "unit_price", "price", 0);
            Map<String, Object> reserved = new HashMap<String, Object>();
            reserved.put("sku", sku);
            reserved.put("qty", qty);
            reserved.put("request_id", requestId);
            Map<String, Object> res = HttpClient.httpJson("POST", Config.INVENTORY_BASE_URL + "/api/stock/reserve", reserved);
            if (Boolean.FALSE.equals(res.get("ok")) && !Boolean.TRUE.equals(res.get("_degraded"))) {
                throw new IllegalArgumentException("insufficient stock for " + sku);
            }
            Map<String, Object> prod = sql.getProduct(sku);
            if (prod != null && price <= 0) {
                price = ((Number) prod.get("price")).doubleValue();
            }
            Map<String, Object> line = new HashMap<String, Object>();
            line.put("sku", sku);
            line.put("qty", qty);
            line.put("unit_price", price);
            lines.add(line);
        }

        double tax = body.get("tax") instanceof Number ? ((Number) body.get("tax")).doubleValue() : 0;
        double sub = 0;
        for (Map<String, Object> l : lines) {
            sub += ((Number) l.get("qty")).intValue() * ((Number) l.get("unit_price")).doubleValue();
        }
        double total = Math.round((sub + tax) * 100.0) / 100.0;

        Map<String, Object> order = new HashMap<String, Object>();
        order.put("customer_id", String.valueOf(firstObj(body, "customer_id", "customerId", "C-1001")));
        order.put("lines", lines);
        order.put("status", "PENDING");
        order.put("region", region);
        order.put("tax", tax);
        order.put("total", total);
        order.put("currency", body.get("currency") != null ? body.get("currency") : "USD");
        order.put("email", firstObj(body, "email", "Email", null));
        Map<String, Object> meta = new HashMap<String, Object>();
        meta.put("request_id", requestId);
        meta.put("source", body.get("source") != null ? body.get("source") : "api");
        order.put("meta", meta);

        Map<String, Object> saved = sql.insertOrder(order);
        mongo.shadowOrder(saved);

        Map<String, Object> billPayload = new HashMap<String, Object>();
        Map<String, Object> bill;
        if (flags.enabled("bill_v2_cutover", region)) {
            billPayload.put("order_id", saved.get("id"));
            billPayload.put("total", saved.get("total"));
            billPayload.put("currency", saved.get("currency"));
            bill = HttpClient.httpJson("POST", Config.BILLING_BASE_URL + "/v2/invoices", billPayload);
        } else {
            billPayload.put("OrderId", saved.get("id"));
            billPayload.put("Amount", saved.get("total"));
            billPayload.put("Cur", saved.get("currency"));
            bill = HttpClient.httpJson("POST", Config.BILLING_BASE_URL + "/v1/charge", billPayload);
        }
        saved.put("billing", bill);
        saved.put("crm", integrations.pushOrderToCrm(saved));
        saved.put("erp", integrations.postErpShipment(saved));
        saved.put("analytics", integrations.track("order_created", saved));

        Map<String, Object> notify = new HashMap<String, Object>();
        notify.put("order_id", saved.get("id"));
        notify.put("email", saved.get("email"));
        notify.put("region", region);
        HttpClient.httpJson("POST", Config.NOTIFY_BASE_URL + "/notify/order-created", notify);

        Map<String, Object> patch = new HashMap<String, Object>();
        patch.put("status", "CONFIRMED");
        patch.put("billing", bill);
        Map<String, Object> updated = sql.updateOrder(((Number) saved.get("id")).intValue(), patch);
        return updated != null ? updated : saved;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> cancelOrder(int orderId, String reason) {
        Map<String, Object> order = sql.getOrder(orderId);
        Map<String, Object> out = new HashMap<String, Object>();
        if (order == null) {
            out.put("ok", false);
            out.put("error", "missing");
            return out;
        }
        Object lines = order.get("lines");
        if (lines instanceof List) {
            for (Object lineObj : (List<?>) lines) {
                Map<String, Object> line = (Map<String, Object>) lineObj;
                Map<String, Object> rel = new HashMap<String, Object>();
                rel.put("sku", line.get("sku"));
                rel.put("qty", line.get("qty"));
                HttpClient.httpJson("POST", Config.INVENTORY_BASE_URL + "/api/stock/release", rel);
            }
        }
        Map<String, Object> patch = new HashMap<String, Object>();
        patch.put("status", "CANCELLED");
        patch.put("cancel_reason", reason);
        Map<String, Object> updated = sql.updateOrder(orderId, patch);
        Map<String, Object> notify = new HashMap<String, Object>();
        notify.put("order_id", orderId);
        notify.put("reason", reason);
        HttpClient.httpJson("POST", Config.NOTIFY_BASE_URL + "/notify/order-cancelled", notify);
        out.put("ok", true);
        out.put("order", updated);
        return out;
    }

    public Map<String, Object> applyInventoryEvent(Map<String, Object> payload) {
        String sku = String.valueOf(payload.get("sku"));
        int touched = 0;
        for (Map<String, Object> o : sql.listOrders(null, 200)) {
            Object lines = o.get("lines");
            if (lines instanceof List) {
                for (Object lineObj : (List<?>) lines) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> line = (Map<String, Object>) lineObj;
                    if (sku.equals(String.valueOf(line.get("sku")))) {
                        touched++;
                        break;
                    }
                }
            }
        }
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("touched", touched);
        out.put("sku", sku);
        return out;
    }

    public Map<String, Object> applyBillingSync(Map<String, Object> payload) {
        int oid = 0;
        Object o = payload.containsKey("order_id") ? payload.get("order_id") : payload.get("OrderId");
        if (o instanceof Number) {
            oid = ((Number) o).intValue();
        }
        String status = String.valueOf(payload.containsKey("status") ? payload.get("status") : payload.get("Status"));
        if ("null".equals(status)) {
            status = "PAID";
        }
        Map<String, Object> patch = new HashMap<String, Object>();
        patch.put("billing_status", status);
        Map<String, Object> updated = sql.updateOrder(oid, patch);
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("ok", updated != null);
        out.put("order", updated);
        return out;
    }

    private static String firstStr(Map<String, Object> m, String a, String b, String c) {
        Object v = m.containsKey(a) ? m.get(a) : m.containsKey(b) ? m.get(b) : m.get(c);
        return v == null ? null : String.valueOf(v);
    }

    private static Object firstObj(Map<String, Object> m, String a, String b, Object def) {
        if (m.containsKey(a) && m.get(a) != null) return m.get(a);
        if (m.containsKey(b) && m.get(b) != null) return m.get(b);
        return def;
    }

    private static double num(Map<String, Object> m, String a, String b, double def) {
        Object v = m.containsKey(a) ? m.get(a) : m.get(b);
        if (v == null) return def;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }
}
