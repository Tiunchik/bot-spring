package org.bot.spring.handlers;

import lombok.extern.slf4j.Slf4j;
import org.bot.spring.dto.MessageContext;
import org.bot.spring.dto.DownloadVideoCommand;
import org.bot.spring.dto.VideoFormatDto;
import org.bot.spring.service.YtDlpService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
public class InstagramMessageHandler extends AbstractMessageHandler {

    private static final String[] INSTAGRAM_PREFIXES = {
            "https://instagram.com",
            "https://www.instagram.com",
            "http://instagram.com",
            "http://www.instagram.com"
    };

    public InstagramMessageHandler(YtDlpService ytDlpService, org.bot.spring.service.TelegramMessageService telegramMessageService) {
        super(ytDlpService, telegramMessageService);
    }

    @Override
    public boolean canHandle(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        // Happy path: сообщение начинается со ссылки
        for (String prefix : INSTAGRAM_PREFIXES) {
            if (text.startsWith(prefix)) {
                return true;
            }
        }
        // Fallback: ссылка где-то внутри текста
        return text.toLowerCase().contains("instagram.com");
    }

    @Override
    protected String downloadVideo(String videoUrl, MessageContext context, Message message) throws IOException, InterruptedException {
        VideoFormatDto maxSize = ytDlpService.getMaxVideoSize(videoUrl);
        // Проверить размер файла
        checkFileSizeAndNotify(maxSize.getFileSizeInMB().divide(BigDecimal.TWO, RoundingMode.DOWN));

        var command = DownloadVideoCommand.builder()
                .fileName(ytDlpService.createFilename(context))
                .videoUrl(videoUrl)
                .folderPath(ytDlpService.pathToDownload())
                .proxy(ytDlpService.getCurrentProxy())
                .build();
        ytDlpService.downloadVideo(command);
        return command.getOutputPath();
    }

}
