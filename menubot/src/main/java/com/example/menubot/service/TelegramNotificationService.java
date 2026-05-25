package com.example.menubot.service;

import com.example.menubot.domain.entity.AppUser;
import com.example.menubot.domain.entity.OrderEntity;
import com.example.menubot.domain.entity.RestaurantEntity;
import com.example.menubot.domain.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Service
public class TelegramNotificationService {

    private final long superAdminTgId;
    private final AppUserRepository userRepository;
    private volatile TelegramMessageSender sender;

    public TelegramNotificationService(@Value("${bot.admin.id}") long superAdminTgId,
                                       AppUserRepository userRepository) {
        this.superAdminTgId = superAdminTgId;
        this.userRepository = userRepository;
    }

    public void setSender(TelegramMessageSender sender) {
        this.sender = sender;
    }

    public void notifySuperAdminNewRestaurant(RestaurantEntity restaurant, AppUser owner) {
        String username = owner.getUsername() != null ? "@" + owner.getUsername() : "ID " + owner.getTgId();
        String text = "🆕 Yangi restoran ro'yxatdan o'tmoqchi\n\n" +
                "📍 Nomi: " + restaurant.getName() + "\n" +
                "👤 Egasi: " + username;

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(
                InlineKeyboardButton.builder().text("✅ Tasdiqlash").callbackData("approve_rest:" + restaurant.getId()).build(),
                InlineKeyboardButton.builder().text("❌ Rad etish").callbackData("reject_rest:" + restaurant.getId()).build()
        ));

        send(superAdminTgId, text, new InlineKeyboardMarkup(rows));
    }

    public void notifyVendorNewOrder(RestaurantEntity restaurant, OrderEntity order) {
        userRepository.findById(restaurant.getOwnerId()).ifPresent(owner -> {
            String text = "🔔 Yangi buyurtma!\n" +
                    "Stol: " + (order.getTableNumber() != null ? order.getTableNumber() : "—") + "\n" +
                    "Summa: " + order.getFinalPrice() + " so'm\n" +
                    "Buyurtma #" + order.getId();
            send(owner.getTgId(), text, null);
        });
    }

    public void notifyVendorCallWaiter(RestaurantEntity restaurant, String tableNumber) {
        userRepository.findById(restaurant.getOwnerId()).ifPresent(owner -> {
            String text = "🚨 JURGENT! " + tableNumber + "-stolda ofitsiant chaqirilmoqda!";
            send(owner.getTgId(), text, null);
        });
    }

    public void notifyClientReviewRequest(long clientTgId, long restaurantId, long orderId) {
        String text = "⭐ Buyurtmangiz yetkazildi! Restoran haqida baho qoldiring.";
        List<List<InlineKeyboardButton>> rows = List.of(List.of(
                InlineKeyboardButton.builder().text("⭐ Baholash").callbackData("review:" + restaurantId + ":" + orderId).build()
        ));
        send(clientTgId, text, new InlineKeyboardMarkup(rows));
    }

    private void send(long chatId, String text, InlineKeyboardMarkup markup) {
        if (sender == null) return;
        SendMessage msg = SendMessage.builder().chatId(chatId).text(text).build();
        if (markup != null) msg.setReplyMarkup(markup);
        sender.send(msg);
    }

    @FunctionalInterface
    public interface TelegramMessageSender {
        void send(SendMessage message);
    }
}
