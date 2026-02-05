package org.bot.spring.handlers;

import lombok.extern.slf4j.Slf4j;
import org.bot.spring.dto.MessageContext;
import org.bot.spring.dto.ProcessCommand;
import org.bot.spring.dto.VideoFormatDto;
import org.bot.spring.exceptions.FileSizeExceededException;
import org.bot.spring.service.YtDlpService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileSystemException;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class InstagramMessageHandler extends AbstractMessageHandler {

    private static final Pattern INSTAGRAM_PATTERN = Pattern.compile(
            "(?:.|\\n|\\r)*(instagram\\.com)(?:.|\\n|\\r)*",
            Pattern.CASE_INSENSITIVE
    );

    public InstagramMessageHandler(YtDlpService ytDlpService, org.bot.spring.service.TelegramMessageService telegramMessageService) {
        super(ytDlpService, telegramMessageService);
    }

    @Override
    public boolean canHandle(String text) {
        if (text == null) {
            return false;
        }
        return INSTAGRAM_PATTERN.matcher(text).matches();
    }

    @Override
    protected String downloadVideo(String videoUrl, MessageContext context, Message message) throws IOException, InterruptedException {
        VideoFormatDto maxSize = ytDlpService.getMaxVideoSize(videoUrl);
        // Проверить размер файла
        checkFileSizeAndNotify(maxSize.getFileSizeInMB().divide(BigDecimal.TWO, RoundingMode.DOWN));

        var command = ProcessCommand.builder()
                .fileName(ytDlpService.createFilename(context))
                .videoUrl(videoUrl)
                .folderPath(ytDlpService.pathToDownload())
                .build();
        ytDlpService.downloadVideo(command);
        return command.getOutputPath();
    }

}
