package com.javastorm.shop.web.admin;

import com.javastorm.shop.domain.OrderStatus;
import com.javastorm.shop.repository.OrderRepository;
import com.javastorm.shop.service.BusinessException;
import com.javastorm.shop.service.OrderService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderRepository orders;
    private final OrderService orderService;

    public AdminOrderController(OrderRepository orders, OrderService orderService) {
        this.orders = orders;
        this.orderService = orderService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) OrderStatus status,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), 20, Sort.by("id").descending());
        model.addAttribute("orders", status == null ? orders.findAll(pageable) : orders.findByStatus(status, pageable));
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("status", status);
        return "admin/orders";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam OrderStatus status, RedirectAttributes flash) {
        try {
            orderService.changeStatus(id, status);
            flash.addFlashAttribute("message", "订单状态已更新为「" + status.getLabel() + "」");
        } catch (BusinessException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders";
    }
}
