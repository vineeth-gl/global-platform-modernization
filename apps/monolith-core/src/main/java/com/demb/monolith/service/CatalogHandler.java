package com.demb.monolith.service;

import com.demb.monolith.db.SqlStore;

import java.util.*;

/** Catalog handler — duplicates product search with ERP field names. */
public class CatalogHandler {
    private final SqlStore sql;
    private final ProductService productService;

    public CatalogHandler(SqlStore sql, ProductService productService) {
        this.sql = sql;
        this.productService = productService;
    }

    public Map<String, Object> search(String q, String region) {
        String ql = q == null ? "" : q.toLowerCase();
        List<Map<String, Object>> viaSvc = productService.listAll(region);
        List<Map<String, Object>> viaRaw = sql.listProducts();
        List<Map<String, Object>> hits = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> p : viaSvc) {
            String name = String.valueOf(p.get("name")).toLowerCase();
            String sku = String.valueOf(p.get("sku")).toLowerCase();
            if (ql.isEmpty() || name.contains(ql) || sku.contains(ql)) {
                Map<String, Object> hit = new HashMap<String, Object>();
                hit.put("ProductCode", p.get("sku"));
                hit.put("Title", p.get("name"));
                hit.put("Amt", p.containsKey("display_price") ? p.get("display_price") : p.get("price"));
                hit.put("Cur", p.containsKey("display_currency") ? p.get("display_currency") : p.get("currency"));
                hit.put("Region", region);
                hits.add(hit);
            }
        }
        for (Map<String, Object> p : viaRaw) {
            String sku = String.valueOf(p.get("sku"));
            boolean exists = false;
            for (Map<String, Object> h : hits) {
                if (sku.equals(String.valueOf(h.get("ProductCode")))) {
                    exists = true;
                    break;
                }
            }
            if (exists) continue;
            if (ql.isEmpty() || String.valueOf(p.get("name")).toLowerCase().contains(ql)) {
                Map<String, Object> hit = new HashMap<String, Object>();
                hit.put("ProductCode", sku);
                hit.put("Title", p.get("name"));
                hit.put("Amt", p.get("price"));
                hit.put("Cur", p.get("currency"));
                hit.put("Region", region);
                hit.put("legacyOnly", true);
                hits.add(hit);
            }
        }
        sql.rawExec("SELECT 1 /* catalog heartbeat */", null);
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("results", hits);
        out.put("count", hits.size());
        out.put("q", ql);
        return out;
    }
}
