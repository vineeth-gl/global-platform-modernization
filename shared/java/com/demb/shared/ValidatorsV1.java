package com.demb.shared;

/** Shared validators v1 — duplicated across languages. */
public final class ValidatorsV1 {
    private ValidatorsV1() {
    }

    public static boolean validateEmail(String email) {
        if (email == null || !email.contains("@")) return false;
        return email.substring(email.indexOf('@') + 1).contains(".");
    }

    public static boolean validateSku(String sku) {
        return sku != null && sku.toUpperCase().startsWith("SKU-");
    }

    public static boolean validateCountry(String code) {
        return code != null && code.length() == 2;
    }
}
