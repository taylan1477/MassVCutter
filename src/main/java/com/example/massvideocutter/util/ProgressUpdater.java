package com.example.massvideocutter.util;

import com.example.massvideocutter.core.BatchProcessFacade;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class ProgressUpdater implements BatchProcessFacade.BatchCallback {

    private final int totalFiles;
    private final AtomicInteger processedFiles;
    private final BiConsumer<Double, File> progressListener;

    public ProgressUpdater(int totalFiles, BiConsumer<Double, File> progressListener) {
        this.totalFiles = totalFiles;
        this.processedFiles = new AtomicInteger(0);
        this.progressListener = progressListener;
    }

    @Override
    public void onFileProcessed(File file, boolean success, String outputPath) {
        int currentCount = processedFiles.incrementAndGet();
        double progress = (double) currentCount / totalFiles;
        if (progressListener != null) {
            progressListener.accept(progress, file);

        }
    }
}
