package org.bot.spring.handlers;

import org.bot.spring.dto.MessageContext;

public interface MessageHandler {
    boolean canHandle(String text);
    void handle(String text, MessageContext context);
}
