package com.example.menubot.service;

import com.example.menubot.domain.entity.AppUser;
import com.example.menubot.domain.entity.RestaurantEntity;
import com.example.menubot.domain.enums.UserRole;
import com.example.menubot.domain.repository.AppUserRepository;
import com.example.menubot.domain.repository.RestaurantEntityRepository;
import com.example.menubot.security.AuthContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WebAppAccessService {

    private final AppUserRepository userRepository;
    private final RestaurantEntityRepository restaurantRepository;
    private final AuthService authService;
    private final long superAdminTgId;

    public WebAppAccessService(AppUserRepository userRepository,
                               RestaurantEntityRepository restaurantRepository,
                               AuthService authService,
                               @Value("${bot.admin.id}") long superAdminTgId) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.authService = authService;
        this.superAdminTgId = superAdminTgId;
    }

    /** URL dagi userId — Telegram tg_id */
    public AppUser resolveByTgId(long tgId) {
        return userRepository.findByTgId(tgId)
                .orElseGet(() -> authService.ensureUserByTgId(tgId, null, "User", ""));
    }

    public boolean canUseWebPanel(AppUser user) {
        return user.getRole() == UserRole.SUPER_ADMIN || user.getRole() == UserRole.VENDOR;
    }

    public boolean isAdmin(AppUser user) {
        return user.getRole() == UserRole.SUPER_ADMIN || user.getTgId() == superAdminTgId;
    }

    public boolean ownsRestaurant(AppUser user, Long restaurantId) {
        if (restaurantId == null) return false;
        if (isAdmin(user)) return true;
        RestaurantEntity r = restaurantRepository.findById(restaurantId).orElse(null);
        return r != null && r.getOwnerId().equals(user.getId());
    }

    public AppUser requireWebUser(long tgId) {
        AppUser user = resolveByTgId(tgId);
        if (!canUseWebPanel(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vendor account required. Send /start in the bot first.");
        }
        return user;
    }

    public <T> T withUser(AppUser user, java.util.function.Supplier<T> action) {
        AuthContext.set(user);
        try {
            return action.get();
        } finally {
            AuthContext.clear();
        }
    }

    public void withUser(AppUser user, Runnable action) {
        AuthContext.set(user);
        try {
            action.run();
        } finally {
            AuthContext.clear();
        }
    }
}
