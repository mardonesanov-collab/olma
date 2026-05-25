package com.example.menubot.service;

import com.example.menubot.domain.entity.CategoryEntity;
import com.example.menubot.domain.entity.ProductEntity;
import com.example.menubot.domain.repository.CategoryEntityRepository;
import com.example.menubot.domain.repository.ProductEntityRepository;
import com.example.menubot.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MenuCatalogService {

    private final CategoryEntityRepository categoryRepository;
    private final ProductEntityRepository productRepository;
    private final TenantAccessService tenantAccess;
    private final MediaStorageService mediaStorage;

    public MenuCatalogService(CategoryEntityRepository categoryRepository,
                              ProductEntityRepository productRepository,
                              TenantAccessService tenantAccess,
                              MediaStorageService mediaStorage) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.tenantAccess = tenantAccess;
        this.mediaStorage = mediaStorage;
    }

    public Map<String, Object> publicMenu(Long restaurantId, String search) {
        tenantAccess.requireApprovedRestaurant(restaurantId);
        List<CategoryEntity> categories = categoryRepository.findByRestaurantIdOrderBySortOrderAsc(restaurantId);
        List<Map<String, Object>> catList = categories.stream().map(cat -> {
            List<ProductEntity> products = search != null && !search.isBlank()
                    ? productRepository.searchAvailableFallback(restaurantId, search.trim())
                    : productRepository.findByRestaurantIdAndAvailableTrueOrderBySortOrderAsc(restaurantId);
            List<ProductEntity> catProducts = products.stream()
                    .filter(p -> p.getCategoryId().equals(cat.getId()))
                    .toList();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", cat.getId());
            m.put("name", cat.getName());
            m.put("items", catProducts.stream().map(this::productDto).toList());
            return m;
        }).filter(c -> !((List<?>) c.get("items")).isEmpty() || search == null).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categories", catList);
        return result;
    }

    @Transactional
    public CategoryEntity createCategory(Long restaurantId, String name) {
        tenantAccess.requireVendorOwner(restaurantId);
        int order = (int) categoryRepository.countByRestaurantId(restaurantId) + 1;
        CategoryEntity c = new CategoryEntity();
        c.setRestaurantId(restaurantId);
        c.setName(name.trim());
        c.setSortOrder(order);
        return categoryRepository.save(c);
    }

    @Transactional
    public void deleteCategory(Long restaurantId, Long categoryId) {
        tenantAccess.requireVendorOwner(restaurantId);
        CategoryEntity c = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
        if (!c.getRestaurantId().equals(restaurantId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        categoryRepository.delete(c);
    }

    @Transactional
    public ProductEntity createProduct(Long restaurantId, Long categoryId, String name, String description,
                                       Long price, MultipartFile image) {
        tenantAccess.requireVendorOwner(restaurantId);
        CategoryEntity cat = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
        if (!cat.getRestaurantId().equals(restaurantId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        ProductEntity p = new ProductEntity();
        p.setRestaurantId(restaurantId);
        p.setCategoryId(categoryId);
        p.setName(name.trim());
        p.setDescription(description);
        p.setPrice(price != null ? price : 0L);
        String path = mediaStorage.store(image, "products");
        if (path != null) p.setImageUrl(mediaStorage.toPublicUrl(path));
        return productRepository.save(p);
    }

    @Transactional
    public ProductEntity updateProduct(Long restaurantId, Long productId, String name, String description,
                                       Long price, Boolean available, MultipartFile image) {
        tenantAccess.requireVendorOwner(restaurantId);
        ProductEntity p = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        if (!p.getRestaurantId().equals(restaurantId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (name != null && !name.isBlank()) p.setName(name.trim());
        if (description != null) p.setDescription(description);
        if (price != null) p.setPrice(price);
        if (available != null) p.setAvailable(available);
        String path = mediaStorage.store(image, "products");
        if (path != null) p.setImageUrl(mediaStorage.toPublicUrl(path));
        return productRepository.save(p);
    }

    public List<ProductEntity> vendorProducts(Long restaurantId) {
        tenantAccess.requireVendorOwner(restaurantId);
        return productRepository.findByRestaurantIdAndAvailableTrueOrderBySortOrderAsc(restaurantId);
    }

    @Transactional
    public void deleteProduct(Long restaurantId, Long productId) {
        tenantAccess.requireVendorOwner(restaurantId);
        ProductEntity p = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
        if (!p.getRestaurantId().equals(restaurantId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        productRepository.delete(p);
    }

    private Map<String, Object> productDto(ProductEntity p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("description", p.getDescription());
        m.put("price", p.getPrice());
        m.put("imageUrl", p.getImageUrl());
        return m;
    }
}
