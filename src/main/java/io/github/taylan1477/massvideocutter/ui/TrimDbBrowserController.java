package io.github.taylan1477.massvideocutter.ui;

import io.github.taylan1477.massvideocutter.core.trimdb.TrimDbApiClient;
import io.github.taylan1477.massvideocutter.model.TrimRecipe;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Controller for the TrimDB Browser dialog.
 * Allows users to search, preview, and download trim recipes from the TrimDB server.
 */
public class TrimDbBrowserController {

    private static final Logger logger = LoggerFactory.getLogger(TrimDbBrowserController.class);

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Label statusLabel;
    @FXML private ListView<TrimRecipe> resultsListView;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Button applyButton;

    private final TrimDbApiClient apiClient = new TrimDbApiClient();
    private TrimRecipe selectedRecipe;

    @FXML
    private void initialize() {
        // Custom cell factory to display recipe info
        resultsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TrimRecipe recipe, boolean empty) {
                super.updateItem(recipe, empty);
                if (empty || recipe == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox box = new VBox(2);
                    box.setStyle("-fx-padding: 6;");

                    Label titleLabel = new Label("📺 " + recipe.getSeries());
                    titleLabel.getStyleClass().add("recipe-title");

                    String desc = recipe.getDescription() != null ? recipe.getDescription() : "";
                    String contributor = recipe.getContributor() != null ? recipe.getContributor() : "Anonymous";

                    Label detailLabel = new Label(
                            String.format("By: %s  |  Episodes: %d  |  %s",
                                    contributor, recipe.getEpisodeCount(), desc)
                    );
                    detailLabel.getStyleClass().add("recipe-detail");

                    box.getChildren().addAll(titleLabel, detailLabel);
                    setGraphic(box);
                }
            }
        });

        // Enable/disable apply button based on selection
        resultsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedRecipe = newVal;
            applyButton.setDisable(newVal == null);
        });

        // Allow searching with Enter key
        searchField.setOnAction(e -> handleSearch());
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();

        searchButton.setDisable(true);
        loadingIndicator.setVisible(true);
        statusLabel.setText("Searching...");
        resultsListView.getItems().clear();

        Task<List<TrimRecipe>> searchTask = new Task<>() {
            @Override
            protected List<TrimRecipe> call() throws Exception {
                return apiClient.searchRecipes(query.isEmpty() ? null : query);
            }
        };

        searchTask.setOnSucceeded(e -> {
            List<TrimRecipe> results = searchTask.getValue();
            loadingIndicator.setVisible(false);
            searchButton.setDisable(false);

            if (results.isEmpty()) {
                statusLabel.setText("No recipes found" + (query.isEmpty() ? "." : " for \"" + query + "\"."));
            } else {
                statusLabel.setText(results.size() + " recipe(s) found.");
                resultsListView.getItems().addAll(results);
            }
        });

        searchTask.setOnFailed(e -> {
            loadingIndicator.setVisible(false);
            searchButton.setDisable(false);
            Throwable ex = searchTask.getException();
            logger.error("TrimDB search failed", ex);

            if (ex != null && ex.getMessage() != null && ex.getMessage().contains("Connection refused")) {
                statusLabel.setText("⚠ Cannot connect to TrimDB server. Is it running?");
            } else {
                statusLabel.setText("⚠ Search failed: " + (ex != null ? ex.getMessage() : "Unknown error"));
            }
        });

        new Thread(searchTask, "trimdb-search").start();
    }

    /**
     * Returns the selected recipe so the caller (MainController) can apply it.
     */
    public TrimRecipe getSelectedRecipe() {
        return selectedRecipe;
    }

    private boolean applied = false;

    @FXML
    private void handleApply() {
        if (selectedRecipe != null) {
            applied = true;
            closeDialog();
        }
    }

    public boolean isApplied() {
        return applied;
    }

    @FXML
    private void handleClose() {
        applied = false;
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) searchField.getScene().getWindow();
        stage.close();
    }
}
