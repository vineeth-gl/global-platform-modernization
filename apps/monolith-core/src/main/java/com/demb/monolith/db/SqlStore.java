package com.demb.monolith.db;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** In-memory stand-in for PostgreSQL DAO — ShopForge era shape. */
public class SqlStore {
    private final Map<Integer, Map<String, Object>> orders = new ConcurrentHashMap<Integer, Map<String, Object>>();
    private final Map<String, Map<String, Object>> customers = new ConcurrentHashMap<String, Map<String, Object>>();
    private final Map<String, Map<String, Object>> products = new ConcurrentHashMap<String, Map<String, Object>>();
    private final AtomicInteger orderSeq = new AtomicInteger(900000);
    private final String dsn;

    public SqlStore(String dsn) {
        this.dsn = dsn;
        seed();
    }

    public String getDsn() {
        return dsn;
    }

    private void seed() {
        Map<String, Object> p1 = new HashMap<String, Object>();
        p1.put("sku", "SKU-100");
        p1.put("name", "Legacy Widget");
        p1.put("price", 19.99);
        p1.put("currency", "USD");
        p1.put("active", true);
        products.put("SKU-100", p1);

        Map<String, Object> p2 = new HashMap<String, Object>();
        p2.put("sku", "SKU-200");
        p2.put("name", "Acquired Acme Gadget");
        p2.put("price", 49.50);
        p2.put("currency", "EUR");
        p2.put("active", true);
        products.put("SKU-200", p2);

        Map<String, Object> p3 = new HashMap<String, Object>();
        p3.put("sku", "SKU-300");
        p3.put("name", "Scarce Item");
        p3.put("price", 199.0);
        p3.put("currency", "USD");
        p3.put("active", true);
        products.put("SKU-300", p3);

        Map<String, Object> c1 = new HashMap<String, Object>();
        c1.put("id", "C-1001");
        c1.put("email", "buyer@example.com");
        c1.put("name", "Primary Buyer");
        c1.put("country", "US");
        c1.put("vip", false);
        c1.put("source", "sql");
        customers.put("C-1001", c1);

        Map<String, Object> o1 = new HashMap<String, Object>();
        o1.put("id", 900001);
        o1.put("customer_id", "C-1001");
        List<Map<String, Object>> lines = new ArrayList<Map<String, Object>>();
        Map<String, Object> line = new HashMap<String, Object>();
        line.put("sku", "SKU-100");
        line.put("qty", 2);
        line.put("unit_price", 19.99);
        lines.add(line);
        o1.put("lines", lines);
        o1.put("status", "CONFIRMED");
        o1.put("region", "us-east");
        o1.put("tax", 2.8);
        o1.put("total", 42.78);
        o1.put("currency", "USD");
        o1.put("email", "buyer@example.com");
        orders.put(900001, o1);
        orderSeq.set(900001);
    }

    public int nextOrderId() {
        return orderSeq.incrementAndGet();
    }

    public Map<String, Object> insertOrder(Map<String, Object> order) {
        Object idObj = order.get("id");
        int id = idObj == null ? nextOrderId() : ((Number) idObj).intValue();
        order.put("id", id);
        String now = new Date().toInstant().toString();
        order.put("created_at", now);
        order.put("updated_at", now);
        orders.put(id, deepCopy(order));
        return deepCopy(order);
    }

    public Map<String, Object> updateOrder(int orderId, Map<String, Object> patch) {
        Map<String, Object> existing = orders.get(orderId);
        if (existing == null) {
            return null;
        }
        existing.putAll(patch);
        existing.put("updated_at", new Date().toInstant().toString());
        return deepCopy(existing);
    }

    public Map<String, Object> getOrder(int orderId) {
        Map<String, Object> o = orders.get(orderId);
        return o == null ? null : deepCopy(o);
    }

    public List<Map<String, Object>> listOrders(String status, int limit) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> o : orders.values()) {
            if (status == null || status.equals(o.get("status"))) {
                rows.add(deepCopy(o));
            }
        }
        Collections.sort(rows, new Comparator<Map<String, Object>>() {
            public int compare(Map<String, Object> a, Map<String, Object> b) {
                return ((Integer) b.get("id")).compareTo((Integer) a.get("id"));
            }
        });
        if (rows.size() > limit) {
            return rows.subList(0, limit);
        }
        return rows;
    }

    public Map<String, Object> upsertCustomer(Map<String, Object> cust) {
        String id = String.valueOf(cust.get("id"));
        customers.put(id, deepCopy(cust));
        return deepCopy(cust);
    }

    public Map<String, Object> getCustomer(String custId) {
        Map<String, Object> c = customers.get(String.valueOf(custId));
        return c == null ? null : deepCopy(c);
    }

    public List<Map<String, Object>> searchCustomers(String q) {
        String ql = q == null ? "" : q.toLowerCase();
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> r : customers.values()) {
            String blob = (String.valueOf(r.get("email")) + " " + r.get("name") + " " + r.get("id")).toLowerCase();
            if (ql.isEmpty() || blob.contains(ql)) {
                out.add(deepCopy(r));
            }
        }
        return out;
    }

    public Map<String, Object> upsertProduct(Map<String, Object> product) {
        products.put(String.valueOf(product.get("sku")), deepCopy(product));
        return deepCopy(product);
    }

    public List<Map<String, Object>> listProducts() {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> p : products.values()) {
            out.add(deepCopy(p));
        }
        return out;
    }

    public Map<String, Object> getProduct(String sku) {
        Map<String, Object> p = products.get(sku);
        return p == null ? null : deepCopy(p);
    }

    public Map<String, Object> rawExec(String sql, Object params) {
        Map<String, Object> r = new HashMap<String, Object>();
        r.put("ok", true);
        r.put("sql", sql);
        r.put("params", params);
        return r;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopy(Map<String, Object> in) {
        Map<String, Object> out = new HashMap<String, Object>();
        for (Map.Entry<String, Object> e : in.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Map) {
                out.put(e.getKey(), deepCopy((Map<String, Object>) v));
            } else if (v instanceof List) {
                List<Object> copy = new ArrayList<Object>();
                for (Object item : (List<?>) v) {
                    if (item instanceof Map) {
                        copy.add(deepCopy((Map<String, Object>) item));
                    } else {
                        copy.add(item);
                    }
                }
                out.put(e.getKey(), copy);
            } else {
                out.put(e.getKey(), v);
            }
        }
        return out;
    }
}
