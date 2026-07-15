package com.demb.billing;

import java.util.*;

public final class AcmeCompat {
    private AcmeCompat() {
    }

    public static Map<String, Object> translateV1Charge(Map<String, Object> body) {
        Map<String, Object> modern = new HashMap<String, Object>();
        Object oid = body.containsKey("OrderId") ? body.get("OrderId") : body.get("order_id");
        modern.put("order_id", oid instanceof Number ? ((Number) oid).intValue() : 0);
        Object amt = body.containsKey("Amount") ? body.get("Amount") : body.get("amount");
        modern.put("amount", amt instanceof Number ? ((Number) amt).doubleValue() : 0);
        Object cur = body.containsKey("Cur") ? body.get("Cur") : body.get("currency");
        modern.put("currency", cur != null ? cur : "USD");
        modern.put("version", 1);
        return modern;
    }

    public static Map<String, Object> fromModern(Map<String, Object> inv) {
        Map<String, Object> acme = new HashMap<String, Object>();
        acme.put("InvoiceNumber", inv.get("id"));
        acme.put("OrderRef", inv.get("order_id"));
        acme.put("TotalAmt", inv.get("amount"));
        acme.put("CurrencyCode", inv.get("currency"));
        acme.put("LegacyFlag", true);
        acme.put("SourceSystem", "AcmeBill");
        return acme;
    }
}
