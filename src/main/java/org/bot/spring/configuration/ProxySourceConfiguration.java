package org.bot.spring.configuration;

import org.bot.spring.service.proxy.source.LocalFileSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация локальных источников прокси.
 * <p>
 * Создаёт бины {@link LocalFileSource} для файлов из {@code resources/proxy_list/}.
 * Для добавления нового файла — добавить новый {@code @Bean} метод.
 *
 * @see LocalFileSource
 */
@Configuration
public class ProxySourceConfiguration {

    /**
     * Список socks5 прокси из GitHub (TheSpeedX/PROXY-List).
     */
    @Bean
    public LocalFileSource theSpeedXSource() {
        return new LocalFileSource("proxy_list/TheSpeedX.xtx", "socks5");
    }

    /**
     * Список socks4 прокси из free-proxy-list.net (сохранён вручную).
     */
    @Bean
    public LocalFileSource freeProxySource() {
        return new LocalFileSource("proxy_list/free-proxy_hardcode.txt", "socks4");
    }
}
