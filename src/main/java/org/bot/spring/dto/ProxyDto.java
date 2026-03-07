package org.bot.spring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO прокси-сервера.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyDto {
    /** IP адрес */
    private String ip;
    /** Порт */
    private int port;
    /** Код страны (опционально, может быть пустым) */
    private String code;
    /** Протокол: "socks4" или "socks5" */
    private String version;

    /**
     * Форматирует прокси для использования в yt-dlp.
     *
     * @return строка вида {@code socks5://ip:port}
     */
    public String toYtDlpFormat() {
        return String.format("%s://%s:%d", version.toLowerCase(), ip, port);
    }
}
