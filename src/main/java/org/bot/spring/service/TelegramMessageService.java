package org.bot.spring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramMessageService {

    private final OkHttpTelegramClient telegramClient;

    /**
     * Отправляет текстовое сообщение
     */
    public void sendTextMessage(long chatId, String text) {
        try {
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .build();
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage(), e);
        }
    }

    /**
     * Отправляет документ (файл)
     */
    public void sendDocument(long chatId, File file, String caption) {
        try {
            SendVideo sendVideo = SendVideo.builder()
                    .chatId(chatId)
                    .video(new InputFile(file))
                    .caption(caption)
                    .build();
            telegramClient.execute(sendVideo);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке документа: {}", e.getMessage(), e);
        }
    }

    /**
     * Отправляет документ без подписи
     */
    public void sendDocument(long chatId, File file) {
        sendDocument(chatId, file, null);
    }
}
