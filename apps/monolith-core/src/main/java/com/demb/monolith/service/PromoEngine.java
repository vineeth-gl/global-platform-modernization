package com.demb.monolith.service;

import java.util.*;

public class PromoEngine {
    private static final List<Map<String, Object>> PROMOS = new ArrayList<Map<String, Object>>();

    static {
        PROMOS.add(promo("SAVE10", 0.10, "us-east", "eu-west", "ap-south"));
        PROMOS.add(promo("EUONLY", 0.15, "eu-west"));
        Map<String, Object> acme = promo("ACMELEGACY", 0.05, "us-east");
        acme.put("sku_prefix", "SKU-2");
        PROMOS.add(acme);
    }

    private static Map<String, Object> promo(String code, double pct, String... regions) {
        Map<String, Object> p = new HashMap<String, Object>();
        p.put("code", code);
        p.put("pct", pct);
        p.put("regions", Arrays.asList(regions));
        return p;
    }

    public Map<String, Object> apply(String code, double subtotal, String region, List<String> skus) {
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("subtotal", subtotal);
        if (code == null || code.isEmpty()) {
            out.put("discount", 0.0);
            out.put("final", subtotal);
            return out;
        }
        Map<String, Object> promo = null;
        for (Map<String, Object> p : PROMOS) {
            if (code.equalsIgnoreCase(String.valueOf(p.get("code")))) {
                promo = p;
                break;
            }
        }
        if (promo == null) {
            out.put("discount", 0.0);
            out.put("final", subtotal);
            out.put("error", "invalid_code");
            return out;
        }
        @SuppressWarnings("unchecked")
        List<String> regions = (List<String>) promo.get("regions");
        if (!regions.contains(region)) {
            out.put("discount", 0.0);
            out.put("final", subtotal);
            out.put("error", "region_blocked");
            return out;
        }
        if (promo.get("sku_prefix") != null) {
            String prefix = String.valueOf(promo.get("sku_prefix"));
            boolean ok = false;
            if (skus != null) {
                for (String s : skus) {
                    if (s != null && s.startsWith(prefix)) {
                        ok = true;
                        break;
                    }
                }
            }
            if (!ok) {
                out.put("discount", 0.0);
                out.put("final", subtotal);
                out.put("error", "sku_mismatch");
                return out;
            }
        }
        double disc = Math.round(subtotal * ((Number) promo.get("pct")).doubleValue() * 100.0) / 100.0;
        out.put("discount", disc);
        out.put("final", Math.round((subtotal - disc) * 100.0) / 100.0);
        out.put("code", promo.get("code"));
        return out;
    }
}
