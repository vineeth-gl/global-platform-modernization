package com.demb.monolith.db;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Mongo bridge for VIP customers — Azure-hosted NoSQL island. */
public class MongoBridge {
    private final Map<String, Map<String, Object>> vipCustomers = new ConcurrentHashMap<String, Map<String, Object>>();
    private final Map<Integer, Map<String, Object>> orderShadows = new ConcurrentHashMap<Integer, Map<String, Object>>();
    private final String uri;

    public MongoBridge(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public Map<String, Object> saveVip(Map<String, Object> cust) {
        String cid = String.valueOf(cust.get("id"));
        Map<String, Object> doc = new HashMap<String, Object>(cust);
        doc.put("source", "mongo");
        doc.put("collection", "vip_customers");
        vipCustomers.put(cid, doc);
        return new HashMap<String, Object>(doc);
    }

    public Map<String, Object> getVip(String custId) {
        Map<String, Object> doc = vipCustomers.get(String.valueOf(custId));
        return doc == null ? null : new HashMap<String, Object>(doc);
    }

    public List<Map<String, Object>> listVips() {
        return new ArrayList<Map<String, Object>>(vipCustomers.values());
    }

    public void shadowOrder(Map<String, Object> order) {
        int id = ((Number) order.get("id")).intValue();
        orderShadows.put(id, new HashMap<String, Object>(order));
    }

    public Map<String, Object> findShadow(int orderId) {
        Map<String, Object> s = orderShadows.get(orderId);
        return s == null ? null : new HashMap<String, Object>(s);
    }
}
