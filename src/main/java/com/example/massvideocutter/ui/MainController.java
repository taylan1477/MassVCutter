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
import java.io.File;


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

    @FXML
    public void initialize() {
        btnSetStart.setOnAction(e -> setTrimPoint(true));
        btnSetEnd.setOnAction(e -> setTrimPoint(false));
    }

    // Video dosyasını seçme işlevi
    @FXML
    private void handleImportButtonAction() {
        // FileChooser nesnesi oluştur
        FileChooser fileChooser = new FileChooser();

        // Dosya uzantılarını filtrele
        FileChooser.ExtensionFilter videoFilter = new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.mkv", "*.avi", "*.mov");
        fileChooser.getExtensionFilters().add(videoFilter);

        // Dosya seçme penceresini göster
        File selectedFile = fileChooser.showOpenDialog(new Stage());

        if (selectedFile != null) {
            // Seçilen dosyayı Media nesnesine dönüştür
            Media media = new Media(selectedFile.toURI().toString());

            // MediaPlayer nesnesini oluştur
            if (mediaPlayer != null) {
                mediaPlayer.stop(); // Eğer önceden bir video oynatılıyorsa durdur
            }

            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer); // MediaPlayer'ı MediaView'a bağla

            // Videoyu başlat
            mediaPlayer.play();
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

    private void setTrimPoint(boolean isStart) {
        String selectedFile = fileListView.getSelectionModel().getSelectedItem();
        if (selectedFile == null || mediaView.getMediaPlayer() == null) return;

        MediaPlayer player = mediaView.getMediaPlayer();
        double currentTime = player.getCurrentTime().toSeconds();

        if (isStart) {
            trimHandler.setStartTime(selectedFile, currentTime);
        } else {
            trimHandler.setEndTime(selectedFile, currentTime);
        }

        // Güncelleme için inspector ListView'a yazdır
        inspector.getItems().removeIf(item -> item.startsWith(selectedFile));
        ManualTrimHandler.TrimPoints points = trimHandler.getTrimPoints(selectedFile);
        inspector.getItems().add(selectedFile + " -> " + points);
    }
}
