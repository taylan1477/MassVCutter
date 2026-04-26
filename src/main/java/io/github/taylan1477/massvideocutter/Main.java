package io.github.taylan1477.massvideocutter;

import io.github.taylan1477.massvideocutter.core.ffmpeg.FFmpegWrapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Objects;

public class Main extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Install global exception handler for JavaFX thread
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            logger.error("Uncaught exception on JavaFX thread", throwable);
            showErrorDialog(throwable);
        });

        // Install global exception handler for background threads
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            logger.error("Uncaught exception on thread: {}", thread.getName(), throwable);
            Platform.runLater(() -> showErrorDialog(throwable));
        });

        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/io/github/taylan1477/massvideocutter/menu.fxml")));
        primaryStage.setTitle("MassVCutter v0.6.0");

        // Create scene
        Scene scene = new Scene(root);

        // Load CSS stylesheet
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/io/github/taylan1477/massvideocutter/css/style.css")).toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();

        logger.info("MassVCutter v0.6.0 started successfully");

        // Check FFmpeg availability after UI is shown
        checkFFmpegAvailability();
    }

    /**
     * Check if FFmpeg is available and show warning dialog if not.
     */
    private void checkFFmpegAvailability() {
        if (!FFmpegWrapper.isAvailable()) {
            logger.warn("FFmpeg not found at: {}", FFmpegWrapper.getExecutablePath());

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("FFmpeg Not Found");
            alert.setHeaderText("FFmpeg is required but was not found");
            alert.setContentText(
                    """
                            MassVCutter requires FFmpeg to process videos.
                            Please install FFmpeg and ensure it is accessible."""
            );

            ButtonType downloadBtn = new ButtonType("Download FFmpeg");
            ButtonType closeBtn = new ButtonType("I'll fix it later", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(downloadBtn, closeBtn);

            alert.showAndWait().ifPresent(response -> {
                if (response == downloadBtn) {
                    try {
                        Desktop.getDesktop().browse(new URI("https://ffmpeg.org/download.html"));
                    } catch (Exception e) {
                        logger.error("Failed to open FFmpeg download page", e);
                    }
                }
            });
        } else {
            logger.info("FFmpeg found at: {}", FFmpegWrapper.getExecutablePath());
        }
    }

    /**
     * Show error dialog for uncaught exceptions.
     */
    private void showErrorDialog(Throwable throwable) {
        try {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Unexpected Error");
            alert.setHeaderText("Something went wrong");
            alert.setContentText(throwable.getMessage() != null
                    ? throwable.getMessage()
                    : throwable.getClass().getSimpleName());

            // Expandable stack trace
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            String stackTrace = sw.toString();

            TextArea textArea = new TextArea(stackTrace);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(textArea, 0, 0);

            alert.getDialogPane().setExpandableContent(expContent);
            alert.showAndWait();
        } catch (Exception e) {
            // Last resort - don't let error dialog itself crash
            logger.error("Failed to show error dialog", e);
        }
    }

    public static void main(String[] args) {
        System.setProperty("jdk.disableAttachMechanism", "true");
        logger.info("Starting MassVCutter...");
        launch(args);
    }
}