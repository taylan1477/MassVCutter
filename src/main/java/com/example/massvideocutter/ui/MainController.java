package com.example.massvideocutter.ui;

import com.example.massvideocutter.core.*;
import com.example.massvideocutter.core.ffmpeg.FFmpegWrapper;
import com.example.massvideocutter.util.ProgressUpdater;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainController {

    // UI Components
    @FXML private Label infoLabel;
    @FXML private ListView<File> fileListView;
    @FXML private ListView<String> inspector;
    @FXML private MenuBar menuBar;
    @FXML private ProgressBar progressBar;
    @FXML private MediaView mediaView;
    @FXML private ImageView introImage;
    @FXML private ImageView outroImage;

    // Timeline container (combined with waveform)
    @FXML private StackPane timelineContainer;

    // Time labels
    @FXML private Label startTimeLabel;
    @FXML private Label currentTimeLabel;
    @FXML private Label endTimeLabel;

    // Method selector toggle buttons
    @FXML private ToggleButton btnManual;
    @FXML private ToggleButton btnAudio;
    @FXML private ToggleButton btnScene;
    @FXML private ToggleGroup methodGroup;

    // Core components
    private MediaPlayer mediaPlayer;
    private TrimFacade trimFacade = new TrimFacade();
    private BatchProcessFacade batchFacade;
    private Map<TrimMethod, TrimStrategy> strategies;

    // Custom timeline control (with integrated waveform)
    private TimelineControl timelineControl;

    // State
    private TrimMethod currentMethod = TrimMethod.MANUAL;

    // Supported video extensions
    private static final List<String> VIDEO_EXTENSIONS = List.of(
            ".mp4", ".mkv", ".ts", ".avi", ".mov", ".webm", ".flv"
    );

    @FXML
    public void initialize() {
        // Initialize core components
        TaskManager taskManager = new TaskManager();
        this.trimFacade = new TrimFacade();
        this.batchFacade = new BatchProcessFacade(trimFacade, taskManager);

        // Initialize strategies
        double silenceThreshold = -30.0;
        double minSilenceDuration = 5.0;
        FFmpegWrapper ffmpegWrapper = new FFmpegWrapper();

        strategies = Map.of(
                TrimMethod.MANUAL, new ManualTrimStrategy(trimFacade),
                TrimMethod.AUDIO_ANALYZER, new AudioAnalyzerStrategy(trimFacade, true) // Anime mode
        );

        // Setup components
        setupFileListView();
        setupDragAndDrop();
        setupMethodSelector();
        setupTimelineControl();
    }

    private void setupFileListView() {
        fileListView.setCellFactory(param -> new ListCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getName());
            }
        });

        fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldFile, newFile) -> {
            if (newFile != null) {
                playVideo(newFile);
                
                // Apply cached detection results if available (Audio mode)
                if (currentMethod == TrimMethod.AUDIO_ANALYZER && detectionResults.containsKey(newFile)) {
                    applyDetectionResult(detectionResults.get(newFile));
                }
            }
        });
    }

    private void setupDragAndDrop() {
        fileListView.setOnDragOver(event -> {
            if (event.getGestureSource() != fileListView && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        fileListView.setOnDragEntered(event -> {
            if (event.getDragboard().hasFiles()) {
                fileListView.setStyle("-fx-border-color: #FFA500; -fx-border-width: 2px; -fx-border-style: dashed;");
            }
        });

        fileListView.setOnDragExited(event -> fileListView.setStyle(""));

        fileListView.setOnDragDropped(event -> {
            var db = event.getDragboard();
            boolean success = false;

            if (db.hasFiles()) {
                for (File file : db.getFiles()) {
                    if (isVideoFile(file) && !fileListView.getItems().contains(file)) {
                        fileListView.getItems().add(file);
                        success = true;
                    }
                }

                if (!fileListView.getItems().isEmpty() && fileListView.getSelectionModel().isEmpty()) {
                    fileListView.getSelectionModel().select(0);
                }

                if (success) {
                    infoLabel.setText(db.getFiles().size() + " file(s) added");
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

    private boolean isVideoFile(File file) {
        String name = file.getName().toLowerCase();
        return VIDEO_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private void setupMethodSelector() {
        btnManual.setSelected(true);
        updateUIForMethod(TrimMethod.MANUAL);
    }

    @FXML
    private void handleMethodChange() {
        if (btnManual.isSelected()) {
            currentMethod = TrimMethod.MANUAL;
        } else if (btnAudio.isSelected()) {
            currentMethod = TrimMethod.AUDIO_ANALYZER;
            // Auto-detect intro/outro when Audio mode is selected
            runAutoDetect();
        } else if (btnScene.isSelected()) {
            currentMethod = TrimMethod.SCENE_DETECTOR;
        }

        updateUIForMethod(currentMethod);
    }

    /**
     * Automatically detect intro/outro for ALL videos in the list
     */
    private void runAutoDetect() {
        List<File> files = new ArrayList<>(fileListView.getItems());
        if (files.isEmpty()) {
            infoLabel.setText("Add videos first!");
            return;
        }

        infoLabel.setText("Analyzing " + files.size() + " video(s)...");
        progressBar.setProgress(0);

        // Store detection results for each file
        java.util.Map<File, VolumeAnalyzer.IntroOutroResult> results = new java.util.concurrent.ConcurrentHashMap<>();

        Task<Void> batchTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                VolumeAnalyzer analyzer = new VolumeAnalyzer();
                
                for (int i = 0; i < files.size(); i++) {
                    File file = files.get(i);
                    final int index = i;
                    
                    Platform.runLater(() -> {
                        infoLabel.setText("Analyzing " + (index + 1) + "/" + files.size() + ": " + file.getName());
                    });

                    try {
                        VolumeAnalyzer.IntroOutroResult result = analyzer.detectIntroOutro(file.getAbsolutePath());
                        if (result != null) {
                            results.put(file, result);
                            
                            // Log to inspector
                            final String logEntry = String.format("✓ %s | Intro: 0-%ss | Outro: %s",
                                    file.getName().substring(0, Math.min(20, file.getName().length())),
                                    formatTime(result.introEnd),
                                    result.outroStart > 0 ? formatTime(result.outroStart) + "s" : "N/A");
                            Platform.runLater(() -> inspector.getItems().add(logEntry));
                        } else {
                            Platform.runLater(() -> inspector.getItems().add("✗ " + file.getName() + " - No detection"));
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to analyze: " + file.getName() + " - " + e.getMessage());
                        Platform.runLater(() -> inspector.getItems().add("✗ " + file.getName() + " - Error"));
                    }

                    // Update progress
                    final double progress = (double) (i + 1) / files.size();
                    Platform.runLater(() -> progressBar.setProgress(progress));
                }
                return null;
            }
        };

        batchTask.setOnSucceeded(e -> {
            progressBar.setProgress(1.0);
            
            int detected = results.size();
            infoLabel.setText("Analysis complete: " + detected + "/" + files.size() + " videos detected");

            // Apply result to currently selected video
            File selected = fileListView.getSelectionModel().getSelectedItem();
            if (selected != null && results.containsKey(selected)) {
                applyDetectionResult(results.get(selected));
            }

            // Store results for later use
            detectionResults = results;
        });

        batchTask.setOnFailed(e -> {
            progressBar.setProgress(0);
            infoLabel.setText("Analysis failed: " + batchTask.getException().getMessage());
        });

        new Thread(batchTask).start();
    }

    // Store detection results for all videos
    private java.util.Map<File, VolumeAnalyzer.IntroOutroResult> detectionResults = new java.util.HashMap<>();

    /**
     * Apply detection result to timeline
     */
    private void applyDetectionResult(VolumeAnalyzer.IntroOutroResult result) {
        timelineControl.setStartMarker(result.recommendedTrimStart);
        timelineControl.setEndMarker(result.recommendedTrimEnd);
        
        startTimeLabel.setText("START: " + formatTime(result.recommendedTrimStart));
        endTimeLabel.setText("END: " + formatTime(result.recommendedTrimEnd));
        
        String info = String.format("Intro: 0-%ss | Outro: %s | Trim: %s-%s",
                formatTime(result.introEnd),
                result.outroStart > 0 ? formatTime(result.outroStart) + "s" : "N/A",
                formatTime(result.recommendedTrimStart),
                formatTime(result.recommendedTrimEnd));
        infoLabel.setText(info);
    }

    private void updateUIForMethod(TrimMethod method) {
        boolean showImagePanels = (method == TrimMethod.MANUAL || method == TrimMethod.SCENE_DETECTOR);

        introImage.setVisible(showImagePanels);
        introImage.setManaged(showImagePanels);
        outroImage.setVisible(showImagePanels);
        outroImage.setManaged(showImagePanels);
    }

    private void setupTimelineControl() {
        timelineControl = new TimelineControl();
        timelineContainer.getChildren().clear();
        timelineContainer.getChildren().add(timelineControl);

        timelineControl.setOnStartMarkerChanged(() -> {
            double time = timelineControl.getStartMarker();
            startTimeLabel.setText("START: " + formatTime(time));
        });

        timelineControl.setOnEndMarkerChanged(() -> {
            double time = timelineControl.getEndMarker();
            endTimeLabel.setText("END: " + formatTime(time));
        });

        timelineControl.setOnSeek(() -> {
            if (mediaPlayer != null) {
                mediaPlayer.seek(Duration.seconds(timelineControl.getCurrentTime()));
            }
        });
    }

    @FXML
    private void handleImportButtonAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Videos");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.ts", "*.mkv", "*.avi", "*.mov")
        );
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(new Stage());

        if (selectedFiles != null) {
            for (File file : selectedFiles) {
                if (!fileListView.getItems().contains(file)) {
                    fileListView.getItems().add(file);
                }
            }

            if (!fileListView.getItems().isEmpty()) {
                fileListView.getSelectionModel().select(0);
            }

            infoLabel.setText(selectedFiles.size() + " file(s) added");
        }
    }

    // Waveform cache to avoid reloading
    private java.util.Map<String, double[]> waveformCache = new java.util.HashMap<>();

    private void playVideo(File file) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        String filePath = file.getAbsolutePath();

        Media media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);

        // SET VOLUME TO MAXIMUM
        mediaPlayer.setVolume(1.0);

        mediaPlayer.setOnReady(() -> {
            double duration = mediaPlayer.getMedia().getDuration().toSeconds();

            // Update timeline control
            timelineControl.setDuration(duration);

            // Apply cached detection results OR reset markers
            if (currentMethod == TrimMethod.AUDIO_ANALYZER && detectionResults.containsKey(file)) {
                VolumeAnalyzer.IntroOutroResult result = detectionResults.get(file);
                timelineControl.setStartMarker(result.recommendedTrimStart);
                timelineControl.setEndMarker(result.recommendedTrimEnd);
                startTimeLabel.setText("START: " + formatTime(result.recommendedTrimStart));
                endTimeLabel.setText("END: " + formatTime(result.recommendedTrimEnd));
            } else {
                timelineControl.setStartMarker(0);
                timelineControl.setEndMarker(duration);
                startTimeLabel.setText("START: 00:00");
                endTimeLabel.setText("END: " + formatTime(duration));
            }

            currentTimeLabel.setText("00:00 / " + formatTime(duration));

            // Load waveform - use cache if available
            if (waveformCache.containsKey(filePath)) {
                timelineControl.setWaveformData(waveformCache.get(filePath));
                infoLabel.setText("Ready");
            } else {
                infoLabel.setText("Loading waveform...");
                timelineControl.loadWaveform(filePath)
                        .thenAccept(waveformData -> Platform.runLater(() -> {
                            if (waveformData != null) {
                                waveformCache.put(filePath, waveformData);
                            }
                            infoLabel.setText("Ready");
                        }));
            }

            // Sync playback position
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                double current = newTime.toSeconds();
                timelineControl.setCurrentTime(current);
                currentTimeLabel.setText(formatTime(current) + " / " + formatTime(duration));
            });
        });

        // Don't auto-play - let user click play button
        mediaPlayer.pause();
    }

    private String formatTime(double seconds) {
        int total = (int) seconds;
        int h = total / 3600;
        int m = (total % 3600) / 60;
        int s = total % 60;
        if (h > 0) {
            return String.format("%d:%02d:%02d", h, m, s);
        }
        return String.format("%02d:%02d", m, s);
    }

    @FXML
    private void handlePlayPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.play();
            }
        }
    }

    @FXML
    private void handleForward() {
        if (mediaPlayer != null) {
            double current = mediaPlayer.getCurrentTime().toSeconds();
            double total = mediaPlayer.getTotalDuration().toSeconds();
            mediaPlayer.seek(Duration.seconds(Math.min(current + 10, total)));
        }
    }

    @FXML
    private void handleRewind() {
        if (mediaPlayer != null) {
            double current = mediaPlayer.getCurrentTime().toSeconds();
            mediaPlayer.seek(Duration.seconds(Math.max(current - 10, 0)));
        }
    }

    @FXML
    private void handleSetStart() {
        if (mediaPlayer != null) {
            double time = mediaPlayer.getCurrentTime().toSeconds();
            timelineControl.setStartMarker(time);
            startTimeLabel.setText("START: " + formatTime(time));
            infoLabel.setText("Start: " + formatTime(time));
        }
    }

    @FXML
    private void handleSetEnd() {
        if (mediaPlayer != null) {
            double time = mediaPlayer.getCurrentTime().toSeconds();
            timelineControl.setEndMarker(time);
            endTimeLabel.setText("END: " + formatTime(time));
            infoLabel.setText("End: " + formatTime(time));
        }
    }

    @FXML
    private void handleTrim() {
        File file = fileListView.getSelectionModel().getSelectedItem();
        if (file == null) {
            infoLabel.setText("Select a video first!");
            return;
        }

        String input = file.getAbsolutePath();
        String output = input.replace(".", "_cut.");
        TrimStrategy strategy = strategies.get(currentMethod);

        if (strategy == null) {
            infoLabel.setText("Method not implemented yet");
            return;
        }

        double start = timelineControl.getStartMarker();
        double end = timelineControl.getEndMarker();

        progressBar.setProgress(-1);
        infoLabel.setText("Processing...");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return strategy.trim(input, output, start, end);
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(1);
            infoLabel.setText(task.getValue() ? "Trim successful!" : "Trim failed.");
        });

        task.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            infoLabel.setText("ERROR: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    private void handleBatchTrim() {
        List<File> files = fileListView.getItems();
        TrimStrategy strategy = strategies.get(currentMethod);

        if (files.isEmpty()) {
            infoLabel.setText("Add files first!");
            return;
        }

        if (strategy == null) {
            infoLabel.setText("Method not implemented yet");
            return;
        }

        double start = timelineControl.getStartMarker();
        double end = timelineControl.getEndMarker();

        progressBar.setProgress(0);
        inspector.getItems().clear();

        ProgressUpdater updater = new ProgressUpdater(files.size(), (progress, file) -> {
            Platform.runLater(() -> {
                progressBar.setProgress(progress);
                inspector.getItems().add(file.getName() + " → done ✓");
                if (progress >= 1.0) {
                    infoLabel.setText("Batch trim complete!");
                }
            });
        });

        batchFacade.processAll(files, start, end, updater);
    }

    @FXML
    private void handleIntroClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Intro Reference Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File selectedFile = fileChooser.showOpenDialog(introImage.getScene().getWindow());

        if (selectedFile != null) {
            introImage.setImage(new Image(selectedFile.toURI().toString()));
        }
    }

    @FXML
    private void handleOutroClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Outro Reference Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File selectedFile = fileChooser.showOpenDialog(outroImage.getScene().getWindow());

        if (selectedFile != null) {
            outroImage.setImage(new Image(selectedFile.toURI().toString()));
        }
    }
}
