package com.demb.integrations.erp;

import java.util.HashMap;
import java.util.Map;

public class SapBridge {
    private final String host;
    private final String user;
    private final String password;

    public SapBridge() {
        this("erp-onprem.corp.internal", "", "");
    }

    public SapBridge(String host, String user, String password) {
        this.host = host != null ? host : "erp-onprem.corp.internal";
        this.user = user != null ? user : "";
        this.password = password != null ? password : "";
    }

    public Map<String, Object> createDelivery(Map<String, Object> order) {
        int oid = order.get("id") instanceof Number ? ((Number) order.get("id")).intValue() : 0;
        Map<String, Object> delivery = new HashMap<String, Object>();
        delivery.put("VBELN", String.format("8%09d", oid));
        delivery.put("order_id", order.get("id"));
        delivery.put("items", order.get("lines"));
        String region = String.valueOf(order.get("region"));
        delivery.put("werks", region.startsWith("us") ? "US01" : "EU01");
        delivery.put("posted_at", System.currentTimeMillis() / 1000.0);
        Map<String, Object> gl = postGl(order);
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("ok", true);
        out.put("system", "sap");
        out.put("delivery", delivery);
        out.put("gl", gl);
        out.put("_host", host);
        return out;
    }

    public Map<String, Object> postGl(Map<String, Object> order) {
        Map<String, Object> gl = new HashMap<String, Object>();
        gl.put("BELNR", "GL-" + order.get("id"));
        gl.put("BUKRS", "1000");
        gl.put("WRBTR", order.get("total"));
        gl.put("WAERS", order.get("currency") != null ? order.get("currency") : "USD");
        return gl;
    }
}
