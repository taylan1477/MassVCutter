package com.example.massvideocutter.ui;

import com.example.massvideocutter.core.ManualTrimHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.*;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;


public class MainController {

    @FXML private Button btnExport, btnForward, btnPlayPause, btnRewind, btnSetEnd, btnSetStart;
    @FXML private ListView<String> fileListView;
    @FXML private ListView<String> inspector;
    @FXML private MenuBar menuBar;
    @FXML private ProgressBar progressBar;
    @FXML private Slider timelineSlider;
    @FXML private MediaView mediaView;  // <-- MediaView burada!

    private ManualTrimHandler trimHandler = new ManualTrimHandler();

    @FXML
    public void initialize() {
        btnSetStart.setOnAction(e -> setTrimPoint(true));
        btnSetEnd.setOnAction(e -> setTrimPoint(false));
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
