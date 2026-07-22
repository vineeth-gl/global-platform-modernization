package com.demb.integrations.crm;

import com.demb.monolith.Config;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class SalesforceAdapter {
    private final String token;
    private final String endpoint = "https://example--partial.my.salesforce.com/services/data/v42.0";

    public SalesforceAdapter() {
        this(Config.SALESFORCE_TOKEN);
    }

    public SalesforceAdapter(String token) {
        this.token = token;
    }

    public Map<String, Object> upsertOpportunity(Map<String, Object> order) {
        String oppId = "006" + md5(String.valueOf(order.get("id"))).substring(0, 12);
        Map<String, Object> doc = new HashMap<String, Object>();
        doc.put("Id", oppId);
        doc.put("Name", "Order-" + order.get("id"));
        doc.put("Amount", order.get("total"));
        doc.put("StageName", "Closed Won");
        doc.put("ExternalId__c", order.get("id"));
        doc.put("synced_at", System.currentTimeMillis() / 1000);
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("ok", true);
        out.put("system", "salesforce");
        out.put("opportunity", doc);
        out.put("_token_used", token != null);
        out.put("_endpoint", endpoint);
        return out;
    }

    private static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] dig = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : dig) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "deadbeef0000";
        }
    }
}
