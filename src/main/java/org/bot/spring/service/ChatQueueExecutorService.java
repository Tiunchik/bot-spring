package org.bot.spring.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class ChatQueueExecutorService {

    private final ConcurrentHashMap<Long, CompletableFuture<Void>> chatQueues = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public void submitTask(Long chatId, Runnable task) {
        chatQueues.compute(chatId, (key, existingFuture) -> {
            if (existingFuture == null || existingFuture.isDone()) {
                log.debug("Создана новая очередь для чата {}", chatId);
                return CompletableFuture.runAsync(task, executor);
            } else {
                log.debug("Задача добавлена в очередь чата {}", chatId);
                return existingFuture.thenRunAsync(task, executor);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        log.info("Завершение работы ChatQueueExecutorService");
        executor.shutdown();
    }
}
