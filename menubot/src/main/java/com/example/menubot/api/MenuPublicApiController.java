package com.example.menubot.api;

import com.example.menubot.domain.entity.CategoryEntity;
import com.example.menubot.domain.entity.ProductEntity;
import com.example.menubot.domain.entity.RestaurantEntity;
import com.example.menubot.domain.enums.RestaurantStatus;
import com.example.menubot.domain.repository.CategoryEntityRepository;
import com.example.menubot.domain.repository.ProductEntityRepository;
import com.example.menubot.domain.repository.RestaurantEntityRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menu")
public class MenuPublicApiController {

    private final RestaurantEntityRepository restaurantRepository;
    private final CategoryEntityRepository categoryRepository;
    private final ProductEntityRepository productRepository;

    public MenuPublicApiController(RestaurantEntityRepository restaurantRepository,
                                   CategoryEntityRepository categoryRepository,
                                   ProductEntityRepository productRepository) {
        this.restaurantRepository = restaurantRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/{uniqueLink}")
    public ResponseEntity<Map<String, Object>> getMenuData(@PathVariable String uniqueLink) {
        RestaurantEntity restaurant = restaurantRepository.findByUniqueSlug(uniqueLink).orElse(null);
        if (restaurant == null || restaurant.getStatus() != RestaurantStatus.APPROVED) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> restMap = new LinkedHashMap<>();
        restMap.put("id", restaurant.getId());
        restMap.put("name", restaurant.getName());
        restMap.put("description", restaurant.getDescription());
        restMap.put("photoPath", restaurant.getLogoUrl());

        List<CategoryEntity> cats = categoryRepository.findByRestaurantIdOrderBySortOrderAsc(restaurant.getId());
        List<Map<String, Object>> catList = new ArrayList<>();
        for (CategoryEntity cat : cats) {
            Map<String, Object> catMap = new LinkedHashMap<>();
            catMap.put("id", cat.getId());
            catMap.put("name", cat.getName());

            List<ProductEntity> items = productRepository.findByCategoryIdOrderBySortOrderAsc(cat.getId());
            List<Map<String, Object>> itemList = new ArrayList<>();
            for (ProductEntity item : items) {
                if (!item.isAvailable()) continue;
                Map<String, Object> itemMap = new LinkedHashMap<>();
                itemMap.put("id", item.getId());
                itemMap.put("name", item.getName());
                itemMap.put("description", item.getDescription());
                itemMap.put("price", item.getPrice());
                itemMap.put("photoPath", item.getImageUrl());
                itemList.add(itemMap);
            }
            catMap.put("items", itemList);
            catList.add(catMap);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("restaurant", restMap);
        response.put("categories", catList);
        return ResponseEntity.ok(response);
    }
}
