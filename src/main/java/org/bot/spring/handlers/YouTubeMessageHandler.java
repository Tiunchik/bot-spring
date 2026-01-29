package org.bot.spring.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bot.spring.dto.MessageContext;
import org.bot.spring.dto.VideoFormatDto;
import org.bot.spring.service.TelegramMessageService;
import org.bot.spring.service.YtDlpService;
import org.springframework.stereotype.Component;

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
            ".*(youtube\\.com|youtu\\.be).*",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean canHandle(String text) {
        if (text == null) {
            return false;
        }
        return YOUTUBE_PATTERN.matcher(text).matches();
    }

    @Override
    public void handle(String url, MessageContext context) {
        try {
            // Шаг 1: Извлечь URL (если передан полный текст)
            String videoUrl = ytDlpService.extractUrl(url);
            if (videoUrl == null) {
                videoUrl = url;
            }

            log.info("Обработка YouTube видео: {}", videoUrl);

            // Шаг 2: Получить список доступных форматов
            telegramMessageService.sendTextMessage(context.getChatId(), "Получаю список форматов...");
            List<VideoFormatDto> formats = ytDlpService.getAvailableFormats(videoUrl);

            if (formats.isEmpty()) {
                telegramMessageService.sendTextMessage(context.getChatId(), "Не удалось получить форматы видео.");
                return;
            }

            // Шаг 3: Выбрать подходящий формат
            VideoFormatDto selectedFormat = ytDlpService.selectBestFormat(formats);

            if (selectedFormat == null) {
                telegramMessageService.sendTextMessage(
                        context.getChatId(),
                        "Не найден подходящий формат (mp4, 720p)."
                );
                return;
            }

            // Проверить размер файла
            if (ytDlpService.isFileSizeExceeded(selectedFormat.getFileSizeInMB())) {
                telegramMessageService.sendTextMessage(
                        context.getChatId(),
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
            telegramMessageService.sendTextMessage(
                    context.getChatId(),
                    String.format("Загружаю видео (формат %s, %s)...",
                            selectedFormat.getContainer(),
                            selectedFormat.getResolution())
            );

            Thread.sleep(500);
            String filePath = ytDlpService.downloadVideo(
                    videoUrl,
                    selectedFormat.getId(),
                    context.getChatId(),
                    context.getMessageId(),
                    context.getUsername()
            );

            // Шаг 5: Отправить результат
            File videoFile = new File(filePath);
            telegramMessageService.sendDocument(context.getChatId(), videoFile);

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
