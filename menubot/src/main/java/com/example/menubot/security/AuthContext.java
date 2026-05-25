package com.example.menubot.security;

import com.example.menubot.domain.entity.AppUser;

public class AuthContext {
    private static final ThreadLocal<AppUser> CURRENT = new ThreadLocal<>();

    public static void set(AppUser user) {
        CURRENT.set(user);
    }

    public static AppUser require() {
        AppUser user = CURRENT.get();
        if (user == null) {
            throw new IllegalStateException("Unauthorized");
        }
        return user;
    }

    public static AppUser optional() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
