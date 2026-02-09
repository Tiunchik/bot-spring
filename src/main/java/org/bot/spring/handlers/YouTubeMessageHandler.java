package org.bot.spring.handlers;

import lombok.extern.slf4j.Slf4j;
import org.bot.spring.dto.MessageContext;
import org.bot.spring.dto.DownloadVideoCommand;
import org.bot.spring.dto.VideoFormatDto;
import org.bot.spring.service.TelegramMessageService;
import org.bot.spring.service.YtDlpService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class YouTubeMessageHandler extends AbstractMessageHandler {

    private static final String[] YOUTUBE_PREFIXES = {
            "https://youtube.com",
            "https://www.youtube.com",
            "https://youtu.be",
            "http://youtube.com",
            "http://www.youtube.com",
            "http://youtu.be"
    };

    public YouTubeMessageHandler(YtDlpService ytDlpService,
                                 TelegramMessageService telegramMessageService) {
        super(ytDlpService, telegramMessageService);
    }

    @Override
    public boolean canHandle(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        // Happy path: сообщение начинается со ссылки
        for (String prefix : YOUTUBE_PREFIXES) {
            if (text.startsWith(prefix)) {
                return true;
            }
        }
        // Fallback: ссылка где-то внутри текста
        return text.contains("youtube.com") || text.contains("youtu.be");
    }

    @Override
    protected String downloadVideo(String videoUrl, MessageContext context, Message message) throws IOException, InterruptedException {
        // Шаг 2: Получить список доступных форматов
        telegramMessageService.editOrsendNewTextMessage(context.getChatId(), message.getMessageId(), "Получаю список форматов...");
        List<VideoFormatDto> formats = ytDlpService.getAvailableFormats(videoUrl);

        if (formats.isEmpty()) {
            telegramMessageService.editOrsendNewTextMessage(context.getChatId(), message.getMessageId(), "Не удалось получить форматы видео.");
            return null;
        }

        // Шаг 3: Выбрать подходящий формат
        VideoFormatDto selected = ytDlpService.selectBestFormat(formats);

        if (selected == null) {
            telegramMessageService.editOrsendNewTextMessage(
                    context.getChatId(),
                    message.getMessageId(),
                    "Не найден подходящий формат."
            );
            return null;
        }

        // Проверить размер файла
        checkFileSizeAndNotify(selected.getFileSizeInMB());

        log.info("Выбран формат: {}", selected);

        // Шаг 4: Загрузить видео
        telegramMessageService.editOrsendNewTextMessage(
                context.getChatId(),
                message.getMessageId(),
                String.format("Загружаю видео (формат %s, %s)...",
                        selected.getContainer(),
                        selected.getResolution())
        );

        var command = DownloadVideoCommand.builder()
                .videoId(selected.getId())
                .fileName(ytDlpService.createFilename(context))
                .videoUrl(videoUrl)
                .folderPath(ytDlpService.pathToDownload())
                .build();
        ytDlpService.downloadVideo(command);
        return command.getOutputPath();
    }
}
