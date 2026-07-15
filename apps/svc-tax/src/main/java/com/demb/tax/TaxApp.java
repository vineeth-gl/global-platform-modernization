package com.demb.tax;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

/** Tax microservice leftover — rate drift vs monolith i18n. Java 8. */
public class TaxApp {
    private static final Gson GSON = new Gson();
    private static final Map<String, Double> RATES = new HashMap<String, Double>();

    static {
        RATES.put("US", 0.0725); // drift vs monolith 0.07
        RATES.put("CA", 0.13);
        RATES.put("GB", 0.20);
        RATES.put("DE", 0.19);
        RATES.put("FR", 0.20);
        RATES.put("IN", 0.18);
        RATES.put("AU", 0.10);
        RATES.put("SG", 0.09); // drift vs 0.08
        RATES.put("JP", 0.10);
        RATES.put("DEFAULT", 0.10);
    }

    public static void main(String[] args) {
        port(Integer.parseInt(env("PORT", "5002")));

        get("/health", (req, res) -> {
            res.type("application/json");
            return "{\"status\":\"ok\",\"service\":\"svc-tax\",\"runtime\":\"java8\"}";
        });

        post("/tax/compute", (req, res) -> {
            res.type("application/json");
            @SuppressWarnings("unchecked")
            Map<String, Object> body = GSON.fromJson(req.body() == null || req.body().isEmpty() ? "{}" : req.body(), Map.class);
            String country = body.get("country") != null ? String.valueOf(body.get("country")).toUpperCase() : "US";
            double subtotal = body.get("subtotal") instanceof Number ? ((Number) body.get("subtotal")).doubleValue() : 0;
            return GSON.toJson(computeTax(country, subtotal));
        });

        System.out.println("svc-tax (Java 8) on " + env("PORT", "5002"));
    }

    public static Map<String, Object> computeTax(String country, double subtotal) {
        Double rate = RATES.containsKey(country) ? RATES.get(country) : RATES.get("DEFAULT");
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("country", country);
        out.put("rate", rate);
        out.put("tax", Math.round(subtotal * rate * 100.0) / 100.0);
        out.put("engine", "tax-sidecar-2018");
        return out;
    }

    /** PascalCase duplicate */
    public static Map<String, Object> ComputeTax(String Country, double SubTotal) {
        return computeTax(Country, SubTotal);
    }

    private static String env(String k, String d) {
        String v = System.getenv(k);
        return v == null || v.isEmpty() ? d : v;
    }
}
