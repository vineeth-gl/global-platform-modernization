package com.demb.monolith.service;

import com.demb.monolith.db.SqlStore;

import java.util.*;

public class ProductService {
    private final SqlStore sql;

    public ProductService(SqlStore sql) {
        this.sql = sql;
    }

    public List<Map<String, Object>> listAll(String region) {
        List<Map<String, Object>> products = sql.listProducts();
        for (Map<String, Object> p : products) {
            p.put("region", region);
            double price = ((Number) p.get("price")).doubleValue();
            if (region != null && region.startsWith("eu") && "USD".equals(p.get("currency"))) {
                p.put("display_price", Math.round(price * 0.92 * 100.0) / 100.0);
                p.put("display_currency", "EUR");
            } else {
                p.put("display_price", price);
                p.put("display_currency", p.get("currency") != null ? p.get("currency") : "USD");
            }
        }
        return products;
    }

    public Map<String, Object> create(Map<String, Object> body) {
        Object skuObj = body.containsKey("sku") ? body.get("sku") : body.get("SKU");
        if (skuObj == null) {
            throw new IllegalArgumentException("sku required");
        }
        Map<String, Object> prod = new HashMap<String, Object>();
        prod.put("sku", String.valueOf(skuObj));
        prod.put("name", body.get("name") != null ? body.get("name") : "Untitled");
        prod.put("price", body.get("price") instanceof Number ? ((Number) body.get("price")).doubleValue() : 0);
        prod.put("currency", body.get("currency") != null ? body.get("currency") : "USD");
        prod.put("active", body.get("active") == null || Boolean.TRUE.equals(body.get("active")));
        return sql.upsertProduct(prod);
    }

    public Map<String, Object> get(String sku) {
        return sql.getProduct(sku);
    }
}
