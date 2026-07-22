package com.demb.monolith;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IntegrationsFacadeTest {
    @Test
    public void oktaConnectorProvidesRecoveryPathForVendorPortal() {
        IntegrationsFacade facade = new IntegrationsFacade();

        Map<String, Object> response = facade.listConnectors();
        Map<String, Object> okta = findConnector(response, "Okta");

        assertEquals("CONNECTED", okta.get("status"));
        assertEquals("Entra ID", okta.get("fallback"));
        assertEquals(Boolean.TRUE, okta.get("retry_available"));
        assertNotNull(okta.get("last_success"));
        assertTrue(String.valueOf(response.get("vendor_portal_runbook")).contains("Entra ID failover"));
    }

    @Test
    public void retryEndpointModelHealsOktaConnector() {
        IntegrationsFacade facade = new IntegrationsFacade();

        Map<String, Object> healed = facade.healConnector("okta");

        assertEquals("Okta", healed.get("connector"));
        assertEquals("CONNECTED", healed.get("status"));
        assertEquals("Entra ID", healed.get("fallback"));
        assertNotNull(healed.get("last_success"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findConnector(Map<String, Object> response, String name) {
        List<Map<String, Object>> connectors = (List<Map<String, Object>>) response.get("connectors");
        for (Map<String, Object> connector : connectors) {
            if (name.equals(connector.get("name"))) {
                return connector;
            }
        }
        throw new AssertionError("Missing connector " + name);
    }
}
