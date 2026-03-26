package com.shopsphere.catalogservice.repository;

import com.shopsphere.catalogservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Featured products for home page
    List<Product> findByIsFeaturedTrueAndIsActiveTrue();

    // Single product — active only
    Optional<Product> findByIdAndIsActiveTrue(Long id);

    // Products by category
    Page<Product> findByCategoryIdAndIsActiveTrue(Long categoryId, Pageable pageable);

    // Full search with filters
    @Query("""
            SELECT p FROM Product p
            WHERE p.isActive = true
            AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                 OR LOWER(p.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            AND (:categoryId IS NULL OR p.category.id = :categoryId)
            AND (:minPrice IS NULL OR p.price >= :minPrice)
            AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            """)
    Page<Product> searchProducts(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    // Low stock check
    List<Product> findByStockQuantityLessThanAndIsActiveTrue(int threshold);
}