package org.bot.spring;

import org.bot.spring.handlers.YouTubeMessageHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class YoutubeCanHandleTest {
    
    private final YouTubeMessageHandler youTubeMessageHandler = new YouTubeMessageHandler(null, null, null);

    @Test
    void extractUrl_shouldReturnNull_whenInputIsNull() {
        
        boolean result = youTubeMessageHandler.canHandle(null);
        assertFalse(result);
    }

    @Test
    void extractUrl_shouldReturnNull_whenInputIsEmpty() {
        boolean result = youTubeMessageHandler.canHandle("");
        assertFalse(result);
    }

    @Test
    void extractUrl_shouldReturnNull_whenInputHasNoUrl() {
        boolean result = youTubeMessageHandler.canHandle("Просто текст без ссылок");
        assertFalse(result);
    }

    @Test
    void extractUrl_shouldReturnNull_whenInputHasOnlySpaces() {
        boolean result = youTubeMessageHandler.canHandle("   ");
        assertFalse(result);
    }

    @Test
    void extractUrl_shouldExtractHttpUrl() {
        String input = "http://youtube.com/video";
        boolean result = youTubeMessageHandler.canHandle(input);
        assertTrue(result);
    }

    @Test
    void extractUrl_shouldExtractHttpsUrl() {
        String input = "https://youtube.com/video";
        boolean result = youTubeMessageHandler.canHandle(input);
        assertTrue(result);
    }

    @Test
    void extractUrl_shouldReturnFirstUrl_whenMultipleUrlsPresent() {
        String input = "https://first.com/video http://second.com/video https://third.com/video";
        boolean result = youTubeMessageHandler.canHandle(input);
        assertFalse(result);
    }

    @Test
    void extractUrl_shouldReturnJustUrl_whenNRandText() {
        String input = "бла-бла и потом ссылка\n" +
                "\n" +
                "https://youtube.com/shorts/8e6GJkZTcfM?si=g7e39Pj57vySWStC";
        boolean result = youTubeMessageHandler.canHandle(input);
        assertTrue(result);
    }

    @Test
    void extractUrl_shouldExtractUrlFromText() {
        String input = "Посмотри это видео: https://youtube.com/watch?v=dQw4w9WgXcQ, оно очень интересное!";
        boolean result = youTubeMessageHandler.canHandle(input);
        assertTrue(result);
    }

    @Test
    void extractUrl_shouldExtractUrlWithParameters() {
        String input = "https://youtube.com/video?id=123&param=value";
        boolean result = youTubeMessageHandler.canHandle(input);
        assertTrue(result);
    }

    @Test
    void extractUrl_shouldExtractUrlWithFragment() {
        String input = "https://youtube.com/video#section";
        boolean result = youTubeMessageHandler.canHandle(input);
        assertTrue(result);
    }

    @Test
    void extractUrl_shouldExtractYouTubeUrl() {
        String input = "Ссылка на видео: https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        boolean result = youTubeMessageHandler.canHandle(input);
        assertTrue(result);
    }

    @Test
    void extractUrl_shouldExtractShortYouTubeUrl() {
        String input = "https://youtu.be/dQw4w9WgXcQ";
        boolean result = youTubeMessageHandler.canHandle(input);
        assertTrue(result);
    }

    @Test
    void extractUrl_shouldExtractInstagramUrl() {
        String input = "Пост в Instagram: https://www.instagram.com/p/ABC123/";
        boolean result = youTubeMessageHandler.canHandle(input);
        assertFalse(result);
    }

    @Test
    void extractUrl_shouldExtractUrlWithWww() {
        String input = "https://www.youtube.com/video";
        boolean result = youTubeMessageHandler.canHandle(input);
        assertTrue(result);
    }

    @Test
    void extractUrl_shouldExtractUrlWithPort() {
        String input = "https://youtube.com:8080/video";
        boolean result = youTubeMessageHandler.canHandle(input);
        assertTrue(result);
    }

    @Test
    void extractUrl_shouldExtractUrlWithPathAndExtension() {
        String input = "https://youtube.com/path/to/video.mp4";
        boolean result = youTubeMessageHandler.canHandle(input);
        assertTrue(result);
    }

    @Test
    void extractUrl_shouldExtractUrlFromMultilineText() {
        String input = "Первая строка\nhttps://youtube.com/video\nВторая строка";
        boolean result = youTubeMessageHandler.canHandle(input);
        assertTrue(result);
    }

    @Test
    void extractUrl_shouldExtractUrlFromBeginningOfString() {
        String input = "https://youtube.com/video - это ссылка";
        boolean result = youTubeMessageHandler.canHandle(input);
        assertTrue(result);
    }

    @Test
    void extractUrl_shouldExtractUrlFromEndOfString() {
        String input = "Ссылка на видео: https://youtube.com/video";
        boolean result = youTubeMessageHandler.canHandle(input);
        assertTrue(result);
    }

    @Test
    void extractUrl_shouldReturnNull_whenUrlWithoutProtocol() {
        boolean result = youTubeMessageHandler.canHandle("youtube.com/video");
        assertTrue(result);
    }

    @Test
    void extractUrl_shouldExtractUrlWithIpAddress() {
        String input = "https://192.168.1.1/video";
        boolean result = youTubeMessageHandler.canHandle(input);
        assertFalse(result);
    }
}
