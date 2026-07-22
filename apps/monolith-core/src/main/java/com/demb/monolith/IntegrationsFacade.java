package com.demb.monolith;

import com.demb.integrations.analytics.SegmentShim;
import com.demb.integrations.crm.SalesforceAdapter;
import com.demb.integrations.erp.SapBridge;
import com.demb.integrations.marketing.HubspotStub;
import com.demb.integrations.payments.StripeLegacyGateway;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IntegrationsFacade {
    private final SalesforceAdapter crm = new SalesforceAdapter();
    private final SapBridge erp = new SapBridge();
    private final StripeLegacyGateway pay = new StripeLegacyGateway();
    private final HubspotStub mkt = new HubspotStub();
    private final SegmentShim analytics = new SegmentShim();

    public Map<String, Object> pushOrderToCrm(Map<String, Object> order) {
        return crm.upsertOpportunity(order);
    }

    public Map<String, Object> postErpShipment(Map<String, Object> order) {
        return erp.createDelivery(order);
    }

    public Map<String, Object> charge(Map<String, Object> order) {
        return pay.chargeOrder(order);
    }

    public Map<String, Object> nurture(Map<String, Object> customer) {
        return mkt.enroll(customer);
    }

    public Map<String, Object> track(String event, Map<String, Object> props) {
        return analytics.track(event, props);
    }

    public SegmentShim getAnalytics() {
        return analytics;
    }

    public Map<String, Object> listConnectors() {
        List<Map<String, Object>> connectors = new ArrayList<Map<String, Object>>();
        connectors.add(connector("Entra ID", "identity", "CONNECTED",
                "Primary workforce IdP", "2026-07-22T10:45:00Z", null, null));
        connectors.add(connector("Okta", "identity", "CONNECTED",
                "Secondary IdP for vendor portal - VendorSight leftover",
                "2026-07-22T10:42:00Z", "Entra ID",
                "Okta vendor sync recovered; Entra remains the automatic failover path."));
        connectors.add(connector("SAP", "erp", "CONNECTED", "Shipment and inventory bridge",
                "2026-07-22T10:44:00Z", null, null));
        connectors.add(connector("Oracle ERP", "erp", "CONNECTED", "Legacy finance handoff",
                "2026-07-22T10:41:00Z", null, null));
        connectors.add(connector("ServiceNow", "it", "CONNECTED", "Support case escalation",
                "2026-07-22T10:40:00Z", null, null));
        connectors.add(connector("SIEM", "security", "CONNECTED", "Security event streaming",
                "2026-07-22T10:39:00Z", null, null));

        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("connectors", connectors);
        response.put("identity_sync_status", "CONNECTED");
        response.put("vendor_portal_runbook", "Retry Okta heal first; use Entra ID failover for vendor analysts if Okta degrades again.");
        return response;
    }

    public Map<String, Object> healConnector(String name) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        if (!"okta".equalsIgnoreCase(name)) {
            response.put("status", "UNKNOWN_CONNECTOR");
            response.put("message", "No automated heal is registered for " + name);
            return response;
        }

        response.put("connector", "Okta");
        response.put("status", "CONNECTED");
        response.put("last_success", "2026-07-22T10:42:00Z");
        response.put("fallback", "Entra ID");
        response.put("message", "Okta vendor portal identity sync is connected; Entra failover remains available.");
        return response;
    }

    private Map<String, Object> connector(String name, String type, String status, String note,
                                          String lastSuccess, String fallback, String guidance) {
        Map<String, Object> connector = new LinkedHashMap<String, Object>();
        connector.put("name", name);
        connector.put("type", type);
        connector.put("status", status);
        connector.put("note", note);
        connector.put("last_success", lastSuccess);
        if (fallback != null) {
            connector.put("fallback", fallback);
        }
        if (guidance != null) {
            connector.put("guidance", guidance);
            connector.put("retry_available", true);
        }
        return connector;
    }
}
