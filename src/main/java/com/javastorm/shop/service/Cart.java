package com.javastorm.shop.service;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 购物车，保存在用户会话中：商品 ID -> 数量。 */
@Component
@SessionScope
public class Cart implements Serializable {

    public static final int MAX_QUANTITY = 99;

    private final Map<Long, Integer> items = new LinkedHashMap<>();

    public void add(Long productId, int quantity) {
        set(productId, items.getOrDefault(productId, 0) + quantity);
    }

    public void set(Long productId, int quantity) {
        if (quantity <= 0) {
            items.remove(productId);
        } else {
            items.put(productId, Math.min(quantity, MAX_QUANTITY));
        }
    }

    public void remove(Long productId) {
        items.remove(productId);
    }

    public void clear() {
        items.clear();
    }

    public Map<Long, Integer> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public int getTotalQuantity() {
        return items.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
