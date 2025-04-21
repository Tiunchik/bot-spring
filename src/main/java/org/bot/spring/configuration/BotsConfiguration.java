package org.bot.spring.configuration;

import org.bot.spring.consumers.EchoConsumer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@Configuration
public class BotsConfiguration {

    @Bean
    public CommandLineRunner runner(
            TelegramBotsLongPollingApplication telegramBotsLongPollingApplication
    ) {
        return (args) -> {
            telegramBotsLongPollingApplication
                    .registerBot(
                            "6445089720:AAFtFad-vg8G62gsCpIGNKPmjeDTUbQfeK4",
                            new EchoConsumer(
                                    new OkHttpTelegramClient("6445089720:AAFtFad-vg8G62gsCpIGNKPmjeDTUbQfeK4")
                            )
                    );
        };
    }


}
