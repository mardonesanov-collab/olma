package com.example.menubot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.example.menubot.domain.entity")
@EnableJpaRepositories(basePackages = "com.example.menubot.domain.repository")
public class MenubotApplication {
    public static void main(String[] args) {
        SpringApplication.run(MenubotApplication.class, args);
        System.out.println("===========================================");
        System.out.println("  MenuBot SaaS ishga tushdi");
        System.out.println("  Web: http://localhost:8080");
        System.out.println("  API: /api/v1/");
        System.out.println("===========================================");
    }
}