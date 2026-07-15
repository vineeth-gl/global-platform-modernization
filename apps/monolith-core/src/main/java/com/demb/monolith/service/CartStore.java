package com.demb.monolith.service;

import java.util.*;

/** Session / cart leftovers from ShopForge storefront. */
public class CartStore {
    public static final CartStore INSTANCE = new CartStore();
    private final Map<String, Cart> carts = new LinkedHashMap<String, Cart>();

    public Cart create(String customerId) {
        String cid = "CART-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        Cart cart = new Cart(cid, customerId);
        carts.put(cid, cart);
        return cart;
    }

    public Cart get(String cartId) {
        return carts.get(cartId);
    }

    public int expireStale(long maxAgeSec) {
        long now = System.currentTimeMillis() / 1000;
        List<String> stale = new ArrayList<String>();
        for (Map.Entry<String, Cart> e : carts.entrySet()) {
            if (now - e.getValue().updatedAt > maxAgeSec) {
                stale.add(e.getKey());
            }
        }
        for (String k : stale) {
            carts.remove(k);
        }
        return stale.size();
    }

    public static class Cart {
        public final String id;
        public String customerId;
        public final List<Map<String, Object>> lines = new ArrayList<Map<String, Object>>();
        public long createdAt = System.currentTimeMillis() / 1000;
        public long updatedAt = createdAt;
        public String promoCode;

        public Cart(String id, String customerId) {
            this.id = id;
            this.customerId = customerId;
        }

        public void add(String sku, int qty, double unitPrice) {
            for (Map<String, Object> line : lines) {
                if (sku.equals(line.get("sku"))) {
                    line.put("qty", ((Number) line.get("qty")).intValue() + qty);
                    updatedAt = System.currentTimeMillis() / 1000;
                    return;
                }
            }
            Map<String, Object> line = new HashMap<String, Object>();
            line.put("sku", sku);
            line.put("qty", qty);
            line.put("unit_price", unitPrice);
            lines.add(line);
            updatedAt = System.currentTimeMillis() / 1000;
        }

        public double subtotal() {
            double s = 0;
            for (Map<String, Object> l : lines) {
                s += ((Number) l.get("qty")).intValue() * ((Number) l.get("unit_price")).doubleValue();
            }
            return Math.round(s * 100.0) / 100.0;
        }

        public Map<String, Object> toOrderPayload() {
            Map<String, Object> out = new HashMap<String, Object>();
            out.put("customer_id", customerId != null ? customerId : "C-1001");
            out.put("lines", new ArrayList<Map<String, Object>>(lines));
            out.put("promo", promoCode);
            out.put("source", "cart");
            return out;
        }
    }
}
