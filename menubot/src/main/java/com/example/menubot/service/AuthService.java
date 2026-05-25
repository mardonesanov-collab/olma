package com.example.menubot.service;

import com.example.menubot.domain.entity.AppUser;
import com.example.menubot.domain.enums.UserRole;
import com.example.menubot.domain.repository.AppUserRepository;
import com.example.menubot.security.TelegramInitDataValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final long superAdminTgId;

    public AuthService(AppUserRepository userRepository,
                       @Value("${bot.admin.id}") long superAdminTgId) {
        this.userRepository = userRepository;
        this.superAdminTgId = superAdminTgId;
    }

    @Transactional
    public AppUser resolveUser(TelegramInitDataValidator.TelegramUserPayload payload) {
        return userRepository.findByTgId(payload.tgId())
                .map(u -> updateProfile(u, payload))
                .orElseGet(() -> createUser(payload));
    }

    /** Web App / bot — Telegram ID bo'yicha foydalanuvchini topish yoki yaratish */
    @Transactional
    public AppUser ensureUserByTgId(long tgId, String username, String firstName, String lastName) {
        var payload = new TelegramInitDataValidator.TelegramUserPayload(
                tgId,
                username,
                firstName != null ? firstName : "User",
                lastName != null ? lastName : ""
        );
        return resolveUser(payload);
    }

    private AppUser createUser(TelegramInitDataValidator.TelegramUserPayload payload) {
        AppUser user = new AppUser();
        user.setTgId(payload.tgId());
        user.setUsername(payload.username());
        user.setFirstName(payload.firstName());
        user.setLastName(payload.lastName());
        user.setRole(payload.tgId() == superAdminTgId ? UserRole.SUPER_ADMIN : UserRole.CLIENT);
        return userRepository.save(user);
    }

    private AppUser updateProfile(AppUser user, TelegramInitDataValidator.TelegramUserPayload payload) {
        user.setUsername(payload.username());
        user.setFirstName(payload.firstName());
        user.setLastName(payload.lastName());
        if (payload.tgId() == superAdminTgId) {
            user.setRole(UserRole.SUPER_ADMIN);
        }
        return userRepository.save(user);
    }

    @Transactional
    public void promoteToVendor(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(UserRole.VENDOR);
        userRepository.save(user);
    }
}
