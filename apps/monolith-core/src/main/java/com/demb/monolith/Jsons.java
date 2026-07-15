package com.demb.monolith;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

public final class Jsons {
    public static final Gson GSON = new GsonBuilder().serializeNulls().create();
    public static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    private Jsons() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> map(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new java.util.HashMap<String, Object>();
        }
        return (Map<String, Object>) GSON.fromJson(json, MAP_TYPE);
    }

    public static String toJson(Object o) {
        return GSON.toJson(o);
    }
}
