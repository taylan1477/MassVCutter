package com.example.massvideocutter.ui;

import com.example.massvideocutter.core.ManualTrimHandler;
import com.example.massvideocutter.core.ffmpeg.FFmpegWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.List;


public class MainController {

    @FXML private Button btnExport, btnImport, btnForward, btnPlayPause, btnRewind, btnSetEnd, btnSetStart;
    @FXML private ListView<File> fileListView;  // String -> File
    @FXML private ListView<String> inspector;
    @FXML private MenuBar menuBar;
    @FXML private ProgressBar progressBar;
    @FXML private Slider timelineSlider;
    @FXML private MediaView mediaView;
    private MediaPlayer mediaPlayer;

    FFmpegWrapper ffmpegWrapper = new FFmpegWrapper();

    private boolean isSliderBeingDragged = false;
    private double startTimeInSec = 0;
    private double endTimeInSec = 0;

    @FXML
    public void initialize() {
        // 2) CellFactory: hücrede sadece dosya adı göster
        fileListView.setCellFactory(param -> new ListCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getName());
            }
        });

        // 3) Seçilen dosyayı oynatma
        fileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldFile, newFile) -> {
            if (newFile != null) {
                playVideo(newFile);
            }
        });

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

    // 4) playVideo artık File alıyor
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
        File selectedFile = fileListView.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            System.out.println("Trim işlemi için bir video seçilmedi.");
            return;
        }

        String inputPath = selectedFile.getAbsolutePath();
        String outputPath = inputPath.replace(".mp4", "_cut.mp4"); // Gerekirse uzantı kontrolü yap

        String start = formatSeconds(startTimeInSec);
        String duration = formatSeconds(endTimeInSec - startTimeInSec);

        boolean success = ffmpegWrapper.trimVideo(inputPath, outputPath, start, duration);
        if (success) {
            System.out.println("Kırpma başarılı!");
        } else {
            System.out.println("Kırpma başarısız.");
        }
    }

    private String formatSeconds(double seconds) {
        int total = (int) seconds;
        int h = total / 3600;
        int m = (total % 3600) / 60;
        int s = total % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
