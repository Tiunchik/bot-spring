package org.bot.spring;

import org.bot.spring.configuration.properties.DownloadProperties;
import org.bot.spring.service.proxy.ProxyProviderService;
import org.bot.spring.service.proxy.ProxyScrapeClient;
import org.bot.spring.service.YtDlpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class YtDlpServiceTest {

    private YtDlpService ytDlpService;
    private DownloadProperties downloadProperties;

    @BeforeEach
    void setUp() {
        downloadProperties = new DownloadProperties();
        downloadProperties.setDownloadPath("/tmp/");
        downloadProperties.setMaxFileSizeMB(new BigDecimal("50"));
        ytDlpService = new YtDlpService(downloadProperties, new ProxyProviderService(new ProxyScrapeClient()));
    }

    @Test
    void extractUrl_shouldReturnNull_whenInputIsNull() {
        String result = ytDlpService.extractUrl(null);
        assertNull(result, "Метод должен возвращать null для null входа");
    }

    @Test
    void extractUrl_shouldReturnNull_whenInputIsEmpty() {
        String result = ytDlpService.extractUrl("");
        assertNull(result, "Метод должен возвращать null для пустой строки");
    }

    @Test
    void extractUrl_shouldReturnNull_whenInputHasNoUrl() {
        String result = ytDlpService.extractUrl("Просто текст без ссылок");
        assertNull(result, "Метод должен возвращать null для строки без URL");
    }

    @Test
    void extractUrl_shouldReturnNull_whenInputHasOnlySpaces() {
        String result = ytDlpService.extractUrl("   ");
        assertNull(result, "Метод должен возвращать null для строки с только пробелами");
    }

    @Test
    void extractUrl_shouldExtractHttpUrl() {
        String input = "http://example.com/video";
        String result = ytDlpService.extractUrl(input);
        assertEquals("http://example.com/video", result, "Метод должен извлекать HTTP URL");
    }

    @Test
    void extractUrl_shouldExtractHttpsUrl() {
        String input = "https://example.com/video";
        String result = ytDlpService.extractUrl(input);
        assertEquals("https://example.com/video", result, "Метод должен извлекать HTTPS URL");
    }

    @Test
    void extractUrl_shouldReturnFirstUrl_whenMultipleUrlsPresent() {
        String input = "https://first.com/video http://second.com/video https://third.com/video";
        String result = ytDlpService.extractUrl(input);
        assertEquals("https://first.com/video", result, "Метод должен возвращать первый найденный URL");
    }

    @Test
    void extractUrl_shouldReturnJustUrl_whenNRandText() {
        String input = "бла-бла и потом ссылка\n" +
                "\n" +
                "https://youtube.com/shorts/8e6GJkZTcfM?si=g7e39Pj57vySWStC";
        String result = ytDlpService.extractUrl(input);
        assertEquals("https://youtube.com/shorts/8e6GJkZTcfM?si=g7e39Pj57vySWStC", result, "Метод должен возвращать первый найденный URL");
    }

    @Test
    void extractUrl_shouldExtractUrlFromText() {
        String input = "Посмотри это видео: https://youtube.com/watch?v=dQw4w9WgXcQ, оно очень интересное!";
        String result = ytDlpService.extractUrl(input);
        assertEquals("https://youtube.com/watch?v=dQw4w9WgXcQ", result, "Метод должен извлекать URL из текста");
    }

    @Test
    void extractUrl_shouldExtractUrlWithParameters() {
        String input = "https://example.com/video?id=123&param=value";
        String result = ytDlpService.extractUrl(input);
        assertEquals("https://example.com/video?id=123&param=value", result, "Метод должен извлекать URL с параметрами");
    }

    @Test
    void extractUrl_shouldExtractUrlWithFragment() {
        String input = "https://example.com/video#section";
        String result = ytDlpService.extractUrl(input);
        assertEquals("https://example.com/video#section", result, "Метод должен извлекать URL с фрагментом");
    }

    @Test
    void extractUrl_shouldExtractYouTubeUrl() {
        String input = "Ссылка на видео: https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        String result = ytDlpService.extractUrl(input);
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", result, "Метод должен извлекать YouTube URL");
    }

    @Test
    void extractUrl_shouldExtractShortYouTubeUrl() {
        String input = "https://youtu.be/dQw4w9WgXcQ";
        String result = ytDlpService.extractUrl(input);
        assertEquals("https://youtu.be/dQw4w9WgXcQ", result, "Метод должен извлекать короткий YouTube URL");
    }

    @Test
    void extractUrl_shouldExtractInstagramUrl() {
        String input = "Пост в Instagram: https://www.instagram.com/p/ABC123/";
        String result = ytDlpService.extractUrl(input);
        assertEquals("https://www.instagram.com/p/ABC123/", result, "Метод должен извлекать Instagram URL");
    }

    @Test
    void extractUrl_shouldExtractUrlWithWww() {
        String input = "https://www.example.com/video";
        String result = ytDlpService.extractUrl(input);
        assertEquals("https://www.example.com/video", result, "Метод должен извлекать URL с www");
    }

    @Test
    void extractUrl_shouldExtractUrlWithPort() {
        String input = "https://example.com:8080/video";
        String result = ytDlpService.extractUrl(input);
        assertEquals("https://example.com:8080/video", result, "Метод должен извлекать URL с портом");
    }

    @Test
    void extractUrl_shouldExtractUrlWithPathAndExtension() {
        String input = "https://example.com/path/to/video.mp4";
        String result = ytDlpService.extractUrl(input);
        assertEquals("https://example.com/path/to/video.mp4", result, "Метод должен извлекать URL с путем и расширением");
    }

    @Test
    void extractUrl_shouldExtractUrlFromMultilineText() {
        String input = "Первая строка\nhttps://example.com/video\nВторая строка";
        String result = ytDlpService.extractUrl(input);
        assertEquals("https://example.com/video", result, "Метод должен извлекать URL из многострочного текста");
    }

    @Test
    void extractUrl_shouldExtractUrlFromBeginningOfString() {
        String input = "https://example.com/video - это ссылка";
        String result = ytDlpService.extractUrl(input);
        assertEquals("https://example.com/video", result, "Метод должен извлекать URL из начала строки");
    }

    @Test
    void extractUrl_shouldExtractUrlFromEndOfString() {
        String input = "Ссылка на видео: https://example.com/video";
        String result = ytDlpService.extractUrl(input);
        assertEquals("https://example.com/video", result, "Метод должен извлекать URL из конца строки");
    }

    @Test
    void extractUrl_shouldReturnNull_whenUrlWithoutProtocol() {
        String result = ytDlpService.extractUrl("example.com/video");
        assertNull(result, "Метод должен возвращать null для URL без протокола");
    }

    @Test
    void extractUrl_shouldExtractUrlWithIpAddress() {
        String input = "https://192.168.1.1/video";
        String result = ytDlpService.extractUrl(input);
        assertEquals("https://192.168.1.1/video", result, "Метод должен извлекать URL с IP-адресом");
    }
}