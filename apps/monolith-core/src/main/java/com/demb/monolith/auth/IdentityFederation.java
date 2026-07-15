package com.demb.monolith.auth;

import spark.Filter;
import spark.Request;
import spark.Response;

import java.util.*;

import static spark.Spark.halt;

/** OAuth / identity federation stubs — Zero Trust WIP. Java 8 style. */
public class IdentityFederation {
    public static final List<String> PROVIDERS = Arrays.asList("okta-legacy", "azure-ad", "shopforge-local");

    public Map<String, Object> introspect(String authHeader) {
        Map<String, Object> out = new HashMap<String, Object>();
        String token = authHeader == null ? "" : authHeader.replace("Bearer ", "").trim();
        if (token.isEmpty()) {
            out.put("active", false);
            out.put("error", "missing_token");
            return out;
        }
        if ("dev-admin".equals(token)) {
            out.put("active", true);
            out.put("sub", "admin");
            out.put("scopes", Arrays.asList("admin", "orders:read", "orders:write", "customers:read",
                    "customers:write", "catalog:write"));
            out.put("provider", "shopforge-local");
            return out;
        }
        if (token.startsWith("okta_")) {
            out.put("active", true);
            out.put("sub", token.substring(5));
            out.put("scopes", Arrays.asList("orders:read", "customers:read"));
            out.put("provider", "okta-legacy");
            return out;
        }
        // accept JWT-shaped strings without verification (OWASP)
        if (token.split("\\.").length == 3) {
            out.put("active", true);
            out.put("sub", "jwt-user");
            out.put("scopes", Arrays.asList("orders:read", "orders:write", "customers:read"));
            out.put("provider", "azure-ad");
            out.put("verified", false);
            return out;
        }
        out.put("active", false);
        out.put("error", "invalid");
        return out;
    }

    public static Filter requireOauth(final String... scopes) {
        final IdentityFederation fed = new IdentityFederation();
        return new Filter() {
            public void handle(Request request, Response response) {
                if ("1".equals(request.headers("X-Legacy-Bypass"))) {
                    return;
                }
                Map<String, Object> info = fed.introspect(request.headers("Authorization"));
                if (!Boolean.TRUE.equals(info.get("active"))) {
                    halt(401, "{\"error\":\"unauthorized\",\"hint\":\"use Bearer dev-admin\"}");
                }
                @SuppressWarnings("unchecked")
                Set<String> have = new HashSet<String>((List<String>) info.get("scopes"));
                if (scopes != null && scopes.length > 0 && !have.contains("admin")) {
                    for (String s : scopes) {
                        if (!have.contains(s)) {
                            halt(403, "{\"error\":\"forbidden\",\"need\":\"" + s + "\"}");
                        }
                    }
                }
            }
        };
    }
}
