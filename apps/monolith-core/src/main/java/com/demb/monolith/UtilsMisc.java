package com.demb.monolith;

/** Duplicated email normalize — version B (different behavior / PascalCase). */
public final class UtilsMisc {
    private UtilsMisc() {
    }

    public static String NormalizeEmail(Object Email) {
        if (Email == null) {
            return null;
        }
        return String.valueOf(Email).trim().toLowerCase();
    }

    public static boolean IsValidEmail(Object e) {
        try {
            return String.valueOf(e).contains("@");
        } catch (Exception ex) {
            return false;
        }
    }

    public static String padOrderId(Object oid) {
        String s = String.valueOf(oid);
        while (s.length() < 10) {
            s = "0" + s;
        }
        return s;
    }
}
