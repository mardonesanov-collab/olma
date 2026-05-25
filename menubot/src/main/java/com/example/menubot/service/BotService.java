package com.example.menubot.service;

import com.example.menubot.model.*;
import com.example.menubot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
@Profile("legacy")
public class BotService {

    private final UserRepository userRepository;
    private final BotHelper bot;
    private final AdminService adminService;
    private final RestaurantService restaurantService;
    private final CallbackHandler callbackHandler;

    @Value("${bot.admin.id}")
    private Long adminId;

    public BotService(UserRepository userRepository,
                      BotHelper bot,
                      AdminService adminService,
                      RestaurantService restaurantService,
                      CallbackHandler callbackHandler) {
        this.userRepository = userRepository;
        this.bot = bot;
        this.adminService = adminService;
        this.restaurantService = restaurantService;
        this.callbackHandler = callbackHandler;
    }

    public void handleUpdate(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                callbackHandler.handle(update);
            } else if (update.hasMessage()) {
                var message = update.getMessage();
                if (message.hasText()) handleText(update);
                else if (message.hasPhoto()) handlePhoto(update);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleText(Update update) {
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        Long userId = update.getMessage().getFrom().getId();

        User user = getOrCreateUser(userId,
                update.getMessage().getFrom().getFirstName(),
                update.getMessage().getFrom().getLastName(),
                update.getMessage().getFrom().getUserName());

        if (text.equals("/start")) {
            bot.sendLanguageSelection(chatId);
            return;
        }

        Language lang = user.getLanguage() != null ? user.getLanguage() : Language.UZBEK;

        if (text.equals("/admin") && userId.equals(adminId)) {
            user.setState(UserState.ADMIN_MENU);
            userRepository.save(user);
            bot.sendAdminMenu(chatId, lang);
            return;
        }

        if (user.getLanguage() == null || user.getState() == null) {
            bot.sendLanguageSelection(chatId);
            return;
        }

        switch (user.getState()) {
            case SELECT_LANGUAGE -> handleLanguageChoice(chatId, text, user);
            case MAIN_MENU -> handleMainMenu(chatId, text, user);
            case ADMIN_MENU -> handleAdminMenuText(chatId, text, user);
            case VIEWING_RESTAURANTS -> restaurantService.sendMyRestaurants(chatId, user);
            case WAITING_RESTAURANT_NAME -> restaurantService.handleRestaurantName(chatId, text, user);
            case WAITING_RESTAURANT_ADDRESS -> restaurantService.handleRestaurantAddress(chatId, text, user);
            case WAITING_RESTAURANT_PHONE -> restaurantService.handleRestaurantPhone(chatId, text, user);
            case WAITING_RESTAURANT_DESCRIPTION -> restaurantService.handleRestaurantDescription(chatId, text, user);
            case WAITING_CATEGORY_NAME -> restaurantService.handleCategoryName(chatId, text, user);
            case WAITING_ITEM_NAME -> restaurantService.handleItemName(chatId, text, user);
            case WAITING_ITEM_PRICE -> restaurantService.handleItemPrice(chatId, text, user);
            case WAITING_ITEM_DESCRIPTION -> restaurantService.handleItemDescription(chatId, text, user);
            default -> bot.sendMessage(chatId, bot.t("unknown_command", lang));
        }
    }

    private void handlePhoto(Update update) {
        Long chatId = update.getMessage().getChatId();
        Long userId = update.getMessage().getFrom().getId();

        User user = getOrCreateUser(userId,
                update.getMessage().getFrom().getFirstName(),
                update.getMessage().getFrom().getLastName(),
                update.getMessage().getFrom().getUserName());

        Language lang = user.getLanguage() != null ? user.getLanguage() : Language.UZBEK;

        if (user.getState() != UserState.WAITING_ITEM_PHOTO) {
            bot.sendMessage(chatId, bot.t("unknown_command", lang));
            return;
        }

        restaurantService.handleItemPhoto(chatId, user, update);
    }

    private void handleLanguageChoice(Long chatId, String text, User user) {
        Language lang;
        String tl = text.toLowerCase();
        if (text.contains("O'zbek") || tl.contains("uzbek") || tl.contains("uz")) lang = Language.UZBEK;
        else if (text.contains("Рус") || tl.contains("rus")) lang = Language.RUSSIAN;
        else if (tl.contains("english") || tl.contains("en")) lang = Language.ENGLISH;
        else lang = Language.UZBEK;

        user.setLanguage(lang);
        user.setState(UserState.MAIN_MENU);
        userRepository.save(user);

        if (user.isAdmin()) {
            bot.sendAdminMenu(chatId, lang);
        } else if (user.isApproved()) {
            bot.sendMainMenu(chatId, lang);
        } else {
            bot.sendMessage(chatId, bot.t("welcome", lang));
            bot.sendMessage(chatId, "⏳ " + bot.t("not_approved", lang));
            adminService.sendApprovalRequestToAdmin(user);
        }
    }

    private void handleMainMenu(Long chatId, String text, User user) {
        Language lang = user.getLanguage();

        if (text.equals(bot.t("add_restaurant", lang))) {
            if (!user.isApproved()) {
                adminService.sendApprovalRequestToAdmin(user);
                bot.sendMessage(chatId, "⏳ " + bot.t("request_sent", lang));
                return;
            }
            restaurantService.startCreateRestaurant(chatId, user);
            return;
        }

        if (text.equals(bot.t("my_restaurants", lang))) {
            user.setState(UserState.VIEWING_RESTAURANTS);
            userRepository.save(user);
            restaurantService.sendMyRestaurants(chatId, user);
            return;
        }

        if (text.equals(bot.t("help", lang))) {
            bot.sendMessage(chatId, bot.t("help_text", lang));
            return;
        }

        bot.sendMainMenu(chatId, lang);
    }

    private void handleAdminMenuText(Long chatId, String text, User user) {
        Language lang = user.getLanguage();

        if (text.equals(bot.t("pending_requests", lang))) {
            adminService.showPendingRequestsInline(chatId, lang);
            return;
        }
        if (text.equals(bot.t("users_list", lang))) {
            adminService.showAllUsersInline(chatId, lang);
            return;
        }
        if (text.equals(bot.t("all_restaurants", lang))) {
            adminService.showAllRestaurantsInline(chatId, lang);
            return;
        }
        if (text.equals(bot.t("my_restaurants", lang))) {
            user.setState(UserState.VIEWING_RESTAURANTS);
            userRepository.save(user);
            restaurantService.sendMyRestaurants(chatId, user);
            return;
        }
        if (text.equals(bot.t("add_restaurant", lang))) {
            restaurantService.startCreateRestaurant(chatId, user);
            return;
        }
        bot.sendAdminMenu(chatId, lang);
    }

    private User getOrCreateUser(Long userId, String firstName, String lastName, String userName) {
        return userRepository.findById(userId).orElseGet(() -> {
            User newUser = new User(userId, firstName, lastName, userName);
            if (userId.equals(adminId)) {
                newUser.setAdmin(true);
                newUser.setApproved(true);
                newUser.setLanguage(Language.UZBEK);
                newUser.setState(UserState.MAIN_MENU);
            }
            return userRepository.save(newUser);
        });
    }
}