package com.javastorm.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;

/** 下单时的商品快照；商品之后改价或删除不影响历史订单。 */
@Entity
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(length = 64)
    private String sku;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    private int quantity;

    protected OrderItem() {
    }

    public OrderItem(Product product, int quantity) {
        this.productId = product.getId();
        this.productName = product.getName();
        this.sku = product.getSku();
        this.unitPrice = product.getPrice();
        this.quantity = quantity;
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getSku() {
        return sku;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }
}
