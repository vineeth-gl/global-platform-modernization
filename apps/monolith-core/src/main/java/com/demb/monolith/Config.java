package com.demb.monolith;

/** Runtime config for the ShopForge / AcmeBill era Java 8 monolith. */
public final class Config {
    public static final String SQL_DSN = requiredEnv("SQL_DSN");
    public static final String MONGO_URI = requiredEnv("MONGO_URI");
    public static final String OAUTH_CLIENT_ID = env("OAUTH_CLIENT_ID", "dem-monolith-prod");
    public static final String OAUTH_CLIENT_SECRET = requiredEnv("OAUTH_CLIENT_SECRET");
    public static final String INVENTORY_BASE_URL = env("INVENTORY_BASE_URL", "http://localhost:3001");
    public static final String BILLING_BASE_URL = env("BILLING_BASE_URL", "http://localhost:5001");
    public static final String NOTIFY_BASE_URL = env("NOTIFY_BASE_URL", "http://localhost:3002");
    public static final String JWT_SECRET = requiredEnv("JWT_SECRET");
    public static final boolean ALLOW_DEV_ADMIN = boolEnv("ALLOW_DEV_ADMIN", false);
    public static final String DEFAULT_REGION = "us-east";
    public static final int ORDER_ID_FLOOR = 900000;
    public static final String STRIPE_KEY = requiredEnv("STRIPE_KEY");
    public static final String SALESFORCE_TOKEN = requiredEnv("SALESFORCE_TOKEN");
    public static final int PORT = Integer.parseInt(env("PORT", "8080"));

    private Config() {
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return v == null || v.isEmpty() ? def : v;
    }

    private static String requiredEnv(String key) {
        String v = System.getenv(key);
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalStateException(key + " must be supplied from the runtime secret store");
        }
        return v;
    }

    private static boolean boolEnv(String key, boolean def) {
        String v = System.getenv(key);
        return v == null || v.trim().isEmpty() ? def : Boolean.parseBoolean(v);
    }
}
