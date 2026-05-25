package com.example.menubot.config;

import com.example.menubot.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap {

    private final AuthService authService;
    private final long superAdminTgId;

    public AdminBootstrap(AuthService authService, @Value("${bot.admin.id}") long superAdminTgId) {
        this.authService = authService;
        this.superAdminTgId = superAdminTgId;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureSuperAdmin() {
        authService.ensureUserByTgId(superAdminTgId, "admin", "Admin", "");
        System.out.println("✅ Super admin (tg_id=" + superAdminTgId + ") bazada tayyor");
    }
}
