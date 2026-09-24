package com.javastorm.shop.domain;

public enum OrderStatus {
    PLACED("待处理"),
    SHIPPED("已发货"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
