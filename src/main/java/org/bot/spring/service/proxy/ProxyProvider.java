package org.bot.spring.service.proxy;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bot.spring.dto.ProxyDto;
import org.bot.spring.service.proxy.source.ProxySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Главный сервис модуля прокси.
 * <p>
 * Агрегирует прокси из всех {@link ProxySource} в единый пул и выдаёт их по кругу (round-robin).
 * Используется для обхода блокировок при скачивании видео через yt-dlp.
 * <p>
 * <b>Жизненный цикл:</b>
 * <ol>
 *   <li>При старте ({@code @PostConstruct}) загружает прокси из всех источников</li>
 *   <li>Каждые 5 минут ({@code @Scheduled}) обновляет список</li>
 *   <li>Прокси перемешиваются (shuffle) для равномерного распределения нагрузки</li>
 * </ol>
 *
 * @see ProxySource
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyProvider {

    private final List<ProxySource> sources;

    private volatile List<ProxyDto> allProxies = new ArrayList<>();
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        refreshAll();
    }

    /**
     * Обновляет все источники и пересобирает общий список прокси.
     * <p>
     * Вызывается при старте и каждые 5 минут по расписанию.
     */
    @Scheduled(fixedRate = 300_000_00)
    public void refreshAll() {
        log.info("Обновление всех источников прокси...");

        for (ProxySource source : sources) {
            try {
                source.refresh();
            } catch (Exception e) {
                log.error("Ошибка при обновлении источника {}", source.getName(), e);
            }
        }

        List<ProxyDto> combined = new ArrayList<>();
        for (ProxySource source : sources) {
            combined.addAll(source.getProxies());
        }

        Collections.shuffle(combined);
        allProxies = combined;
        currentIndex.set(0);

        long socks5 = combined.stream().filter(p -> "socks5".equalsIgnoreCase(p.getVersion())).count();
        long socks4 = combined.stream().filter(p -> "socks4".equalsIgnoreCase(p.getVersion())).count();
        log.info("Всего загружено {} прокси из {} источников (socks5: {}, socks4: {})",
                combined.size(), sources.size(), socks5, socks4);
    }

    /**
     * Возвращает следующий прокси из пула (любой протокол).
     *
     * @return следующий прокси или null если пул пуст
     */
    public ProxyDto getNextProxy() {
        List<ProxyDto> list = allProxies;
        if (list.isEmpty()) {
            return null;
        }
        int index = currentIndex.getAndUpdate(i -> (i + 1) % list.size());
        return list.get(index);
    }

    /**
     * Возвращает следующий socks5 прокси.
     * <p>
     * Если socks5 нет — fallback на любой доступный прокси.
     *
     * @return прокси или null если пул пуст
     */
    public ProxyDto getNextSocks5Proxy() {
        List<ProxyDto> socks5List = allProxies.stream()
                .filter(p -> "socks5".equalsIgnoreCase(p.getVersion()))
                .toList();

        if (socks5List.isEmpty()) {
            log.warn("Нет socks5 прокси, используем любой доступный");
            return getNextProxy();
        }

        int index = currentIndex.getAndUpdate(i -> (i + 1) % socks5List.size());
        return socks5List.get(index);
    }

    /**
     * Возвращает следующий прокси в формате для yt-dlp.
     * <p>
     * Формат: {@code socks5://ip:port}
     *
     * @return строка прокси или null если пул пуст
     */
    public String getCurrentProxy() {
        ProxyDto proxy = getNextSocks5Proxy();
        if (proxy != null) {
            String proxyStr = proxy.toYtDlpFormat();
            log.debug("Выдан прокси: {}", proxyStr);
            return proxyStr;
        }
        return null;
    }

    /**
     * @return общее количество прокси в пуле
     */
    public int getProxyCount() {
        return allProxies.size();
    }
}
