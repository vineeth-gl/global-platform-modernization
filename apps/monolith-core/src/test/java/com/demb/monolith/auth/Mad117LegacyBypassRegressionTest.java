package com.demb.monolith.auth;

import org.junit.Test;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

/**
 * MAD-117 regression: auth depends on Bearer token, not X-Legacy-Bypass.
 * Bypass short-circuits were removed from MonolithApp.require and IdentityFederation.requireOauth.
 */
public class Mad117LegacyBypassRegressionTest {

    private final IdentityFederation fed = new IdentityFederation();

    @Test
    public void missingAuthorization_isInactive() {
        Map<String, Object> info = fed.introspect(null);
        assertFalse(Boolean.TRUE.equals(info.get("active")));
        assertEquals("missing_token", info.get("error"));
    }

    @Test
    public void invalidBearer_isInactive() {
        Map<String, Object> info = fed.introspect("Bearer invalid");
        assertFalse(Boolean.TRUE.equals(info.get("active")));
        assertEquals("invalid", info.get("error"));
    }

    @Test
    public void emptyBearer_isInactive() {
        Map<String, Object> info = fed.introspect("Bearer ");
        assertFalse(Boolean.TRUE.equals(info.get("active")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void devAdminBearer_isActiveWithAdminScope() {
        Map<String, Object> info = fed.introspect("Bearer dev-admin");
        assertTrue(Boolean.TRUE.equals(info.get("active")));
        assertEquals("admin", info.get("sub"));
        List<String> scopes = (List<String>) info.get("scopes");
        assertTrue(scopes.contains("admin"));
        assertTrue(scopes.contains("orders:read"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void oktaToken_isActiveWithReadScopes() {
        Map<String, Object> info = fed.introspect("Bearer okta_user1");
        assertTrue(Boolean.TRUE.equals(info.get("active")));
        List<String> scopes = (List<String>) info.get("scopes");
        assertTrue(scopes.contains("orders:read"));
        assertFalse(scopes.contains("admin"));
    }
}