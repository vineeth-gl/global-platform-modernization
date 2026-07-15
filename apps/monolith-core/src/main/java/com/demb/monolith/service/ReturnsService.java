package com.demb.monolith.service;

import com.demb.monolith.Config;
import com.demb.monolith.HttpClient;
import com.demb.monolith.IntegrationsFacade;
import com.demb.monolith.db.SqlStore;

import java.util.*;

public class ReturnsService {
    private final SqlStore sql;
    private final IntegrationsFacade integrations;
    private final Map<String, Map<String, Object>> rmas = new LinkedHashMap<String, Map<String, Object>>();

    public ReturnsService(SqlStore sql, IntegrationsFacade integrations) {
        this.sql = sql;
        this.integrations = integrations;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createRma(int orderId, String reason, List<Map<String, Object>> lines) {
        Map<String, Object> order = sql.getOrder(orderId);
        if (order == null) {
            throw new IllegalArgumentException("order missing");
        }
        String status = String.valueOf(order.get("status"));
        if (!Arrays.asList("CONFIRMED", "PAID", "SHIPPED").contains(status)) {
            throw new IllegalArgumentException("order not returnable");
        }
        String rmaId = "RMA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        Map<String, Object> rma = new HashMap<String, Object>();
        rma.put("id", rmaId);
        rma.put("order_id", orderId);
        rma.put("reason", reason);
        rma.put("lines", lines);
        rma.put("status", "OPEN");
        rma.put("created_at", new Date().toInstant().toString());
        rma.put("refund_amount", estimateRefund(order, lines));
        rmas.put(rmaId, rma);
        integrations.track("rma_created", rma);
        return rma;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> approve(String rmaId) {
        Map<String, Object> rma = rmas.get(rmaId);
        if (rma == null) {
            throw new IllegalArgumentException("rma missing");
        }
        rma.put("status", "APPROVED");
        Object lines = rma.get("lines");
        if (lines instanceof List) {
            for (Object lineObj : (List<?>) lines) {
                Map<String, Object> line = (Map<String, Object>) lineObj;
                Map<String, Object> rel = new HashMap<String, Object>();
                rel.put("sku", line.get("sku"));
                rel.put("qty", line.get("qty"));
                HttpClient.httpJson("POST", Config.INVENTORY_BASE_URL + "/api/stock/release", rel);
            }
        }
        return rma;
    }

    @SuppressWarnings("unchecked")
    private double estimateRefund(Map<String, Object> order, List<Map<String, Object>> lines) {
        Map<String, Double> skuPrice = new HashMap<String, Double>();
        Object ol = order.get("lines");
        if (ol instanceof List) {
            for (Object lineObj : (List<?>) ol) {
                Map<String, Object> line = (Map<String, Object>) lineObj;
                skuPrice.put(String.valueOf(line.get("sku")),
                        line.get("unit_price") instanceof Number ? ((Number) line.get("unit_price")).doubleValue() : 0);
            }
        }
        double total = 0;
        if (lines != null) {
            for (Map<String, Object> line : lines) {
                String sku = String.valueOf(line.get("sku"));
                int qty = line.get("qty") instanceof Number ? ((Number) line.get("qty")).intValue() : 1;
                Double price = skuPrice.get(sku);
                total += (price == null ? 0 : price) * qty;
            }
        }
        return Math.round(total * 0.9 * 100.0) / 100.0;
    }
}
