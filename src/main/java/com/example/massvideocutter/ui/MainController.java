package com.example.massvideocutter.ui;

import com.example.massvideocutter.core.ManualTrimHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
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
    @FXML private ListView<String> fileListView;
    @FXML private ListView<String> inspector;
    @FXML private MenuBar menuBar;
    @FXML private ProgressBar progressBar;
    @FXML private Slider timelineSlider;
    @FXML private MediaView mediaView;
    private MediaPlayer mediaPlayer;

    private final ManualTrimHandler trimHandler = new ManualTrimHandler();

    private boolean isSliderBeingDragged = false;
    private double startTimeInSec = 0;
    private double endTimeInSec = 0;

    @FXML
    public void initialize() {

        // Slider sürüklenme olaylarını burada ayarlayabilirsiniz (mediaPlayer'a bağlı değil)
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
        FileChooser.ExtensionFilter videoFilter = new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.mkv", "*.avi", "*.mov");
        fileChooser.getExtensionFilters().add(videoFilter);
        File selectedFile = fileChooser.showOpenDialog(new Stage());

        if (selectedFile != null) {
            Media media = new Media(selectedFile.toURI().toString());

            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }

            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);

            // MEDIAPLAYER HAZIR OLDUĞUNDA YAPILACAKLAR
            mediaPlayer.setOnReady(() -> {
                Duration total = mediaPlayer.getMedia().getDuration();
                timelineSlider.setMax(total.toSeconds());

                // Zamanlayıcıyı mediaPlayer'a bağla
                mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                    if (!isSliderBeingDragged) {
                        timelineSlider.setValue(newTime.toSeconds());
                    }
                });
            });

            mediaPlayer.play();
        }
        fileChooser.setTitle("Videoları Seç");
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(new Stage());

        if (selectedFiles != null) {
            for (File file : selectedFiles) {
                String filePath = file.toURI().toString();
                if (!fileListView.getItems().contains(filePath)) {
                    fileListView.getItems().add(filePath);
                }
            }
        }
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
    private void onSetStartClicked() {
        startTimeInSec = timelineSlider.getValue();
        System.out.println("Start set to: " + startTimeInSec + " seconds");
    }

    @FXML
    private void onSetEndClicked() {
        endTimeInSec = timelineSlider.getValue();
        System.out.println("End set to: " + endTimeInSec + " seconds");
    }
}
