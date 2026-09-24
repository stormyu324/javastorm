package com.javastorm.shop;

import com.javastorm.shop.domain.CustomerOrder;
import com.javastorm.shop.domain.OrderStatus;
import com.javastorm.shop.domain.Product;
import com.javastorm.shop.domain.ProductStatus;
import com.javastorm.shop.repository.CategoryRepository;
import com.javastorm.shop.repository.OrderRepository;
import com.javastorm.shop.repository.ProductRepository;
import com.javastorm.shop.service.BusinessException;
import com.javastorm.shop.service.Cart;
import com.javastorm.shop.service.OrderService;
import com.javastorm.shop.service.ProductImportService;
import com.javastorm.shop.service.ProductImportService.ImportResult;
import com.javastorm.shop.web.form.CheckoutForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired OrderRepository orders;
    @Autowired ProductImportService importService;
    @Autowired OrderService orderService;

    @BeforeEach
    void clean() {
        orders.deleteAll();
        products.deleteAll();
        categories.deleteAll();
    }

    @Test
    void adminPagesRequireLogin() throws Exception {
        mvc.perform(get("/admin/products")).andExpect(redirectedUrlPattern("**/admin/login"));
        mvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProductWithUploadedImageAndPublish() throws Exception {
        MockMultipartFile image = new MockMultipartFile("images", "shirt.png", "image/png", new byte[]{1, 2, 3});
        mvc.perform(multipart("/admin/products/save").file(image)
                        .param("name", "测试T恤")
                        .param("price", "59.90")
                        .param("stock", "10")
                        .param("status", "ON_SALE")
                        .param("imageUrlsText", "https://example.com/front.jpg")
                        .with(csrf()))
                .andExpect(redirectedUrl("/admin/products"));

        Product p = products.findAll().get(0);
        assertThat(p.getSku()).startsWith("P");
        assertThat(p.getImageUrls()).hasSize(2);
        assertThat(p.getMainImage()).isEqualTo("https://example.com/front.jpg");
        assertThat(p.getImageUrls().get(1)).startsWith("/uploads/products/").endsWith(".png");

        mvc.perform(get(p.getImageUrls().get(1))).andExpect(status().isOk());
        mvc.perform(get("/")).andExpect(content().string(containsString("测试T恤")));
        mvc.perform(get("/products/" + p.getId())).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsNonImageUpload() throws Exception {
        MockMultipartFile bad = new MockMultipartFile("images", "evil.html", "text/html", "<script>".getBytes());
        mvc.perform(multipart("/admin/products/save").file(bad)
                        .param("name", "x").param("price", "1").param("stock", "1").param("status", "DRAFT")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("不支持的图片格式")));
        assertThat(products.count()).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void batchUnpublishHidesFromStorefront() throws Exception {
        Product p = product("SKU-1", 5, ProductStatus.ON_SALE);
        mvc.perform(post("/admin/products/batch").param("ids", p.getId().toString()).param("op", "OFF_SHELF").with(csrf()))
                .andExpect(redirectedUrl("/admin/products"));
        assertThat(products.findById(p.getId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.OFF_SHELF);
        mvc.perform(get("/")).andExpect(content().string(not(containsString("商品SKU-1"))));
        mvc.perform(get("/products/" + p.getId())).andExpect(status().isNotFound());
    }

    @Test
    void csvImportCreatesUpdatesAndReportsErrors() {
        product("EXIST-1", 1, ProductStatus.DRAFT);
        String csv = """
                名称,SKU,分类,售价,原价,库存,状态,图片,描述
                新商品,NEW-1,服装,59.9,99,10,,https://a.com/1.jpg|https://a.com/2.jpg,好看
                改名后的商品,EXIST-1,,19.9,,,上架,,
                没有价格,BAD-1,,,,,,,
                自动编码,,家居,"¥1,000.00",,3,草稿,,
                """;
        ImportResult r = importService.importCsv(csv.getBytes(StandardCharsets.UTF_8), ProductStatus.ON_SALE);

        assertThat(r.created()).isEqualTo(2);
        assertThat(r.updated()).isEqualTo(1);
        assertThat(r.errors()).singleElement().satisfies(e -> {
            assertThat(e.line()).isEqualTo(4);
            assertThat(e.message()).contains("售价");
        });

        Product created = products.findBySku("NEW-1").orElseThrow();
        assertThat(created.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(created.getCategory().getName()).isEqualTo("服装");
        assertThat(created.getImageUrls()).containsExactly("https://a.com/1.jpg", "https://a.com/2.jpg");

        Product updated = products.findBySku("EXIST-1").orElseThrow();
        assertThat(updated.getName()).isEqualTo("改名后的商品");
        assertThat(updated.getPrice()).isEqualByComparingTo("19.90");
        assertThat(updated.getStock()).isEqualTo(1); // 库存列留空则不修改
        assertThat(updated.getStatus()).isEqualTo(ProductStatus.ON_SALE);

        Product auto = products.findAll().stream().filter(x -> x.getName().equals("自动编码")).findFirst().orElseThrow();
        assertThat(auto.getPrice()).isEqualByComparingTo("1000.00");
        assertThat(auto.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(auto.getCategory().getName()).isEqualTo("家居");
    }

    @Test
    void csvImportHandlesGbkFromExcelAndEnglishHeaders() {
        String csv = "name,price,stock,category\r\n中文商品,12.5,4,数码\r\n";
        ImportResult r = importService.importCsv(csv.getBytes(Charset.forName("GBK")), ProductStatus.DRAFT);
        assertThat(r.created()).isEqualTo(1);
        Product p = products.findAll().get(0);
        assertThat(p.getName()).isEqualTo("中文商品");
        assertThat(p.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(p.getCategory().getName()).isEqualTo("数码");
    }

    @Test
    void placingOrderDeductsStockAndCancellingRestoresIt() {
        Product p = product("ORD-1", 5, ProductStatus.ON_SALE);
        Cart cart = new Cart();
        cart.add(p.getId(), 3);

        CustomerOrder order = orderService.placeOrder(cart, checkout());
        assertThat(order.getTotal()).isEqualByComparingTo("30.00");
        assertThat(products.findById(p.getId()).orElseThrow().getStock()).isEqualTo(2);
        assertThat(cart.isEmpty()).isTrue();

        cart.add(p.getId(), 3);
        assertThatThrownBy(() -> orderService.placeOrder(cart, checkout()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("库存不足");
        assertThat(products.findById(p.getId()).orElseThrow().getStock()).isEqualTo(2);

        orderService.changeStatus(order.getId(), OrderStatus.CANCELLED);
        assertThat(products.findById(p.getId()).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test
    void storefrontCheckoutFlow() throws Exception {
        Product p = product("WEB-1", 2, ProductStatus.ON_SALE);
        var session = new org.springframework.mock.web.MockHttpSession();
        mvc.perform(post("/cart/add").session(session).param("productId", p.getId().toString()).with(csrf()))
                .andExpect(redirectedUrl("/products/" + p.getId()));
        mvc.perform(get("/cart").session(session)).andExpect(content().string(containsString("商品WEB-1")));
        mvc.perform(post("/checkout").session(session).with(csrf())
                        .param("customerName", "张三").param("phone", "13800000000").param("address", "北京市"))
                .andExpect(redirectedUrl("/order-success"));
        assertThat(orders.count()).isEqualTo(1);
        assertThat(products.findById(p.getId()).orElseThrow().getStock()).isEqualTo(1);
    }

    private Product product(String sku, int stock, ProductStatus status) {
        Product p = new Product();
        p.setName("商品" + sku);
        p.setSku(sku);
        p.setPrice(new BigDecimal("10.00"));
        p.setStock(stock);
        p.setStatus(status);
        return products.save(p);
    }

    private static CheckoutForm checkout() {
        CheckoutForm f = new CheckoutForm();
        f.setCustomerName("张三");
        f.setPhone("13800000000");
        f.setAddress("北京市朝阳区");
        return f;
    }
}
