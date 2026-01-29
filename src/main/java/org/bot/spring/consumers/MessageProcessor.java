package org.bot.spring.consumers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bot.spring.configuration.properties.BotProperties;
import org.bot.spring.dto.MessageContext;
import org.bot.spring.handlers.MessageHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.List;

import static java.util.Objects.nonNull;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageProcessor implements LongPollingUpdateConsumer {

    private final BotProperties botProperties;
    private final List<MessageHandler> messageHandlers;

    @Override
    public void consume(List<Update> updates) {
        for (var update : updates) {
            try {
                processUpdate(update);
            } catch (Exception e) {
                log.error("Ошибка при обработке обновления", e);
            }
        }
    }

    private void processUpdate(Update update) {
        Message receivedMessage = update.getMessage();
        if (nonNull(update.getChannelPost())) {
            receivedMessage = update.getChannelPost();
        }

        if (receivedMessage == null) {
            log.warn("Получено обновление без сообщения");
            return;
        }

        // Проверка авторизации пользователя
        if (!isUserAuthorized(receivedMessage.getFrom())) {
            log.warn("Неавторизованный пользователь: {}", receivedMessage.getFrom().getUserName());
            return;
        }

        // Проверка типа сообщения - должно быть текстовым
        if (!receivedMessage.hasText()) {
            log.info("Сообщение не содержит текста, игнорируем");
            return;
        }

        String text = receivedMessage.getText();
        long chatId = receivedMessage.getChatId();
        
        log.info("Получено сообщение от {}: {}", receivedMessage.getFrom().getUserName(), text);

        // Создаем контекст сообщения
        MessageContext context = new MessageContext(
                chatId,
                receivedMessage.getMessageId(),
                receivedMessage.getFrom().getUserName()
        );

        // Поиск подходящего обработчика
        boolean handled = false;
        for (MessageHandler handler : messageHandlers) {
            if (handler.canHandle(text)) {
                log.info("Обработчик {} выбран для обработки сообщения", handler.getClass().getSimpleName());
                handler.handle(text, context);
                handled = true;
                break;
            }
        }

        if (!handled) {
            log.info("Не найден подходящий обработчик для сообщения: {}", text);
        }
    }

    /**
     * Проверяет, авторизован ли пользователь
     */
    private boolean isUserAuthorized(User user) {
        if (user == null) {
            return false;
        }

        String username = user.getUserName();
        if (username == null || username.isEmpty()) {
            log.warn("Пользователь без username: {}", user.getId());
            return false;
        }

        List<String> allowedUsers = botProperties.getAllowedUsersList();
        if (allowedUsers.isEmpty()) {
            log.warn("Список разрешенных пользователей пуст");
            return false;
        }

        boolean isAuthorized = allowedUsers.contains(username);
        if (!isAuthorized) {
            log.warn("Пользователь {} не в списке разрешенных", username);
        }
        
        return isAuthorized;
    }
}
