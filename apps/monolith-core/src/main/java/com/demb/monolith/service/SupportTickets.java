package com.demb.monolith.service;

import com.demb.monolith.I18nRegions;

import java.util.*;

/** Support ticket bridge — 24x7 follow-the-sun. */
public class SupportTickets {
    private static final Map<String, Integer> PRIORITIES = new HashMap<String, Integer>();
    private final Map<String, Map<String, Object>> tickets = new LinkedHashMap<String, Map<String, Object>>();

    static {
        PRIORITIES.put("P1", 4);
        PRIORITIES.put("P2", 24);
        PRIORITIES.put("P3", 72);
        PRIORITIES.put("P4", 168);
    }

    public Map<String, Object> create(String subject, String priority, String country, String body) {
        String tid = "INC" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        String pod = I18nRegions.supportPodForHour(cal.get(Calendar.HOUR_OF_DAY));
        if (!PRIORITIES.containsKey(priority)) {
            priority = "P3";
        }
        Map<String, Object> ticket = new HashMap<String, Object>();
        ticket.put("id", tid);
        ticket.put("subject", subject);
        ticket.put("priority", priority);
        ticket.put("country", country);
        ticket.put("body", body);
        ticket.put("pod", pod);
        ticket.put("sla_hours", PRIORITIES.get(priority));
        ticket.put("status", "OPEN");
        ticket.put("created_at", new Date().toInstant().toString());
        ticket.put("updates", new ArrayList<Map<String, Object>>());
        tickets.put(tid, ticket);
        return ticket;
    }

    public List<Map<String, Object>> listOpen(String pod) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> t : tickets.values()) {
            if (!"OPEN".equals(t.get("status"))) continue;
            if (pod == null || pod.equals(t.get("pod"))) {
                rows.add(t);
            }
        }
        return rows;
    }
}
