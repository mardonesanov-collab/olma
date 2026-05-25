package com.example.menubot.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {

    /**
     * Barcha React Router yo'llarini `index.html` ga forward qilamiz.
     * - "/"      → Dashboard
     * - "/webapp/{userId}"
     * - "/webapp/{userId}/restaurant/{restId}"
     * - "/webapp/{userId}/restaurant/{restId}/category/{catId}"
     * va h.k.
     *
     * Lekin `/api/**`, `/uploads/**`, `/assets/**` larni MUTLAQO o'tkazib yubormaymiz.
     */
    @GetMapping(value = {
            "/",
            "/webapp",
            "/webapp/**"
    })
    public String forwardReact() {
        return "forward:/index.html";
    }

    /**
     * Menu sahifasi (public link)
     */
    @GetMapping(value = { "/menu/**" })
    public String forwardMenu() {
        return "forward:/index.html";
    }
}