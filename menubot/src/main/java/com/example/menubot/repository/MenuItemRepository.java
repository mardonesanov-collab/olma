package com.example.menubot.repository;

import com.example.menubot.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.context.annotation.Profile;
import java.util.List;

@Profile("legacy")
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByRestaurantIdOrderBySortOrderAsc(Long restaurantId);
    List<MenuItem> findByCategoryIdOrderBySortOrderAsc(Long categoryId);
    long countByRestaurantId(Long restaurantId);
    long countByCategoryId(Long categoryId);
}