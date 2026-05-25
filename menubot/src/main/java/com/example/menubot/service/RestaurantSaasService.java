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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class RestaurantSaasService {

    private final RestaurantEntityRepository restaurantRepository;
    private final AuthService authService;
    private final MediaStorageService mediaStorage;
    private final QrCodeService qrCodeService;
    private final TelegramNotificationService notifications;
    private final TenantAccessService tenantAccess;

    public RestaurantSaasService(RestaurantEntityRepository restaurantRepository,
                                 AuthService authService,
                                 MediaStorageService mediaStorage,
                                 QrCodeService qrCodeService,
                                 TelegramNotificationService notifications,
                                 TenantAccessService tenantAccess) {
        this.restaurantRepository = restaurantRepository;
        this.authService = authService;
        this.mediaStorage = mediaStorage;
        this.qrCodeService = qrCodeService;
        this.notifications = notifications;
        this.tenantAccess = tenantAccess;
    }

    @Transactional
    public RestaurantEntity registerVendorRestaurant(String name, String description) {
        AppUser user = AuthContext.require();
        if (restaurantRepository.existsByOwnerIdAndNameIgnoreCase(user.getId(), name)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Restaurant with this name already exists");
        }

        authService.promoteToVendor(user.getId());

        RestaurantEntity r = new RestaurantEntity();
        r.setOwnerId(user.getId());
        r.setName(name.trim());
        r.setDescription(description);
        r.setStatus(user.getRole() == UserRole.SUPER_ADMIN
                ? RestaurantStatus.APPROVED
                : RestaurantStatus.PENDING);
        r.setUniqueSlug(generateSlug(name));
        r = restaurantRepository.save(r);

        if (user.getRole() != UserRole.SUPER_ADMIN) {
            notifications.notifySuperAdminNewRestaurant(r, user);
        }
        return r;
    }

    @Transactional
    public RestaurantEntity approve(Long restaurantId) {
        RestaurantEntity r = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Not found"));
        r.setStatus(RestaurantStatus.APPROVED);
        return restaurantRepository.save(r);
    }

    @Transactional
    public RestaurantEntity reject(Long restaurantId) {
        RestaurantEntity r = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Not found"));
        r.setStatus(RestaurantStatus.BLOCKED);
        return restaurantRepository.save(r);
    }

    public List<RestaurantEntity> myRestaurants() {
        AppUser user = AuthContext.require();
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return restaurantRepository.findAll();
        }
        return restaurantRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public RestaurantEntity updateSettings(Long restaurantId, String name, String description,
                                           BigDecimal serviceFeePercent,
                                           MultipartFile logo, MultipartFile banner) {
        RestaurantEntity r = tenantAccess.requireVendorOwner(restaurantId);
        if (name != null && !name.isBlank()) r.setName(name.trim());
        if (description != null) r.setDescription(description.trim());
        if (serviceFeePercent != null) r.setServiceFeePercent(serviceFeePercent);

        String logoPath = mediaStorage.store(logo, "logos");
        if (logoPath != null) r.setLogoUrl(mediaStorage.toPublicUrl(logoPath));

        String bannerPath = mediaStorage.store(banner, "banners");
        if (bannerPath != null) r.setBannerUrl(mediaStorage.toPublicUrl(bannerPath));

        return restaurantRepository.save(r);
    }

    @Transactional
    public String generateTableQr(Long restaurantId, String tableNumber) {
        tenantAccess.requireVendorOwner(restaurantId);
        String stored = qrCodeService.generateAndStorePng(restaurantId, tableNumber);
        String url = mediaStorage.toPublicUrl(stored);
        RestaurantEntity r = restaurantRepository.findById(restaurantId).orElseThrow();
        r.setQrCodeUrl(url);
        restaurantRepository.save(r);
        return url;
    }

    private String generateSlug(String name) {
        String base = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (base.isBlank()) base = "restaurant";
        return base + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

}
