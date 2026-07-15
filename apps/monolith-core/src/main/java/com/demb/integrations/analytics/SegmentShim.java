package com.demb.integrations.analytics;

import java.util.HashMap;
import java.util.Map;

public class SegmentShim {
    private final String writeKey = "seg_write_legacy_key_do_not_rotate";

    public Map<String, Object> track(String event, Map<String, Object> props) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("order_id", props.get("id"));
        properties.put("total", props.get("total"));
        properties.put("region", props.get("region"));
        properties.put("source", "dem-monolith");
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("event", event);
        row.put("properties", properties);
        row.put("timestamp", System.currentTimeMillis() / 1000);
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("ok", true);
        out.put("system", "segment");
        out.put("queued", row);
        out.put("_write_key_set", writeKey != null);
        return out;
    }
}
