package com.javastorm.shop.domain;

import java.util.Locale;

public enum ProductStatus {
    DRAFT("草稿"),
    ON_SALE("上架"),
    OFF_SHELF("下架");

    private final String label;

    ProductStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 同时接受英文枚举名和中文标签，例如 "ON_SALE" 或 "上架"。 */
    public static ProductStatus parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        for (ProductStatus s : values()) {
            if (s.name().equalsIgnoreCase(v) || s.label.equals(v)) {
                return s;
            }
        }
        if (v.toLowerCase(Locale.ROOT).equals("onsale")) {
            return ON_SALE;
        }
        throw new IllegalArgumentException("未知的商品状态: " + value);
    }
}
