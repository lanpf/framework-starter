package com.cloud.framework.starter.outbox.jpa.rabbitmq.test.support;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.core.task.AsyncTaskExecutor;

public class PausableAsyncTaskExecutor implements AsyncTaskExecutor, AutoCloseable {
    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private final ExecutorService delegate = Executors.newSingleThreadExecutor();

    @Override
    public void execute(Runnable task) {
        this.tasks.add(task);
    }

    public Integer pendingTaskCount() {
        return this.tasks.size();
    }

    public void releaseAll() {
        Runnable task;
        while ((task = this.tasks.poll()) != null) {
            this.delegate.execute(task);
        }
    }

    @Override
    public void close() {
        this.delegate.shutdownNow();
    }
}
