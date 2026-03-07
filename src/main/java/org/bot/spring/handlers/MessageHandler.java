package org.bot.spring.handlers;

import org.bot.spring.dto.MessageContext;

//TODO: добавить тикток
public interface MessageHandler {
    boolean canHandle(String text);
    void handle(String text, MessageContext context);
}
