package com.example.menubot.service;

import com.example.menubot.model.Language;
import com.example.menubot.model.User;
import com.example.menubot.model.UserState;
import com.example.menubot.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@org.springframework.context.annotation.Profile("legacy")
@Component
public class CallbackHandler {

    private final UserRepository userRepository;
    private final BotHelper bot;
    private final AdminService adminService;
    private final RestaurantService restaurantService;

    public CallbackHandler(UserRepository userRepository,
                           BotHelper bot,
                           AdminService adminService,
                           RestaurantService restaurantService) {
        this.userRepository = userRepository;
        this.bot = bot;
        this.adminService = adminService;
        this.restaurantService = restaurantService;
    }

    public void handle(Update update) {
        var cb = update.getCallbackQuery();
        String data = cb.getData();
        Long chatId = cb.getMessage().getChatId();
        Integer msgId = cb.getMessage().getMessageId();
        Long userId = cb.getFrom().getId();

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            bot.sendMessage(chatId, "Iltimos /start ni bosing");
            return;
        }

        Language lang = user.getLanguage() != null ? user.getLanguage() : Language.UZBEK;

        if (data.equals("noop")) return;

        if (data.startsWith("lang_")) {
            handleLanguageCallback(chatId, msgId, data, user);
            return;
        }

        if (data.equals("back_main")) {
            user.setState(UserState.MAIN_MENU);
            userRepository.save(user);
            bot.deleteMessage(chatId, msgId);
            if (user.isAdmin()) bot.sendAdminMenu(chatId, lang);
            else bot.sendMainMenu(chatId, lang);
            return;
        }

        if (data.equals("back_restaurants")) {
            user.setState(UserState.VIEWING_RESTAURANTS);
            userRepository.save(user);
            bot.deleteMessage(chatId, msgId);
            restaurantService.sendMyRestaurants(chatId, user);
            return;
        }

        if (data.startsWith("approve_")) {
            adminService.approveUser(chatId, msgId,
                    Long.parseLong(data.replace("approve_", "")), lang);
            return;
        }

        if (data.startsWith("reject_")) {
            adminService.rejectUser(chatId, msgId,
                    Long.parseLong(data.replace("reject_", "")), lang);
            return;
        }

        if (data.startsWith("restaurant_")) {
            Long restId = Long.parseLong(data.replace("restaurant_", ""));
            restaurantService.selectRestaurantAndShow(chatId, restId, user, lang, msgId);
            return;
        }

        if (data.startsWith("view_menu_")) {
            Long restId = Long.parseLong(data.replace("view_menu_", ""));
            bot.deleteMessage(chatId, msgId);
            restaurantService.showMenuItems(chatId, restId, lang);
            return;
        }

        if (data.startsWith("categories_")) {
            Long restId = Long.parseLong(data.replace("categories_", ""));
            bot.deleteMessage(chatId, msgId);
            restaurantService.showCategories(chatId, restId, lang);
            return;
        }

        if (data.startsWith("add_category_")) {
            Long restId = Long.parseLong(data.replace("add_category_", ""));
            restaurantService.startCreateCategory(chatId, restId, user, msgId);
            return;
        }

        if (data.startsWith("category_")) {
            Long catId = Long.parseLong(data.replace("category_", ""));
            restaurantService.selectCategoryAndShow(chatId, catId, user, lang, msgId);
            return;
        }

        if (data.startsWith("add_item_")) {
            Long catId = Long.parseLong(data.replace("add_item_", ""));
            restaurantService.startCreateItem(chatId, catId, user, msgId);
            return;
        }

        if (data.startsWith("share_")) {
            Long restId = Long.parseLong(data.replace("share_", ""));
            bot.deleteMessage(chatId, msgId);
            restaurantService.shareRestaurant(chatId, restId, lang);
            return;
        }
    }

    private void handleLanguageCallback(Long chatId, Integer msgId, String data, User user) {
        Language chosen = switch (data) {
            case "lang_ru" -> Language.RUSSIAN;
            case "lang_en" -> Language.ENGLISH;
            default -> Language.UZBEK;
        };
        user.setLanguage(chosen);
        user.setState(UserState.MAIN_MENU);
        userRepository.save(user);
        bot.deleteMessage(chatId, msgId);

        if (user.isAdmin()) {
            bot.sendAdminMenu(chatId, chosen);
        } else if (user.isApproved()) {
            bot.sendMainMenu(chatId, chosen);
        } else {
            bot.sendMessage(chatId, bot.t("welcome", chosen));
            bot.sendMessage(chatId, "⏳ " + bot.t("not_approved", chosen));
            adminService.sendApprovalRequestToAdmin(user);
        }
    }
}