package cl.vss.cotizador.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Clase para gestionar la información de versión de la aplicación.
 * Muestra un diálogo con la versión actual, estado (SNAPSHOT/Release),
 * desarrolladores y fecha.
 */
public class Version {
    
    // Información de versión
    private static final String VERSION = "1.0";
    private static final boolean IS_SNAPSHOT = true; // Cambiar a false para versión de producción
    private static final String[] DEVELOPERS = {"CSM", "ASM"};
    private static final LocalDate RELEASE_DATE = LocalDate.now();
    
    /**
     * Obtiene la versión completa del sistema
     * @return String con la versión (ej: "1.0-SNAPSHOT" o "1.0")
     */
    public static String getVersion() {
        return VERSION + (IS_SNAPSHOT ? "-SNAPSHOT" : "");
    }
    
    /**
     * Indica si es una versión SNAPSHOT
     * @return true si es SNAPSHOT, false si es release
     */
    public static boolean isSnapshot() {
        return IS_SNAPSHOT;
    }
    
    /**
     * Obtiene los desarrolladores
     * @return Array con los nombres de los desarrolladores
     */
    public static String[] getDevelopers() {
        return DEVELOPERS;
    }
    
    /**
     * Obtiene la fecha formateada
     * @return String con la fecha en formato dd/MM/yyyy
     */
    public static String getFormattedDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return RELEASE_DATE.format(formatter);
    }
    
    /**
     * Muestra un diálogo modal con la información de versión
     * @param owner Stage padre del diálogo
     */
    public static void mostrarDialogoVersion(Stage owner) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("Acerca de - Cotizador VSS");
        dialog.setResizable(false);
        
        // Título
        Label lblTitulo = new Label("Sistema de Cotización VSS");
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        // Versión
        Label lblVersion = new Label("Versión: " + getVersion());
        lblVersion.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; " + 
                           (IS_SNAPSHOT ? "-fx-text-fill: #e67e22;" : "-fx-text-fill: #27ae60;"));
        
        // Estado
        Label lblEstado = new Label(IS_SNAPSHOT ? "⚠️ Versión en Desarrollo" : "✅ Versión de Producción");
        lblEstado.setStyle("-fx-font-size: 12px; -fx-font-style: italic; " +
                          (IS_SNAPSHOT ? "-fx-text-fill: #e67e22;" : "-fx-text-fill: #27ae60;"));
        
        // Desarrolladores
        Label lblDevTitle = new Label("Desarrolladores:");
        lblDevTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #34495e; -fx-padding: 10 0 5 0;");
        
        Label lblDevelopers = new Label(String.join(" & ", DEVELOPERS));
        lblDevelopers.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        
        // Fecha
        Label lblFecha = new Label("Fecha: " + getFormattedDate());
        lblFecha.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6; -fx-padding: 10 0 0 0;");
        
        // Botón cerrar
        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; " +
                          "-fx-padding: 8 20; -fx-cursor: hand;");
        btnCerrar.setOnAction(e -> dialog.close());
        
        // Hover effect para el botón
        btnCerrar.setOnMouseEntered(e -> btnCerrar.setStyle(
            "-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-padding: 8 20; -fx-cursor: hand;"));
        btnCerrar.setOnMouseExited(e -> btnCerrar.setStyle(
            "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-padding: 8 20; -fx-cursor: hand;"));
        
        // Layout
        VBox layout = new VBox(10);
        layout.getChildren().addAll(
            lblTitulo,
            lblVersion,
            lblEstado,
            lblDevTitle,
            lblDevelopers,
            lblFecha,
            btnCerrar
        );
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(25));
        layout.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; " +
                       "-fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        Scene scene = new Scene(layout, 380, 280);
        dialog.setScene(scene);
        dialog.centerOnScreen();
        dialog.showAndWait();
    }
}
