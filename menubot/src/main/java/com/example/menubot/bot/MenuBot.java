package com.example.menubot.bot;

import com.example.menubot.service.BotHelper;
import com.example.menubot.service.SaasBotService;
import com.example.menubot.service.TelegramBotGateway;
import com.example.menubot.service.TelegramNotificationService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class MenuBot extends TelegramLongPollingBot {

    private final SaasBotService saasBotService;
    private final BotHelper botHelper;
    private final TelegramNotificationService notifications;
    private final TelegramBotGateway botGateway;

    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.username}")
    private String botUsername;

    public MenuBot(@Lazy SaasBotService saasBotService, BotHelper botHelper,
                   TelegramNotificationService notifications, TelegramBotGateway botGateway) {
        this.saasBotService = saasBotService;
        this.botHelper = botHelper;
        this.notifications = notifications;
        this.botGateway = botGateway;
    }

    @PostConstruct
    public void init() {
        botHelper.setBot(this);
        botGateway.register(msg -> {
            try {
                execute(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        notifications.setSender(botGateway::send);
    }

    @Override
    public String getBotUsername() { return botUsername; }

    @Override
    public String getBotToken() { return botToken; }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            saasBotService.handleUpdate(update);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}