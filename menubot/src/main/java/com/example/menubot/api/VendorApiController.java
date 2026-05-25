package com.example.menubot.api;

import com.example.menubot.domain.entity.CategoryEntity;
import com.example.menubot.domain.entity.OrderEntity;
import com.example.menubot.domain.entity.ProductEntity;
import com.example.menubot.domain.entity.RestaurantEntity;
import com.example.menubot.domain.enums.OrderStatus;
import com.example.menubot.service.MenuCatalogService;
import com.example.menubot.service.OrderSaasService;
import com.example.menubot.service.RestaurantSaasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vendor")
public class VendorApiController {

    private final RestaurantSaasService restaurantService;
    private final MenuCatalogService menuService;
    private final OrderSaasService orderService;

    public VendorApiController(RestaurantSaasService restaurantService,
                               MenuCatalogService menuService,
                               OrderSaasService orderService) {
        this.restaurantService = restaurantService;
        this.menuService = menuService;
        this.orderService = orderService;
    }

    @PostMapping("/restaurants")
    public ResponseEntity<RestaurantEntity> register(@RequestParam String name,
                                                     @RequestParam(required = false) String description) {
        return ResponseEntity.ok(restaurantService.registerVendorRestaurant(name, description));
    }

    @GetMapping("/restaurants")
    public ResponseEntity<List<RestaurantEntity>> myRestaurants() {
        return ResponseEntity.ok(restaurantService.myRestaurants());
    }

    @PutMapping("/restaurants/{restaurantId}/settings")
    public ResponseEntity<RestaurantEntity> settings(@PathVariable Long restaurantId,
                                                     @RequestParam(required = false) String name,
                                                     @RequestParam(required = false) String description,
                                                     @RequestParam(required = false) BigDecimal serviceFeePercent,
                                                     @RequestParam(required = false) MultipartFile logo,
                                                     @RequestParam(required = false) MultipartFile banner) {
        return ResponseEntity.ok(restaurantService.updateSettings(restaurantId, name, description, serviceFeePercent, logo, banner));
    }

    @PostMapping("/restaurants/{restaurantId}/qr")
    public ResponseEntity<Map<String, String>> generateQr(@PathVariable Long restaurantId,
                                                          @RequestParam String tableNumber) {
        String url = restaurantService.generateTableQr(restaurantId, tableNumber);
        return ResponseEntity.ok(Map.of("qrUrl", url));
    }

    @PostMapping("/restaurants/{restaurantId}/categories")
    public ResponseEntity<CategoryEntity> addCategory(@PathVariable Long restaurantId,
                                                      @RequestParam String name) {
        return ResponseEntity.ok(menuService.createCategory(restaurantId, name));
    }

    @DeleteMapping("/restaurants/{restaurantId}/categories/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long restaurantId, @PathVariable Long categoryId) {
        menuService.deleteCategory(restaurantId, categoryId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/restaurants/{restaurantId}/products")
    public ResponseEntity<ProductEntity> addProduct(@PathVariable Long restaurantId,
                                                    @RequestParam Long categoryId,
                                                    @RequestParam String name,
                                                    @RequestParam(required = false) String description,
                                                    @RequestParam(required = false) Long price,
                                                    @RequestParam(required = false) MultipartFile image) {
        return ResponseEntity.ok(menuService.createProduct(restaurantId, categoryId, name, description, price, image));
    }

    @PutMapping("/restaurants/{restaurantId}/products/{productId}")
    public ResponseEntity<ProductEntity> updateProduct(@PathVariable Long restaurantId,
                                                       @PathVariable Long productId,
                                                       @RequestParam(required = false) String name,
                                                       @RequestParam(required = false) String description,
                                                       @RequestParam(required = false) Long price,
                                                       @RequestParam(required = false) Boolean available,
                                                       @RequestParam(required = false) MultipartFile image) {
        return ResponseEntity.ok(menuService.updateProduct(restaurantId, productId, name, description, price, available, image));
    }

    @GetMapping("/restaurants/{restaurantId}/orders")
    public ResponseEntity<List<OrderEntity>> orders(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(orderService.vendorOrders(restaurantId));
    }

    @PatchMapping("/restaurants/{restaurantId}/orders/{orderId}/status")
    public ResponseEntity<OrderEntity> orderStatus(@PathVariable Long restaurantId,
                                                     @PathVariable Long orderId,
                                                     @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateStatus(restaurantId, orderId, status));
    }

    @GetMapping("/restaurants/{restaurantId}/analytics")
    public ResponseEntity<Map<String, Object>> analytics(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(orderService.analytics(restaurantId));
    }
}
