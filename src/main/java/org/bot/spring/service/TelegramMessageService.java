package org.bot.spring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.math.BigDecimal;

import static java.util.Objects.nonNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramMessageService {

    private final OkHttpTelegramClient telegramClient;

    /**
     * Отправляет текстовое сообщение
     */
    public Message sendTextMessage(long chatId, String text) {
        try {
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .build();
            return telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Пытается отредактировать сообщение, либо отправляет текстовое сообщение
     */
    public Message editOrsendNewTextMessage(long chatId, Integer messageId, String text) {
        if (nonNull(messageId) && messageId != 0) {
            Object flag = editTextMessage(chatId, messageId, text);
            if (flag == null) return sendTextMessage(chatId, text);
            return null;
        } else {
            return sendTextMessage(chatId, text);
        }
    }

    /**
     * Отправляет документ (файл)
     */
    public Message sendVideo(long chatId, File file, String caption) {
        try {
            SendVideo sendVideo = SendVideo.builder()
                    .chatId(chatId)
                    .video(new InputFile(file))
                    .caption(caption)
                    .build();
            return telegramClient.execute(sendVideo);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке документа: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Отправляет документ без подписи
     */
    public Message sendVideo(long chatId, File file) {
        return sendVideo(chatId, file, null);
    }

    /**
     * Редактирует ранее отправленное сообщение по его Id
     */
    public Object editTextMessage(long chatId, int messageId, String text) {
        try {
            EditMessageText editMessageText = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(text)
                    .build();
            telegramClient.execute(editMessageText);
            return BigDecimal.ONE;
        } catch (TelegramApiException e) {
            log.error("Ошибка при редактировании сообщения: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Удаляет ранее отправленное сообщение по его Id - добавление видео
     */
    public Object deleteMessage(Long chatId, Integer messageId) {
        try {
            DeleteMessage deleteMessage = new DeleteMessage(chatId.toString(), messageId);
            telegramClient.execute(deleteMessage);
            return BigDecimal.ONE;
        } catch (TelegramApiException e) {
            log.error("Ошибка при удалении сообщения: {}", e.getMessage(), e);
            return null;
        }
    }
}
