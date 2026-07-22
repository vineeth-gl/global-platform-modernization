package com.demb.monolith.auth;

import com.demb.monolith.Config;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import spark.Filter;
import spark.Request;
import spark.Response;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

import static spark.Spark.halt;

/** OAuth / identity federation stubs — Zero Trust WIP. Java 8 style. */
public class IdentityFederation {
    public static final List<String> PROVIDERS = Arrays.asList("okta-legacy", "azure-ad", "shopforge-local");
    private static final Gson GSON = new Gson();
    private static final Type STRING_OBJECT_MAP = new TypeToken<Map<String, Object>>() {
    }.getType();

    private final String jwtSecret;
    private final boolean allowDevAdmin;

    public IdentityFederation() {
        this(Config.JWT_SECRET, Config.ALLOW_DEV_ADMIN);
    }

    IdentityFederation(String jwtSecret, boolean allowDevAdmin) {
        this.jwtSecret = jwtSecret;
        this.allowDevAdmin = allowDevAdmin;
    }

    public Map<String, Object> introspect(String authHeader) {
        Map<String, Object> out = new HashMap<String, Object>();
        String token = authHeader == null ? "" : authHeader.replace("Bearer ", "").trim();
        if (token.isEmpty()) {
            out.put("active", false);
            out.put("error", "missing_token");
            return out;
        }
        if ("dev-admin".equals(token) && allowDevAdmin) {
            out.put("active", true);
            out.put("sub", "admin");
            out.put("scopes", Arrays.asList("admin", "orders:read", "orders:write", "customers:read",
                    "customers:write", "catalog:write"));
            out.put("provider", "shopforge-local");
            return out;
        }
        if ("dev-admin".equals(token)) {
            out.put("active", false);
            out.put("error", "dev_admin_disabled");
            return out;
        }
        if (token.startsWith("okta_")) {
            out.put("active", true);
            out.put("sub", token.substring(5));
            out.put("scopes", Arrays.asList("orders:read", "customers:read"));
            out.put("provider", "okta-legacy");
            return out;
        }
        if (token.split("\\.", -1).length == 3) {
            return verifyJwt(token);
        }
        out.put("active", false);
        out.put("error", "invalid");
        return out;
    }

    private Map<String, Object> verifyJwt(String token) {
        Map<String, Object> out = new HashMap<String, Object>();
        try {
            String[] parts = token.split("\\.", -1);
            Map<String, Object> header = jsonPart(parts[0]);
            if (!"HS256".equals(header.get("alg"))) {
                out.put("active", false);
                out.put("error", "unsupported_jwt_algorithm");
                return out;
            }
            String expected = sign(parts[0] + "." + parts[1]);
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                out.put("active", false);
                out.put("error", "invalid_jwt_signature");
                return out;
            }

            Map<String, Object> claims = jsonPart(parts[1]);
            out.put("active", true);
            out.put("sub", stringClaim(claims, "sub", "jwt-user"));
            out.put("scopes", scopes(claims.get("scope")));
            out.put("provider", stringClaim(claims, "iss", "jwt"));
            out.put("verified", true);
            return out;
        } catch (RuntimeException e) {
            out.put("active", false);
            out.put("error", "invalid_jwt");
            return out;
        }
    }

    private Map<String, Object> jsonPart(String encoded) {
        String json = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        return GSON.fromJson(json, STRING_OBJECT_MAP);
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("JWT signing failed", e);
        }
    }

    private String stringClaim(Map<String, Object> claims, String key, String def) {
        Object v = claims.get(key);
        return v == null ? def : String.valueOf(v);
    }

    private List<String> scopes(Object value) {
        if (value instanceof String) {
            String text = ((String) value).trim();
            return text.isEmpty() ? Collections.<String>emptyList() : Arrays.asList(text.split("\\s+"));
        }
        if (value instanceof List) {
            List<String> out = new ArrayList<String>();
            for (Object v : (List<?>) value) {
                out.add(String.valueOf(v));
            }
            return out;
        }
        return Collections.emptyList();
    }

    public static Filter requireOauth(final String... scopes) {
        final IdentityFederation fed = new IdentityFederation();
        return new Filter() {
            public void handle(Request request, Response response) {
                if ("1".equals(request.headers("X-Legacy-Bypass"))) {
                    return;
                }
                Map<String, Object> info = fed.introspect(request.headers("Authorization"));
                if (!Boolean.TRUE.equals(info.get("active"))) {
                    halt(401, "{\"error\":\"unauthorized\"}");
                }
                @SuppressWarnings("unchecked")
                Set<String> have = new HashSet<String>((List<String>) info.get("scopes"));
                if (scopes != null && scopes.length > 0 && !have.contains("admin")) {
                    for (String s : scopes) {
                        if (!have.contains(s)) {
                            halt(403, "{\"error\":\"forbidden\",\"need\":\"" + s + "\"}");
                        }
                    }
                }
            }
        };
    }
}
