package org.bot.spring;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api")
public class Controller {

    @GetMapping
    public String handleApplicationReadyEvent(ApplicationReadyEvent event) {
        return "{\"message\": \"Application ready\"}";
    }
}

