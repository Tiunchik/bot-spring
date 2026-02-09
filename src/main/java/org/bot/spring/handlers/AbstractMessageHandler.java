package org.bot.spring.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bot.spring.dto.MessageContext;
import org.bot.spring.exceptions.FileSizeExceededException;
import org.bot.spring.exceptions.YtDlpExitException;
import org.bot.spring.service.TelegramMessageService;
import org.bot.spring.service.YtDlpService;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

import static java.util.Objects.nonNull;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractMessageHandler implements MessageHandler {

    protected final YtDlpService ytDlpService;
    protected final TelegramMessageService telegramMessageService;

    @Override
    public void handle(String text, MessageContext context) {
        Message message = null;
        try {
            // Шаг 1: Извлечь URL (если передан полный текст)
            String videoUrl = ytDlpService.extractUrl(text);
            if (videoUrl == null) {
                videoUrl = text;
            }
            String textWithoutUrl = text.replace(videoUrl, "");
            log.info("Обработка видео: {}", videoUrl);

            // Шаг 2: Отправить начальное сообщение
            message = telegramMessageService.sendTextMessage(context.getChatId(), "Начата обработка сообщения");

            // Шаг 3: Выполнить специфичную логику загрузки
            String filePath = downloadVideo(videoUrl, context, message);

            if (filePath == null) {
                telegramMessageService.editTextMessage(context.getChatId(), message.getMessageId(), "Не удалось загрузить видео");
                return;
            }

            // Шаг 4: Отправить результат
            File videoFile = new File(filePath);
            String messageText = "@" + context.getUsername() + ": " + textWithoutUrl;
            Object videoSendResult = telegramMessageService.sendVideo(context.getChatId(), videoFile, videoUrl);
            if (nonNull(videoSendResult)) {
                telegramMessageService.editTextMessage(context.getChatId(), message.getMessageId(), messageText);
                telegramMessageService.deleteMessage(context.getChatId(), context.getMessageId());
            } else {
                telegramMessageService.editOrsendNewTextMessage(context.getChatId(), message.getMessageId(), "Процесс загрузки был прерван");
            }

            // Шаг 5: Удалить временный файл
            ytDlpService.deleteFile(filePath);
            log.info("Видео успешно отправлено и удалено: {}", filePath);

        } catch (YtDlpExitException fse) {
            log.error("Ошибка загрузки видео: \n{}", fse.getMessage());
            telegramMessageService.editOrsendNewTextMessage(context.getChatId(), message.getMessageId(),
                    "Не удалось скачать файл по техническим причинам =(\n" +
                            "Пишите админу");
        } catch (FileSizeExceededException fse) {
            log.error("Ошибка загрузки видео: {}", fse.getMessage());
            telegramMessageService.editOrsendNewTextMessage(context.getChatId(), message.getMessageId(), fse.getMessage());
        } catch (InterruptedException | IOException e) {
            log.error("Ошибка загрузки видео: {}", e.getMessage());
            telegramMessageService.editOrsendNewTextMessage(context.getChatId(), message.getMessageId(), "Процесс был прерван " + e.getMessage());
        }
    }

    /**
     * Абстрактный метод для специфичной логики загрузки видео
     *
     * @param videoUrl URL видео
     * @param context контекст сообщения
     * @param message сообщение для редактирования
     * @return путь к загруженному файлу или null, если загрузка не удалась
     */
    protected abstract String downloadVideo(String videoUrl, MessageContext context, Message message) throws IOException, InterruptedException;

    /**
     * Проверить размер файла и отправить сообщение, если превышен
     *
     * @param fileSizeInMB размер файла в MB
     */
    protected void checkFileSizeAndNotify(BigDecimal fileSizeInMB) {
        if (ytDlpService.isFileSizeExceeded(fileSizeInMB)) {
            throw new FileSizeExceededException(fileSizeInMB, ytDlpService.getMaxFileSize());
        }
    }
}
