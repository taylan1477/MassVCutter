package io.github.taylan1477.massvideocutter.ui;

import io.github.taylan1477.massvideocutter.core.*;
import io.github.taylan1477.massvideocutter.core.ffmpeg.FFmpegWrapper;
import io.github.taylan1477.massvideocutter.core.trimdb.EpisodeMatcher;
import io.github.taylan1477.massvideocutter.core.trimdb.RecipeManager;
import io.github.taylan1477.massvideocutter.model.EpisodeTrim;
import io.github.taylan1477.massvideocutter.model.TrimRecipe;
import io.github.taylan1477.massvideocutter.util.AppSettings;
import io.github.taylan1477.massvideocutter.util.ProgressUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

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
    @FXML private Button btnPlayPause;

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
        setupInspector();
        setupDragAndDrop();
        setupMethodSelector();
        setupTimelineControl();
        setupKeyboardShortcuts();
    }

    private void setupFileListView() {
        fileListView.setCellFactory(param -> new ListCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setContextMenu(null);
                } else {
                    setText(item.getName());
                    
                    ContextMenu contextMenu = new ContextMenu();
                    MenuItem deleteItem = new MenuItem("Remove from List (Q/Del)");
                    deleteItem.setOnAction(e -> {
                        fileListView.getItems().remove(item);
                        infoLabel.setText("Removed: " + item.getName());
                    });
                    
                    MenuItem openLocItem = new MenuItem("Open File Location");
                    openLocItem.setOnAction(e -> {
                        try {
                            java.awt.Desktop.getDesktop().open(item.getParentFile());
                        } catch (Exception ex) {
                            logger.error("Failed to open file location", ex);
                        }
                    });
                    
                    contextMenu.getItems().addAll(deleteItem, new SeparatorMenuItem(), openLocItem);
                    setContextMenu(contextMenu);
                }
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

    private void setupInspector() {
        inspector.setCellFactory(param -> new ListCell<String>() {
            private final javafx.scene.control.Label label = new javafx.scene.control.Label();
            {
                label.setWrapText(true);
                // Keep style consistent with the theme, use padding to adjust height and alignment
                label.setStyle("-fx-text-fill: #cccccc; -fx-padding: 8px 5px;");
                // Bind width to ListView width so the text knows when to wrap
                label.prefWidthProperty().bind(inspector.widthProperty().subtract(24));
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    label.setText(item);
                    setGraphic(label);
                    setText(null);
                    // Force minimum height to ensure it fits ~2 lines
                    setMinHeight(50.0);
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
            infoLabel.setText("Scene Detection is coming in v1.1 \u2014 use Manual or Audio mode for now");
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
                        logger.error("Failed to analyze: {} - {}", file.getName(), e.getMessage());
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
        
        String lastDirStr = AppSettings.getInstance().getLastOpenedDirectory();
        if (lastDirStr != null && !lastDirStr.isEmpty()) {
            File lastDir = new File(lastDirStr);
            if (lastDir.exists() && lastDir.isDirectory()) {
                fileChooser.setInitialDirectory(lastDir);
            }
        }
        
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(new Stage());

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            AppSettings.getInstance().setLastOpenedDirectory(selectedFiles.get(0).getParentFile().getAbsolutePath());
            AppSettings.getInstance().save();
            
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

        try {
            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
        } catch (Exception e) {
            logger.error("Failed to load video: {}", file.getName(), e);
            infoLabel.setText("Error: Could not load " + file.getName());
            return;
        }

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

            // Update Play/Pause button UI when state changes
            mediaPlayer.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                if (btnPlayPause != null) {
                    if (newStatus == MediaPlayer.Status.PLAYING) {
                        btnPlayPause.setText("⏸"); // Pause symbol
                    } else {
                        btnPlayPause.setText("▶"); // Play symbol
                    }
                }
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
        String output = AppSettings.getInstance().getOutputPath(input);
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

    @FXML
    private void handleAbout() {
        Alert about = new Alert(Alert.AlertType.NONE);
        about.setTitle("About MassVCutter");
        about.setHeaderText("MassVCutter v0.6.0");

        // Custom content with clickable GitHub link
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(8);
        content.setPadding(new javafx.geometry.Insets(10));

        Label titleLabel = new Label("MassVCutter");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #FFA500;");

        Label versionLabel = new Label("Version 0.6.0");
        versionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #cccccc;");

        Label descLabel = new Label("Mass Video Cutter — Batch trim tool with\naudio-based intro/outro detection for anime.");
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaaaaa;");
        descLabel.setWrapText(true);

        Label devLabel = new Label("Developer: Taylan Özgür Özdemir");
        devLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #cccccc;");

        javafx.scene.control.Hyperlink githubLink = new javafx.scene.control.Hyperlink("github.com/taylan1477/MassVCutter");
        githubLink.setStyle("-fx-font-size: 12px; -fx-text-fill: #00BFFF;");
        githubLink.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI("https://github.com/taylan1477/MassVCutter"));
            } catch (Exception ex) {
                logger.error("Failed to open GitHub link", ex);
            }
        });

        Label licenseLabel = new Label("License: MIT");
        licenseLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

        Label techLabel = new Label("Built with Java 23 • JavaFX 23 • FFmpeg");
        techLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

        content.getChildren().addAll(titleLabel, versionLabel, descLabel, 
                new javafx.scene.control.Separator(),
                devLabel, githubLink,
                new javafx.scene.control.Separator(),
                licenseLabel, techLabel);

        about.getDialogPane().setContent(content);
        about.getDialogPane().setPrefWidth(360);
        about.getButtonTypes().add(ButtonType.CLOSE);

        // Apply dark styling to match the app
        about.getDialogPane().setStyle(
                "-fx-background-color: #2b2b2b;" +
                "-fx-border-color: #FFA500; -fx-border-width: 1px;"
        );
        about.getDialogPane().lookup(".header-panel").setStyle(
                "-fx-background-color: #1a1a1a;"
        );

        about.showAndWait();
    }

    // ============================
    // Keyboard Shortcuts
    // ============================

    private void setupKeyboardShortcuts() {
        // Attach after scene is available
        Platform.runLater(() -> {
            Scene scene = menuBar.getScene();
            if (scene == null) return;

            scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                // Don't intercept if a text field is focused
                if (scene.getFocusOwner() instanceof TextField) return;

                boolean shift = event.isShiftDown();

                switch (event.getCode()) {
                    case SPACE -> {
                        handlePlayPause();
                        event.consume();
                    }
                    case ENTER -> {
                        if (shift) {
                            handleTrim();
                        } else {
                            handleBatchTrim();
                        }
                        event.consume();
                    }
                    case W -> {
                        handleSetStart();
                        event.consume();
                    }
                    case S -> {
                        if (!event.isControlDown()) { // Don't capture Ctrl+S
                            handleSetEnd();
                            event.consume();
                        }
                    }
                    case A -> {
                        if (!event.isControlDown()) {
                            seekRelative(shift ? -30 : -5);
                            event.consume();
                        }
                    }
                    case D -> {
                        seekRelative(shift ? 30 : 5);
                        event.consume();
                    }
                    case LEFT -> {
                        seekRelative(shift ? -30 : -5);
                        event.consume();
                    }
                    case RIGHT -> {
                        seekRelative(shift ? 30 : 5);
                        event.consume();
                    }
                    case Q, DELETE -> {
                        removeSelectedFile();
                        event.consume();
                    }
                    default -> {}
                }
            });

            logger.info("Keyboard shortcuts registered");
        });
    }

    private void seekRelative(double seconds) {
        if (mediaPlayer == null) return;

        double currentSec = mediaPlayer.getCurrentTime().toSeconds();
        double targetSec = Math.max(0, Math.min(currentSec + seconds, mediaPlayer.getTotalDuration().toSeconds()));

        mediaPlayer.seek(Duration.seconds(targetSec));
        timelineControl.setCurrentTime(targetSec);

        logger.debug("Seek: {}s → {}s", String.format("%.1f", currentSec), String.format("%.1f", targetSec));
    }

    private void removeSelectedFile() {
        File selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            fileListView.getItems().remove(selected);
            infoLabel.setText("Removed: " + selected.getName());
            logger.debug("Removed file: {}", selected.getName());

            // If no files left, clear player
            if (fileListView.getItems().isEmpty() && mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
        }
    }

    // ============================
    // Settings Dialog
    // ============================

    @FXML
    private void handleSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/io/github/taylan1477/massvideocutter/settings.fxml")));
            Parent root = loader.load();

            Stage settingsStage = new Stage();
            settingsStage.setTitle("Settings — MassVCutter");
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.initOwner(menuBar.getScene().getWindow());

            Scene scene = new Scene(root);
            // Apply same stylesheet
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/io/github/taylan1477/massvideocutter/css/style.css")).toExternalForm());

            settingsStage.setScene(scene);
            settingsStage.setResizable(false);
            settingsStage.showAndWait();
        } catch (Exception e) {
            logger.error("Failed to open Settings dialog", e);
        }
    }

    @FXML
    private void handleExportRecipe() {
        Map<File, VolumeAnalyzer.IntroOutroResult> exportData = new java.util.HashMap<>(detectionResults);

        // Fallback: If empty, use the current manual trim on the active video
        if (exportData.isEmpty()) {
            File selectedFile = fileListView.getSelectionModel().getSelectedItem();
            if (selectedFile != null && mediaPlayer != null) {
                double duration = mediaPlayer.getTotalDuration().toSeconds();
                double start = timelineControl.getStartMarker();
                double end = timelineControl.getEndMarker();
                if (end == 0) end = duration;
                
                VolumeAnalyzer.IntroOutroResult mockResult = new VolumeAnalyzer.IntroOutroResult(
                    0, start, end, duration, start, end, duration
                );
                exportData.put(selectedFile, mockResult);
            }
        }

        if (exportData.isEmpty()) {
            infoLabel.setText("No analysis results or active video to export.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/taylan1477/massvideocutter/export_dialog.fxml"));
            Parent root = loader.load();
            
            ExportDialogController controller = loader.getController();
            controller.initData(exportData);

            Stage stage = new Stage();
            stage.setTitle("Export Trim Recipe");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            
            // Set dark theme
            stage.getScene().getStylesheets().add(Objects.requireNonNull(getClass().getResource("/io/github/taylan1477/massvideocutter/css/style.css")).toExternalForm());
            
            stage.showAndWait();

            if (controller.isConfirmed()) {
                TrimRecipe recipe = controller.getRecipe();
                
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save Trim Recipe");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Trim Recipe (*.trimrecipe)", "*.trimrecipe"));
                fileChooser.setInitialFileName(recipe.getSeries().replaceAll("[^a-zA-Z0-9.-]", "_") + ".trimrecipe");
                
                File dest = fileChooser.showSaveDialog(fileListView.getScene().getWindow());
                if (dest != null) {
                    RecipeManager manager = new RecipeManager();
                    manager.exportRecipe(dest, recipe);
                    infoLabel.setText("Recipe exported successfully!");
                }
            }
        } catch (Exception e) {
            logger.error("Failed to export recipe", e);
            infoLabel.setText("Error exporting recipe.");
        }
    }

    @FXML
    private void handleImportRecipe() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Trim Recipe");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Trim Recipe (*.trimrecipe)", "*.trimrecipe"),
                new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json")
        );

        File source = fileChooser.showOpenDialog(fileListView.getScene().getWindow());
        if (source != null) {
            try {
                RecipeManager manager = new RecipeManager();
                TrimRecipe recipe = manager.importRecipe(source);
                EpisodeMatcher matcher = new EpisodeMatcher();
                
                int matchedCount = 0;
                
                for (File file : fileListView.getItems()) {
                    try {
                        double duration = VolumeAnalyzer.getVideoDuration(file.getAbsolutePath());
                        var matchOpt = matcher.match(file, duration, recipe.getEpisodes());
                        
                        if (matchOpt.isPresent()) {
                            EpisodeTrim matchedEp = matchOpt.get();
                            
                            // Convert back to IntroOutroResult format for detectionResults
                            double introStart = matchedEp.getIntroStart() != null ? matchedEp.getIntroStart() : -1;
                            double introEnd = matchedEp.getIntroEnd() != null ? matchedEp.getIntroEnd() : -1;
                            double outroStart = matchedEp.getOutroStart() != null ? matchedEp.getOutroStart() : -1;
                            double outroEnd = matchedEp.getOutroEnd() != null ? matchedEp.getOutroEnd() : -1;
                            
                            double trimStart = (introEnd != -1) ? introEnd : 0;
                            double trimEnd = (outroStart != -1) ? outroStart : duration;
                            
                            VolumeAnalyzer.IntroOutroResult result = new VolumeAnalyzer.IntroOutroResult(
                                introStart, introEnd, outroStart, outroEnd, trimStart, trimEnd, duration
                            );
                            
                            detectionResults.put(file, result);
                            inspector.getItems().add(file.getName() + ": Matched Ep " + matchedEp.getEp());
                            matchedCount++;
                            
                            // If this is the currently selected file, apply the new result immediately
                            if (file.equals(fileListView.getSelectionModel().getSelectedItem())) {
                                applyDetectionResult(result);
                            }
                        }
                    } catch (Exception ex) {
                        logger.error("Error matching file: {}", file.getName(), ex);
                    }
                }
                
                infoLabel.setText(String.format("Imported recipe: Matched %d/%d files.", matchedCount, fileListView.getItems().size()));
                currentMethod = TrimMethod.AUDIO_ANALYZER; // Switch to audio analyzer mode to view results
                methodGroup.selectToggle(btnAudio);
                
            } catch (Exception e) {
                logger.error("Failed to import recipe", e);
                infoLabel.setText("Error importing recipe.");
            }
        }
    }
}
