package com.example.menubot.controller;

import com.example.menubot.model.*;
import com.example.menubot.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@org.springframework.context.annotation.Profile("legacy")
@RestController
@RequestMapping("/api")
public class WebAppController {

    private final RestaurantRepository restaurantRepository;
    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.upload.dir}")
    private String uploadDir;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    public WebAppController(RestaurantRepository restaurantRepository,
                            MenuCategoryRepository categoryRepository,
                            MenuItemRepository menuItemRepository,
                            UserRepository userRepository) {
        this.restaurantRepository = restaurantRepository;
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.userRepository = userRepository;
    }

    // ─────────────────────────────────────────
    // 🔒 Helper: foydalanuvchini tekshirish
    // ─────────────────────────────────────────
    private User validateUser(Long userId) {
        if (userId == null) return null;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isApproved()) return null;
        return user;
    }

    private boolean ownsRestaurant(User user, Long restaurantId) {
        if (user == null || restaurantId == null) return false;
        Restaurant r = restaurantRepository.findById(restaurantId).orElse(null);
        return r != null && r.getOwnerId().equals(user.getId());
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", false);
        r.put("message", message);
        return r;
    }

    // ─────────────────────────────────────────
    // 👤 USER STATUS
    // ─────────────────────────────────────────
    @GetMapping("/user/{userId}/check")
    public ResponseEntity<Map<String, Object>> checkUser(@PathVariable Long userId) {
        Map<String, Object> response = new LinkedHashMap<>();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            response.put("exists", false);
            response.put("approved", false);
            response.put("isAdmin", false);
            return ResponseEntity.ok(response);
        }
        response.put("exists", true);
        response.put("approved", user.isApproved());
        response.put("isAdmin", user.isAdmin());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("username", user.getUsername());
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────
    // 🍽 RESTAURANTS
    // ─────────────────────────────────────────
    @GetMapping("/restaurants")
    public ResponseEntity<?> getUserRestaurants(@RequestParam Long userId) {
        User user = validateUser(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(errorResponse("User not approved or not found"));
        }

        List<Restaurant> restaurants = restaurantRepository.findByOwnerIdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Restaurant r : restaurants) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("ownerId", r.getOwnerId());
            map.put("name", r.getName());
            map.put("address", r.getAddress());
            map.put("phone", r.getPhone());
            map.put("description", r.getDescription());
            map.put("photoPath", r.getPhotoPath() != null ? "/uploads/" + r.getPhotoPath() : null);
            map.put("uniqueLink", r.getUniqueLink());
            map.put("publicUrl", baseUrl + "/menu/" + r.getUniqueLink());
            map.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
            map.put("categoryCount", categoryRepository.countByRestaurantId(r.getId()));
            map.put("itemCount", menuItemRepository.countByRestaurantId(r.getId()));
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/restaurant/{restId}/link")
    public ResponseEntity<Map<String, Object>> getMenuLink(@PathVariable Long restId,
                                                           @RequestParam Long userId) {
        User user = validateUser(userId);
        if (user == null || !ownsRestaurant(user, restId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(errorResponse("Not authorized"));
        }
        Restaurant restaurant = restaurantRepository.findById(restId).orElse(null);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("link", baseUrl + "/menu/" + restaurant.getUniqueLink());
        response.put("restaurantName", restaurant.getName());
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────
    // 📂 CATEGORIES
    // ─────────────────────────────────────────
    @GetMapping("/restaurant/{restId}/categories")
    public ResponseEntity<List<Map<String, Object>>> getCategories(@PathVariable Long restId) {
        List<MenuCategory> categories = categoryRepository.findByRestaurantIdOrderBySortOrderAsc(restId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (MenuCategory cat : categories) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", cat.getId());
            map.put("restaurantId", cat.getRestaurantId());
            map.put("name", cat.getName());
            map.put("sortOrder", cat.getSortOrder());
            map.put("count", menuItemRepository.countByCategoryId(cat.getId()));
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/category/add")
    public ResponseEntity<Map<String, Object>> addCategory(@RequestParam Long userId,
                                                           @RequestParam Long restaurantId,
                                                           @RequestParam String name) {
        User user = validateUser(userId);
        if (user == null || !ownsRestaurant(user, restaurantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse("Ruxsat yo'q"));
        }
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(errorResponse("Nom bo'sh bo'lishi mumkin emas"));
        }

        long count = categoryRepository.countByRestaurantId(restaurantId);
        MenuCategory category = new MenuCategory(restaurantId, name.trim(), (int) count + 1);
        categoryRepository.save(category);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("id", category.getId());
        response.put("name", category.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/category/{catId}")
    public ResponseEntity<Map<String, Object>> deleteCategory(@PathVariable Long catId,
                                                              @RequestParam Long userId) {
        User user = validateUser(userId);
        if (user == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse("Auth"));

        MenuCategory cat = categoryRepository.findById(catId).orElse(null);
        if (cat == null) return ResponseEntity.badRequest().body(errorResponse("Topilmadi"));

        if (!ownsRestaurant(user, cat.getRestaurantId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse("Ruxsat yo'q"));
        }

        // Kategoriyadagi items'lardan rasmlarni o'chirib yubormaymiz fayl tizimidan,
        // lekin DB'dan o'chiramiz
        List<MenuItem> items = menuItemRepository.findByCategoryIdOrderBySortOrderAsc(catId);
        for (MenuItem item : items) {
            deletePhotoFile(item.getPhotoPath());
        }
        menuItemRepository.deleteAll(items);
        categoryRepository.delete(cat);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────
    // 🍴 ITEMS
    // ─────────────────────────────────────────
    @GetMapping("/category/{catId}/items")
    public ResponseEntity<List<Map<String, Object>>> getItems(@PathVariable Long catId) {
        List<MenuItem> items = menuItemRepository.findByCategoryIdOrderBySortOrderAsc(catId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (MenuItem item : items) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.getId());
            map.put("categoryId", item.getCategoryId());
            map.put("restaurantId", item.getRestaurantId());
            map.put("name", item.getName());
            map.put("price", item.getPrice());
            map.put("description", item.getDescription());
            map.put("photoPath", item.getPhotoPath() != null ? "/uploads/" + item.getPhotoPath() : null);
            map.put("available", item.isAvailable());
            map.put("sortOrder", item.getSortOrder());
            result.add(map);
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
        User user = validateUser(userId);
        if (user == null || !ownsRestaurant(user, restaurantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse("Ruxsat yo'q"));
        }

        MenuCategory cat = categoryRepository.findById(categoryId).orElse(null);
        if (cat == null || !cat.getRestaurantId().equals(restaurantId)) {
            return ResponseEntity.badRequest().body(errorResponse("Kategoriya noto'g'ri"));
        }

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(errorResponse("Nom kerak"));
        }

        MenuItem item = new MenuItem(restaurantId, categoryId, name.trim());
        item.setDescription(description != null ? description.trim() : null);
        item.setPrice(price);
        item.setAvailable(true);

        String savedPhoto = savePhoto(photo);
        if (savedPhoto != null) {
            item.setPhotoPath(savedPhoto);
        }

        long order = menuItemRepository.countByCategoryId(categoryId);
        item.setSortOrder((int) order + 1);
        menuItemRepository.save(item);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("id", item.getId());
        response.put("photoPath", item.getPhotoPath() != null ? "/uploads/" + item.getPhotoPath() : null);
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
        User user = validateUser(userId);
        if (user == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse("Auth"));

        MenuItem item = menuItemRepository.findById(itemId).orElse(null);
        if (item == null) return ResponseEntity.badRequest().body(errorResponse("Topilmadi"));

        if (!ownsRestaurant(user, item.getRestaurantId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse("Ruxsat yo'q"));
        }

        if (name != null && !name.trim().isEmpty()) item.setName(name.trim());
        if (description != null) item.setDescription(description.trim());
        if (price != null) item.setPrice(price);
        if (available != null) item.setAvailable(available);

        if (photo != null && !photo.isEmpty()) {
            String savedPhoto = savePhoto(photo);
            if (savedPhoto != null) {
                deletePhotoFile(item.getPhotoPath());
                item.setPhotoPath(savedPhoto);
            }
        }

        menuItemRepository.save(item);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("photoPath", item.getPhotoPath() != null ? "/uploads/" + item.getPhotoPath() : null);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<Map<String, Object>> deleteItem(@PathVariable Long itemId,
                                                          @RequestParam Long userId) {
        User user = validateUser(userId);
        if (user == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse("Auth"));

        MenuItem item = menuItemRepository.findById(itemId).orElse(null);
        if (item == null) return ResponseEntity.badRequest().body(errorResponse("Topilmadi"));

        if (!ownsRestaurant(user, item.getRestaurantId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse("Ruxsat yo'q"));
        }

        deletePhotoFile(item.getPhotoPath());
        menuItemRepository.delete(item);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────
    // 📸 PHOTO HELPERS
    // ─────────────────────────────────────────
    private String savePhoto(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) return null;

        // Tekshiruvlar
        if (photo.getSize() > MAX_FILE_SIZE) return null;
        String contentType = photo.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            return null;
        }

        try {
            String original = photo.getOriginalFilename() != null ? photo.getOriginalFilename() : "img.jpg";
            String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".jpg";
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext.toLowerCase();
            Path targetPath = Paths.get(uploadDir, fileName);
            Files.createDirectories(targetPath.getParent());
            Files.copy(photo.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void deletePhotoFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) return;
        try {
            Files.deleteIfExists(Paths.get(uploadDir, fileName));
        } catch (Exception ignored) {}
    }
}