package com.demb.monolith.auth;

import org.junit.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.Assert.*;

public class IdentityFederationTest {
    private static final String SECRET = "rotated-test-secret";

    @Test
    public void rejectsDevAdminUnlessExplicitlyAllowed() {
        IdentityFederation federation = new IdentityFederation(SECRET, false);

        Map<String, Object> result = federation.introspect("Bearer dev-admin");

        assertEquals(false, result.get("active"));
        assertEquals("dev_admin_disabled", result.get("error"));
    }

    @Test
    public void allowsDevAdminOnlyWhenLocalFlagIsEnabled() {
        IdentityFederation federation = new IdentityFederation(SECRET, true);

        Map<String, Object> result = federation.introspect("Bearer dev-admin");

        assertEquals(true, result.get("active"));
        assertEquals("admin", result.get("sub"));
    }

    @Test
    public void rejectsUnsignedJwtShapedToken() {
        IdentityFederation federation = new IdentityFederation(SECRET, false);
        String token = encoded("{\"alg\":\"none\"}") + "." + encoded("{\"sub\":\"attacker\"}") + ".";

        Map<String, Object> result = federation.introspect("Bearer " + token);

        assertEquals(false, result.get("active"));
        assertEquals("unsupported_jwt_algorithm", result.get("error"));
    }

    @Test
    public void rejectsJwtWithInvalidSignature() {
        IdentityFederation federation = new IdentityFederation(SECRET, false);
        String token = encoded("{\"alg\":\"HS256\"}") + "." + encoded("{\"sub\":\"attacker\"}") + ".bad";

        Map<String, Object> result = federation.introspect("Bearer " + token);

        assertEquals(false, result.get("active"));
        assertEquals("invalid_jwt_signature", result.get("error"));
    }

    @Test
    public void acceptsJwtWithValidSignature() throws Exception {
        IdentityFederation federation = new IdentityFederation(SECRET, false);
        String header = encoded("{\"alg\":\"HS256\"}");
        String payload = encoded("{\"sub\":\"user-123\",\"scope\":\"orders:read customers:read\",\"iss\":\"azure-ad\"}");
        String token = header + "." + payload + "." + sign(header + "." + payload);

        Map<String, Object> result = federation.introspect("Bearer " + token);

        assertEquals(true, result.get("active"));
        assertEquals("user-123", result.get("sub"));
        assertEquals("azure-ad", result.get("provider"));
        assertEquals(true, result.get("verified"));
    }

    private static String encoded(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
