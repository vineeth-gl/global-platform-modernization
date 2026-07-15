package com.demb.monolith.service;

import com.demb.monolith.IntegrationsFacade;
import com.demb.monolith.db.MongoBridge;
import com.demb.monolith.db.SqlStore;

import java.util.*;

/** Reporting god-object. */
public class ReportEngine {
    private final SqlStore sql;
    private final MongoBridge mongo;
    private final IntegrationsFacade integrations;

    public ReportEngine(SqlStore sql, MongoBridge mongo, IntegrationsFacade integrations) {
        this.sql = sql;
        this.mongo = mongo;
        this.integrations = integrations;
    }

    public Map<String, Object> salesByRegion(int days) {
        Map<String, Map<String, Object>> byRegion = new HashMap<String, Map<String, Object>>();
        for (Map<String, Object> o : sql.listOrders(null, 500)) {
            String r = o.get("region") != null ? String.valueOf(o.get("region")) : "unknown";
            if (!byRegion.containsKey(r)) {
                Map<String, Object> agg = new HashMap<String, Object>();
                agg.put("count", 0);
                agg.put("total", 0.0);
                byRegion.put(r, agg);
            }
            Map<String, Object> agg = byRegion.get(r);
            agg.put("count", ((Integer) agg.get("count")) + 1);
            double t = o.get("total") instanceof Number ? ((Number) o.get("total")).doubleValue() : 0;
            agg.put("total", ((Double) agg.get("total")) + t);
        }
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("generated_at", new Date().toInstant().toString());
        out.put("days", days);
        out.put("regions", byRegion);
        return out;
    }

    public Map<String, Object> vipVsStandard() {
        List<Map<String, Object>> sqlC = sql.searchCustomers("");
        List<Map<String, Object>> vipC = mongo.listVips();
        Set<String> sqlIds = new HashSet<String>();
        for (Map<String, Object> c : sqlC) sqlIds.add(String.valueOf(c.get("id")));
        Set<String> vipIds = new HashSet<String>();
        for (Map<String, Object> v : vipC) vipIds.add(String.valueOf(v.get("id")));
        sqlIds.retainAll(vipIds);
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("sql_customers", sqlC.size());
        out.put("mongo_vips", vipC.size());
        out.put("overlap_ids", new ArrayList<String>(sqlIds));
        out.put("warning", "VIP records may be missing from SQL");
        return out;
    }

    public Map<String, Object> slaBurn() {
        double target = 0.9995;
        int downtime = 22;
        int minutesMonth = 30 * 24 * 60;
        double availability = 1.0 - (downtime / (double) minutesMonth);
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("target", target);
        out.put("observed", Math.round(availability * 1_000_000.0) / 1_000_000.0);
        out.put("breach", availability < target);
        out.put("downtime_minutes", downtime);
        return out;
    }

    public Map<String, Object> pushMarketingDigest() {
        return integrations.track("weekly_digest", salesByRegion(30));
    }
}
