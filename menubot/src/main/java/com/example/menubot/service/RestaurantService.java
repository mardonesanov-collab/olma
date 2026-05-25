package com.example.menubot.service;

import com.example.menubot.model.*;
import com.example.menubot.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;
import java.util.stream.Collectors;

@org.springframework.context.annotation.Profile("legacy")
@Service
public class RestaurantService {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final FileService fileService;
    private final BotHelper bot;

    @Value("${app.base-url}")
    private String baseUrl;

    public RestaurantService(UserRepository userRepository,
                             RestaurantRepository restaurantRepository,
                             MenuCategoryRepository categoryRepository,
                             MenuItemRepository menuItemRepository,
                             FileService fileService,
                             BotHelper bot) {
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.fileService = fileService;
        this.bot = bot;
    }

    public void startCreateRestaurant(Long chatId, User user) {
        user.setState(UserState.WAITING_RESTAURANT_NAME);
        userRepository.save(user);
        bot.sendMessage(chatId, bot.t("restaurant_name", user.getLanguage()));
    }

    public void handleRestaurantName(Long chatId, String text, User user) {
        Map<String, Object> tmp = bot.getTmp(user);
        tmp.put("name", text);
        bot.saveTmp(user, tmp);
        user.setState(UserState.WAITING_RESTAURANT_ADDRESS);
        userRepository.save(user);
        bot.sendMessage(chatId, bot.t("restaurant_address", user.getLanguage()));
    }

    public void handleRestaurantAddress(Long chatId, String text, User user) {
        Map<String, Object> tmp = bot.getTmp(user);
        tmp.put("address", text);
        bot.saveTmp(user, tmp);
        user.setState(UserState.WAITING_RESTAURANT_PHONE);
        userRepository.save(user);
        bot.sendMessage(chatId, bot.t("restaurant_phone", user.getLanguage()));
    }

    public void handleRestaurantPhone(Long chatId, String text, User user) {
        Map<String, Object> tmp = bot.getTmp(user);
        tmp.put("phone", text);
        bot.saveTmp(user, tmp);
        user.setState(UserState.WAITING_RESTAURANT_DESCRIPTION);
        userRepository.save(user);
        bot.sendMessage(chatId, bot.t("restaurant_description", user.getLanguage()));
    }

    public void handleRestaurantDescription(Long chatId, String text, User user) {
        Language lang = user.getLanguage();
        Map<String, Object> tmp = bot.getTmp(user);

        Restaurant r = new Restaurant(user.getId(), (String) tmp.get("name"));
        r.setAddress((String) tmp.get("address"));
        r.setPhone((String) tmp.get("phone"));
        r.setDescription(text);
        restaurantRepository.save(r);

        bot.clearTmp(user);
        user.setState(UserState.MAIN_MENU);
        user.setSelectedRestaurantId(r.getId());
        userRepository.save(user);

        bot.sendMessage(chatId, "✅ " + bot.t("restaurant_saved", lang));

        // WebApp tugmasi
        String webAppUrl = baseUrl + "/webapp/" + user.getId();
        SendMessage sm = new SendMessage();
        sm.setChatId(chatId.toString());
        sm.setText("🎉 Restoran qo'shildi!\n\n🌐 Endi panelda taom va kategoriyalar qo'shing 👇");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(Collections.singletonList(bot.webAppBtn("🍽 Panelni Ochish", webAppUrl)));
        rows.add(Collections.singletonList(bot.btn("🔗 Menyu havolasi", "share_" + r.getId())));
        markup.setKeyboard(rows);
        sm.setReplyMarkup(markup);
        bot.send(sm);
    }

    public void startCreateCategory(Long chatId, Long restId, User user, Integer msgId) {
        user.setSelectedRestaurantId(restId);
        user.setState(UserState.WAITING_CATEGORY_NAME);
        userRepository.save(user);
        if (msgId != null) bot.deleteMessage(chatId, msgId);
        bot.sendMessage(chatId, bot.t("category_name", user.getLanguage()));
    }

