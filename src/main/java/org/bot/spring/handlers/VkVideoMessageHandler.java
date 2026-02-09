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
public class VkVideoMessageHandler extends YouTubeMessageHandler {

    private static final String[] VK_VIDEO_PREFIXES = {
            "https://vk.com",
            "https://www.vk.com",
            "http://vk.com",
            "http://www.vk.com",
    };

    public VkVideoMessageHandler(YtDlpService ytDlpService,
                                 TelegramMessageService telegramMessageService) {
        super(ytDlpService, telegramMessageService);
    }

    @Override
    public boolean canHandle(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        // Happy path: сообщение начинается со ссылки
        for (String prefix : VK_VIDEO_PREFIXES) {
            if (text.startsWith(prefix)) {
                return true;
            }
        }
        // Fallback: ссылка где-то внутри текста
        return text.contains("vk.com");
    }
}
