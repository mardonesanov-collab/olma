package com.example.menubot.service;

import com.example.menubot.model.Language;
import com.example.menubot.model.Restaurant;
import com.example.menubot.model.User;
import com.example.menubot.repository.RestaurantRepository;
import com.example.menubot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;
import java.util.stream.Collectors;

@org.springframework.context.annotation.Profile("legacy")
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final BotHelper bot;

    @Value("${bot.admin.id}")
    private Long adminId;

    @Value("${app.base-url}")
    private String baseUrl;

    public AdminService(UserRepository userRepository,
                        RestaurantRepository restaurantRepository,
                        BotHelper bot) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.bot = bot;
    }

    public void approveUser(Long chatId, Integer msgId, Long reqId, Language lang) {
        User requester = userRepository.findById(reqId).orElse(null);
        if (requester == null) return;

        requester.setApproved(true);
        userRepository.save(requester);

        bot.deleteMessage(chatId, msgId);
        bot.sendMessage(chatId, bot.t("user_approved", lang, requester.getFirstName()));

        Language uLang = requester.getLanguage() != null ? requester.getLanguage() : Language.UZBEK;
        String webAppUrl = baseUrl + "/webapp/" + requester.getId();

        SendMessage sm = new SendMessage();
        sm.setChatId(requester.getId().toString());
        sm.setText("✅ " + bot.t("request_approved", uLang) + "\n\n🌐 Restoran panelingizga kiring 👇");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(Collections.singletonList(
                bot.webAppBtn("🍽 Restoran Panelini Ochish", webAppUrl)
        ));
        markup.setKeyboard(rows);
        sm.setReplyMarkup(markup);
        bot.send(sm);

        bot.sendMainMenu(requester.getId(), uLang);
    }

    public void rejectUser(Long chatId, Integer msgId, Long reqId, Language lang) {
        User requester = userRepository.findById(reqId).orElse(null);
        if (requester == null) return;

        bot.deleteMessage(chatId, msgId);
        bot.sendMessage(chatId, bot.t("user_rejected", lang, requester.getFirstName()));

        Language uLang = requester.getLanguage() != null ? requester.getLanguage() : Language.UZBEK;
        bot.sendMessage(reqId, bot.t("request_rejected", uLang));
    }

    public void sendApprovalRequestToAdmin(User requester) {
        if (requester.isApproved() || requester.isAdmin()) return;

        SendMessage sm = new SendMessage();
        sm.setChatId(adminId.toString());
        sm.setText(bot.t("admin_new_request", Language.UZBEK,
                requester.getFirstName() + (requester.getLastName() != null ? " " + requester.getLastName() : ""),
                requester.getId(),
                requester.getUsername() != null ? "@" + requester.getUsername() : "-"));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(Arrays.asList(
                bot.btn("✅ " + bot.t("approve", Language.UZBEK), "approve_" + requester.getId()),
                bot.btn("❌ " + bot.t("reject", Language.UZBEK), "reject_" + requester.getId())
        ));
        markup.setKeyboard(rows);
        sm.setReplyMarkup(markup);
        bot.send(sm);
    }

    public void showPendingRequestsInline(Long chatId, Language lang) {
        List<User> pending = userRepository.findByApprovedFalse().stream()
                .filter(u -> !u.isAdmin())
                .collect(Collectors.toList());

        if (pending.isEmpty()) {
            bot.sendMessage(chatId, bot.t("no_pending", lang));
            return;
        }

        SendMessage sm = new SendMessage();
        sm.setChatId(chatId.toString());
        sm.setText(bot.t("pending_list", lang));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (User u : pending) {
            String name = u.getFirstName() + (u.getLastName() != null ? " " + u.getLastName() : "");
            InlineKeyboardButton noop = new InlineKeyboardButton();
            noop.setText("👤 " + name + " (ID:" + u.getId() + ")");
            noop.setCallbackData("noop");
            rows.add(Collections.singletonList(noop));
            rows.add(Arrays.asList(
                    bot.btn("✅ " + bot.t("approve", lang), "approve_" + u.getId()),
                    bot.btn("❌ " + bot.t("reject", lang), "reject_" + u.getId())
            ));
        }
        rows.add(Collections.singletonList(bot.btn("◀️ " + bot.t("back", lang), "back_main")));
        markup.setKeyboard(rows);
        sm.setReplyMarkup(markup);
        bot.send(sm);
    }

    public void showAllUsersInline(Long chatId, Language lang) {
        List<User> users = userRepository.findByAdminFalse();
        StringBuilder sb = new StringBuilder(bot.t("users_title", lang)).append("\n\n");
        for (User u : users) {
            sb.append("👤 ").append(u.getFirstName());
            if (u.getLastName() != null) sb.append(" ").append(u.getLastName());
            sb.append(u.isApproved() ? " ✅" : " ⏳");
            if (u.getUsername() != null) sb.append(" @").append(u.getUsername());
            sb.append("\n");
        }
        bot.sendMessage(chatId, sb.toString());
    }

    public void showAllRestaurantsInline(Long chatId, Language lang) {
        List<Restaurant> all = restaurantRepository.findAll();
        if (all.isEmpty()) {
            bot.sendMessage(chatId, bot.t("no_restaurants", lang));
            return;
        }
        StringBuilder sb = new StringBuilder(bot.t("all_restaurants_title", lang)).append("\n\n");
        for (Restaurant r : all) {
            User owner = userRepository.findById(r.getOwnerId()).orElse(null);
            String ownerName = owner != null ? owner.getFirstName() : "Noma'lum";
            sb.append("🍽 ").append(r.getName()).append(" — ").append(ownerName).append("\n");
            sb.append("   🔗 ").append(baseUrl).append("/menu/").append(r.getUniqueLink()).append("\n\n");
        }
        bot.sendMessage(chatId, sb.toString());
    }
}