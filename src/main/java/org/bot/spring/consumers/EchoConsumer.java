package org.bot.spring.consumers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Slf4j
public class EchoConsumer implements LongPollingUpdateConsumer {

    private final OkHttpTelegramClient telegramClient;

    public EchoConsumer(OkHttpTelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    @Override
    public void consume(List<Update> updates) {
        for (var update : updates) {
            try {
                Message receivedMessage = update.getMessage();
                SendMessage sendMessage = SendMessage.builder()
                        .chatId(receivedMessage.getChatId())
                        .text(receivedMessage.hasText() ? receivedMessage.getText() : "В полученном сообщении нет текста")
                        .build();
                telegramClient.execute(sendMessage);
            } catch (TelegramApiException e) {
                log.atInfo().log("Ошибка", e);
            }
        }
    }
}
