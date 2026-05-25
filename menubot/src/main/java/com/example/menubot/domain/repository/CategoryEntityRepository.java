package com.example.menubot.domain.repository;

import com.example.menubot.domain.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryEntityRepository extends JpaRepository<CategoryEntity, Long> {
    List<CategoryEntity> findByRestaurantIdOrderBySortOrderAsc(Long restaurantId);
    long countByRestaurantId(Long restaurantId);
}
