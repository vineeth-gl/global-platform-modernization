package com.demb.monolith;

/**
 * Platform configuration — all secrets loaded from environment (no credentials in source).
 */
public final class Config {
    public static final String SQL_DSN = env("SQL_DSN", "inmemory://local/sql-store");
    public static final String MONGO_URI = env("MONGO_URI", "inmemory://local/mongo-store");
    public static final String OAUTH_CLIENT_ID = env("OAUTH_CLIENT_ID", "dem-monolith-dev");
    public static final String OAUTH_CLIENT_SECRET = env("OAUTH_CLIENT_SECRET", "");
    public static final String INVENTORY_BASE_URL = env("INVENTORY_BASE_URL", "http://localhost:3001");
    public static final String BILLING_BASE_URL = env("BILLING_BASE_URL", "http://localhost:5001");
    public static final String NOTIFY_BASE_URL = env("NOTIFY_BASE_URL", "http://localhost:3002");
    public static final String JWT_SECRET = env("JWT_SECRET", "");
    /** HS256 verification key for user/service JWTs (training sample uses shared secret). */
    public static final String JWT_PUBLIC_KEY = env("JWT_PUBLIC_KEY", env("JWT_SECRET", ""));
    public static final String SERVICE_JWT_SECRET = env("SERVICE_JWT_SECRET", "");
    public static final String WEBHOOK_SECRET = env("WEBHOOK_SECRET", "");
    public static final String DEV_ADMIN_TOKEN = env("DEV_ADMIN_TOKEN", "");
    public static final boolean ALLOW_DEV_ADMIN = "true".equalsIgnoreCase(env("ALLOW_DEV_ADMIN", "false"));
    public static final String STRIPE_KEY = env("STRIPE_API_KEY", "");
    public static final String SALESFORCE_TOKEN = env("SALESFORCE_TOKEN", "");
    public static final String SAP_HOST = env("SAP_HOST", "erp-onprem.corp.internal");
    public static final String SAP_USER = env("SAP_USER", "");
    public static final String SAP_PASSWORD = env("SAP_PASSWORD", "");
    public static final String DEFAULT_REGION = "us-east";
    public static final int ORDER_ID_FLOOR = 900000;
    public static final int PORT = Integer.parseInt(env("PORT", "8080"));

    private Config() {
    }

    static String env(String key, String def) {
        String v = System.getenv(key);
        return v == null || v.isEmpty() ? def : v;
    }
}
