package com.example.massvideocutter.ui;

import com.example.massvideocutter.core.*;

import com.example.massvideocutter.core.ffmpeg.FFmpegWrapper;
import com.example.massvideocutter.util.ProgressUpdater;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.control.Label;

import javafx.scene.image.ImageView; // Doğru import
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class MainController {

    @FXML private Label infolabel;
    @FXML private ListView<File> fileListView;  // String -> File
    @FXML private ListView<String> inspector;
    @FXML private MenuBar menuBar;
    @FXML private ProgressBar progressBar;
    @FXML private Slider timelineSlider;
    @FXML private ChoiceBox<TrimMethod> choiceMethod;
    private Map<TrimMethod, TrimStrategy> strategies;
    @FXML private MediaView mediaView;
    private MediaPlayer mediaPlayer;
    @FXML private ImageView outroimage;
    @FXML private ImageView introimage;

    TrimFacade trimFacade = new TrimFacade();

    private boolean isSliderBeingDragged = false;
    private double startTimeInSec = 0;
    private double endTimeInSec = 0;
    private BatchProcessFacade batchFacade;

    @FXML
    public void initialize() {

        TaskManager taskManager = new TaskManager();
        this.trimFacade   = new TrimFacade();
        this.batchFacade  = new BatchProcessFacade(trimFacade, taskManager);

        // CellFactory: hücrede sadece dosya adı göster
        fileListView.setCellFactory(param -> new ListCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getName());
            }
        });

        // Seçilen dosyayı oynatma
        fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldFile, newFile) -> {
            if (newFile != null) {
                playVideo(newFile);
            }
        });

        // 1. ChoiceBox’a enum ekle
        choiceMethod.getItems().addAll(TrimMethod.values());
        choiceMethod.setValue(TrimMethod.MANUAL);

        double silenceThreshold   = -30.0;  // örnek dB eşiği
        double minSilenceDuration = 5.0;    // örnek saniye cinsinden
        FFmpegWrapper ffmpegWrapper = new FFmpegWrapper();

        strategies = Map.of(
                TrimMethod.MANUAL, new ManualTrimStrategy(trimFacade),
                TrimMethod.AUDIO_ANALYZER, new AudioAnalyzerStrategy(
                        trimFacade,
                        ffmpegWrapper,
                        new AudioAnalyzer(),      // burayı ekledik
                        silenceThreshold,
                        minSilenceDuration
                )
        );


        // Zaten slider init vs. burada kalabilir…
        timelineSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            isSliderBeingDragged = isChanging;
            if (!isChanging && mediaPlayer != null) {
                mediaPlayer.seek(Duration.seconds(timelineSlider.getValue()));
            }
        });

        timelineSlider.setOnMouseReleased(event -> {
            if (mediaPlayer != null) {
                mediaPlayer.seek(Duration.seconds(timelineSlider.getValue()));
            }
        });
    }

    @FXML
    private void handleImportButtonAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Videoları Seç");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Video Files", "*.mp4","*.ts", "*.mkv", "*.avi", "*.mov")
        );
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(new Stage());

        if (selectedFiles != null) {
            for (File file : selectedFiles) {
                if (!fileListView.getItems().contains(file)) {
                    fileListView.getItems().add(file);
                }
            }

            // İlk videoyu seç ve oynat
            if (!fileListView.getItems().isEmpty()) {
                fileListView.getSelectionModel().select(0);
            }
        }
    }

    // playVideo artık File alıyor
    private void playVideo(File file) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
        Media media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);

        mediaPlayer.setOnReady(() -> {
            Duration total = mediaPlayer.getMedia().getDuration();
            timelineSlider.setMax(total.toSeconds());
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (!isSliderBeingDragged) {
                    timelineSlider.setValue(newTime.toSeconds());
                }
            });
        });

        mediaPlayer.play();
    }

    // Play/Pause işlevi
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

    // Forward (ileri sarma) işlevi
    @FXML
    private void handleForward() {
        if (mediaPlayer != null) {
            double currentTime = mediaPlayer.getCurrentTime().toSeconds();
            double totalDuration = mediaPlayer.getTotalDuration().toSeconds();
            double newTime = Math.min(currentTime + 10, totalDuration);  // 10 saniye ileri sar
            mediaPlayer.seek(javafx.util.Duration.seconds(newTime));
        }
    }

    // Rewind (geri sarma) işlevi
    @FXML
    private void handleRewind() {
        if (mediaPlayer != null) {
            double currentTime = mediaPlayer.getCurrentTime().toSeconds();
            double newTime = Math.max(currentTime - 10, 0);  // 10 saniye geri sar
            mediaPlayer.seek(javafx.util.Duration.seconds(newTime));
        }
    }

    @FXML
    private void handleSetStart() {
        startTimeInSec = timelineSlider.getValue();
        System.out.println("Start set to: " + startTimeInSec + " seconds");
    }

    @FXML
    private void handleSetEnd() {
        endTimeInSec = timelineSlider.getValue();
        System.out.println("End set to: " + endTimeInSec + " seconds");
    }

    @FXML
    private void handleTrim() {
        File file = fileListView.getSelectionModel().getSelectedItem();
        if (file == null) {
            infolabel.setText("Önce video seç!");
            return;
        }

        String in  = file.getAbsolutePath();
        String out = in.replace(".", "_cut.");
        TrimMethod method = choiceMethod.getValue();
        TrimStrategy strategy = strategies.get(method);

        // BELİRSİZ PROGRESS BAŞLAT
        progressBar.setProgress(-1); // -> animasyon başlar
        infolabel.setText("İşleniyor...");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return strategy.trim(in, out, startTimeInSec, endTimeInSec);
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(1);
            boolean ok = task.getValue();
            infolabel.setText(ok ? "Trim başarılı!" : "Trim başarısız.");
        });

        task.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            infolabel.setText("HATA: " + task.getException().getMessage());
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }


    @FXML
    private void handleBatchTrim() {
        List<File> files = fileListView.getItems();
        TrimMethod method = choiceMethod.getValue();
        TrimStrategy strategy = strategies.get(method);

        if (files.isEmpty()) {
            infolabel.setText("Önce dosya seçmelisin!");
            return;
        }

        progressBar.setProgress(0);
        inspector.getItems().clear();   // Önceki logları temizle

        // 👉 ProgressUpdater yarat
        ProgressUpdater updater = new ProgressUpdater(files.size(), (progress, file) -> {
            Platform.runLater(() -> {
                progressBar.setProgress(progress);
                inspector.getItems().add(file.getName() + " → tamamlandı");
                if (progress >= 1.0) {
                    infolabel.setText("Toplu kırpma tamamlandı!");
                }
            });
        });
        // ✅ processAll’a ProgressUpdater ver
        batchFacade.processAll(files, startTimeInSec, endTimeInSec, updater);
    }

    @FXML
    private void handleIntroClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Intro Resmi Seç");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Dosyaları", "*.png")
        );
        File selectedFile = fileChooser.showOpenDialog(introimage.getScene().getWindow());

        if (selectedFile != null) {
            introimage.setImage(new Image(selectedFile.toURI().toString()));
        }
    }

    @FXML
    private void handleOutroClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Outro Resmi Seç");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Dosyaları", "*.png")
        );
        File selectedFile = fileChooser.showOpenDialog(outroimage.getScene().getWindow());

        if (selectedFile != null) {
            outroimage.setImage(new Image(selectedFile.toURI().toString()));
        }
    }

}
