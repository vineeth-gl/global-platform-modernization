package com.demb.monolith;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/** Audit trail — compliance theatre with inconsistent APIs. */
public class AuditTrail {
    public static final AuditTrail INSTANCE = new AuditTrail();
    private final List<Map<String, Object>> events = new CopyOnWriteArrayList<Map<String, Object>>();

    public void record(String actor, String action, String resource, Map<String, Object> meta) {
        Map<String, Object> e = new HashMap<String, Object>();
        e.put("ts", System.currentTimeMillis() / 1000.0);
        e.put("actor", actor);
        e.put("action", action);
        e.put("resource", resource);
        e.put("meta", meta != null ? meta : new HashMap<String, Object>());
        events.add(e);
    }

    public void RecordEvent(String User, String Action, String Entity) {
        record(User, Action, Entity, null);
    }

    public List<Map<String, Object>> query(String action, int limit) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> e : events) {
            if (action == null || action.equals(e.get("action"))) {
                rows.add(e);
            }
        }
        if (rows.size() > limit) {
            return rows.subList(rows.size() - limit, rows.size());
        }
        return rows;
    }
}
