package org.bot.spring.configuration;

import lombok.RequiredArgsConstructor;
import org.bot.spring.configuration.properties.BotProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;

@Configuration
@RequiredArgsConstructor
public class TelegramClientConfiguration {

    private final BotProperties botProperties;

    @Bean
    public OkHttpTelegramClient telegramClient() {
        return new OkHttpTelegramClient(botProperties.getToken());
    }

}