    public void handleCategoryName(Long chatId, String text, User user) {
        Language lang = user.getLanguage();
        Long restId = user.getSelectedRestaurantId();

        if (restId == null) {
            bot.sendMessage(chatId, "❌ " + bot.t("error", lang));
            user.setState(UserState.MAIN_MENU);
            userRepository.save(user);
            return;
        }

        long count = categoryRepository.countByRestaurantId(restId);
        MenuCategory cat = new MenuCategory(restId, text, (int) count + 1);
        categoryRepository.save(cat);

        user.setState(UserState.MAIN_MENU);
        userRepository.save(user);

        bot.sendMessage(chatId, "✅ " + bot.t("category_saved", lang));
        showRestaurantActions(chatId, restId, lang);
    }

    public void startCreateItem(Long chatId, Long catId, User user, Integer msgId) {
        user.setSelectedCategoryId(catId);
        MenuCategory cat = categoryRepository.findById(catId).orElse(null);
        if (cat != null) user.setSelectedRestaurantId(cat.getRestaurantId());
        user.setState(UserState.WAITING_ITEM_NAME);
        userRepository.save(user);
        if (msgId != null) bot.deleteMessage(chatId, msgId);
        bot.sendMessage(chatId, bot.t("item_name", user.getLanguage()));
    }

    public void handleItemName(Long chatId, String text, User user) {
        Map<String, Object> tmp = bot.getTmp(user);
        tmp.put("item_name", text);
        bot.saveTmp(user, tmp);
        user.setState(UserState.WAITING_ITEM_PRICE);
        userRepository.save(user);
        bot.sendMessage(chatId, bot.t("item_price", user.getLanguage()));
    }

    public void handleItemPrice(Long chatId, String text, User user) {
        long price;
        try {
            String clean = text.replaceAll("[^0-9]", "");
            if (clean.isEmpty()) throw new NumberFormatException();
            price = Long.parseLong(clean);
        } catch (Exception e) {
            bot.sendMessage(chatId, "⚠️ " + bot.t("invalid_number", user.getLanguage()));
            return;
        }

        Map<String, Object> tmp = bot.getTmp(user);
        tmp.put("item_price", price);
        bot.saveTmp(user, tmp);
        user.setState(UserState.WAITING_ITEM_DESCRIPTION);
        userRepository.save(user);
        bot.sendMessage(chatId, bot.t("item_description", user.getLanguage()));
    }

    public void handleItemDescription(Long chatId, String text, User user) {
        Map<String, Object> tmp = bot.getTmp(user);
        tmp.put("item_description", text);
        bot.saveTmp(user, tmp);
        user.setState(UserState.WAITING_ITEM_PHOTO);
        userRepository.save(user);
        bot.sendMessage(chatId, bot.t("send_photo", user.getLanguage()));
    }

    public void handleItemPhoto(Long chatId, User user, Update update) {
        Language lang = user.getLanguage();

        List<PhotoSize> photos = update.getMessage().getPhoto();
        String fileId = photos.get(photos.size() - 1).getFileId();
        String localPath = fileService.saveTelegramPhoto(fileId);

        Map<String, Object> tmp = bot.getTmp(user);
        Long restId = user.getSelectedRestaurantId();
        Long catId = user.getSelectedCategoryId();

        if (restId != null && catId != null) {
            MenuItem item = new MenuItem(restId, catId, (String) tmp.get("item_name"));
            item.setPrice((Long) tmp.get("item_price"));
            item.setDescription((String) tmp.get("item_description"));
            item.setPhotoPath(localPath);
            item.setPhotoFileId(fileId);
            long order = menuItemRepository.countByCategoryId(catId);
            item.setSortOrder((int) order + 1);
            menuItemRepository.save(item);
        }

        bot.clearTmp(user);
        user.setState(UserState.MAIN_MENU);
        userRepository.save(user);

        bot.sendMessage(chatId, "✅ " + bot.t("item_saved", lang));
        if (restId != null) showRestaurantActions(chatId, restId, lang);
    }

