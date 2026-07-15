package com.demb.monolith.service;

import com.demb.monolith.db.MongoBridge;
import com.demb.monolith.db.SqlStore;

import java.util.*;

/** Customer service — SQL + Mongo split brain. */
public class CustomerService {
    private final SqlStore sql;
    private final MongoBridge mongo;

    public CustomerService(SqlStore sql, MongoBridge mongo) {
        this.sql = sql;
        this.mongo = mongo;
    }

    public List<Map<String, Object>> search(String q) {
        List<Map<String, Object>> sqlRows = sql.searchCustomers(q);
        List<Map<String, Object>> vipRows = mongo.listVips();
        if (q != null && !q.isEmpty()) {
            String ql = q.toLowerCase();
            List<Map<String, Object>> filtered = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> v : vipRows) {
                if (String.valueOf(v).toLowerCase().contains(ql)) {
                    filtered.add(v);
                }
            }
            vipRows = filtered;
        }
        Map<String, Map<String, Object>> merged = new LinkedHashMap<String, Map<String, Object>>();
        for (Map<String, Object> r : sqlRows) {
            merged.put(String.valueOf(r.get("id")), r);
        }
        for (Map<String, Object> v : vipRows) {
            String id = String.valueOf(v.get("id"));
            Map<String, Object> base = merged.containsKey(id) ? new HashMap<String, Object>(merged.get(id)) : new HashMap<String, Object>();
            base.putAll(v);
            base.put("vip", true);
            merged.put(id, base);
        }
        return new ArrayList<Map<String, Object>>(merged.values());
    }

    public Map<String, Object> create(Map<String, Object> body, boolean vip) {
        String cid = body.get("id") != null ? String.valueOf(body.get("id"))
                : "C-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        Map<String, Object> cust = new HashMap<String, Object>();
        cust.put("id", cid);
        cust.put("email", first(body, "email", "Email", ""));
        cust.put("name", first(body, "name", "Name", "Unknown"));
        cust.put("country", first(body, "country", "Country", "US"));
        cust.put("vip", vip);
        cust.put("source", vip ? "mongo" : "sql");
        if (vip) {
            return mongo.saveVip(cust);
        }
        return sql.upsertCustomer(cust);
    }

    public Map<String, Object> get(String custId) {
        Map<String, Object> vip = mongo.getVip(custId);
        if (vip != null) {
            return vip;
        }
        return sql.getCustomer(custId);
    }

    /** ShopForge naming */
    public Map<String, Object> LoadCust(String custId) {
        return get(custId);
    }

    /** React portal team naming */
    public Map<String, Object> fetchCustomer(String custId) {
        return get(custId);
    }

    private static Object first(Map<String, Object> m, String a, String b, Object def) {
        if (m.containsKey(a) && m.get(a) != null) return m.get(a);
        if (m.containsKey(b) && m.get(b) != null) return m.get(b);
        return def;
    }
}
