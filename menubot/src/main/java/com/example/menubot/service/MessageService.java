package com.example.menubot.service;

import com.example.menubot.model.Language;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;

@Service
public class MessageService {

    private final Map<Language, ResourceBundle> bundles = new HashMap<>();

    @PostConstruct
    public void init() {
        ResourceBundle.Control utf8 = new ResourceBundle.Control() {
            @Override
            public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                            ClassLoader loader, boolean reload) throws java.io.IOException {
                String bundleName = toBundleName(baseName, locale);
                String resourceName = toResourceName(bundleName, "properties");
                try (InputStream is = loader.getResourceAsStream(resourceName)) {
                    if (is == null) return null;
                    try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        return new PropertyResourceBundle(reader);
                    }
                }
            }
        };

        try {
            bundles.put(Language.UZBEK, ResourceBundle.getBundle("messages", new Locale("uz"), utf8));
        } catch (Exception ignored) {}
        try {
            bundles.put(Language.RUSSIAN, ResourceBundle.getBundle("messages", new Locale("ru"), utf8));
        } catch (Exception ignored) {}
        try {
            bundles.put(Language.ENGLISH, ResourceBundle.getBundle("messages", new Locale("en"), utf8));
        } catch (Exception ignored) {}
    }

    public String get(String key, Language lang) {
        ResourceBundle bundle = bundles.get(lang != null ? lang : Language.UZBEK);
        if (bundle == null) return "{" + key + "}";
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "{" + key + "}";
        }
    }

    public String get(String key, Language lang, Object... args) {
        String msg = get(key, lang);
        try {
            return MessageFormat.format(msg, args);
        } catch (Exception e) {
            return msg;
        }
    }
}