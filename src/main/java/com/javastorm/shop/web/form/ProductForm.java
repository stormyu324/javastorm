package com.javastorm.shop.web.form;

import com.javastorm.shop.domain.Product;
import com.javastorm.shop.domain.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class ProductForm {

    private Long id;

    @NotBlank(message = "请填写商品名称")
    @Size(max = 200, message = "名称最多 200 字")
    private String name;

    /** 留空则自动生成。 */
    @Size(max = 64, message = "SKU 最多 64 个字符")
    private String sku;

    private Long categoryId;

    @NotNull(message = "请填写售价")
    @DecimalMin(value = "0.00", message = "售价不能为负数")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "原价不能为负数")
    private BigDecimal originalPrice;

    @Min(value = 0, message = "库存不能为负数")
    private int stock;

    @Size(max = 10000, message = "描述最多 10000 字")
    private String description;

    @NotNull
    private ProductStatus status = ProductStatus.ON_SALE;

    /** 图片地址，每行一个，第一行为主图。上传的新图片会追加到末尾。 */
    private String imageUrlsText = "";

    public static ProductForm from(Product p) {
        ProductForm f = new ProductForm();
        f.id = p.getId();
        f.name = p.getName();
        f.sku = p.getSku();
        f.categoryId = p.getCategory() == null ? null : p.getCategory().getId();
        f.price = p.getPrice();
        f.originalPrice = p.getOriginalPrice();
        f.stock = p.getStock();
        f.description = p.getDescription();
        f.status = p.getStatus();
        f.imageUrlsText = String.join("\n", p.getImageUrls());
        return f;
    }

    public List<String> imageUrlList() {
        if (imageUrlsText == null) {
            return List.of();
        }
        return Arrays.stream(imageUrlsText.split("\\R"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public String getImageUrlsText() {
        return imageUrlsText;
    }

    public void setImageUrlsText(String imageUrlsText) {
        this.imageUrlsText = imageUrlsText;
    }
}
