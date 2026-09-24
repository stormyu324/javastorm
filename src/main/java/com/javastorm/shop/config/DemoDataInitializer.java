package com.javastorm.shop.config;

import com.javastorm.shop.domain.Category;
import com.javastorm.shop.domain.Product;
import com.javastorm.shop.domain.ProductStatus;
import com.javastorm.shop.repository.CategoryRepository;
import com.javastorm.shop.repository.ProductRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** 首次启动（数据库为空）时写入几条示例数据，方便直接看到效果。可用 shop.demo-data=false 关闭。 */
@Component
@ConditionalOnProperty(name = "shop.demo-data", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements ApplicationRunner {

    private final CategoryRepository categories;
    private final ProductRepository products;

    public DemoDataInitializer(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categories.count() > 0 || products.count() > 0) {
            return;
        }
        Category clothing = categories.save(new Category("服装"));
        Category home = categories.save(new Category("家居"));
        Category digital = categories.save(new Category("数码"));

        product("纯棉圆领T恤", "DEMO-001", clothing, "59.00", "99.00", 120, "100% 纯棉面料，柔软透气，四季百搭。");
        product("休闲牛仔裤", "DEMO-002", clothing, "159.00", null, 60, "经典直筒版型，弹力舒适。");
        product("陶瓷马克杯", "DEMO-003", home, "35.00", "45.00", 200, "高温烧制，容量 350ml，可微波炉加热。");
        product("北欧风抱枕", "DEMO-004", home, "49.00", null, 80, "可拆洗外套，填充饱满。");
        product("无线蓝牙耳机", "DEMO-005", digital, "199.00", "299.00", 40, "蓝牙 5.3，续航 30 小时。");
        product("快充数据线", "DEMO-006", digital, "29.90", null, 3, "支持 60W 快充，编织线身更耐用。");
    }

    private void product(String name, String sku, Category category, String price, String originalPrice,
                         int stock, String description) {
        Product p = new Product();
        p.setName(name);
        p.setSku(sku);
        p.setCategory(category);
        p.setPrice(new BigDecimal(price));
        p.setOriginalPrice(originalPrice == null ? null : new BigDecimal(originalPrice));
        p.setStock(stock);
        p.setDescription(description);
        p.setStatus(ProductStatus.ON_SALE);
        products.save(p);
    }
}
