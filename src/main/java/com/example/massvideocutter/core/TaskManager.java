package com.example.massvideocutter.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Basit bir Thread Pool yöneticisi.
 * submit() ile Runnable iş parçacıklarını kuyruğa ekler.
 * shutdown() ile havuzun kapanmasını sağlar.
 */
public class TaskManager {

    private final ExecutorService executor;

    /**
     * Varsayılan olarak, CPU çekirdek sayısı kadar thread yaratır.
     */
    public TaskManager() {
        int threads = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(threads);
    }

    /**
     * Arka planda çalıştırılacak işi kuyruğa ekler.
     * @param task Runnable iş
     */
    public void submit(Runnable task) {
        executor.submit(task);
    }

    /**
     * Havuzu düzgünce kapatır, bekleyen işlere süre tanır.
     * Genellikle uygulama kapanırken çağrılır.
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
