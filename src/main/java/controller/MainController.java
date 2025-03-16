package main.java.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import main.java.service.VideoService;

public class MainController {
    @FXML
    private Button startButton;

    private final VideoService videoService = new VideoService();

    @FXML
    public void handleStartButtonClick() {
        videoService.processVideo("input.mp4");
    }
}