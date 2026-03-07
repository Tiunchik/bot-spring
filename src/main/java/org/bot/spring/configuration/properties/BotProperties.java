package org.bot.spring.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "bot")
public class BotProperties {
    public static final String STRING = ";";

    private String token;
    private String allowedUsers;

    public List<String> getAllowedUsersList() {
        if (allowedUsers == null || allowedUsers.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return List.of(allowedUsers.split(STRING));
    }
}
