package com.javastorm.shop.web.admin;

import com.javastorm.shop.domain.Product;
import com.javastorm.shop.domain.ProductStatus;
import com.javastorm.shop.repository.CategoryRepository;
import com.javastorm.shop.service.BusinessException;
import com.javastorm.shop.service.ProductImportService;
import com.javastorm.shop.service.ProductService;
import com.javastorm.shop.web.form.ProductForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final ProductImportService importService;
    private final CategoryRepository categories;

    public AdminProductController(ProductService productService, ProductImportService importService,
                                  CategoryRepository categories) {
        this.productService = productService;
        this.importService = importService;
        this.categories = categories;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) ProductStatus status,
                       @RequestParam(required = false) Long category,
                       @RequestParam(defaultValue = "0") int page,
                       HttpServletRequest request,
                       Model model) {
        String query = request.getQueryString();
        model.addAttribute("back", "/admin/products" + (query == null ? "" : "?" + query));
        model.addAttribute("products", productService.search(q, status, category,
                PageRequest.of(Math.max(page, 0), 20, Sort.by("id").descending())));
        model.addAttribute("categories", categories.findAllByOrderBySortOrderAscIdAsc());
        model.addAttribute("statuses", ProductStatus.values());
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("category", category);
        return "admin/products";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        return form(new ProductForm(), model);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return form(ProductForm.from(productService.get(id)), model);
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") ProductForm form, BindingResult binding,
                       @RequestParam(value = "images", required = false) List<MultipartFile> images,
                       @RequestParam(value = "next", required = false) String next,
                       Model model, RedirectAttributes flash) {
        if (binding.hasErrors()) {
            return form(form, model);
        }
        Product saved;
        try {
            saved = productService.save(form, images);
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return form(form, model);
        }
        flash.addFlashAttribute("message", "已保存「" + saved.getName() + "」（" + saved.getStatus().getLabel() + "）");
        if ("new".equals(next)) {
            // 保存并继续添加：沿用分类，减少重复填写
            flash.addFlashAttribute("lastCategoryId", form.getCategoryId());
            return "redirect:/admin/products/new";
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/duplicate")
    public String duplicate(@PathVariable Long id, RedirectAttributes flash) {
        Product copy = productService.duplicate(id);
        flash.addFlashAttribute("message", "已复制为草稿，修改后即可上架");
        return "redirect:/admin/products/" + copy.getId() + "/edit";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam ProductStatus status,
                               @RequestParam(required = false) String back, RedirectAttributes flash) {
        productService.changeStatus(List.of(id), status);
        flash.addFlashAttribute("message", "已" + status.getLabel());
        return "redirect:" + safeBack(back);
    }

    @PostMapping("/batch")
    public String batch(@RequestParam(value = "ids", required = false) List<Long> ids,
                        @RequestParam("op") String action,
                        @RequestParam(required = false) String back,
                        RedirectAttributes flash) {
        if (ids == null || ids.isEmpty()) {
            flash.addFlashAttribute("error", "请先勾选商品");
            return "redirect:" + safeBack(back);
        }
        int n;
        String label;
        if ("delete".equals(action)) {
            n = productService.delete(ids);
            label = "删除";
        } else {
            ProductStatus status = ProductStatus.valueOf(action);
            n = productService.changeStatus(ids, status);
            label = status.getLabel();
        }
        flash.addFlashAttribute("message", "已" + label + " " + n + " 个商品");
        return "redirect:" + safeBack(back);
    }

    @GetMapping("/import")
    public String importPage(Model model) {
        model.addAttribute("statuses", ProductStatus.values());
        model.addAttribute("headers", ProductImportService.HEADERS);
        return "admin/import";
    }

    @PostMapping("/import")
    public String doImport(@RequestParam("file") MultipartFile file,
                           @RequestParam(defaultValue = "ON_SALE") ProductStatus defaultStatus,
                           Model model) throws IOException {
        model.addAttribute("statuses", ProductStatus.values());
        model.addAttribute("headers", ProductImportService.HEADERS);
        if (file.isEmpty()) {
            model.addAttribute("error", "请选择 CSV 文件");
            return "admin/import";
        }
        try {
            model.addAttribute("result", importService.importCsv(file.getBytes(), defaultStatus));
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "admin/import";
    }

    @GetMapping("/import/template.csv")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("商品导入模板.csv", StandardCharsets.UTF_8).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(importService.template());
    }

    private String form(ProductForm form, Model model) {
        if (form.getId() == null && form.getCategoryId() == null && model.containsAttribute("lastCategoryId")) {
            form.setCategoryId((Long) model.getAttribute("lastCategoryId"));
        }
        model.addAttribute("form", form);
        model.addAttribute("categories", categories.findAllByOrderBySortOrderAscIdAsc());
        model.addAttribute("statuses", ProductStatus.values());
        return "admin/product-form";
    }

    /** 只允许跳回后台商品列表，防止开放重定向。 */
    private static String safeBack(String back) {
        if (back != null && back.startsWith("/admin/products") && !back.contains("//") && !back.contains("\\")) {
            return back;
        }
        return "/admin/products";
    }
}
