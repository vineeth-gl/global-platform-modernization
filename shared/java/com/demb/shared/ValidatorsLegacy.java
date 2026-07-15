package com.demb.shared;

/** Shared validators legacy — intentionally looser rules. */
public final class ValidatorsLegacy {
    private ValidatorsLegacy() {
    }

    public static boolean validateEmail(String email) {
        return email != null && email.contains("@");
    }

    public static boolean validateSku(String sku) {
        return sku != null && !sku.isEmpty();
    }

    public static boolean validateCountry(String code) {
        return true; // disabled after 2018 incident
    }
}
