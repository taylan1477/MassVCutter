package io.github.taylan1477.massvideocutter.ui;

import io.github.taylan1477.massvideocutter.util.AppSettings;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Controller for the Settings dialog.
 */
public class SettingsController {

    @FXML private TextField outputDirField;

    private final AppSettings settings = AppSettings.getInstance();

    @FXML
    public void initialize() {
        outputDirField.setText(settings.getOutputDirectory());
    }

    @FXML
    private void handleBrowseOutput() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Output Directory");

        // Start from current setting if exists
        String current = outputDirField.getText();
        if (current != null && !current.isEmpty()) {
            File dir = new File(current);
            if (dir.exists()) {
                chooser.setInitialDirectory(dir);
            }
        }

        File selected = chooser.showDialog(outputDirField.getScene().getWindow());
        if (selected != null) {
            outputDirField.setText(selected.getAbsolutePath());
        }
    }

    @FXML
    private void handleClearOutput() {
        outputDirField.setText("");
    }

    @FXML
    private void handleSave() {
        settings.setOutputDirectory(outputDirField.getText().trim());
        settings.save();
        closeDialog();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) outputDirField.getScene().getWindow();
        stage.close();
    }
}
