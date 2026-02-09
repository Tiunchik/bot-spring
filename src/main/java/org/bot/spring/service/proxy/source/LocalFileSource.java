package org.bot.spring.service.proxy.source;

import lombok.extern.slf4j.Slf4j;
import org.bot.spring.dto.ProxyDto;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Источник прокси из локального файла в resources.
 * <p>
 * Читает файл формата {@code ip:port} (одна запись на строку).
 * Протокол (socks4/socks5) задаётся при создании и применяется ко всем прокси из файла.
 * <p>
 * Не требует сети — используется как fallback или для статичных списков.
 * <p>
 * Бины создаются в {@link org.bot.spring.configuration.ProxySourceConfiguration}.
 *
 * @see ProxySource
 */
@Slf4j
public class LocalFileSource implements ProxySource {

    private static final Pattern IP_PORT_PATTERN = Pattern.compile("^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})$");

    private final String resourcePath;
    private final String protocol;
    private final String name;
    private volatile List<ProxyDto> proxies = new ArrayList<>();

    /**
     * @param resourcePath путь к файлу в classpath (например, "proxy_list/proxies.txt")
     * @param protocol     протокол прокси: "socks4" или "socks5"
     */
    public LocalFileSource(String resourcePath, String protocol) {
        this.resourcePath = resourcePath;
        this.protocol = protocol;
        this.name = "LocalFile[" + resourcePath + "]";
    }

    @Override
    public List<ProxyDto> getProxies() {
        return proxies;
    }

    @Override
    public void refresh() {
        log.info("Загрузка прокси из файла: {}", resourcePath);
        try {
            List<ProxyDto> loaded = loadFromResource();
            if (!loaded.isEmpty()) {
                proxies = loaded;
                log.info("{}: загружено {} прокси", name, loaded.size());
            } else {
                log.warn("{}: файл пуст или не содержит валидных прокси", name);
            }
        } catch (IOException e) {
            log.error("{}: ошибка чтения файла", name, e);
        }
    }

    private List<ProxyDto> loadFromResource() throws IOException {
        List<ProxyDto> result = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource(resourcePath);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                var matcher = IP_PORT_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String ip = matcher.group(1);
                    int port = Integer.parseInt(matcher.group(2));
                    result.add(new ProxyDto(ip, port, "", protocol));
                }
            }
        }

        return result;
    }

    @Override
    public String getName() {
        return name;
    }
}
