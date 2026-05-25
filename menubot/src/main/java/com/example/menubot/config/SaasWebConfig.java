package com.example.menubot.config;

import com.example.menubot.security.TelegramAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaasWebConfig implements WebMvcConfigurer {

    private final TelegramAuthInterceptor authInterceptor;

    public SaasWebConfig(TelegramAuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/public/**",
                        "/api/menu/**",
                        "/uploads/**"
                );
    }
}
