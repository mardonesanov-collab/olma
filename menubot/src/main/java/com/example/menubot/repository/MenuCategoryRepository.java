package com.example.menubot.repository;

import com.example.menubot.model.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.context.annotation.Profile;
import java.util.List;

@Profile("legacy")
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {
    List<MenuCategory> findByRestaurantIdOrderBySortOrderAsc(Long restaurantId);
    long countByRestaurantId(Long restaurantId);
}