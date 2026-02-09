package org.bot.spring.service.proxy;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bot.spring.dto.ProxyDto;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyProviderService {

    private final ProxyScrapeClient proxyScrapeClient;

    private volatile List<ProxyDto> proxyList = new ArrayList<>();
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        refreshProxyList();
    }

    @Scheduled(fixedRate = 300_000) // 5 минут?
    public void refreshProxyList() {
        log.info("Обновление списка прокси...");
        try {
            List<ProxyDto> newList = proxyScrapeClient.fetchProxies();
            if (!newList.isEmpty()) {
                proxyList = newList;
                currentIndex.set(0);
                long socks5Count = newList.stream()
                        .filter(p -> "socks5".equalsIgnoreCase(p.getVersion()))
                        .count();
                long socks4Count = newList.stream()
                        .filter(p -> "socks4".equalsIgnoreCase(p.getVersion()))
                        .count();
                log.info("Загружено {} прокси (socks5: {}, socks4: {})", newList.size(), socks5Count, socks4Count);
            } else {
                log.warn("Не удалось получить прокси, сохраняем старый список ({} шт)", proxyList.size());
            }
        } catch (Exception e) {
            log.error("Ошибка при обновлении списка прокси", e);
        }
    }

    public ProxyDto getNextProxy() {
        List<ProxyDto> list = proxyList;
        if (list.isEmpty()) {
            return null;
        }
        int index = currentIndex.getAndUpdate(i -> (i + 1) % list.size());
        return list.get(index);
    }

    public ProxyDto getNextSocks5Proxy() {
        List<ProxyDto> socks5List = proxyList.stream()
                .filter(p -> "socks5".equalsIgnoreCase(p.getVersion()))
                .toList();

        if (socks5List.isEmpty()) {
            log.warn("Нет доступных socks5 прокси, пробуем socks4");
            return getNextProxy();
        }

        int index = currentIndex.getAndUpdate(i -> (i + 1) % socks5List.size());
        return socks5List.get(index);
    }

    public int getProxyCount() {
        return proxyList.size();
    }
}
