package io.github.taylan1477.massvideocutter.core;

import io.github.taylan1477.massvideocutter.util.AppSettings;

import java.io.File;
import java.util.List;

public class BatchProcessFacade {

    private final TrimFacade trimFacade;
    private final TaskManager taskManager;

    public BatchProcessFacade(TrimFacade trimFacade, TaskManager taskManager) {
        this.trimFacade   = trimFacade;
        this.taskManager  = taskManager;
    }

    /**
     * Process all selected files in batch.
     *
     * @param files       Files to trim
     * @param startSec    Start time (seconds)
     * @param endSec      End time (seconds)
     * @param callback    Called when each file is processed
     */
    public void processAll(List<File> files, double startSec, double endSec, BatchCallback callback) {
        for (File file : files) {
            taskManager.submit(() -> {
                String input  = file.getAbsolutePath();
                String output = AppSettings.getInstance().getOutputPath(input);
                boolean ok    = trimFacade.trimVideo(input, output, startSec, endSec);
                callback.onFileProcessed(file, ok, output);
            });
        }
    }

    public interface BatchCallback {
        void onFileProcessed(File file, boolean success, String outputPath);
    }
}
