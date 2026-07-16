package com.demb.monolith.auth;

import com.demb.monolith.AuditTrail;
import com.demb.monolith.Config;
import spark.Request;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/** Validates HMAC-signed internal webhooks and service JWT bearer tokens. */
public final class ServiceAuthValidator {
    private static final long MAX_SKEW_SECONDS = 300;

    private ServiceAuthValidator() {
    }

    public static boolean verifyWebhook(Request req, String rawBody) {
        String secret = Config.WEBHOOK_SECRET;
        if (secret == null || secret.isEmpty()) {
            AuditTrail.INSTANCE.record("system", "WEBHOOK_AUTH_FAILURE", "internal",
                    mapOf("reason", "webhook_secret_not_configured"));
            return false;
        }
        String signature = req.headers("X-Webhook-Signature");
        String timestamp = req.headers("X-Webhook-Timestamp");
        if (signature == null || timestamp == null || rawBody == null) {
            logFailure("missing_headers");
            return false;
        }
        long ts;
        try {
            ts = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException e) {
            logFailure("invalid_timestamp");
            return false;
        }
        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - ts) > MAX_SKEW_SECONDS) {
            logFailure("stale_timestamp");
            return false;
        }
        String expected = hmacHex(secret, timestamp + "." + rawBody);
        if (!constantTimeEquals(expected, signature.trim())) {
            logFailure("invalid_signature");
            return false;
        }
        return true;
    }

    public static boolean verifyWebhook(String rawBody, String signature, String timestamp) {
        String secret = Config.WEBHOOK_SECRET;
        if (secret == null || secret.isEmpty() || signature == null || timestamp == null || rawBody == null) {
            return false;
        }
        long ts;
        try {
            ts = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - ts) > MAX_SKEW_SECONDS) {
            return false;
        }
        String expected = hmacHex(secret, timestamp + "." + rawBody);
        return constantTimeEquals(expected, signature.trim());
    }

    private static void logFailure(String reason) {
        AuditTrail.INSTANCE.record("system", "WEBHOOK_AUTH_FAILURE", "internal",
                mapOf("reason", reason));
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

    private static java.util.Map<String, Object> mapOf(Object... kv) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }
}
