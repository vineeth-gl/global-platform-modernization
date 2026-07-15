package com.demb.monolith;

import java.util.*;

/** 30+ countries / multi-timezone business rules. */
public final class I18nRegions {
    public static final Map<String, String> COUNTRY_REGION = new HashMap<String, String>();
    public static final Map<String, Double> TAX_RATES = new HashMap<String, Double>();
    public static final Map<String, String> TIMEZONES = new HashMap<String, String>();

    static {
        String[] us = {"US", "CA", "MX", "BR"};
        for (String c : us) COUNTRY_REGION.put(c, "us-east");
        String[] eu = {"GB", "DE", "FR", "NL", "ES", "IT", "SE", "NO", "DK", "FI", "PL", "IE", "BE", "AT", "CH", "PT", "AE", "ZA", "NG"};
        for (String c : eu) COUNTRY_REGION.put(c, "eu-west");
        String[] ap = {"IN", "SG", "AU", "JP", "KR", "NZ", "HK", "TW", "TH", "MY", "PH", "ID", "VN"};
        for (String c : ap) COUNTRY_REGION.put(c, "ap-south");

        TAX_RATES.put("US", 0.07);
        TAX_RATES.put("CA", 0.13);
        TAX_RATES.put("GB", 0.20);
        TAX_RATES.put("DE", 0.19);
        TAX_RATES.put("FR", 0.20);
        TAX_RATES.put("IN", 0.18);
        TAX_RATES.put("AU", 0.10);
        TAX_RATES.put("SG", 0.08);
        TAX_RATES.put("JP", 0.10);
        TAX_RATES.put("DEFAULT", 0.10);

        TIMEZONES.put("us-east", "America/New_York");
        TIMEZONES.put("eu-west", "Europe/Dublin");
        TIMEZONES.put("ap-south", "Asia/Kolkata");
    }

    private I18nRegions() {
    }

    public static String resolveRegion(String headerRegion, String country) {
        if ("us-east".equals(headerRegion) || "eu-west".equals(headerRegion) || "ap-south".equals(headerRegion)) {
            return headerRegion;
        }
        if (country != null) {
            String mapped = COUNTRY_REGION.get(country.toUpperCase());
            if (mapped != null) {
                return mapped;
            }
        }
        return "us-east";
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> applyTaxRules(Map<String, Object> body, String region) {
        String country = String.valueOf(body.containsKey("country") ? body.get("country")
                : body.containsKey("Country") ? body.get("Country") : "US").toUpperCase();
        Double rate = TAX_RATES.containsKey(country) ? TAX_RATES.get(country) : TAX_RATES.get("DEFAULT");
        if (!body.containsKey("tax") && body.get("lines") instanceof List) {
            double sub = 0.0;
            for (Object item : (List<?>) body.get("lines")) {
                Map<String, Object> line = (Map<String, Object>) item;
                double qty = num(line, "qty", "quantity", 1);
                double price = num(line, "unit_price", "price", 0);
                sub += qty * price;
            }
            body.put("tax", Math.round(sub * rate * 100.0) / 100.0);
            body.put("_tax_rate", rate);
            body.put("_tax_country", country);
        }
        body.put("_region", region);
        body.put("_tz", TIMEZONES.get(region));
        return body;
    }

    public static String supportPodForHour(int utcHour) {
        if (utcHour >= 0 && utcHour < 12) return "APAC";
        if (utcHour >= 6 && utcHour < 18) return "EMEA";
        return "AMER";
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
