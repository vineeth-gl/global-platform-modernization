package com.demb.billing;

import java.util.*;

public class Ledger {
    private final Map<String, Map<String, Object>> invoices = new LinkedHashMap<String, Map<String, Object>>();
    private final List<Map<String, Object>> acmeArchive = new ArrayList<Map<String, Object>>();

    public Map<String, Object> createInvoice(Map<String, Object> data) {
        String invId = "INV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("id", invId);
        row.put("order_id", data.get("order_id") instanceof Number ? ((Number) data.get("order_id")).intValue() : 0);
        row.put("amount", data.get("amount") instanceof Number ? ((Number) data.get("amount")).doubleValue() : 0);
        row.put("currency", data.get("currency") != null ? data.get("currency") : "USD");
        row.put("version", data.get("version") != null ? data.get("version") : 1);
        row.put("status", "OPEN");
        row.put("created_at", new Date().toInstant().toString());
        invoices.put(invId, row);
        return new HashMap<String, Object>(row);
    }

    public Map<String, Object> get(String invId) {
        Map<String, Object> row = invoices.get(invId);
        return row == null ? null : new HashMap<String, Object>(row);
    }

    public List<Map<String, Object>> byOrder(int orderId) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> v : invoices.values()) {
            if (orderId == ((Number) v.get("order_id")).intValue()) {
                out.add(new HashMap<String, Object>(v));
            }
        }
        return out;
    }

    public void archiveAcme(Map<String, Object> acmeDoc) {
        acmeArchive.add(new HashMap<String, Object>(acmeDoc));
    }

    public Map<String, Object> reconcile() {
        double total = 0;
        for (Map<String, Object> v : invoices.values()) {
            total += ((Number) v.get("amount")).doubleValue();
        }
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("invoice_count", invoices.size());
        out.put("acme_archive_count", acmeArchive.size());
        out.put("amount_sum", Math.round(total * 100.0) / 100.0);
        out.put("drift_warning", acmeArchive.size() != invoices.size());
        return out;
    }
}
