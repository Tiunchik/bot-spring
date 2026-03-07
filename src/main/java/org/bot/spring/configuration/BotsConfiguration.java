package org.bot.spring.configuration;

import lombok.RequiredArgsConstructor;
import org.bot.spring.configuration.properties.BotProperties;
import org.bot.spring.consumers.MessageProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@Configuration
@RequiredArgsConstructor
public class BotsConfiguration {

    private final BotProperties botProperties;
    private final MessageProcessor messageProcessor;

    @Bean
    public CommandLineRunner runner(
            TelegramBotsLongPollingApplication telegramBotsLongPollingApplication
    ) {
        return (args) -> {
            telegramBotsLongPollingApplication
                    .registerBot(
                            botProperties.getToken(),
                            messageProcessor
                    );
        };
    }


}
