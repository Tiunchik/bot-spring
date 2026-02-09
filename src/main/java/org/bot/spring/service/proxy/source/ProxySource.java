package org.bot.spring.service.proxy.source;

import org.bot.spring.dto.ProxyDto;

import java.util.List;

/**
 * Источник прокси-серверов.
 * <p>
 * Каждая реализация отвечает за получение прокси из определённого источника
 * (API, файл, база данных и т.д.). Все реализации автоматически собираются
 * в {@link org.bot.spring.service.proxy.ProxyProvider} через Spring DI.
 */
public interface ProxySource {

    /**
     * Возвращает текущий список прокси из этого источника.
     *
     * @return список прокси (может быть пустым, но не null)
     */
    List<ProxyDto> getProxies();

    /**
     * Обновляет список прокси из источника.
     * <p>
     * Вызывается при старте приложения и периодически по расписанию.
     * Реализация должна обрабатывать ошибки внутри метода.
     */
    void refresh();

    /**
     * Имя источника для логирования.
     *
     * @return человекочитаемое имя (например, "ProxyScrape", "LocalFile[xxx.txt]")
     */
    String getName();

    /**
     * Проверяет, есть ли доступные прокси в этом источнике.
     *
     * @return true если список прокси не пуст
     */
    default boolean isAvailable() {
        return !getProxies().isEmpty();
    }
}
