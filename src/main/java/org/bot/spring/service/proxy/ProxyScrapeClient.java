package org.bot.spring.service.proxy;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.bot.spring.dto.ProxyDto;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ProxyScrapeClient {

    private static final String API_URL = "https://api.proxyscrape.com/v4/free-proxy-list/get?request=display_proxies&proxy_format=ipport&format=json";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public List<ProxyDto> fetchProxies() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("ProxyScrape API вернул статус: {}", response.statusCode());
                return List.of();
            }

            return parseResponse(response.body());

        } catch (IOException | InterruptedException e) {
            log.error("Ошибка при запросе к ProxyScrape API", e);
            return List.of();
        }
    }

    private List<ProxyDto> parseResponse(String json) {
        List<ProxyDto> proxies = new ArrayList<>();

        try {
            JSONObject root = JSON.parseObject(json);
            JSONArray proxiesArray = root.getJSONArray("proxies");

            if (proxiesArray == null) {
                log.warn("Поле 'proxies' отсутствует в ответе");
                return proxies;
            }

            for (int i = 0; i < proxiesArray.size(); i++) {
                JSONObject proxyObj = proxiesArray.getJSONObject(i);

                String ip = proxyObj.getString("ip");
                Integer port = proxyObj.getInteger("port");
                String protocol = proxyObj.getString("protocol");
                Boolean alive = proxyObj.getBoolean("alive");

                if (ip == null || port == null || protocol == null) {
                    continue;
                }

                if (alive != null && !alive) {
                    continue;
                }

                if ("socks5".equalsIgnoreCase(protocol) || "socks4".equalsIgnoreCase(protocol)) {
                    String countryCode = "";
                    JSONObject ipData = proxyObj.getJSONObject("ip_data");
                    if (ipData != null) {
                        countryCode = ipData.getString("countryCode");
                    }

                    proxies.add(new ProxyDto(ip, port, countryCode != null ? countryCode : "", protocol));
                }
            }

            log.info("Распарсено {} socks прокси из ProxyScrape API", proxies.size());

        } catch (Exception e) {
            log.error("Ошибка парсинга JSON от ProxyScrape", e);
        }

        return proxies;
    }
}
