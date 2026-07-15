package com.demb.monolith.service;

import java.util.*;

public class MerchandisingService {
    private static final Map<String, List<String>> CATEGORY_TREE = new HashMap<String, List<String>>();
    private static final Map<String, String> SEO = new HashMap<String, String>();
    private final ProductService productService;
    private final Map<String, Map<String, Object>> overrides = new HashMap<String, Map<String, Object>>();

    static {
        CATEGORY_TREE.put("root", Arrays.asList("hardware", "software", "services"));
        CATEGORY_TREE.put("hardware", Arrays.asList("widgets", "gadgets", "cables"));
        SEO.put("widgets", "Buy {name} — legacy ShopForge widgets");
        SEO.put("gadgets", "Acme {name} gadgets online");
        SEO.put("default", "{name} | DEMb catalog");
    }

    public MerchandisingService(ProductService productService) {
        this.productService = productService;
    }

    public String categorize(String sku) {
        if (sku != null && sku.startsWith("SKU-1")) return "widgets";
        if (sku != null && sku.startsWith("SKU-2")) return "gadgets";
        if (sku != null && sku.startsWith("SKU-5")) return "support";
        return "hardware";
    }

    public Map<String, Object> enrich(Map<String, Object> product, String region) {
        String sku = String.valueOf(product.get("sku"));
        String cat = categorize(sku);
        String tmpl = SEO.containsKey(cat) ? SEO.get(cat) : SEO.get("default");
        Map<String, Object> out = new HashMap<String, Object>(product);
        out.put("category", cat);
        out.put("seo_title", tmpl.replace("{name}", String.valueOf(product.get("name"))));
        out.put("region", region);
        List<String> badges = new ArrayList<String>();
        if (product.get("price") instanceof Number && ((Number) product.get("price")).doubleValue() > 100) {
            badges.add("premium");
        }
        out.put("badges", badges);
        if (overrides.containsKey(sku)) {
            out.putAll(overrides.get(sku));
        }
        return out;
    }

    public List<Map<String, Object>> listEnriched(String region) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> p : productService.listAll(region)) {
            out.add(enrich(p, region));
        }
        return out;
    }
}
