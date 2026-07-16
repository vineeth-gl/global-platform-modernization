package com.demb.monolith.auth;

import com.demb.monolith.Config;
import com.google.gson.Gson;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Short-lived HMAC-SHA256 service JWT for mesh calls (5-minute TTL). */
public final class ServiceJwt {
    private static final Gson GSON = new Gson();
    private static final long TTL_SECONDS = 300;

    private ServiceJwt() {
    }

    public static String generate(String issuer, String subject, List<String> audience, List<String> scopes) {
        String secret = Config.SERVICE_JWT_SECRET;
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("SERVICE_JWT_SECRET is required for service-to-service calls");
        }
        long now = System.currentTimeMillis() / 1000;
        Map<String, Object> header = new HashMap<String, Object>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("iss", issuer);
        claims.put("sub", subject);
        claims.put("aud", audience);
        claims.put("scope", String.join(" ", scopes));
        claims.put("iat", now);
        claims.put("exp", now + TTL_SECONDS);
        String headerPart = base64Url(GSON.toJson(header));
        String payloadPart = base64Url(GSON.toJson(claims));
        String signingInput = headerPart + "." + payloadPart;
        String signature = hmacSha256(signingInput, secret);
        return signingInput + "." + signature;
    }

    public static String generateForMonolith() {
        return generate(
                "monolith-core",
                "service:monolith-core",
                Arrays.asList("svc-inventory", "svc-billing", "svc-notify"),
                Arrays.asList("internal:write"));
    }

    public static Map<String, Object> verify(String token) {
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("active", false);
        if (token == null || token.isEmpty()) {
            out.put("error", "missing_token");
            return out;
        }
        String secret = Config.SERVICE_JWT_SECRET;
        if (secret == null || secret.isEmpty()) {
            out.put("error", "service_jwt_not_configured");
            return out;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            out.put("error", "invalid_format");
            return out;
        }
        String signingInput = parts[0] + "." + parts[1];
        String expected = hmacSha256(signingInput, secret);
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
        out.put("sub", claims.get("sub"));
        String scopeStr = claims.get("scope") != null ? String.valueOf(claims.get("scope")) : "";
        out.put("scopes", Arrays.asList(scopeStr.split("\\s+")));
        return out;
    }

    private static String base64Url(String raw) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64UrlBytes(raw);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC signing failed", e);
        }
    }

    private static String base64UrlBytes(byte[] raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
