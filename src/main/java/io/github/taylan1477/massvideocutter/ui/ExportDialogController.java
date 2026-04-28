package io.github.taylan1477.massvideocutter.ui;

import io.github.taylan1477.massvideocutter.core.VolumeAnalyzer;
import io.github.taylan1477.massvideocutter.core.trimdb.EpisodeMatcher;
import io.github.taylan1477.massvideocutter.model.EpisodeTrim;
import io.github.taylan1477.massvideocutter.model.TrimRecipe;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExportDialogController {

    @FXML private TextField seriesNameField;
    @FXML private TextField descriptionField;
    @FXML private TextField contributorField;
    @FXML private Label statsLabel;

    private boolean confirmed = false;
    private TrimRecipe generatedRecipe;

    public void initData(Map<File, VolumeAnalyzer.IntroOutroResult> detectionResults) {
        // Build the recipe from detection results
        generatedRecipe = new TrimRecipe();
        List<EpisodeTrim> episodes = new ArrayList<>();

        for (Map.Entry<File, VolumeAnalyzer.IntroOutroResult> entry : detectionResults.entrySet()) {
            File file = entry.getKey();
            VolumeAnalyzer.IntroOutroResult result = entry.getValue();
            
            Double duration = result.videoDuration;
            if (duration == null || duration <= 0) continue; // Duration is required for matching
            
            int epNum = EpisodeMatcher.extractEpisodeNumber(file.getName());
            if (epNum == -1) epNum = episodes.size() + 1; // Fallback to index if regex fails

            EpisodeTrim ep = new EpisodeTrim(epNum, duration);
            
            if (result.introStart != -1 && result.introEnd != -1) {
                ep.setIntroStart(result.introStart);
                ep.setIntroEnd(result.introEnd);
            }
            if (result.outroStart != -1 && result.outroEnd != -1) {
                ep.setOutroStart(result.outroStart);
                ep.setOutroEnd(result.outroEnd);
            }
            
            episodes.add(ep);
        }

        generatedRecipe.setEpisodes(episodes);
        statsLabel.setText("Episodes to export: " + episodes.size() + "\nFormat: JSON");
    }

    @FXML
    private void handleExport() {
        if (seriesNameField.getText().trim().isEmpty()) {
            seriesNameField.setStyle("-fx-border-color: red;");
            return;
        }

        generatedRecipe.setSeries(seriesNameField.getText().trim());
        generatedRecipe.setDescription(descriptionField.getText().trim());
        generatedRecipe.setContributor(contributorField.getText().trim().isEmpty() ? "Anonymous" : contributorField.getText().trim());
        
        confirmed = true;
        closeDialog();
    }

    @FXML
    private void handleCancel() {
        confirmed = false;
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) seriesNameField.getScene().getWindow();
        stage.close();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public TrimRecipe getRecipe() {
        return generatedRecipe;
    }
}
