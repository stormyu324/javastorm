package com.javastorm.shop.service;

import com.javastorm.shop.domain.Product;

import java.math.BigDecimal;
import java.util.List;

/** 购物车展示数据：按当前商品价格计算。 */
public record CartView(List<Line> lines, BigDecimal total) {

    public record Line(Product product, int quantity) {
        public BigDecimal getSubtotal() {
            return product.getPrice().multiply(BigDecimal.valueOf(quantity));
        }

        public boolean isAvailable() {
            return product.isOnSale() && product.getStock() >= quantity;
        }
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public boolean isAllAvailable() {
        return lines.stream().allMatch(Line::isAvailable);
    }
}
