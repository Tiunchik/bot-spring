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
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class YouTubeMessageHandler implements MessageHandler {

    private final YtDlpService ytDlpService;
    private final TelegramMessageService telegramMessageService;

    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
            "(?:.|\\n|\\r)*(youtube\\.com|youtu\\.be)(?:.|\\n|\\r)*"
    );

    @Override
    public boolean canHandle(String text) {
        if (text == null) {
            return false;
        }
        return YOUTUBE_PATTERN.matcher(text).matches();
    }

    @Override
    public void handle(String text, MessageContext context) {
        try {
            // Шаг 1: Извлечь URL (если передан полный текст)
            String videoUrl = ytDlpService.extractUrl(text);
            if (videoUrl == null) {
                videoUrl = text;
            }
            String textWithoutUrl = text.replace(videoUrl, "");

            log.info("Обработка YouTube видео: {}", videoUrl);

            // Шаг 2: Получить список доступных форматов
            Message message = telegramMessageService.sendTextMessage(context.getChatId(), "Получаю список форматов...");
            List<VideoFormatDto> formats = ytDlpService.getAvailableFormats(videoUrl);

            if (formats.isEmpty()) {
                telegramMessageService.editOrsendNewTextMessage(context.getChatId(), message.getMessageId(),"Не удалось получить форматы видео.");
                return;
            }

            // Шаг 3: Выбрать подходящий формат
            VideoFormatDto selectedFormat = ytDlpService.selectBestFormat(formats);

            if (selectedFormat == null) {
                telegramMessageService.editOrsendNewTextMessage(
                        context.getChatId(),
                        message.getMessageId(),
                        "Не найден подходящий формат."
                );
                return;
            }

            // Проверить размер файла
            if (ytDlpService.isFileSizeExceeded(selectedFormat.getFileSizeInMB())) {
                telegramMessageService.editOrsendNewTextMessage(
                        context.getChatId(),
                        message.getMessageId(),
                        String.format(
                                "Размер файла (%.2f MB) превышает максимально допустимый (%.2f MB).",
                                selectedFormat.getFileSizeInMB(),
                                ytDlpService.getMaxFileSize()
                        )
                );
                return;
            }

            log.info("Выбран формат: ID={}, {}, {}, размер={:.2f} MB",
                    selectedFormat.getId(),
                    selectedFormat.getContainer(),
                    selectedFormat.getResolution(),
                    selectedFormat.getFileSizeInMB());

            // Шаг 4: Загрузить видео
            telegramMessageService.editOrsendNewTextMessage(
                    context.getChatId(),
                    message.getMessageId(),
                    String.format("Загружаю видео (формат %s, %s)...",
                            selectedFormat.getContainer(),
                            selectedFormat.getResolution())
            );

            String filePath = ytDlpService.downloadYoutubeVideo(
                    videoUrl,
                    selectedFormat.getId(),
                    context.getChatId(),
                    context.getMessageId(),
                    context.getUsername()
            );

            // Шаг 5: Отправить результат
            File videoFile = new File(filePath);
            String caption;
            if (textWithoutUrl.isBlank()) {
                caption = "Video from @" + context.getUsername();
            } else {
                caption = "Video from @" + context.getUsername() + ": " + textWithoutUrl;
            }
            telegramMessageService.sendVideo(context.getChatId(), videoFile, caption);
            telegramMessageService.deleteMessage(context.getChatId(), message.getMessageId());
            telegramMessageService.deleteMessage(context.getChatId(), context.getMessageId());

            // Удалить временный файл
            ytDlpService.deleteFile(filePath);

            log.info("Видео успешно отправлено и удалено: {}", filePath);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Процесс был прерван", e);
            telegramMessageService.sendTextMessage(context.getChatId(), "Процесс загрузки был прерван.");
        } catch (Exception e) {
            log.error("Ошибка при обработке YouTube видео", e);
            telegramMessageService.sendTextMessage(
                    context.getChatId(),
                    "Произошла ошибка при загрузке видео: " + e.getMessage()
            );
        }
    }
}
