package com.javastorm.shop.web.admin;

import com.javastorm.shop.domain.OrderStatus;
import com.javastorm.shop.domain.ProductStatus;
import com.javastorm.shop.repository.OrderRepository;
import com.javastorm.shop.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {

    static final int LOW_STOCK = 5;

    private final ProductRepository products;
    private final OrderRepository orders;

    public AdminDashboardController(ProductRepository products, OrderRepository orders) {
        this.products = products;
        this.orders = orders;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("onSale", products.countByStatus(ProductStatus.ON_SALE));
        model.addAttribute("drafts", products.countByStatus(ProductStatus.DRAFT));
        model.addAttribute("offShelf", products.countByStatus(ProductStatus.OFF_SHELF));
        model.addAttribute("lowStock", products.countByStockLessThanEqualAndStatus(LOW_STOCK, ProductStatus.ON_SALE));
        model.addAttribute("pendingOrders", orders.countByStatus(OrderStatus.PLACED));
        model.addAttribute("lowStockThreshold", LOW_STOCK);
        return "admin/dashboard";
    }
}
