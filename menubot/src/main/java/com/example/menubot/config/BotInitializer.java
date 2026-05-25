package com.example.menubot.config;

import com.example.menubot.bot.MenuBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton;
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonWebApp;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Bot faqat ilova to'liq ishga tushgandan keyin ulanadi (8080 band bo'lsa, polling boshlanmaydi).
 */
@Component
@ConditionalOnProperty(name = "bot.enabled", havingValue = "true", matchIfMissing = true)
public class BotInitializer {

    private final MenuBot menuBot;
    private final String baseUrl;

    public BotInitializer(MenuBot menuBot, @Value("${app.base-url}") String baseUrl) {
        this.menuBot = menuBot;
        this.baseUrl = baseUrl.replaceAll("/$", "");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        System.out.println("🌐 Web App base URL: " + baseUrl);

        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(menuBot);
            System.out.println("✅ Telegram bot ulandi: @" + menuBot.getBotUsername());
            syncMenuButton();
        } catch (TelegramApiException e) {
            System.err.println("❌ Botni ro'yxatdan o'tkazishda xatolik: " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("409")) {
                System.err.println("   → Boshqa joyda bot allaqachon ishlayapti (IntelliJ + terminal).");
                System.err.println("   → Faqat BIR nusxa qoldiring yoki bot.enabled=false qiling.");
            }
        }
    }

    private void syncMenuButton() {
        try {
            WebAppInfo webAppInfo = new WebAppInfo(baseUrl);
            menuBot.execute(SetChatMenuButton.builder()
                    .menuButton(MenuButtonWebApp.builder()
                            .text("Menu bot")
                            .webAppInfo(webAppInfo)
                            .build())
                    .build());
            System.out.println("✅ Telegram Menu Button yangilandi: " + baseUrl);
        } catch (TelegramApiException e) {
            System.err.println("⚠ Menu Button yangilanmadi: " + e.getMessage());
            System.err.println("   → @BotFather → Bot Settings → Menu Button → URL: " + baseUrl);
        }
    }
}
