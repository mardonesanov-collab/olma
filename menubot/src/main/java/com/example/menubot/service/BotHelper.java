package com.example.menubot.service;

import com.example.menubot.bot.MenuBot;
import com.example.menubot.model.Language;
import com.example.menubot.model.User;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;

import java.util.*;

@Component
public class BotHelper {

    private MenuBot bot;
    private final MessageService msg;
    private final Map<Long, Map<String, Object>> tempStore = new HashMap<>();

    public BotHelper(MessageService msg) {
        this.msg = msg;
    }

    public void setBot(MenuBot bot) { this.bot = bot; }

    public String t(String key, Language lang) { return msg.get(key, lang); }
    public String t(String key, Language lang, Object... args) { return msg.get(key, lang, args); }

    public void send(SendMessage sm) {
        try {
            if (bot != null) bot.execute(sm);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage sm = new SendMessage();
        sm.setChatId(chatId.toString());
        sm.setText(text);
        send(sm);
    }

    public void deleteMessage(Long chatId, Integer msgId) {
        try {
            DeleteMessage dm = new DeleteMessage();
            dm.setChatId(chatId.toString());
            dm.setMessageId(msgId);
            if (bot != null) bot.execute(dm);
        } catch (Exception ignored) {}
    }

    public InlineKeyboardButton btn(String text, String callbackData) {
        InlineKeyboardButton b = new InlineKeyboardButton();
        b.setText(text);
        b.setCallbackData(callbackData);
        return b;
    }

    public InlineKeyboardButton webAppBtn(String text, String url) {
        InlineKeyboardButton b = new InlineKeyboardButton();
        b.setText(text);
        WebAppInfo info = new WebAppInfo();
        info.setUrl(url);
        b.setWebApp(info);
        return b;
    }

    public String formatPrice(Long price) {
        if (price == null) return "0";
        String s = price.toString();
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            if (count > 0 && count % 3 == 0) result.insert(0, " ");
            result.insert(0, s.charAt(i));
            count++;
        }
        return result.toString();
    }

    public Map<String, Object> getTmp(User user) {
        return tempStore.computeIfAbsent(user.getId(), k -> new HashMap<>());
    }

    public void saveTmp(User user, Map<String, Object> data) {
        tempStore.put(user.getId(), data);
    }

    public void clearTmp(User user) {
        tempStore.remove(user.getId());
    }

    public void sendLanguageSelection(Long chatId) {
        SendMessage sm = new SendMessage();
        sm.setChatId(chatId.toString());
        sm.setText("🌐 Tilni tanlang / Выберите язык / Select language:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(Arrays.asList(
                btn("🇺🇿 O'zbekcha", "lang_uz"),
                btn("🇷🇺 Русский", "lang_ru"),
                btn("🇬🇧 English", "lang_en")
        ));
        markup.setKeyboard(rows);
        sm.setReplyMarkup(markup);
        send(sm);
    }

    public void sendMainMenu(Long chatId, Language lang) {
        SendMessage sm = new SendMessage();
        sm.setChatId(chatId.toString());
        sm.setText(t("main_menu", lang));

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(t("add_restaurant", lang));
        row1.add(t("my_restaurants", lang));
        rows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(t("help", lang));
        rows.add(row2);

        markup.setKeyboard(rows);
        sm.setReplyMarkup(markup);
        send(sm);
    }

    public void sendAdminMenu(Long chatId, Language lang) {
        SendMessage sm = new SendMessage();
        sm.setChatId(chatId.toString());
        sm.setText(t("admin_panel", lang));

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(t("pending_requests", lang));
        row1.add(t("users_list", lang));
        rows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(t("all_restaurants", lang));
        row2.add(t("my_restaurants", lang));
        rows.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add(t("add_restaurant", lang));
        rows.add(row3);

        markup.setKeyboard(rows);
        sm.setReplyMarkup(markup);
        send(sm);
    }
}