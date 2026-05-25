package com.example.menubot.security;

import com.example.menubot.domain.entity.AppUser;
import com.example.menubot.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TelegramAuthInterceptor implements HandlerInterceptor {

    private final TelegramInitDataValidator validator;
    private final AuthService authService;

    public TelegramAuthInterceptor(TelegramInitDataValidator validator, AuthService authService) {
        this.validator = validator;
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String initData = request.getHeader("X-Telegram-Init-Data");
        if (initData == null || initData.isBlank()) {
            initData = request.getParameter("initData");
        }

        var payload = validator.validate(initData);
        if (payload.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid Telegram initData\"}");
            return false;
        }

        AppUser user = authService.resolveUser(payload.get());
        AuthContext.set(user);
        request.setAttribute("currentUser", user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }
}
