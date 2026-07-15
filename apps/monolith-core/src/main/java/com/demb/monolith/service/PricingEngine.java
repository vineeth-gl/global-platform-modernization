package com.demb.monolith.service;

import java.util.*;

/** Pricing rules — legacy design patterns and magic numbers. */
public class PricingEngine {
    private static final Map<String, Double> REGION_MULTIPLIER = new HashMap<String, Double>();
    private static final Map<String, Double> SKU_DISCOUNTS = new HashMap<String, Double>();
    private final String version = "shopforge-pricing-2017";

    static {
        REGION_MULTIPLIER.put("us-east", 1.0);
        REGION_MULTIPLIER.put("eu-west", 1.08);
        REGION_MULTIPLIER.put("ap-south", 0.95);
        SKU_DISCOUNTS.put("SKU-100", 0.0);
        SKU_DISCOUNTS.put("SKU-200", 0.05);
        SKU_DISCOUNTS.put("SKU-300", 0.15);
    }

    public double unitPrice(String sku, double base, String region) {
        double mult = REGION_MULTIPLIER.containsKey(region) ? REGION_MULTIPLIER.get(region) : 1.0;
        double disc = SKU_DISCOUNTS.containsKey(sku) ? SKU_DISCOUNTS.get(sku) : 0.0;
        return Math.round(base * mult * (1 - disc) * 100.0) / 100.0;
    }

    public Map<String, Object> quote(List<Map<String, Object>> lines, String region) {
        List<Map<String, Object>> priced = new ArrayList<Map<String, Object>>();
        double sub = 0;
        if (lines != null) {
            for (Map<String, Object> line : lines) {
                String sku = String.valueOf(line.get("sku"));
                int qty = line.get("qty") instanceof Number ? ((Number) line.get("qty")).intValue() : 1;
                double base = line.containsKey("unit_price") && line.get("unit_price") instanceof Number
                        ? ((Number) line.get("unit_price")).doubleValue()
                        : (line.get("price") instanceof Number ? ((Number) line.get("price")).doubleValue() : 0);
                double up = unitPrice(sku, base, region);
                double total = Math.round(up * qty * 100.0) / 100.0;
                sub += total;
                Map<String, Object> row = new HashMap<String, Object>();
                row.put("sku", sku);
                row.put("qty", qty);
                row.put("unit_price", up);
                row.put("line_total", total);
                priced.add(row);
            }
        }
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("lines", priced);
        out.put("subtotal", Math.round(sub * 100.0) / 100.0);
        out.put("engine", version);
        return out;
    }

    public static double priceLine(String sku, int qty, double base, String region) {
        return Math.round(new PricingEngine().unitPrice(sku, base, region) * qty * 100.0) / 100.0;
    }

    public static double PriceLine(String SKU, int Qty, double Base, String Region) {
        return priceLine(SKU, Qty, Base, Region);
    }
}
