package com.javastorm.shop.service;

import com.javastorm.shop.domain.Category;
import com.javastorm.shop.domain.Product;
import com.javastorm.shop.domain.ProductStatus;
import com.javastorm.shop.repository.CategoryRepository;
import com.javastorm.shop.repository.ProductRepository;
import com.javastorm.shop.web.form.ProductForm;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ProductService {

    private static final DateTimeFormatter SKU_TIME = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ImageStorageService images;

    public ProductService(ProductRepository products, CategoryRepository categories, ImageStorageService images) {
        this.products = products;
        this.categories = categories;
        this.images = images;
    }

    @Transactional(readOnly = true)
    public Page<Product> search(String keyword, ProductStatus status, Long categoryId, Pageable pageable) {
        return products.findAll(spec(keyword, status, categoryId), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Product> searchStorefront(String keyword, Long categoryId, Pageable pageable) {
        return search(keyword, ProductStatus.ON_SALE, categoryId, pageable);
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        return products.findById(id).orElseThrow(() -> new BusinessException("商品不存在"));
    }

    /** 新建或更新商品。上传的图片追加在已填写的图片地址之后。 */
    @Transactional
    public Product save(ProductForm form, List<MultipartFile> uploads) {
        Product product = form.getId() == null ? new Product() : get(form.getId());

        String sku = StringUtils.hasText(form.getSku()) ? form.getSku().trim() : generateSku();
        products.findBySku(sku)
                .filter(other -> !other.getId().equals(product.getId()))
                .ifPresent(other -> {
                    throw new BusinessException("SKU「" + sku + "」已被商品「" + other.getName() + "」使用");
                });

        List<String> imageUrls = new ArrayList<>(form.imageUrlList());
        imageUrls.addAll(images.storeAll(uploads));

        product.setName(form.getName().trim());
        product.setSku(sku);
        product.setCategory(form.getCategoryId() == null ? null
                : categories.findById(form.getCategoryId()).orElse(null));
        product.setPrice(form.getPrice());
        product.setOriginalPrice(form.getOriginalPrice());
        product.setStock(form.getStock());
        product.setDescription(form.getDescription());
        product.setStatus(form.getStatus());
        product.setImageUrls(imageUrls);
        return products.save(product);
    }

    @Transactional
    public int changeStatus(Collection<Long> ids, ProductStatus status) {
        List<Product> list = products.findAllById(ids);
        list.forEach(p -> p.setStatus(status));
        return list.size();
    }

    @Transactional
    public int delete(Collection<Long> ids) {
        List<Product> list = products.findAllById(ids);
        products.deleteAll(list);
        return list.size();
    }

    /** 复制一个商品作为草稿，方便上架同款不同规格的商品。 */
    @Transactional
    public Product duplicate(Long id) {
        Product source = get(id);
        Product copy = new Product();
        copy.setName(source.getName() + "（副本）");
        copy.setSku(generateSku());
        copy.setCategory(source.getCategory());
        copy.setPrice(source.getPrice());
        copy.setOriginalPrice(source.getOriginalPrice());
        copy.setStock(source.getStock());
        copy.setDescription(source.getDescription());
        copy.setStatus(ProductStatus.DRAFT);
        copy.setImageUrls(source.getImageUrls());
        return products.save(copy);
    }

    public String generateSku() {
        String sku;
        do {
            sku = "P" + LocalDateTime.now().format(SKU_TIME)
                    + String.format(Locale.ROOT, "%03d", ThreadLocalRandom.current().nextInt(1000));
        } while (products.existsBySku(sku));
        return sku;
    }

    private static Specification<Product> spec(String keyword, ProductStatus status, Long categoryId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("sku")), like)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.<Category>get("category").get("id"), categoryId));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
