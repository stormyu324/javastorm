package com.javastorm.shop.web;

import com.javastorm.shop.domain.Product;
import com.javastorm.shop.repository.CategoryRepository;
import com.javastorm.shop.repository.ProductRepository;
import com.javastorm.shop.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class StoreController {

    private final ProductService productService;
    private final ProductRepository products;
    private final CategoryRepository categories;

    public StoreController(ProductService productService, ProductRepository products, CategoryRepository categories) {
        this.productService = productService;
        this.products = products;
        this.categories = categories;
    }

    @GetMapping("/")
    public String index(@RequestParam(required = false) String q,
                        @RequestParam(required = false) Long category,
                        @RequestParam(defaultValue = "new") String sort,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        Sort order = switch (sort) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            default -> Sort.by("createdAt").descending().and(Sort.by("id").descending());
        };
        Page<Product> result = productService.searchStorefront(q, category, PageRequest.of(Math.max(page, 0), 12, order));
        model.addAttribute("products", result);
        model.addAttribute("categories", categories.findAllByOrderBySortOrderAscIdAsc());
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("sort", sort);
        return "store/index";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Product product = products.findById(id)
                .filter(Product::isOnSale)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("product", product);
        return "store/product";
    }
}
