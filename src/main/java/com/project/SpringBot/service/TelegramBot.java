package com.project.SpringBot.service;


import com.project.SpringBot.config.BotConfig;
import com.project.SpringBot.exceptions.InvalidTypeOfMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static boolean infinityMessagesFlag;
    private static boolean waitCityFlag;

    private static long infinityMessagesChatId;
    private static long waitCityChatId;

    private MessageCreator creator;
    private GeographicalWeatherService geoWeatherService;
    private final BotConfig config;

    public TelegramBot(BotConfig config) {
        this.config = config;
    }

    public void setServices(MessageCreator creator, GeographicalWeatherService geographicalWeatherService) {
        this.creator = creator;
        this.geoWeatherService = geographicalWeatherService;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) gotTextMessage(update);

        else if (update.hasMessage() && update.getMessage().hasSticker()) gotStickerMessage(update);
    }

    /**
        ----------------------------------------
        Блок подразделения сообщения на его виды
    */
    private void gotTextMessage(Update update) {
        Message message = update.getMessage();
        Chat chat = message.getChat();
        Optional<String> username = Optional.ofNullable(chat.getUserName());
        String firstName = chat.getFirstName();
        long chatId = message.getChatId();

        if (waitCityFlag && chatId == waitCityChatId) {
            waitCityFlag = false;
            waitCityChatId = -1;
            log.info("User with username: " + username.orElse("null")
                    + ", firstname: " + firstName
                    + ", chatId: " + chatId + ", entered " + message.getText().toLowerCase() + " as city");
            try {
                sendToUser("text", chatId, geoWeatherService.getCoordinates(message.getText()));
            } catch (HttpClientErrorException e) {
                sendToUser("text", chatId, "Города, название которого вы ввели, не существует");
                log.error("error occurred: " + e.getMessage());
            }
            return;
        }

        if (infinityMessagesFlag && chatId == infinityMessagesChatId) {
            infinityMessagesFlag = false;
            sendToUser("text", chatId, "Спам завершен");
            return;
        }

        switch (message.getText()) {
            case "/hello" -> {
                log.info("User with username: " + username.orElse("null")
                        + ", firstname: " + firstName
                        + ", chatId: " + chatId + ", used /hello");
                helloCommandReceived(chatId, username, firstName);
            }
            case "/infinitymessages" -> {
                log.info("User with username: " + username.orElse("null")
                        + ", firstname: " + firstName
                        + ", chatId: " + chatId + ", used /infinitylove");
                infinityMessagesFlag = true;
                infinityMessagesChatId = chatId;
                infinitymessages(infinityMessagesChatId, "{Ваш вариант сообщения}");
            }
            case "/image" -> {
                log.info("User with username: " + username.orElse("null")
                        + ", firstname: " + firstName
                        + ", chatId: " + chatId + ", used /image");
                imageCommandReceived(chatId);
            }
            case "/coords" -> {
                log.info("User with username: " + username.orElse("null")
                        + ", firstname: " + firstName
                        + ", chatId: " + chatId + ", used /coords");
                sendToUser("text", chatId, "Введите название города на английском");
                waitCityFlag = true;
                waitCityChatId = chatId;
            }
            case "/weather" -> {
                log.info("User with username: " + username.orElse("null")
                        + ", firstname: " + firstName
                        + ", chatId: " + chatId + ", used /weather");
                sendToUser("text", chatId, geoWeatherService.getWeather("moscow"));
            }
            default -> defaultTextMessageReceived(chatId, message, chat);
        }
    }

    private void gotStickerMessage(Update update) {
        Message message = update.getMessage();
        Chat chat = message.getChat();
        String firstName = chat.getFirstName();
        long chatId = message.getChatId();
        String stickerFileId = message.getSticker().getFileId();

        sendToUser("sticker", chatId, stickerFileId);

        sendToUser("text", chatId, "Я тоже так могу, " + firstName + "!");
    }
    /**
        Блок подразделения сообщения на его виды
        ----------------------------------------
    */

    /** Проверка на аккаунт кого-либо */
    private boolean isSomeone(Chat chat, long chatId, String username) {
        return Optional.ofNullable(chat.getUserName()).orElse("").equals(username)
               && chat.getId() == chatId;
    }

    /** Команда /image */
    private void imageCommandReceived(long chatId) {

        sendToUser("text", chatId, "По вашему запросу удалось найти 1 фото");
        sendToUser("image", chatId, "{image.jpg}");
    }

    /**
     *  ------------------------------
     *  Блок команды /infinitymessages
    */
    private void infinitymessages(long chatId, String message) {

        sendToUser("text", chatId, "Спам активирован");

        new infinitymessagesThread(chatId, message).start();
    }

    final private class infinitymessagesThread extends Thread {

        private final long chatId;
        private final String message;

        public infinitymessagesThread(long chatId, String message) {
            this.chatId = chatId;
            this.message = message;
        }

        @Override
        public void run() {
            while (infinityMessagesFlag) {
                try {
                    sendToUser("text", chatId, message);
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
            }
            interrupt();
            log.info("/infinitymessages выключен" );
        }
    }
    /**
     *  Блок команды /inifinitymessages
     *  -------------------------------
    */

    /** Команда /hello */
    private void helloCommandReceived(long chatId, Optional<String> username, String firstName) {

        String answer = "Привет, " + firstName;

        sendToUser("text", chatId, answer);
    }

    /** Ответ на необработанные случаи */
    private void defaultTextMessageReceived(long chatId, Message message, Chat chat) {

        log.info("User with username: " + chat.getUserName()
                + ", chatId: " + chatId + ", wrote: " + message.getText());

        sendToUser("text", chatId, "Я тебя не понял..( Посмотри пожалуйста список команд!");
    }

    private void sendToUser(String type, long chatId, String value) {
        try {
            switch (type) {
                case "text" -> {
                    execute(creator.getMessage(chatId, value));
                    log.info("Sent message: " + value + " to chat: " + chatId);
                }
                case "sticker" -> {
                    execute(creator.getSticker(chatId, value));
                    log.info("Sent sticker: " + value + " to chat: " + chatId);
                }
                case "image" -> {
                    execute(creator.getImage(chatId, value));
                    log.info("Sent image from path: " + value + " to chat: " + chatId);
                }
                default -> {
                    log.error("Error occurred: Invalid type of message");
                    throw new InvalidTypeOfMessage();
                }
            }
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

}