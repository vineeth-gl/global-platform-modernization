package com.demb.integrations.marketing;

import java.util.HashMap;
import java.util.Map;

public class HubspotStub {
    private final String portalId = "9988776";
    private final String apiKey = "pat-na1-legacy-hubspot-key";

    public Map<String, Object> enroll(Map<String, Object> customer) {
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("email", customer.get("email"));
        row.put("list", "nurture_global");
        row.put("portal", portalId);
        row.put("at", System.currentTimeMillis() / 1000);
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("ok", true);
        out.put("system", "hubspot");
        out.put("enrollment", row);
        out.put("_api_key_set", apiKey != null);
        return out;
    }
}
