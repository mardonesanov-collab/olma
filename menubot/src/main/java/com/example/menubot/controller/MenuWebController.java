package com.example.menubot.controller;

import com.example.menubot.model.*;
import com.example.menubot.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@org.springframework.context.annotation.Profile("legacy")
@RestController
@RequestMapping("/api/menu")
public class MenuWebController {

    private final RestaurantRepository restaurantRepository;
    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;

    public MenuWebController(RestaurantRepository restaurantRepository,
                             MenuCategoryRepository categoryRepository,
                             MenuItemRepository menuItemRepository) {
        this.restaurantRepository = restaurantRepository;
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @GetMapping("/{uniqueLink}")
    public ResponseEntity<Map<String, Object>> getMenuData(@PathVariable String uniqueLink) {
        Restaurant restaurant = restaurantRepository.findByUniqueLink(uniqueLink).orElse(null);
        if (restaurant == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> restMap = new LinkedHashMap<>();
        restMap.put("id", restaurant.getId());
        restMap.put("name", restaurant.getName());
        restMap.put("address", restaurant.getAddress());
        restMap.put("phone", restaurant.getPhone());
        restMap.put("description", restaurant.getDescription());
        restMap.put("photoPath", restaurant.getPhotoPath() != null ? "/uploads/" + restaurant.getPhotoPath() : null);

        List<MenuCategory> cats = categoryRepository.findByRestaurantIdOrderBySortOrderAsc(restaurant.getId());
        List<Map<String, Object>> catList = new ArrayList<>();
        for (MenuCategory cat : cats) {
            Map<String, Object> catMap = new LinkedHashMap<>();
            catMap.put("id", cat.getId());
            catMap.put("name", cat.getName());

            List<MenuItem> items = menuItemRepository.findByCategoryIdOrderBySortOrderAsc(cat.getId());
            List<Map<String, Object>> itemList = new ArrayList<>();
            for (MenuItem item : items) {
                if (!item.isAvailable()) continue;
                Map<String, Object> itemMap = new LinkedHashMap<>();
                itemMap.put("id", item.getId());
                itemMap.put("name", item.getName());
                itemMap.put("description", item.getDescription());
                itemMap.put("price", item.getPrice());
                itemMap.put("photoPath", item.getPhotoPath() != null ? "/uploads/" + item.getPhotoPath() : null);
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