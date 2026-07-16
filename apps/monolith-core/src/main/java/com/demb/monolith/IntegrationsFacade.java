package com.demb.monolith;

import com.demb.integrations.analytics.SegmentShim;
import com.demb.integrations.crm.SalesforceAdapter;
import com.demb.integrations.erp.SapBridge;
import com.demb.integrations.marketing.HubspotStub;
import com.demb.integrations.payments.StripeLegacyGateway;

import java.util.Map;

public class IntegrationsFacade {
    private final SalesforceAdapter crm = new SalesforceAdapter(Config.SALESFORCE_TOKEN);
    private final SapBridge erp = new SapBridge(Config.SAP_HOST, Config.SAP_USER, Config.SAP_PASSWORD);
    private final StripeLegacyGateway pay = new StripeLegacyGateway(Config.STRIPE_KEY);
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
}
