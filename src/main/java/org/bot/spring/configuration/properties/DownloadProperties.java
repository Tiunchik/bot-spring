package org.bot.spring.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties(prefix = "download")
public class DownloadProperties {
    private BigDecimal maxFileSizeMB = new BigDecimal(50);
    private String downloadPath = "/tmp/";
}
