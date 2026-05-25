package com.example.menubot.api;

import com.example.menubot.domain.entity.RestaurantEntity;
import com.example.menubot.domain.repository.RestaurantEntityRepository;
import com.example.menubot.exception.ApiException;
import com.example.menubot.service.MenuCatalogService;
import com.example.menubot.service.OrderSaasService;
import com.example.menubot.service.TenantAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/public")
public class PublicApiController {

    private static final Pattern START_APP = Pattern.compile("^(\\d+)_table(.+)$");

    private final RestaurantEntityRepository restaurantRepository;
    private final MenuCatalogService menuCatalogService;
    private final TenantAccessService tenantAccess;

    public PublicApiController(RestaurantEntityRepository restaurantRepository,
                               MenuCatalogService menuCatalogService,
                               TenantAccessService tenantAccess) {
        this.restaurantRepository = restaurantRepository;
        this.menuCatalogService = menuCatalogService;
        this.tenantAccess = tenantAccess;
    }

    @GetMapping("/restaurants/{restaurantId}")
    public ResponseEntity<Map<String, Object>> restaurant(@PathVariable Long restaurantId) {
        RestaurantEntity r = tenantAccess.requireApprovedRestaurant(restaurantId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", r.getId());
        body.put("name", r.getName());
        body.put("description", r.getDescription());
        body.put("logoUrl", r.getLogoUrl());
        body.put("bannerUrl", r.getBannerUrl());
        body.put("serviceFeePercent", r.getServiceFeePercent());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/restaurants/{restaurantId}/menu")
    public ResponseEntity<Map<String, Object>> menu(@PathVariable Long restaurantId,
                                                    @RequestParam(required = false) String q) {
        return ResponseEntity.ok(menuCatalogService.publicMenu(restaurantId, q));
    }

    @GetMapping("/parse-startapp")
    public ResponseEntity<Map<String, Object>> parseStartApp(@RequestParam String startapp) {
        Matcher m = START_APP.matcher(startapp);
        if (!m.matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid startapp param");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("restaurantId", Long.parseLong(m.group(1)));
        body.put("tableNumber", m.group(2));
        return ResponseEntity.ok(body);
    }
}
