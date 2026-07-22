package com.demb.monolith.auth;

import org.junit.Test;
import spark.Filter;
import spark.HaltException;
import spark.Request;
import spark.Response;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IdentityFederationTest {
    @Test
    public void requireOauthRejectsLegacyBypassWithoutToken() throws Exception {
        Request request = mock(Request.class);
        Response response = mock(Response.class);
        when(request.headers("X-Legacy-Bypass")).thenReturn("1");
        when(request.headers("Authorization")).thenReturn(null);

        Filter filter = IdentityFederation.requireOauth("orders:read");

        try {
            filter.handle(request, response);
            fail("legacy bypass header must not satisfy authentication");
        } catch (HaltException e) {
            org.junit.Assert.assertEquals(401, e.statusCode());
        }
    }

    @Test
    public void requireOauthAllowsValidTokenWithScope() throws Exception {
        Request request = mock(Request.class);
        Response response = mock(Response.class);
        when(request.headers("X-Legacy-Bypass")).thenReturn("1");
        when(request.headers("Authorization")).thenReturn("Bearer dev-admin");

        Filter filter = IdentityFederation.requireOauth("orders:read");

        filter.handle(request, response);
    }
}
