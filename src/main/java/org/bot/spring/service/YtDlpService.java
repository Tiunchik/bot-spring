package org.bot.spring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bot.spring.configuration.properties.DownloadProperties;
import org.bot.spring.dto.VideoFormatDto;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class YtDlpService {

    private final DownloadProperties downloadProperties;

    /**
     * Получает список доступных форматов видео по URL
     */
    public List<VideoFormatDto> getAvailableFormats(String url) throws IOException, InterruptedException {
        List<VideoFormatDto> formats = new ArrayList<>();
        
        ProcessBuilder processBuilder = new ProcessBuilder("yt-dlp", "-F", url);
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            // Паттерн для парсинга строки формата
            // ID      EXT   RESOLUTION FPS CH │   FILESIZE    TBR PROTO │ VCODEC           VBR ACODEC      ABR ASR MORE INFO
            //───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
            // sb2     mhtml 25x45        1    │                   mhtml │ images                                   storyboard
            // sb3     mhtml 48x27        2    │                   mhtml │ images                                   storyboard
            // sb1     mhtml 50x90        1    │                   mhtml │ images                                   storyboard
            // sb0     mhtml 101x180      1    │                   mhtml │ images                                   storyboard
            // 139-drc m4a   audio only      2 │  246.29KiB    49k https │ audio only           mp4a.40.5   49k 22k [ru] Russian original, low, DRC, m4a_dash
            // 249-drc webm  audio only      2 │  282.85KiB    56k https │ audio only           opus        56k 48k [ru] Russian original, low, DRC, webm_dash
            // 139-0   m4a   audio only      2 │  246.61KiB    49k https │ audio only           mp4a.40.5   49k 22k [en-US] English (US) (default), low, m4a_dash
            // 139-1   m4a   audio only      2 │  246.30KiB    49k https │ audio only           mp4a.40.5   49k 22k [ru] Russian original, low, m4a_dash
            // 249-0   webm  audio only      2 │  291.16KiB    58k https │ audio only           opus        58k 48k [en-US] English (US) (default), low, webm_dash
            // 249-1   webm  audio only      2 │  282.58KiB    56k https │ audio only           opus        56k 48k [ru] Russian original, low, webm_dash
            // 140-drc m4a   audio only      2 │  650.76KiB   130k https │ audio only           mp4a.40.2  130k 44k [ru] Russian original, medium, DRC, m4a_dash
            // 251-drc webm  audio only      2 │  714.89KiB   142k https │ audio only           opus       142k 48k [ru] Russian original, medium, DRC, webm_dash
            // 140-0   m4a   audio only      2 │  651.95KiB   130k https │ audio only           mp4a.40.2  130k 44k [en-US] English (US) (default), medium, m4a_dash
            // 140-1   m4a   audio only      2 │  650.76KiB   130k https │ audio only           mp4a.40.2  130k 44k [ru] Russian original, medium, m4a_dash
            // 251-0   webm  audio only      2 │  718.56KiB   143k https │ audio only           opus       143k 48k [en-US] English (US) (default), medium, webm_dash
            // 251-1   webm  audio only      2 │  714.00KiB   142k https │ audio only           opus       142k 48k [ru] Russian original, medium, webm_dash                                   storyboard

            
            while ((line = reader.readLine()) != null) {
                // Пропускаем заголовок и разделитель
                extracted(line, formats);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("yt-dlp завершился с кодом " + exitCode);
        }
        
        return formats;
    }

    public void extracted(String line, List<VideoFormatDto> formats) {
        Pattern pattern = Pattern.compile(
                "^\\s*([\\w-]+)\\s+(\\w+)\\s+([\\dx]+|audio only)");
        Pattern sizePattern = Pattern.compile("([\\d.]+(KiB|MiB|GiB))");

        if (line.contains("ID") || line.contains("[youtube]") || line.contains("[line]") || line.contains("----------") ||
            line.contains("audio only") || line.contains("video only") || line.contains("mhtml") ||
            line.trim().isEmpty()) {
            return;
        }

        Matcher matcher = pattern.matcher(line);
        Matcher sizeMatcher = sizePattern.matcher(line);
        if (matcher.find() && sizeMatcher.find()) {
            String idStr = matcher.group(1);
            String container = matcher.group(2);
            String resolution = matcher.group(3);
            String fullFileSize = sizeMatcher.group(1);
            String unit = sizeMatcher.group(2);
            String fileSizeStr = fullFileSize.substring(0, fullFileSize.length() - unit.length());

            // Пропускаем форматы без размера файла
            if (fileSizeStr == null || fileSizeStr.isEmpty()) {
                return;
            }

            // Пропускаем форматы без разрешения (например, только аудио)
            if (!resolution.matches("\\d+x\\d+")) {
                return;
            }

            VideoFormatDto dto = new VideoFormatDto();

            // ID может быть числом или строкой с дефисами
            dto.setId(idStr);

            dto.setContainer(container);
            dto.setResolution(resolution);

            BigDecimal size = new BigDecimal(fileSizeStr);
            if ("GiB".equals(unit)) {
                size = size.multiply(new BigDecimal("1024"));
            } else if ("KiB".equals(unit)) {
                size = size.divide(new BigDecimal("1024"), 4, RoundingMode.HALF_UP);
            }
            dto.setFileSizeInMB(size);

            formats.add(dto);
        }
    }

    /**
     * Выбирает лучший формат по критериям:
     * - формат должен быть mp4
     * - разрешение должно быть 720p
     * - если несколько, выбирается с наименьшим размером файла
     */
    public VideoFormatDto selectBestFormat(List<VideoFormatDto> formats) {
        return formats.stream()
                .filter(f -> "mp4".equalsIgnoreCase(f.getContainer()))
                .filter(f -> f.getResolution().contains("360") || f.getResolution().contains("240"))
                .min((f1, f2) -> f1.getFileSizeInMB().compareTo(f2.getFileSizeInMB()))
                .orElse(null);
    }

    /**
     * Скачивает видео в указанном формате
     * @param url URL видео
     * @param formatId ID формата
     * @param chatId ID чата
     * @param messageId ID сообщения
     * @param username Имя пользователя
     * @return Путь к скачанному файлу
     */
    public String downloadVideo(String url, String formatId, long chatId, int messageId, String username)
            throws IOException, InterruptedException {
        
        String fileName = String.format("%d-%d-%s.mp4", chatId, messageId, username != null ? username : "unknown");
        String outputPath = downloadProperties.getDownloadPath() + fileName;
        
        ProcessBuilder processBuilder = new ProcessBuilder(
                "yt-dlp",
                "-f", formatId,
                "-P", downloadProperties.getDownloadPath(),
                "-o", fileName,
                url
        );
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("yt-dlp: {}", line);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("yt-dlp завершился с кодом " + exitCode);
        }
        
        File file = new File(outputPath);
        if (!file.exists()) {
            throw new IOException("Файл не был создан: " + outputPath);
        }
        
        return outputPath;
    }

    /**
     * Удаляет файл по указанному пути
     */
    public void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                log.info("Файл успешно удален: {}", filePath);
            } else {
                log.warn("Не удалось удалить файл: {}", filePath);
            }
        }
    }

    /**
     * Проверяет, превышает ли размер файла максимально допустимый
     */
    public boolean isFileSizeExceeded(BigDecimal fileSizeMB) {
        return fileSizeMB.compareTo(downloadProperties.getMaxFileSizeMB()) > 0;
    }

    /**
     * Возвращает максимально допустимый размер файла в MB
     */
    public BigDecimal getMaxFileSize() {
        return downloadProperties.getMaxFileSizeMB();
    }

    /**
     * Извлекает URL из текста сообщения
     */
    public String extractUrl(String text) {
        if (text == null) {
            return null;
        }
        
        // Паттерн для поиска URL
        Pattern urlPattern = Pattern.compile("(https?://[^\\s]+)");
        Matcher matcher = urlPattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
}
