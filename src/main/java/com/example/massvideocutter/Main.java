package com.example.massvideocutter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/example/massvideocutter/menu.fxml")));
        primaryStage.setTitle("MassVCutter");

        // Scene oluşturma
        Scene scene = new Scene(root);

        // CSS dosyasını ekleme
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/example/massvideocutter/css/style.css")).toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        System.setProperty("jdk.disableAttachMechanism", "true");
        launch(args);
    }
}