package com.example.menubot.api;

import com.example.menubot.domain.entity.AppUser;
import com.example.menubot.domain.entity.CategoryEntity;
import com.example.menubot.domain.entity.ProductEntity;
import com.example.menubot.domain.entity.RestaurantEntity;
import com.example.menubot.domain.repository.CategoryEntityRepository;
import com.example.menubot.domain.repository.ProductEntityRepository;
import com.example.menubot.domain.repository.RestaurantEntityRepository;
import com.example.menubot.service.MenuCatalogService;
import com.example.menubot.service.WebAppAccessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * React Web App uchun /api/* (legacy WebAppController o'rniga, SaaS bazasi bilan).
 * userId parametri — Telegram tg_id.
 */
@RestController
@RequestMapping("/api")
public class WebAppApiController {

    private final WebAppAccessService access;
    private final RestaurantEntityRepository restaurantRepository;
    private final CategoryEntityRepository categoryRepository;
    private final ProductEntityRepository productRepository;
    private final MenuCatalogService menuCatalogService;
    private final String baseUrl;

    public WebAppApiController(WebAppAccessService access,
                               RestaurantEntityRepository restaurantRepository,
                               CategoryEntityRepository categoryRepository,
                               ProductEntityRepository productRepository,
                               MenuCatalogService menuCatalogService,
                               @Value("${app.base-url}") String baseUrl) {
        this.access = access;
        this.restaurantRepository = restaurantRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.menuCatalogService = menuCatalogService;
        this.baseUrl = baseUrl.replaceAll("/$", "");
    }

