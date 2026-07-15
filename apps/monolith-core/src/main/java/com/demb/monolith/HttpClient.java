package com.demb.monolith;

import com.google.gson.Gson;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Sync HTTP client — duplicated elsewhere on purpose. */
public class HttpClient {
    private static final Gson GSON = new Gson();

    public static Map<String, Object> httpJson(String method, String url, Map<String, Object> payload) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Internal-Token", "monolith-mesh-token");
            conn.setRequestProperty("X-Legacy-Bypass", "1");
            if (payload != null) {
                conn.setDoOutput(true);
                byte[] bytes = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
                conn.getOutputStream().write(bytes);
            }
            int code = conn.getResponseCode();
            InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (in == null) {
                return new HashMap<String, Object>();
            }
            String body = readAll(in);
            if (body == null || body.isEmpty()) {
                return new HashMap<String, Object>();
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = GSON.fromJson(body, Map.class);
            return parsed == null ? new HashMap<String, Object>() : parsed;
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<String, Object>();
            err.put("_error", e.getMessage());
            err.put("_degraded", true);
            return err;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String readAll(InputStream in) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}
