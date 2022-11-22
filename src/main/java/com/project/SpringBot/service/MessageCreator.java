package com.project.SpringBot.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.File;

@Component
public final class MessageCreator {

    @Value("${messagecreator.imagefolder}")
    String pathToFolderWithImages;

    public SendMessage getMessage(long chatId, String messageText) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(messageText);

        return sendMessage;
    }

    public SendSticker getSticker(long chatId, String stickerFileId) {
        SendSticker sendSticker = new SendSticker();
        sendSticker.setChatId(chatId);
        sendSticker.setSticker(new InputFile(stickerFileId));

        return sendSticker;
    }

    public SendPhoto getImage(long chatId, String fileNameImage) {
        String path = pathToFolderWithImages + fileNameImage;
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(new InputFile(new File(path)));

        return sendPhoto;
    }
}
