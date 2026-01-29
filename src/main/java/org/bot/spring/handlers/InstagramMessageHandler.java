package org.bot.spring.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bot.spring.dto.MessageContext;
import org.bot.spring.service.TelegramMessageService;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstagramMessageHandler implements MessageHandler {

    private final TelegramMessageService telegramMessageService;

    private static final Pattern INSTAGRAM_PATTERN = Pattern.compile(
            ".*instagram\\.com.*",
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
    public void handle(String url, MessageContext context) {
        log.info("Обработка Instagram ссылки от пользователя: {}", context.getUsername());
        telegramMessageService.sendTextMessage(
                context.getChatId(),
                "Я пока не могу скачивать файлы из Instagram."
        );
    }
}
