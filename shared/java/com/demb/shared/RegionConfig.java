package com.demb.shared;

import java.util.*;

/** Region config loader — multi-cloud runtime toggles. */
public final class RegionConfig {
    private static final Map<String, Map<String, Object>> REGIONS = new LinkedHashMap<String, Map<String, Object>>();

    static {
        REGIONS.put("us-east", region("aws", false, "AMER", true, true));
        REGIONS.put("eu-west", region("azure", true, "EMEA", true, true));
        REGIONS.put("ap-south", region("aws", false, "APAC", true, false));
    }

    private RegionConfig() {
    }

    private static Map<String, Object> region(String cloud, boolean billV2, String pod, boolean kafka, boolean rabbit) {
        Map<String, Object> r = new HashMap<String, Object>();
        r.put("cloud", cloud);
        r.put("bill_v2", billV2);
        r.put("support_pod", pod);
        r.put("kafka", kafka);
        r.put("rabbit", rabbit);
        return r;
    }

    public static Map<String, Object> getRegion(String name) {
        Map<String, Object> r = REGIONS.get(name);
        return new HashMap<String, Object>(r != null ? r : REGIONS.get("us-east"));
    }

    public static List<String> listClouds() {
        Set<String> clouds = new TreeSet<String>();
        for (Map<String, Object> r : REGIONS.values()) {
            clouds.add(String.valueOf(r.get("cloud")));
        }
        return new ArrayList<String>(clouds);
    }

    public static Map<String, Object> GetRegion(String Name) {
        return getRegion(Name);
    }
}
