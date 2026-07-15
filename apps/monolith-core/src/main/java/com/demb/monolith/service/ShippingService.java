package com.demb.monolith.service;

import com.demb.monolith.db.SqlStore;

import java.security.MessageDigest;
import java.util.*;

public class ShippingService {
    private static final Map<String, List<String>> CARRIERS = new HashMap<String, List<String>>();
    private static final Map<String, Integer> SLA_DAYS = new HashMap<String, Integer>();
    private final SqlStore sql;
    private final Map<String, Map<String, Object>> shipments = new LinkedHashMap<String, Map<String, Object>>();

    static {
        CARRIERS.put("us-east", Arrays.asList("UPS", "FedEx", "USPS"));
        CARRIERS.put("eu-west", Arrays.asList("DHL", "DPD", "GLS"));
        CARRIERS.put("ap-south", Arrays.asList("BlueDart", "Delhivery", "JapanPost"));
        SLA_DAYS.put("UPS", 2);
        SLA_DAYS.put("FedEx", 2);
        SLA_DAYS.put("DHL", 3);
        SLA_DAYS.put("DEFAULT", 5);
    }

    public ShippingService(SqlStore sql) {
        this.sql = sql;
    }

    public String chooseCarrier(String region, String priority) {
        List<String> options = CARRIERS.containsKey(region) ? CARRIERS.get(region) : Arrays.asList("GENERIC");
        if ("express".equals(priority)) {
            return options.get(0);
        }
        return options.get(options.size() - 1);
    }

    public Map<String, Object> createShipment(int orderId, String region) {
        Map<String, Object> order = sql.getOrder(orderId);
        if (order == null) {
            throw new IllegalArgumentException("order missing");
        }
        String carrier = chooseCarrier(region, "standard");
        String tracking = "TRK" + md5(orderId + "-" + carrier).substring(0, 12).toUpperCase();
        Map<String, Object> shipment = new HashMap<String, Object>();
        shipment.put("order_id", orderId);
        shipment.put("carrier", carrier);
        shipment.put("tracking", tracking);
        shipment.put("region", region);
        shipment.put("eta_days", SLA_DAYS.containsKey(carrier) ? SLA_DAYS.get(carrier) : SLA_DAYS.get("DEFAULT"));
        shipment.put("status", "LABEL_CREATED");
        shipments.put(tracking, shipment);
        Map<String, Object> patch = new HashMap<String, Object>();
        patch.put("shipping", shipment);
        patch.put("status", "SHIPPED");
        sql.updateOrder(orderId, patch);
        return shipment;
    }

    public Map<String, Object> track(String tracking) {
        Map<String, Object> s = shipments.get(tracking);
        if (s == null) {
            Map<String, Object> err = new HashMap<String, Object>();
            err.put("error", "not_found");
            err.put("tracking", tracking);
            return err;
        }
        return s;
    }

    private static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] dig = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "deadbeefcafe";
        }
    }
}
