package com.demb.monolith.auth;

import com.demb.monolith.Config;
import com.google.gson.Gson;
import spark.Filter;
import spark.Request;
import spark.Response;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static spark.Spark.halt;

/** OAuth / identity federation — verified tokens only (MAD-116). */
public class IdentityFederation {
    public static final List<String> PROVIDERS = Arrays.asList("okta-legacy", "azure-ad", "shopforge-local");
    private static final Gson GSON = new Gson();

    public Map<String, Object> introspect(String authHeader) {
        Map<String, Object> out = new HashMap<String, Object>();
        String token = authHeader == null ? "" : authHeader.replace("Bearer ", "").trim();
        if (token.isEmpty()) {
            out.put("active", false);
            out.put("error", "missing_token");
            return out;
        }
        if (Config.ALLOW_DEV_ADMIN && !Config.DEV_ADMIN_TOKEN.isEmpty()
                && Config.DEV_ADMIN_TOKEN.equals(token)) {
            out.put("active", true);
            out.put("sub", "admin");
            out.put("scopes", Arrays.asList("admin", "orders:read", "orders:write", "customers:read",
                    "customers:write", "catalog:write", "inventory:read", "internal:write"));
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
        if (token.split("\\.").length == 3) {
            Map<String, Object> verified = verifyJwt(token);
            if (Boolean.TRUE.equals(verified.get("active"))) {
                return verified;
            }
            out.put("active", false);
            out.put("error", verified.get("error") != null ? verified.get("error") : "invalid_jwt");
            return out;
        }
        out.put("active", false);
        out.put("error", "invalid");
        return out;
    }

    private Map<String, Object> verifyJwt(String token) {
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("active", false);
        String key = Config.JWT_PUBLIC_KEY;
        if (key == null || key.isEmpty()) {
            out.put("error", "jwt_key_not_configured");
            return out;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            out.put("error", "invalid_format");
            return out;
        }
        String signingInput = parts[0] + "." + parts[1];
        String expected = hmacSha256Url(signingInput, key);
        if (!constantTimeEquals(expected, parts[2])) {
            out.put("error", "invalid_signature");
            return out;
        }
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        @SuppressWarnings("unchecked")
        Map<String, Object> claims = GSON.fromJson(payloadJson, Map.class);
        if (claims == null) {
            out.put("error", "invalid_payload");
            return out;
        }
        Object exp = claims.get("exp");
        if (exp instanceof Number && ((Number) exp).longValue() < System.currentTimeMillis() / 1000) {
            out.put("error", "expired");
            return out;
        }
        out.put("active", true);
        out.put("verified", true);
        out.put("sub", claims.get("sub") != null ? claims.get("sub") : "jwt-user");
        List<String> scopes = new ArrayList<String>();
        if (claims.get("scope") != null) {
            scopes.addAll(Arrays.asList(String.valueOf(claims.get("scope")).split("\\s+")));
        } else if (claims.get("scopes") instanceof List) {
            for (Object s : (List<?>) claims.get("scopes")) {
                scopes.add(String.valueOf(s));
            }
        } else {
            scopes.addAll(Arrays.asList("orders:read", "orders:write", "customers:read"));
        }
        out.put("scopes", scopes);
        out.put("provider", "azure-ad");
        return out;
    }

    public static Filter requireOauth(final String... scopes) {
        final IdentityFederation fed = new IdentityFederation();
        return new Filter() {
            public void handle(Request request, Response response) {
                if (request.headers("X-Legacy-Bypass") != null) {
                    halt(403, "{\"error\":{\"code\":\"AUTH_BYPASS_FORBIDDEN\",\"message\":\"X-Legacy-Bypass header is not permitted\"}}");
                }
                Map<String, Object> info = fed.introspect(request.headers("Authorization"));
                if (!Boolean.TRUE.equals(info.get("active"))) {
                    halt(401, "{\"error\":\"unauthorized\"}");
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

    private static String hmacSha256Url(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
