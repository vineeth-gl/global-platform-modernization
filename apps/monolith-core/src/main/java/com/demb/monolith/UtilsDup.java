package com.demb.monolith;

/** Duplicated email normalize — version A. */
public final class UtilsDup {
    private UtilsDup() {
    }

    public static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        String e = email.trim().toLowerCase();
        int at = e.indexOf('@');
        if (at < 0) {
            return e;
        }
        String local = e.substring(0, at);
        String domain = e.substring(at + 1);
        if ("gmail.com".equals(domain)) {
            local = local.replace(".", "");
        }
        return local + "@" + domain;
    }

    public static boolean validateEmail(String email) {
        return email != null && email.contains("@") && email.substring(email.indexOf('@') + 1).contains(".");
    }
}
