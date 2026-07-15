package com.demb.monolith;

import java.util.HashMap;
import java.util.Map;

/** Feature flags — region-aware (bill_v2 canary hell). */
public class FeatureFlags {
    private final Map<String, Object> cache = new HashMap<String, Object>();

    public FeatureFlags() {
        Map<String, Object> bill = new HashMap<String, Object>();
        bill.put("us-east", false);
        bill.put("eu-west", true);
        bill.put("ap-south", false);
        bill.put("default", false);
        cache.put("bill_v2_cutover", bill);
        Map<String, Object> defFalse = new HashMap<String, Object>();
        defFalse.put("default", false);
        cache.put("react_portal_only", defFalse);
        Map<String, Object> defTrue = new HashMap<String, Object>();
        defTrue.put("default", true);
        cache.put("kafka_notify", defTrue);
        cache.put("rabbit_compat", defTrue);
        cache.put("zero_trust_enforce", defFalse);
        cache.put("canary_inventory", defFalse);
        cache.put("blue_green_monolith", defTrue);
    }

    public boolean enabled(String name, String region) {
        Object entry = cache.get(name);
        if (entry == null) {
            return false;
        }
        if (entry instanceof Boolean) {
            return (Boolean) entry;
        }
        if (entry instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) entry;
            if (m.containsKey(region)) {
                return Boolean.TRUE.equals(m.get(region));
            }
            return Boolean.TRUE.equals(m.get("default"));
        }
        return false;
    }

    public Map<String, Object> dump() {
        return new HashMap<String, Object>(cache);
    }
}
