package com.example.menubot.service;

import com.example.menubot.domain.entity.RestaurantEntity;
import com.example.menubot.domain.repository.AppUserRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class SaasCallbackHandler {

    private final RestaurantSaasService restaurantSaasService;
    private final AppUserRepository userRepository;
    private final TelegramNotificationService notifications;

    public SaasCallbackHandler(RestaurantSaasService restaurantSaasService,
                               AppUserRepository userRepository,
                               TelegramNotificationService notifications) {
        this.restaurantSaasService = restaurantSaasService;
        this.userRepository = userRepository;
        this.notifications = notifications;
    }

    public boolean handle(Update update, TelegramNotificationService.TelegramMessageSender sender) {
        if (!update.hasCallbackQuery()) return false;
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (data.startsWith("approve_rest:")) {
            long restId = Long.parseLong(data.substring("approve_rest:".length()));
            RestaurantEntity r = restaurantSaasService.approve(restId);
            userRepository.findById(r.getOwnerId()).ifPresent(owner -> {
                SendMessage msg = SendMessage.builder()
                        .chatId(owner.getTgId())
                        .text("✅ \"" + r.getName() + "\" restoraningiz tasdiqlandi! Endi Web App orqali menyuni boshqaring.")
                        .build();
                sender.send(msg);
            });
            sender.send(SendMessage.builder().chatId(chatId).text("✅ Restoran tasdiqlandi: " + r.getName()).build());
            return true;
        }

        if (data.startsWith("reject_rest:")) {
            long restId = Long.parseLong(data.substring("reject_rest:".length()));
            RestaurantEntity r = restaurantSaasService.reject(restId);
            userRepository.findById(r.getOwnerId()).ifPresent(owner -> {
                sender.send(SendMessage.builder()
                        .chatId(owner.getTgId())
                        .text("❌ \"" + r.getName() + "\" arizangiz rad etildi.")
                        .build());
            });
            sender.send(SendMessage.builder().chatId(chatId).text("❌ Restoran rad etildi.").build());
            return true;
        }

        return false;
    }
}
