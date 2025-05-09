package com.example.massvideocutter.core;

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
     * Seçilen dosyaları topluca işler.
     *
     * @param files       Kırpılacak dosyalar.
     * @param startSec    Başlangıç zamanı (saniye).
     * @param endSec      Bitiş zamanı (saniye).
     * @param callback    Her dosya tamamlandığında çağrılacak.
     */
    public void processAll(List<File> files, double startSec, double endSec, BatchCallback callback) {
        for (File file : files) {
            taskManager.submit(() -> {
                String input  = file.getAbsolutePath();
                String output = input.replace(".", "_cut.");
                boolean ok    = trimFacade.trimVideo(input, output, startSec, endSec);
                callback.onFileProcessed(file, ok, output);
            });
        }
    }

    public interface BatchCallback {
        void onFileProcessed(File file, boolean success, String outputPath);
    }
}
