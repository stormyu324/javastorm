package com.javastorm.shop.web.admin;

import com.javastorm.shop.domain.Category;
import com.javastorm.shop.repository.CategoryRepository;
import com.javastorm.shop.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryRepository categories;
    private final ProductRepository products;

    public AdminCategoryController(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    @GetMapping
    public String list(Model model) {
        Map<Category, Long> counts = new LinkedHashMap<>();
        for (Category c : categories.findAllByOrderBySortOrderAscIdAsc()) {
            counts.put(c, products.countByCategory(c));
        }
        model.addAttribute("categories", counts);
        return "admin/categories";
    }

    @PostMapping
    public String create(@RequestParam String name, @RequestParam(defaultValue = "0") int sortOrder,
                         RedirectAttributes flash) {
        String n = name.trim();
        if (!StringUtils.hasText(n) || n.length() > 50) {
            flash.addFlashAttribute("error", "分类名称需为 1~50 个字");
        } else if (categories.findByName(n).isPresent()) {
            flash.addFlashAttribute("error", "分类「" + n + "」已存在");
        } else {
            Category c = new Category(n);
            c.setSortOrder(sortOrder);
            categories.save(c);
            flash.addFlashAttribute("message", "已添加分类「" + n + "」");
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @RequestParam String name, @RequestParam int sortOrder,
                         RedirectAttributes flash) {
        String n = name.trim();
        Category c = categories.findById(id).orElse(null);
        if (c == null) {
            flash.addFlashAttribute("error", "分类不存在");
        } else if (!StringUtils.hasText(n) || n.length() > 50) {
            flash.addFlashAttribute("error", "分类名称需为 1~50 个字");
        } else if (categories.findByName(n).filter(o -> !o.getId().equals(id)).isPresent()) {
            flash.addFlashAttribute("error", "分类「" + n + "」已存在");
        } else {
            c.setName(n);
            c.setSortOrder(sortOrder);
            categories.save(c);
            flash.addFlashAttribute("message", "已保存");
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        categories.findById(id).ifPresent(c -> {
            if (products.countByCategory(c) > 0) {
                flash.addFlashAttribute("error", "分类「" + c.getName() + "」下还有商品，不能删除");
            } else {
                categories.delete(c);
                flash.addFlashAttribute("message", "已删除分类「" + c.getName() + "」");
            }
        });
        return "redirect:/admin/categories";
    }
}