    public void showRestaurantActions(Long chatId, Long restId, Language lang) {
        Restaurant r = restaurantRepository.findById(restId).orElse(null);
        if (r == null) { bot.sendMessage(chatId, "❌ " + bot.t("error", lang)); return; }

        int catCount = (int) categoryRepository.countByRestaurantId(restId);
        int itemCount = (int) menuItemRepository.countByRestaurantId(restId);

        StringBuilder sb = new StringBuilder();
        sb.append("🍽 *").append(r.getName()).append("*\n\n");
        if (r.getDescription() != null && !r.getDescription().isEmpty())
            sb.append(r.getDescription()).append("\n");
        if (r.getAddress() != null && !r.getAddress().isEmpty())
            sb.append("📍 ").append(r.getAddress()).append("\n");
        if (r.getPhone() != null && !r.getPhone().isEmpty())
            sb.append("📞 ").append(r.getPhone()).append("\n");
        sb.append("\n📊 Kategoriyalar: ").append(catCount).append(" | Taomlar: ").append(itemCount);

        SendMessage sm = new SendMessage();
        sm.setChatId(chatId.toString());
        sm.setText(sb.toString());
        sm.setParseMode("Markdown");

        String webAppUrl = baseUrl + "/webapp/" + r.getOwnerId() + "/restaurant/" + r.getId();

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(Collections.singletonList(bot.webAppBtn("⚙️ Panel orqali boshqarish", webAppUrl)));
        rows.add(Arrays.asList(
                bot.btn("📂 " + bot.t("categories", lang), "categories_" + restId),
                bot.btn("➕ " + bot.t("add_category", lang), "add_category_" + restId)
        ));
        rows.add(Arrays.asList(
                bot.btn("👁 " + bot.t("view_menu", lang), "view_menu_" + restId),
                bot.btn("🔗 " + bot.t("share", lang), "share_" + restId)
        ));
        rows.add(Collections.singletonList(bot.btn("◀️ " + bot.t("back", lang), "back_restaurants")));
        markup.setKeyboard(rows);
        sm.setReplyMarkup(markup);
        bot.send(sm);
    }

    public void sendMyRestaurants(Long chatId, User user) {
        Language lang = user.getLanguage();
        List<Restaurant> list = restaurantRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId());

        if (list.isEmpty()) {
            bot.sendMessage(chatId, "📭 " + bot.t("no_restaurants", lang));
            return;
        }

        SendMessage sm = new SendMessage();
        sm.setChatId(chatId.toString());
        sm.setText("🍽 " + bot.t("your_restaurants", lang));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        String webAppUrl = baseUrl + "/webapp/" + user.getId();
        rows.add(Collections.singletonList(bot.webAppBtn("🌐 Web Panel Ochish", webAppUrl)));

