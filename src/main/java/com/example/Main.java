package com.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("Cotizador JavaFX - Proyecto de Prueba");

        Label label = new Label("Bienvenido al Cotizador!");
        Button button = new Button("Hacer clic aqu&amp;acute;   &iacute;");
        
        button.setOnAction(e -> {
            label.setText("¡Botón presionado! JavaFX funciona correctamente.");
        });

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        vbox.getChildren().addAll(label, button);

        Scene scene = new Scene(vbox, 400, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Configurar UTF-8 para caracteres especiales
        System.setProperty("file.encoding", "UTF-8");
        launch(args);
    }
}