package com.example.menubot.service;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.function.Consumer;

@Component
public class TelegramBotGateway {

    private volatile Consumer<SendMessage> sender;

    public void register(Consumer<SendMessage> sender) {
        this.sender = sender;
    }

    public void send(SendMessage message) {
        if (sender != null) {
            sender.accept(message);
        }
    }
}
