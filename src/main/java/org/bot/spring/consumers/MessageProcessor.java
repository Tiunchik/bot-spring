package org.bot.spring.consumers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bot.spring.configuration.properties.BotProperties;
import org.bot.spring.dto.MessageContext;
import org.bot.spring.handlers.MessageHandler;
import org.bot.spring.service.ChatQueueExecutorService;
import org.bot.spring.service.TelegramMessageService;
import org.springframework.stereotype.Component;
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
    private final TelegramMessageService telegramMessageService;
    private final ChatQueueExecutorService chatQueueExecutorService;

    @Override
    public void consume(List<Update> updates) {
        for (var update : updates) {
            Long chatId = extractChatId(update);
            if (chatId != null) {
                chatQueueExecutorService.submitTask(chatId, () -> {
                    try {
                        processUpdate(update);
                    } catch (Exception e) {
                        log.error("Ошибка при обработке обновления", e);
                    }
                });
            }
        }
    }

    private Long extractChatId(Update update) {
        if (update.getMessage() != null) {
            return update.getMessage().getChatId();
        }
        if (update.getChannelPost() != null) {
            return update.getChannelPost().getChatId();
        }
        return null;
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

        // Проверка типа сообщения - не должно содержать видео, аудио, фото, документа и стикера
        if (receivedMessage.hasVideo()
                || receivedMessage.hasAudio()
                || receivedMessage.hasPhoto()
                || receivedMessage.hasDocument()
                || receivedMessage.hasSticker()) {
            log.info("Сообщение не должно содержать видео, аудио, фото, документа и стикера");
            return;
        }

        String text = receivedMessage.getText();
        long chatId = receivedMessage.getChatId();


        if (text.length() > 400) {
            log.info("Сообщение слишком длинное, игнорируем");
            return;
        }

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
            //TODO: Переделать логику проверки текста - вначале искать ссылки, а потом делать регэксп, иначе на больишх файлах приходит пздц
            if (handler.canHandle(text)) {
                log.info("Обработчик {} выбран для обработки сообщения", handler.getClass().getSimpleName());
                try {
                    handler.handle(text, context);
                } catch (Exception e) {
                    log.error("Ошибка при обработке сообщения хендлером {}", handler.getClass().getSimpleName(), e);
                    telegramMessageService.sendTextMessage(chatId, "Произошла ошибка при обработке: " + e.getMessage());
                }
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
