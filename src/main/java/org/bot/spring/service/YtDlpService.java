package org.bot.spring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bot.spring.configuration.properties.DownloadProperties;
import org.bot.spring.dto.MessageContext;
import org.bot.spring.dto.DownloadVideoCommand;
import org.bot.spring.dto.VideoFormatDto;
import org.bot.spring.exceptions.YtDlpExitException;
import org.bot.spring.service.proxy.ProxyProvider;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.nonNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class YtDlpService {

    private final DownloadProperties downloadProperties;

    /**
     * Пытается найти самое большое видео в списке
     */
    public VideoFormatDto getMaxVideoSizeForInstagram(String url) throws IOException, InterruptedException {
        List<VideoFormatDto> formats = new ArrayList<>();

        ProcessBuilder processBuilder = new ProcessBuilder("yt-dlp", "-F", url);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Пропускаем заголовок и разделитель
                getMaxVideoSize(line, formats);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("yt-dlp завершился с кодом " + exitCode);
        }

        return formats.stream()
                .filter(it -> nonNull(it.getFileSizeInMB()))
                .max(Comparator.comparing(VideoFormatDto::getFileSizeInMB))
                .get();
    }

    public void getMaxVideoSize(String line, List<VideoFormatDto> formats) {
        Pattern sizePattern = Pattern.compile("([\\d.]+(KiB|MiB|GiB))");

        if (line.contains("ID") || line.contains("[youtube]") || line.contains("[line]") || line.contains("----------") ||
                line.contains("audio only") || line.contains("mhtml") ||
                line.trim().isEmpty()) {
            return;
        }

        Matcher sizeMatcher = sizePattern.matcher(line);
        if (sizeMatcher.find()) {
            String fullFileSize = sizeMatcher.group(1);
            String unit = sizeMatcher.group(2);
            String fileSizeStr = fullFileSize.substring(0, fullFileSize.length() - unit.length());

            // Пропускаем форматы без размера файла
            if (fileSizeStr.isEmpty()) {
                return;
            }

            VideoFormatDto dto = new VideoFormatDto();

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
     * Получает список доступных форматов видео по URL
     */
    public List<VideoFormatDto> getAvailableFormats(String url) throws IOException, InterruptedException {
        List<VideoFormatDto> formats = new ArrayList<>();

        ProcessBuilder processBuilder = new ProcessBuilder("yt-dlp", "-F", url);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        
        var ytDlpLog = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Пропускаем заголовок и разделитель
                extracted(line, formats);
                ytDlpLog.append(line);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new YtDlpExitException(exitCode, ytDlpLog.toString());
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
                .filter(f -> f.getResolution().contains("640") || f.getResolution().contains("360") || f.getResolution().contains("240"))
                .min((f1, f2) -> f1.getFileSizeInMB().compareTo(f2.getFileSizeInMB()))
                .orElse(null);
    }

    public String createFilename(MessageContext ctx) {
        return String.format("%d-%d-%s.mp4", ctx.getChatId(), ctx.getMessageId(), ctx.getUsername() != null ? ctx.getUsername() : "unknown");
    }

    public String pathToDownload() {
        return downloadProperties.getDownloadPath();
    }
    
    /**
     * Скачивает видео с помощью yt-dlp
     */
    public void downloadVideo(DownloadVideoCommand command) throws IOException, InterruptedException {
        val process = new ProcessBuilder(command.getListCommands())
                .redirectErrorStream(true)
                .start();

        try (val reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("yt-dlp: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("yt-dlp завершился с кодом " + exitCode);
        }

        File file = new File(command.getOutputPath());
        if (!file.exists()) {
            throw new IOException("Файл не был создан: " + command.getOutputPath());
        }
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
        Pattern urlPattern = Pattern.compile("(https?:\\/\\/[^\\s!,]+)");
        Matcher matcher = urlPattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }


}
