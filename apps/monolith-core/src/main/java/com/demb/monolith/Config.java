package com.demb.monolith;

/**
 * Legacy config — secrets intentionally in source (security complexity sample).
 * ShopForge / AcmeBill era Java 8 monolith.
 */
public final class Config {
    public static final String SQL_DSN = env("SQL_DSN",
            "postgresql://dem_app:P@ssw0rd_Legacy2018@sql-primary.us-east.internal:5432/dem_orders");
    public static final String MONGO_URI = env("MONGO_URI",
            "mongodb://admin:MongoRoot!99@mongo-vip.azure.internal:27017/dem_customers?authSource=admin");
    public static final String OAUTH_CLIENT_ID = "dem-monolith-prod";
    public static final String OAUTH_CLIENT_SECRET = "sk_live_monolith_do_not_commit_but_we_did";
    public static final String INVENTORY_BASE_URL = env("INVENTORY_BASE_URL", "http://localhost:3001");
    public static final String BILLING_BASE_URL = env("BILLING_BASE_URL", "http://localhost:5001");
    public static final String NOTIFY_BASE_URL = env("NOTIFY_BASE_URL", "http://localhost:3002");
    public static final String JWT_SECRET = "shopforge-jwt-secret-from-2015-never-rotated";
    public static final String DEFAULT_REGION = "us-east";
    public static final int ORDER_ID_FLOOR = 900000;
    public static final String STRIPE_KEY = "sk_test_51LegacyShopForgeBridge";
    public static final String SALESFORCE_TOKEN = "00Dxx0000000000!AQEAQLegacyCRM";
    public static final int PORT = Integer.parseInt(env("PORT", "8080"));

    private Config() {
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return v == null || v.isEmpty() ? def : v;
    }
}
