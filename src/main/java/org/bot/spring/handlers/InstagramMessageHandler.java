package org.bot.spring.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bot.spring.dto.MessageContext;
import org.bot.spring.dto.VideoFormatDto;
import org.bot.spring.service.TelegramMessageService;
import org.bot.spring.service.YtDlpService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Objects.nonNull;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstagramMessageHandler implements MessageHandler {

    private final YtDlpService ytDlpService;
    private final TelegramMessageService telegramMessageService;

    private static final Pattern INSTAGRAM_PATTERN = Pattern.compile(
            "(?:.|\\n|\\r)*(instagram\\.com)(?:.|\\n|\\r)*",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean canHandle(String text) {
        if (text == null) {
            return false;
        }
        return INSTAGRAM_PATTERN.matcher(text).matches();
    }

    @Override
    public void handle(String text, MessageContext context) {
        try {
            // Шаг 1: Извлечь URL (если передан полный текст)
            String videoUrl = ytDlpService.extractUrl(text);
            if (videoUrl == null) {
                videoUrl = text;
            }
            String textWithOutUrl = text.replace(videoUrl, "");
            log.info("Обработка Instagram видео: {}", videoUrl);

            Message message = telegramMessageService.sendTextMessage(context.getChatId(), "Пытаюсь начать скачивание видео...");

            String filePath = ytDlpService.downloadInstagramVideo(
                    videoUrl,
                     context.getChatId(),
                    context.getMessageId(),
                    context.getUsername()
            );

            VideoFormatDto maxSize = ytDlpService.getMaxVideoSize(videoUrl);
            // Проверить размер файла
            if (ytDlpService.isFileSizeExceeded(maxSize.getFileSizeInMB().divide(BigDecimal.TWO, RoundingMode.DOWN))) {
                telegramMessageService.editOrsendNewTextMessage(
                        context.getChatId(),
                        message.getMessageId(),
                        String.format(
                                "Размер файла (%.2f MB) превышает максимально допустимый (%.2f MB).",
                                maxSize.getFileSizeInMB(),
                                ytDlpService.getMaxFileSize()
                        )
                );
                return;
            }

            // Отправить результат
            File videoFile = new File(filePath);
            String messageText = "@" + context.getUsername() + ": " + textWithOutUrl;
            Object videoSendResult = telegramMessageService.sendVideo(context.getChatId(), videoFile, videoUrl);
            if (nonNull(videoSendResult)) {
                telegramMessageService.editTextMessage(context.getChatId(), message.getMessageId(), messageText);
                telegramMessageService.deleteMessage(context.getChatId(), context.getMessageId());
            } else {
                telegramMessageService.editOrsendNewTextMessage(context.getChatId(), context.getMessageId(), "Процесс загрузки был прерван.");
            }
            // Удалить временный файл
            ytDlpService.deleteFile(filePath);

            log.info("Видео успешно отправлено и удалено: {}", filePath);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Процесс был прерван", e);
            telegramMessageService.editOrsendNewTextMessage(context.getChatId(), context.getMessageId(), "Процесс загрузки был прерван.");
        } catch (Exception e) {
            log.error("Ошибка при обработке YouTube видео", e);
            telegramMessageService.editOrsendNewTextMessage(
                    context.getChatId(),
                    context.getMessageId(),
                    "Произошла ошибка при загрузке видео: " + e.getMessage()
            );
        }
    }
}