    @GetMapping("/user/{tgId}/check")
    public ResponseEntity<Map<String, Object>> checkUser(@PathVariable Long tgId) {
        AppUser user = access.resolveByTgId(tgId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("exists", true);
        response.put("approved", access.canUseWebPanel(user));
        response.put("isAdmin", access.isAdmin(user));
        response.put("role", user.getRole().name());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("username", user.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/restaurants")
    public ResponseEntity<List<Map<String, Object>>> getUserRestaurants(@RequestParam Long userId) {
        AppUser user = access.requireWebUser(userId);
        List<RestaurantEntity> restaurants = access.isAdmin(user)
                ? restaurantRepository.findAll()
                : restaurantRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId());

        List<Map<String, Object>> result = new ArrayList<>();
        for (RestaurantEntity r : restaurants) {
            result.add(toRestaurantDto(r));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/restaurant/{restId}/link")
    public ResponseEntity<Map<String, Object>> getMenuLink(@PathVariable Long restId,
                                                           @RequestParam Long userId) {
        AppUser user = access.requireWebUser(userId);
        if (!access.ownsRestaurant(user, restId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Not authorized"));
        }
        RestaurantEntity restaurant = restaurantRepository.findById(restId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("link", baseUrl + "/menu/" + restaurant.getUniqueSlug());
        response.put("restaurantName", restaurant.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/restaurant/{restId}/categories")
    public ResponseEntity<List<Map<String, Object>>> getCategories(@PathVariable Long restId) {
        List<CategoryEntity> categories = categoryRepository.findByRestaurantIdOrderBySortOrderAsc(restId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (CategoryEntity cat : categories) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", cat.getId());
            map.put("restaurantId", cat.getRestaurantId());
            map.put("name", cat.getName());
            map.put("sortOrder", cat.getSortOrder());
            map.put("count", productRepository.countByCategoryId(cat.getId()));
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/category/add")
    public ResponseEntity<Map<String, Object>> addCategory(@RequestParam Long userId,
                                                            @RequestParam Long restaurantId,
                                                            @RequestParam String name) {
        AppUser user = access.requireWebUser(userId);
        if (!access.ownsRestaurant(user, restaurantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Ruxsat yo'q"));
        }
        CategoryEntity category = access.withUser(user,
                () -> menuCatalogService.createCategory(restaurantId, name));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("id", category.getId());
        response.put("name", category.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/category/{catId}")
    public ResponseEntity<Map<String, Object>> deleteCategory(@PathVariable Long catId,
                                                              @RequestParam Long userId) {
        AppUser user = access.requireWebUser(userId);
        CategoryEntity cat = categoryRepository.findById(catId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!access.ownsRestaurant(user, cat.getRestaurantId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Ruxsat yo'q"));
        }
        access.withUser(user, () -> menuCatalogService.deleteCategory(cat.getRestaurantId(), catId));
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/category/{catId}/items")
    public ResponseEntity<List<Map<String, Object>>> getItems(@PathVariable Long catId) {
        List<ProductEntity> items = productRepository.findByCategoryIdOrderBySortOrderAsc(catId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ProductEntity item : items) {
            result.add(toItemDto(item));
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/item/add")
    public ResponseEntity<Map<String, Object>> addItem(@RequestParam Long userId,
                                                       @RequestParam Long restaurantId,
                                                       @RequestParam Long categoryId,
                                                       @RequestParam String name,
                                                       @RequestParam(required = false) String description,
                                                       @RequestParam(required = false) Long price,
                                                       @RequestParam(required = false) MultipartFile photo) {
        AppUser user = access.requireWebUser(userId);
        if (!access.ownsRestaurant(user, restaurantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Ruxsat yo'q"));
        }
        ProductEntity item = access.withUser(user, () ->
                menuCatalogService.createProduct(restaurantId, categoryId, name, description, price, photo));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("id", item.getId());
        response.put("photoPath", item.getImageUrl());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/item/{itemId}")
    public ResponseEntity<Map<String, Object>> updateItem(@PathVariable Long itemId,
                                                          @RequestParam Long userId,
                                                          @RequestParam(required = false) String name,
                                                          @RequestParam(required = false) String description,
                                                          @RequestParam(required = false) Long price,
                                                          @RequestParam(required = false) Boolean available,
                                                          @RequestParam(required = false) MultipartFile photo) {
        AppUser user = access.requireWebUser(userId);
        ProductEntity existing = productRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!access.ownsRestaurant(user, existing.getRestaurantId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Ruxsat yo'q"));
        }
        ProductEntity item = access.withUser(user, () ->
                menuCatalogService.updateProduct(existing.getRestaurantId(), itemId, name, description, price, available, photo));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("photoPath", item.getImageUrl());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<Map<String, Object>> deleteItem(@PathVariable Long itemId,
                                                          @RequestParam Long userId) {
        AppUser user = access.requireWebUser(userId);
        ProductEntity item = productRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!access.ownsRestaurant(user, item.getRestaurantId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("Ruxsat yo'q"));
        }
        access.withUser(user, () -> menuCatalogService.deleteProduct(item.getRestaurantId(), itemId));
        return ResponseEntity.ok(Map.of("success", true));
    }

    private Map<String, Object> toRestaurantDto(RestaurantEntity r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("ownerId", r.getOwnerId());
        map.put("name", r.getName());
        map.put("description", r.getDescription());
        map.put("photoPath", r.getLogoUrl());
        map.put("uniqueLink", r.getUniqueSlug());
        map.put("status", r.getStatus().name());
        map.put("publicUrl", baseUrl + "/menu/" + r.getUniqueSlug());
        map.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        map.put("categoryCount", categoryRepository.countByRestaurantId(r.getId()));
        map.put("itemCount", productRepository.countByRestaurantId(r.getId()));
        return map;
    }

    private Map<String, Object> toItemDto(ProductEntity item) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("categoryId", item.getCategoryId());
        map.put("restaurantId", item.getRestaurantId());
        map.put("name", item.getName());
        map.put("price", item.getPrice());
        map.put("description", item.getDescription());
        map.put("photoPath", item.getImageUrl());
        map.put("available", item.isAvailable());
        map.put("sortOrder", item.getSortOrder());
        return map;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", false);
        r.put("message", message);
        return r;
    }
}
