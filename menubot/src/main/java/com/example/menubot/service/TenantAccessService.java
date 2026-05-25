package com.example.menubot.service;

import com.example.menubot.domain.entity.AppUser;
import com.example.menubot.domain.entity.RestaurantEntity;
import com.example.menubot.domain.enums.RestaurantStatus;
import com.example.menubot.domain.enums.UserRole;
import com.example.menubot.domain.repository.RestaurantEntityRepository;
import com.example.menubot.exception.ApiException;
import com.example.menubot.security.AuthContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TenantAccessService {

    private final RestaurantEntityRepository restaurantRepository;

    public TenantAccessService(RestaurantEntityRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    public RestaurantEntity requireApprovedRestaurant(Long restaurantId) {
        RestaurantEntity restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Restaurant not found"));
        if (restaurant.getStatus() != RestaurantStatus.APPROVED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Restaurant not approved");
        }
        return restaurant;
    }

    public RestaurantEntity requireVendorOwner(Long restaurantId) {
        AppUser user = AuthContext.require();
        RestaurantEntity restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Restaurant not found"));

        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return restaurant;
        }
        if (user.getRole() != UserRole.VENDOR || !restaurant.getOwnerId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return restaurant;
    }
}
