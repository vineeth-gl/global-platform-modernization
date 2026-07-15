package com.demb.billing;

import java.util.*;

/** AcmeBill rating engine — acquired platform fees. */
public final class RatingEngine {
    private static final List<Map<String, Object>> FEE_SCHEDULE = new ArrayList<Map<String, Object>>();

    static {
        FEE_SCHEDULE.add(fee("CONV", "Convenience fee", 0.019, 0.3, null));
        FEE_SCHEDULE.add(fee("FX", "FX markup", 0.025, 0.0, Arrays.asList("eu-west", "ap-south")));
        Map<String, Object> rush = new HashMap<String, Object>();
        rush.put("code", "RUSH");
        rush.put("name", "Rush settlement");
        rush.put("flat", 2.5);
        rush.put("regions", Arrays.asList("us-east"));
        FEE_SCHEDULE.add(rush);
        FEE_SCHEDULE.add(fee("LEGACY_ACME", "Acme platform fee", 0.005, 0.1, null));
    }

    private RatingEngine() {
    }

    private static Map<String, Object> fee(String code, String name, double pct, double min, List<String> regions) {
        Map<String, Object> f = new HashMap<String, Object>();
        f.put("code", code);
        f.put("name", name);
        f.put("pct", pct);
        f.put("min", min);
        f.put("regions", regions);
        return f;
    }

    public static Map<String, Object> rate(double amount, String currency, String region) {
        List<Map<String, Object>> fees = new ArrayList<Map<String, Object>>();
        double totalFees = 0;
        for (Map<String, Object> fee : FEE_SCHEDULE) {
            @SuppressWarnings("unchecked")
            List<String> regions = (List<String>) fee.get("regions");
            if (regions != null && !regions.contains(region)) continue;
            double val;
            if (fee.containsKey("flat")) {
                val = ((Number) fee.get("flat")).doubleValue();
            } else {
                val = Math.max(((Number) fee.get("min")).doubleValue(),
                        Math.round(amount * ((Number) fee.get("pct")).doubleValue() * 100.0) / 100.0);
            }
            Map<String, Object> line = new HashMap<String, Object>();
            line.put("code", fee.get("code"));
            line.put("name", fee.get("name"));
            line.put("amount", val);
            fees.add(line);
            totalFees += val;
        }
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("principal", amount);
        out.put("currency", currency);
        out.put("region", region);
        out.put("fees", fees);
        out.put("fee_total", Math.round(totalFees * 100.0) / 100.0);
        out.put("grand_total", Math.round((amount + totalFees) * 100.0) / 100.0);
        out.put("engine", "acme-rating-2014");
        return out;
    }

    public static Map<String, Object> compareV1V2(double amount, String currency, String region) {
        Map<String, Object> v1 = rate(amount, currency, region);
        // v2 drops LEGACY_ACME — approximate by subtracting that fee line
        double v2Total = ((Number) v1.get("grand_total")).doubleValue();
        for (Object f : (List<?>) v1.get("fees")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> fee = (Map<String, Object>) f;
            if ("LEGACY_ACME".equals(fee.get("code"))) {
                v2Total -= ((Number) fee.get("amount")).doubleValue();
            }
        }
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("v1", v1);
        out.put("v2_grand_total", Math.round(v2Total * 100.0) / 100.0);
        out.put("delta", Math.round((((Number) v1.get("grand_total")).doubleValue() - v2Total) * 100.0) / 100.0);
        return out;
    }
}
