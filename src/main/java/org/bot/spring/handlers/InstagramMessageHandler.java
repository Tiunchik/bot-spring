package org.bot.spring.handlers;

import lombok.extern.slf4j.Slf4j;
import org.bot.spring.dto.MessageContext;
import org.bot.spring.dto.DownloadVideoCommand;
import org.bot.spring.dto.VideoFormatDto;
import org.bot.spring.service.TelegramMessageService;
import org.bot.spring.service.YtDlpService;
import org.bot.spring.service.proxy.ProxyProvider;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

//TODO: доделать логику по прокси, пока нас спасёт только сингапур :)
// + логика чеканья списка прокси через yt-dlp -F запросы к метаданным
@Slf4j
@Component
public class InstagramMessageHandler extends AbstractMessageHandler {

    private static final String[] INSTAGRAM_PREFIXES = {
            "https://instagram.com",
            "https://www.instagram.com",
            "http://instagram.com",
            "http://www.instagram.com"
    };
    private final ProxyProvider proxyProvider;

    public InstagramMessageHandler(YtDlpService ytDlpService,
                                   TelegramMessageService telegramMessageService,
                                   ProxyProvider proxyProvider) {
        super(ytDlpService, telegramMessageService);
        this.proxyProvider = proxyProvider;
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
        VideoFormatDto maxSize = ytDlpService.getMaxVideoSizeForInstagram(videoUrl);
        // Проверить размер файла
        checkFileSizeAndNotify(maxSize.getFileSizeInMB().divide(BigDecimal.TWO, RoundingMode.DOWN));
        DownloadVideoCommand commandTemplate = null;
        String proxy = proxyProvider.getCurrentProxy();
        for (int i = 0; i < 3; i++) {
            try {
                commandTemplate = DownloadVideoCommand.builder()
                        .fileName(ytDlpService.createFilename(context))
                        .videoUrl(videoUrl)
                        .folderPath(ytDlpService.pathToDownload())
                        .proxy(proxy)
                        .build();
                ytDlpService.downloadVideo(commandTemplate);
            } catch (Exception e) {
                i++;
                if (i == 3) throw new RuntimeException("Не удалось скачать видео через прокси");
                proxy = proxyProvider.getNextProxy().toYtDlpFormat();
                log.info("Повторная попытка скачать через прокси - {}", proxy);
            }
        }
        if (commandTemplate == null) throw new RuntimeException("Не удалось скачать видео через прокси");
        return commandTemplate.getOutputPath();
    }

}
