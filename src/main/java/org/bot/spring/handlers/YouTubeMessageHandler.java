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
import java.util.regex.Pattern;

@Slf4j
@Component
public class YouTubeMessageHandler extends AbstractMessageHandler {

    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
            "(?:.|\\n|\\r)*(youtube\\.com|youtu\\.be)(?:.|\\n|\\r)*"
    );

    public YouTubeMessageHandler(YtDlpService ytDlpService,
                                 TelegramMessageService telegramMessageService) {
        super(ytDlpService, telegramMessageService);
    }

    @Override
    public boolean canHandle(String text) {
        if (text == null) {
            return false;
        }
        return YOUTUBE_PATTERN.matcher(text).matches();
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

        var commandTemplate = DownloadVideoCommand.builder()
                .videoId(selected.getId())
                .fileName(ytDlpService.createFilename(context))
                .videoUrl(videoUrl)
                .folderPath(ytDlpService.pathToDownload());
        
        // Если прокси наебал, то пробуем качать напрямую
        try {
            ytDlpService.downloadVideo(
                    commandTemplate
                            .proxy(ytDlpService.getCurrentProxy())
                            .build()
            );
        } catch (Exception e) {
            log.info("Прокси наебал! Пробуем качать напрямую!");
            ytDlpService.downloadVideo(
                    commandTemplate
                            .proxy(null) // да, тут нужно сетать null явно =)
                            .build()
            );
        }
       
        return commandTemplate.build().getOutputPath();
    }
}
