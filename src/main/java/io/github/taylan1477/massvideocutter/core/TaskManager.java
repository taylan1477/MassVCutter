package io.github.taylan1477.massvideocutter.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Simple thread pool manager.
 * Use submit() to queue Runnable tasks for background execution.
 * Use shutdown() to gracefully close the pool.
 */
public class TaskManager {

    private final ExecutorService executor;

    /**
     * Creates a thread pool sized to the number of available CPU cores.
     */
    public TaskManager() {
        int threads = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(threads);
    }

    /**
     * Submit a task for background execution.
     * @param task Runnable task to execute
     */
    public void submit(Runnable task) {
        executor.submit(task);
    }

    /**
     * Gracefully shuts down the pool, allowing pending tasks time to complete.
     * Typically called on application exit.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
