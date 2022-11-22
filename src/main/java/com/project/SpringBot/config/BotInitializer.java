package com.project.SpringBot.config;


import com.project.SpringBot.service.MessageCreator;
import com.project.SpringBot.service.TelegramBot;
import com.project.SpringBot.service.GeographicalWeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Component
public class BotInitializer {

    @Autowired
    TelegramBot bot;

    @Autowired
    MessageCreator creator;

    @Autowired
    GeographicalWeatherService geographicalWeatherService;

    @EventListener({ContextRefreshedEvent.class})
    public void initialization() {

        TelegramBotsApi telegramBotsApi;

        try {
            telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(bot);
            bot.setServices(creator, geographicalWeatherService);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }
}
