package com.demb.integrations.erp;

import java.util.HashMap;
import java.util.Map;

public class SapBridge {
    private final String host = "erp-onprem.corp.internal";
    private final String user = "RFC_DEMb";
    private final String password = "SapRfc!2016"; // secret in code

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
        out.put("_user", user);
        out.put("_password_set", password != null);
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
