package com.demb.billing;

import com.google.gson.Gson;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static spark.Spark.*;

/** AcmeBill-acquired billing service — v1 PascalCase + v2 REST. Java 8. */
public class BillingApp {
    private static final Gson GSON = new Gson();
    private static final Ledger LEDGER = new Ledger();
    private static final String MONOLITH = env("MONOLITH_URL", "http://localhost:8080");
    private static final String INVENTORY = env("INVENTORY_URL", "http://localhost:3001");

    public static void main(String[] args) {
        port(Integer.parseInt(env("PORT", "5001")));

        get("/health", (req, res) -> {
            res.type("application/json");
            return "{\"status\":\"ok\",\"service\":\"svc-billing\",\"engine\":\"acmebill-fork\",\"runtime\":\"java8\"}";
        });

        post("/v1/charge", (req, res) -> {
            res.type("application/json");
            Map<String, Object> body = map(req.body());
            String region = req.headers("X-Region") != null ? req.headers("X-Region") : "us-east";
            Map<String, Object> rated = RatingEngine.rate(
                    num(body, "Amount", "amount", "total"),
                    str(body, "Cur", "currency", "USD"),
                    region);
            Map<String, Object> modern = AcmeCompat.translateV1Charge(body);
            modern.put("amount", rated.get("grand_total"));
            Map<String, Object> inv = LEDGER.createInvoice(modern);
            inv.put("inventory_probe", httpGet(INVENTORY + "/api/stock/check?sku=SKU-100"));
            inv.put("rating", rated);
            Map<String, Object> syncBody = mapOf("order_id", inv.get("order_id"), "status", "CHARGED_V1");
            httpPostSigned(MONOLITH + "/api/v1/internal/billing-sync", syncBody);
            return GSON.toJson(mapOf("Status", "OK", "InvoiceId", inv.get("id"), "Amount", inv.get("amount"), "raw", inv));
        });

        post("/v2/invoices", (req, res) -> {
            res.type("application/json");
            Map<String, Object> body = map(req.body());
            String region = req.headers("X-Region") != null ? req.headers("X-Region") : "eu-west";
            Map<String, Object> rated = RatingEngine.rate(
                    num(body, "total", "amount", "Amount"),
                    str(body, "currency", "Cur", "USD"),
                    region);
            Map<String, Object> modern = new HashMap<String, Object>();
            modern.put("order_id", body.get("order_id") instanceof Number ? ((Number) body.get("order_id")).intValue() : 0);
            modern.put("amount", rated.get("grand_total"));
            modern.put("currency", str(body, "currency", "Cur", "USD"));
            modern.put("version", 2);
            Map<String, Object> inv = LEDGER.createInvoice(modern);
            Map<String, Object> syncBody = mapOf("order_id", inv.get("order_id"), "status", "PAID");
            httpPostSigned(MONOLITH + "/api/v1/internal/billing-sync", syncBody);
            LEDGER.archiveAcme(AcmeCompat.fromModern(inv));
            inv.put("rating", rated);
            inv.put("cutover_compare", RatingEngine.compareV1V2(
                    num(body, "total", "amount", "Amount"),
                    str(body, "currency", "Cur", "USD"), region));
            res.status(201);
            return GSON.toJson(inv);
        });

        get("/v2/invoices/:id", (req, res) -> {
            res.type("application/json");
            Map<String, Object> row = LEDGER.get(req.params("id"));
            if (row == null) {
                res.status(404);
                return "{\"error\":\"not_found\"}";
            }
            return GSON.toJson(row);
        });

        get("/v1/Invoices/GetByOrder", (req, res) -> {
            res.type("application/json");
            int oid = req.queryParams("OrderId") != null ? Integer.parseInt(req.queryParams("OrderId")) : 0;
            return GSON.toJson(mapOf("Invoices", LEDGER.byOrder(oid)));
        });

        post("/internal/reconciliation", (req, res) -> {
            res.type("application/json");
            return GSON.toJson(LEDGER.reconcile());
        });

        System.out.println("svc-billing (Java 8 / AcmeBill) on " + env("PORT", "5001"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(String json) {
        if (json == null || json.trim().isEmpty()) return new HashMap<String, Object>();
        return GSON.fromJson(json, Map.class);
    }

    private static String env(String k, String d) {
        String v = System.getenv(k);
        return v == null || v.isEmpty() ? d : v;
    }

    private static double num(Map<String, Object> m, String a, String b, String c) {
        Object v = m.containsKey(a) ? m.get(a) : m.containsKey(b) ? m.get(b) : m.get(c);
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            return v == null ? 0 : Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return 0;
        }
    }

    private static String str(Map<String, Object> m, String a, String b, String def) {
        if (m.containsKey(a) && m.get(a) != null) return String.valueOf(m.get(a));
        if (m.containsKey(b) && m.get(b) != null) return String.valueOf(m.get(b));
        return def;
    }

    private static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(String.valueOf(kv[i]), kv[i + 1]);
        return m;
    }

    private static Map<String, Object> httpGet(String url) {
        return http("GET", url, null);
    }

    private static void httpPostSigned(String url, Map<String, Object> body) {
        http("POST", url, body);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> http(String method, String url, Map<String, Object> body) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod(method);
            c.setConnectTimeout(2000);
            c.setReadTimeout(2000);
            c.setRequestProperty("Authorization", "Bearer " + MeshAuth.serviceJwt());
            String jsonBody = body != null ? GSON.toJson(body) : null;
            if (jsonBody != null) {
                c.setDoOutput(true);
                Map<String, String> wh = MeshAuth.webhookHeaders(jsonBody);
                for (Map.Entry<String, String> h : wh.entrySet()) {
                    c.setRequestProperty(h.getKey(), h.getValue());
                }
                c.getOutputStream().write(jsonBody.getBytes(StandardCharsets.UTF_8));
            } else {
                c.setRequestProperty("Content-Type", "application/json");
            }
            InputStream in = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
            if (in == null) return new HashMap<String, Object>();
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            Map<String, Object> parsed = GSON.fromJson(sb.toString(), Map.class);
            return parsed != null ? parsed : new HashMap<String, Object>();
        } catch (Exception e) {
            return mapOf("_error", e.getMessage());
        }
    }
}
