package com.example.telegram_bot.bot;

import com.example.telegram_bot.update.UpdateConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;

@Component
public class MyTelegramBot implements SpringLongPollingBot {

    private final UpdateConsumer update;

    @Value("${telegram.bot.token}")
    private String botToken;

    public MyTelegramBot(UpdateConsumer update) {
        this.update = update;
    }


    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return update;
    }
}
