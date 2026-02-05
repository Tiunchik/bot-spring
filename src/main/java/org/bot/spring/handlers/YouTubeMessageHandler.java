package org.bot.spring.handlers;

import lombok.extern.slf4j.Slf4j;
import org.bot.spring.configuration.properties.DownloadProperties;
import org.bot.spring.dto.MessageContext;
import org.bot.spring.dto.ProcessCommand;
import org.bot.spring.dto.VideoFormatDto;
import org.bot.spring.service.TelegramMessageService;
import org.bot.spring.service.YtDlpService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.io.IOException;
import java.nio.file.FileSystemException;
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

        //TODO: добавить команду --proxy для yt-dlp - google how - для наполнения команды нужен парсер на сайт
        //напиши парсер на java для сайта https://free-proxy-list.net/ru/socks-proxy.html
        //нужно в итоге получать json содержащий объекты с полями: code ip port version
        // у меня прокси работало только с socks5, проверить работу с socks4, мб тоже покатит
        var command = ProcessCommand.builder()
                .videoId(selected.getId())
                .fileName(ytDlpService.createFilename(context))
                .videoUrl(videoUrl)
                .folderPath(ytDlpService.pathToDownload())
                .build();
        ytDlpService.downloadVideo(command);
        return command.getOutputPath();
    }
}
