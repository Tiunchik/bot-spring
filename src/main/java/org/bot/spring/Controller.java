package org.bot.spring;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/")
public class Controller {


    @EventListener
    public void handleApplicationReadyEvent(ApplicationReadyEvent event) {
    }
}

