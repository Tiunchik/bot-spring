package org.bot.spring.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.TelegramOkHttpClientFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class LongPollingConfiguration {

    @Bean(destroyMethod = "close")
    public ScheduledExecutorService virtualTreadsExecutorService() {
        return Executors.newScheduledThreadPool(100, Thread.ofVirtual().factory());
    }


    @Bean(destroyMethod = "close")
    public TelegramBotsLongPollingApplication telegramBotsLongPollingApplication(ScheduledExecutorService virtualTreadsExecutorService,
                                                                                 ObjectMapper mapper) {
        return new TelegramBotsLongPollingApplication(
                () -> mapper,
                new TelegramOkHttpClientFactory.DefaultOkHttpClientCreator(),
                () -> virtualTreadsExecutorService
        );
    }

}
