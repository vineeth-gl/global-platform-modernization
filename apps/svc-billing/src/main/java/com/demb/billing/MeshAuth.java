package com.demb.billing;

import com.google.gson.Gson;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Service JWT and webhook signing for billing mesh calls. */
final class MeshAuth {
    private static final Gson GSON = new Gson();
    private static final long TTL_SECONDS = 300;

    private MeshAuth() {
    }

    static String serviceJwt() {
        String secret = env("SERVICE_JWT_SECRET", "");
        if (secret.isEmpty()) {
            throw new IllegalStateException("SERVICE_JWT_SECRET is required");
        }
        long now = System.currentTimeMillis() / 1000;
        Map<String, Object> header = new HashMap<String, Object>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("iss", "svc-billing");
        claims.put("sub", "service:svc-billing");
        claims.put("aud", Arrays.asList("monolith-core", "svc-inventory"));
        claims.put("scope", "internal:write");
        claims.put("iat", now);
        claims.put("exp", now + TTL_SECONDS);
        String headerPart = base64Url(GSON.toJson(header));
        String payloadPart = base64Url(GSON.toJson(claims));
        String signingInput = headerPart + "." + payloadPart;
        return signingInput + "." + hmacSha256Url(signingInput, secret);
    }

    static Map<String, String> webhookHeaders(String rawBody) {
        String secret = env("WEBHOOK_SECRET", "");
        if (secret.isEmpty()) {
            throw new IllegalStateException("WEBHOOK_SECRET is required");
        }
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signature = hmacHex(secret, timestamp + "." + rawBody);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-Webhook-Timestamp", timestamp);
        headers.put("X-Webhook-Signature", signature);
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private static String env(String k, String d) {
        String v = System.getenv(k);
        return v == null || v.isEmpty() ? d : v;
    }

    private static String base64Url(String raw) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSha256Url(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failed", e);
        }
    }

    private static String hmacHex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
