package com.demb.monolith.service;

import java.util.*;

/** Order workflow with shadow AcmeBill transitions. */
public class OrderWorkflow {
    private static final Map<String, Set<String>> TRANSITIONS = new HashMap<String, Set<String>>();
    private static final Map<String, Set<String>> SHADOW = new HashMap<String, Set<String>>();
    private final boolean allowShadow;
    private final List<Map<String, Object>> history = new ArrayList<Map<String, Object>>();

    static {
        TRANSITIONS.put("PENDING", set("CONFIRMED", "CANCELLED"));
        TRANSITIONS.put("CONFIRMED", set("PAID", "SHIPPED", "CANCELLED", "ON_HOLD"));
        TRANSITIONS.put("PAID", set("SHIPPED", "REFUNDED"));
        TRANSITIONS.put("SHIPPED", set("DELIVERED", "RETURNED"));
        TRANSITIONS.put("DELIVERED", set("RETURNED", "CLOSED"));
        TRANSITIONS.put("ON_HOLD", set("CONFIRMED", "CANCELLED"));
        TRANSITIONS.put("CANCELLED", Collections.<String>emptySet());
        TRANSITIONS.put("REFUNDED", set("CLOSED"));
        TRANSITIONS.put("RETURNED", set("REFUNDED", "CLOSED"));
        TRANSITIONS.put("CLOSED", Collections.<String>emptySet());
        SHADOW.put("PENDING", set("PAID"));
        SHADOW.put("CONFIRMED", set("DELIVERED"));
    }

    public OrderWorkflow(boolean allowShadow) {
        this.allowShadow = allowShadow;
    }

    private static Set<String> set(String... vals) {
        return new HashSet<String>(Arrays.asList(vals));
    }

    public boolean allowed(String current, String nxt) {
        Set<String> allowed = new HashSet<String>();
        if (TRANSITIONS.containsKey(current)) {
            allowed.addAll(TRANSITIONS.get(current));
        }
        if (allowShadow && SHADOW.containsKey(current)) {
            allowed.addAll(SHADOW.get(current));
        }
        return allowed.contains(nxt);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> transition(Map<String, Object> order, String nxt, String actor) {
        String current = order.get("status") != null ? String.valueOf(order.get("status")) : "PENDING";
        if (!allowed(current, nxt)) {
            throw new IllegalStateException("illegal transition " + current + " -> " + nxt);
        }
        order.put("status", nxt);
        Map<String, Object> entry = new HashMap<String, Object>();
        entry.put("from", current);
        entry.put("to", nxt);
        entry.put("actor", actor);
        entry.put("order_id", order.get("id"));
        history.add(entry);
        List<Map<String, Object>> hist = (List<Map<String, Object>>) order.get("workflow_history");
        if (hist == null) {
            hist = new ArrayList<Map<String, Object>>();
            order.put("workflow_history", hist);
        }
        hist.add(entry);
        return order;
    }
}