        for (Restaurant r : list) {
            rows.add(Collections.singletonList(bot.btn("🍽 " + r.getName(), "restaurant_" + r.getId())));
        }
        rows.add(Collections.singletonList(bot.btn("◀️ " + bot.t("back", lang), "back_main")));
        markup.setKeyboard(rows);
        sm.setReplyMarkup(markup);
        bot.send(sm);
    }

    public void showCategories(Long chatId, Long restId, Language lang) {
        Restaurant r = restaurantRepository.findById(restId).orElse(null);
        if (r == null) { bot.sendMessage(chatId, "❌ " + bot.t("error", lang)); return; }

        List<MenuCategory> cats = categoryRepository.findByRestaurantIdOrderBySortOrderAsc(restId);

        SendMessage sm = new SendMessage();
        sm.setChatId(chatId.toString());
        sm.setText("📂 " + r.getName() + " - " + bot.t("categories", lang));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (MenuCategory cat : cats) {
            long cnt = menuItemRepository.countByCategoryId(cat.getId());
            rows.add(Collections.singletonList(
                    bot.btn(cat.getName() + " (" + cnt + ")", "category_" + cat.getId())
            ));
        }
        rows.add(Collections.singletonList(bot.btn("➕ " + bot.t("add_category", lang), "add_category_" + restId)));
        rows.add(Collections.singletonList(bot.btn("◀️ " + bot.t("back", lang), "restaurant_" + restId)));
        markup.setKeyboard(rows);
        sm.setReplyMarkup(markup);
        bot.send(sm);
    }

    public void showCategoryActions(Long chatId, Long catId, Language lang) {
        MenuCategory cat = categoryRepository.findById(catId).orElse(null);
        if (cat == null) { bot.sendMessage(chatId, "❌ " + bot.t("error", lang)); return; }

        List<MenuItem> items = menuItemRepository.findByCategoryIdOrderBySortOrderAsc(catId);

        StringBuilder sb = new StringBuilder("📂 " + cat.getName()).append("\n\n");
        if (items.isEmpty()) {
            sb.append("📭 " + bot.t("no_items", lang));
        } else {
            int i = 1;
            for (MenuItem item : items) {
                sb.append(i++).append(". ").append(item.getName());
                if (item.getPrice() != null) {
                    sb.append(" — ").append(bot.formatPrice(item.getPrice())).append(" so'm");
                }
                sb.append("\n");
            }
        }

        SendMessage sm = new SendMessage();
        sm.setChatId(chatId.toString());
        sm.setText(sb.toString());

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(Collections.singletonList(bot.btn("➕ " + bot.t("add_item", lang), "add_item_" + catId)));
        rows.add(Collections.singletonList(bot.btn("◀️ " + bot.t("back", lang), "categories_" + cat.getRestaurantId())));
        markup.setKeyboard(rows);
        sm.setReplyMarkup(markup);
        bot.send(sm);
    }

    public void showMenuItems(Long chatId, Long restId, Language lang) {
        Restaurant r = restaurantRepository.findById(restId).orElse(null);
        if (r == null) { bot.sendMessage(chatId, "❌ " + bot.t("error", lang)); return; }

        List<MenuCategory> cats = categoryRepository.findByRestaurantIdOrderBySortOrderAsc(restId);
        List<MenuItem> allItems = menuItemRepository.findByRestaurantIdOrderBySortOrderAsc(restId);

        StringBuilder sb = new StringBuilder();
        sb.append("🍽 *").append(r.getName()).append("* - Menyu\n");

        if (allItems.isEmpty() && cats.isEmpty()) {
            sb.append("\n📭 " + bot.t("no_items", lang));
        } else {
            for (MenuCategory cat : cats) {
                List<MenuItem> catItems = allItems.stream()
                        .filter(i -> i.getCategoryId() != null && i.getCategoryId().equals(cat.getId()))
                        .collect(Collectors.toList());
                if (catItems.isEmpty()) continue;

                sb.append("\n📂 *").append(cat.getName()).append("*\n");
                for (MenuItem item : catItems) {
                    sb.append("  • ").append(item.getName());
                    if (item.getPrice() != null)
                        sb.append(" — ").append(bot.formatPrice(item.getPrice())).append(" so'm");
                    sb.append("\n");
                    if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                        sb.append("    _").append(item.getDescription()).append("_\n");
                    }
                }
            }
        }

        SendMessage sm = new SendMessage();
        sm.setChatId(chatId.toString());
        String txt = sb.toString();
        sm.setText(txt.length() > 4000 ? txt.substring(0, 3997) + "..." : txt);
        sm.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(Collections.singletonList(bot.btn("◀️ " + bot.t("back", lang), "restaurant_" + restId)));
        markup.setKeyboard(rows);
        sm.setReplyMarkup(markup);
        bot.send(sm);
    }

    public void shareRestaurant(Long chatId, Long restId, Language lang) {
        Restaurant r = restaurantRepository.findById(restId).orElse(null);
        if (r == null) { bot.sendMessage(chatId, "❌ " + bot.t("error", lang)); return; }

        String link = baseUrl + "/menu/" + r.getUniqueLink();
        bot.sendMessage(chatId, "🌐 " + bot.t("share_text", lang) + ":\n\n" + link);
        showRestaurantActions(chatId, restId, lang);
    }

    public void selectRestaurantAndShow(Long chatId, Long restId, User user, Language lang, Integer msgId) {
        user.setSelectedRestaurantId(restId);
        userRepository.save(user);
        if (msgId != null) bot.deleteMessage(chatId, msgId);
        showRestaurantActions(chatId, restId, lang);
    }

    public void selectCategoryAndShow(Long chatId, Long catId, User user, Language lang, Integer msgId) {
        user.setSelectedCategoryId(catId);
        MenuCategory cat = categoryRepository.findById(catId).orElse(null);
        if (cat != null) user.setSelectedRestaurantId(cat.getRestaurantId());
        userRepository.save(user);
        if (msgId != null) bot.deleteMessage(chatId, msgId);
        showCategoryActions(chatId, catId, lang);
    }
}