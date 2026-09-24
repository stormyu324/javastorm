package com.javastorm.shop.web;

import com.javastorm.shop.domain.CustomerOrder;
import com.javastorm.shop.domain.Product;
import com.javastorm.shop.repository.ProductRepository;
import com.javastorm.shop.service.BusinessException;
import com.javastorm.shop.service.Cart;
import com.javastorm.shop.service.CartView;
import com.javastorm.shop.service.OrderService;
import com.javastorm.shop.web.form.CheckoutForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CartController {

    private final Cart cart;
    private final OrderService orderService;
    private final ProductRepository products;

    public CartController(Cart cart, OrderService orderService, ProductRepository products) {
        this.cart = cart;
        this.orderService = orderService;
        this.products = products;
    }

    @GetMapping("/cart")
    public String cart(Model model) {
        model.addAttribute("cart", orderService.view(cart));
        return "store/cart";
    }

    @PostMapping("/cart/add")
    public String add(@RequestParam Long productId, @RequestParam(defaultValue = "1") int quantity,
                      @RequestParam(required = false) String buyNow, RedirectAttributes flash) {
        Product p = products.findById(productId).filter(Product::isOnSale)
                .orElseThrow(() -> new BusinessException("商品不存在或已下架"));
        if (p.getStock() <= 0) {
            throw new BusinessException("「" + p.getName() + "」已售罄");
        }
        cart.add(productId, Math.max(quantity, 1));
        if (buyNow != null) {
            return "redirect:/checkout";
        }
        flash.addFlashAttribute("message", "已加入购物车：" + p.getName());
        return "redirect:/products/" + productId;
    }

    @PostMapping("/cart/update")
    public String update(@RequestParam Long productId, @RequestParam int quantity) {
        cart.set(productId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String remove(@RequestParam Long productId) {
        cart.remove(productId);
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String checkout(Model model) {
        CartView view = orderService.view(cart);
        if (view.isEmpty()) {
            return "redirect:/cart";
        }
        model.addAttribute("cart", view);
        model.addAttribute("form", new CheckoutForm());
        return "store/checkout";
    }

    @PostMapping("/checkout")
    public String placeOrder(@Valid @ModelAttribute("form") CheckoutForm form, BindingResult binding, Model model,
                             RedirectAttributes flash) {
        CartView view = orderService.view(cart);
        if (view.isEmpty()) {
            return "redirect:/cart";
        }
        if (binding.hasErrors()) {
            model.addAttribute("cart", view);
            return "store/checkout";
        }
        try {
            CustomerOrder order = orderService.placeOrder(cart, form);
            // 通过 flash 传递，避免任何人凭订单号查看他人的收货信息
            flash.addFlashAttribute("order", order);
            return "redirect:/order-success";
        } catch (BusinessException e) {
            model.addAttribute("cart", orderService.view(cart));
            model.addAttribute("error", e.getMessage());
            return "store/checkout";
        }
    }

    @GetMapping("/order-success")
    public String orderPlaced(Model model) {
        if (!model.containsAttribute("order")) {
            return "redirect:/";
        }
        return "store/order-success";
    }
}
