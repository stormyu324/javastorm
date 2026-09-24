package com.javastorm.shop.repository;

import com.javastorm.shop.domain.Category;
import com.javastorm.shop.domain.Product;
import com.javastorm.shop.domain.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    long countByStatus(ProductStatus status);

    long countByCategory(Category category);

    long countByStockLessThanEqualAndStatus(int stock, ProductStatus status);
}
