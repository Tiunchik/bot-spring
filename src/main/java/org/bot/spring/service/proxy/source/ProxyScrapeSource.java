package org.bot.spring.service.proxy.source;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bot.spring.dto.ProxyDto;
import org.bot.spring.service.proxy.client.ProxyScrapeClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Источник прокси из API proxyscrape.com.
 * <p>
 * Загружает список бесплатных socks4/socks5 прокси по сети.
 * Протокол определяется автоматически из ответа API.
 * Фильтруются только живые прокси ({@code alive: true}).
 *
 * @see ProxyScrapeClient
 * @see ProxySource
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProxyScrapeSource implements ProxySource {

    private final ProxyScrapeClient client;
    private volatile List<ProxyDto> proxies = new ArrayList<>();

    @Override
    public List<ProxyDto> getProxies() {
        return proxies;
    }

    @Override
    public void refresh() {
        log.info("Обновление прокси из ProxyScrape API...");
        try {
            List<ProxyDto> fetched = client.fetchProxies();
            if (!fetched.isEmpty()) {
                proxies = fetched;
                long socks5 = fetched.stream().filter(p -> "socks5".equalsIgnoreCase(p.getVersion())).count();
                long socks4 = fetched.stream().filter(p -> "socks4".equalsIgnoreCase(p.getVersion())).count();
                log.info("{}: загружено {} прокси (socks5: {}, socks4: {})", getName(), fetched.size(), socks5, socks4);
            } else {
                log.warn("{}: API вернул пустой список", getName());
            }
        } catch (Exception e) {
            log.error("{}: ошибка при обновлении", getName(), e);
        }
    }

    @Override
    public String getName() {
        return "ProxyScrape";
    }
}
