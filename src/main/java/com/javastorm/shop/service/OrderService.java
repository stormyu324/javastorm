package com.javastorm.shop.service;

import com.javastorm.shop.domain.CustomerOrder;
import com.javastorm.shop.domain.OrderItem;
import com.javastorm.shop.domain.OrderStatus;
import com.javastorm.shop.domain.Product;
import com.javastorm.shop.repository.OrderRepository;
import com.javastorm.shop.repository.ProductRepository;
import com.javastorm.shop.web.form.CheckoutForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ProductRepository products;
    private final OrderRepository orders;

    public OrderService(ProductRepository products, OrderRepository orders) {
        this.products = products;
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public CartView view(Cart cart) {
        List<CartView.Line> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> e : cart.getItems().entrySet()) {
            Product p = products.findById(e.getKey()).orElse(null);
            if (p == null) {
                continue;
            }
            CartView.Line line = new CartView.Line(p, e.getValue());
            lines.add(line);
            total = total.add(line.getSubtotal());
        }
        return new CartView(lines, total);
    }

    /** 下单并扣减库存；任一商品不可购买时整单失败。 */
    @Transactional
    public CustomerOrder placeOrder(Cart cart, CheckoutForm form) {
        if (cart.isEmpty()) {
            throw new BusinessException("购物车是空的");
        }
        CustomerOrder order = new CustomerOrder();
        order.setOrderNo(LocalDateTime.now().format(ORDER_NO_TIME)
                + String.format(Locale.ROOT, "%04d", ThreadLocalRandom.current().nextInt(10000)));
        order.setCustomerName(form.getCustomerName().trim());
        order.setPhone(form.getPhone().trim());
        order.setAddress(form.getAddress().trim());
        order.setNote(form.getNote());

        for (Map.Entry<Long, Integer> e : cart.getItems().entrySet()) {
            Product p = products.findById(e.getKey())
                    .orElseThrow(() -> new BusinessException("购物车中有商品已被删除，请刷新购物车"));
            int qty = e.getValue();
            if (!p.isOnSale()) {
                throw new BusinessException("「" + p.getName() + "」已下架");
            }
            if (p.getStock() < qty) {
                throw new BusinessException("「" + p.getName() + "」库存不足，仅剩 " + p.getStock() + " 件");
            }
            p.setStock(p.getStock() - qty);
            order.addItem(new OrderItem(p, qty));
        }
        CustomerOrder saved = orders.save(order);
        cart.clear();
        return saved;
    }

    /** 修改订单状态；取消订单时把库存加回去。 */
    @Transactional
    public void changeStatus(Long orderId, OrderStatus status) {
        CustomerOrder order = orders.findById(orderId).orElseThrow(() -> new BusinessException("订单不存在"));
        if (order.getStatus() == status) {
            return;
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("已取消的订单不能再修改状态");
        }
        if (status == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                if (item.getProductId() != null) {
                    products.findById(item.getProductId())
                            .ifPresent(p -> p.setStock(p.getStock() + item.getQuantity()));
                }
            }
        }
        order.setStatus(status);
    }
}
