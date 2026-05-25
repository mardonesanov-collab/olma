package com.example.menubot.service;

import com.example.menubot.domain.entity.AppUser;
import com.example.menubot.domain.enums.UserRole;
import com.example.menubot.security.TelegramInitDataValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;

import java.util.List;

@Service
public class SaasBotService {

    private final SaasCallbackHandler saasCallbackHandler;
    private final AuthService authService;
    private final TelegramBotGateway botGateway;
    private final String baseUrl;
    private final long superAdminTgId;

    public SaasBotService(SaasCallbackHandler saasCallbackHandler,
                          AuthService authService,
                          TelegramBotGateway botGateway,
                          @Value("${app.base-url}") String baseUrl,
                          @Value("${bot.admin.id}") long superAdminTgId) {
        this.saasCallbackHandler = saasCallbackHandler;
        this.authService = authService;
        this.botGateway = botGateway;
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.superAdminTgId = superAdminTgId;
    }

    public void handleUpdate(Update update) {
        if (update.hasCallbackQuery()) {
            if (saasCallbackHandler.handle(update, botGateway::send)) {
                return;
            }
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText().trim();
            long chatId = update.getMessage().getChatId();
            var from = update.getMessage().getFrom();

            if (text.equals("/start") || text.equals("/admin")) {
                var payload = new TelegramInitDataValidator.TelegramUserPayload(
                        from.getId(),
                        from.getUserName(),
                        from.getFirstName() != null ? from.getFirstName() : "",
                        from.getLastName() != null ? from.getLastName() : ""
                );
                AppUser user = authService.resolveUser(payload);
                String webAppUrl = baseUrl + "/webapp/" + user.getTgId();

                boolean isAdmin = user.getRole() == UserRole.SUPER_ADMIN || from.getId() == superAdminTgId;
                String welcome = isAdmin
                        ? "👋 Admin, xush kelibsiz!\n\n📱 Web App — barcha restoranlar va menyular."
                        : "👋 MenuBot SaaS\n\n📱 Web App orqali menyuni boshqaring yoki buyurtma bering.";

                botGateway.send(SendMessage.builder()
                        .chatId(chatId)
                        .text(welcome)
                        .replyMarkup(InlineKeyboardMarkup.builder()
                                .keyboard(List.of(List.of(
                                        InlineKeyboardButton.builder()
                                                .text(isAdmin ? "🛠 Admin panel" : "📱 Web App")
                                                .webApp(new WebAppInfo(webAppUrl))
                                                .build()
                                )))
                                .build())
                        .build());
            }
        }
    }
}
