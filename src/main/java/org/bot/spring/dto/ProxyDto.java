package org.bot.spring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyDto {
    private String ip;
    private int port;
    private String code;
    private String version;

    public String toYtDlpFormat() {
        return String.format("%s://%s:%d", version.toLowerCase(), ip, port);
    }
}
