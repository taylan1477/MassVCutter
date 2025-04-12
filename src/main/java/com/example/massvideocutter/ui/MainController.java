package com.example.massvideocutter.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;


public class MainController {

    @FXML private Button btnExport, btnForward, btnPlayPause, btnRewind, btnSetEnd, btnSetStart;
    @FXML private ListView<String> fileListView;
    @FXML private ListView<String> inspector;
    @FXML private MenuBar menuBar;
    @FXML private ProgressBar progressBar;
    @FXML private Slider timelineSlider;
}
