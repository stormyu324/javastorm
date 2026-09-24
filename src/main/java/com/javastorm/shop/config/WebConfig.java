package com.javastorm.shop.config;

import com.javastorm.shop.service.ImageStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 把上传目录映射到 /uploads/**，让浏览器能直接访问商品图片。 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ImageStorageService images;

    public WebConfig(ImageStorageService images) {
        this.images = images;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(ImageStorageService.URL_PREFIX + "**")
                .addResourceLocations(images.getRoot().toUri().toString() + "/");
    }
}
