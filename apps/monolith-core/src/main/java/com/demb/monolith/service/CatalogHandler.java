package com.demb.monolith.service;

import com.demb.monolith.db.SqlStore;

import java.util.*;

/** Catalog handler — duplicates product search with ERP field names. */
public class CatalogHandler {
    private static final int MAX_QUERY_LENGTH = 80;
    private static final String SAFE_QUERY = "[A-Za-z0-9 _.-]*";
    private static final String SAFE_REGION = "[A-Za-z0-9_-]*";

    private final SqlStore sql;
    private final ProductService productService;

    public CatalogHandler(SqlStore sql, ProductService productService) {
        this.sql = sql;
        this.productService = productService;
    }

    public Map<String, Object> search(String q, String region) {
        String ql = normalizeQuery(q);
        String safeRegion = normalizeRegion(region);
        List<Map<String, Object>> viaSvc = productService.listAll(safeRegion);
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
                hit.put("Region", safeRegion);
                hits.add(hit);
            }
        }
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("results", hits);
        out.put("count", hits.size());
        out.put("q", ql);
        return out;
    }

    private String normalizeQuery(String q) {
        String value = q == null ? "" : q.trim();
        if (value.length() > MAX_QUERY_LENGTH || !value.matches(SAFE_QUERY)) {
            throw new IllegalArgumentException("catalog search contains unsupported characters");
        }
        return value.toLowerCase();
    }

    private String normalizeRegion(String region) {
        String value = region == null ? "" : region.trim().toLowerCase();
        if (!value.matches(SAFE_REGION)) {
            throw new IllegalArgumentException("catalog region contains unsupported characters");
        }
        return value;
    }
}
