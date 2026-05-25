package com.example.menubot.domain.repository;

import com.example.menubot.domain.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductEntityRepository extends JpaRepository<ProductEntity, Long> {
    List<ProductEntity> findByCategoryIdOrderBySortOrderAsc(Long categoryId);

    long countByCategoryId(Long categoryId);

    long countByRestaurantId(Long restaurantId);

    List<ProductEntity> findByRestaurantIdAndAvailableTrueOrderBySortOrderAsc(Long restaurantId);

    @Query(value = """
            SELECT * FROM products p
            WHERE p.restaurant_id = :restaurantId AND p.is_available = true
            AND p.search_vector @@ plainto_tsquery('simple', :q)
            ORDER BY ts_rank(p.search_vector, plainto_tsquery('simple', :q)) DESC
            """, nativeQuery = true)
    List<ProductEntity> searchAvailable(@Param("restaurantId") Long restaurantId, @Param("q") String query);

    @Query("SELECT p FROM ProductEntity p WHERE p.restaurantId = :restaurantId AND p.available = true " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<ProductEntity> searchAvailableFallback(@Param("restaurantId") Long restaurantId, @Param("q") String query);
}
