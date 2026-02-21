package cl.vss.cotizador.view;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;

public class CargaCotizacionProgressController {

    private final Button botonCargar;
    private final ComboBox<?> comboBrokers;
    private final ProgressBar progressBar;
    private final Label lblEstado;
    private final HBox vista;
    private boolean enCurso;

    public CargaCotizacionProgressController(Button botonCargar, ComboBox<?> comboBrokers) {
        this(botonCargar, comboBrokers, "Carga:", "Listo");
    }

    public CargaCotizacionProgressController(
        Button botonCargar,
        ComboBox<?> comboBrokers,
        String etiqueta,
        String estadoInicial
    ) {
        this.botonCargar = botonCargar;
        this.comboBrokers = comboBrokers;
        this.progressBar = new ProgressBar();
        this.lblEstado = new Label(estadoInicial);
        this.vista = new HBox(6, new Label(etiqueta), progressBar, lblEstado);
        this.enCurso = false;

        configurarControles();
    }

    private void configurarControles() {
        progressBar.setPrefWidth(240);
        progressBar.setPrefHeight(20);
        progressBar.setMinHeight(20);
        progressBar.setMaxHeight(20);
        progressBar.setMinWidth(240);
        progressBar.setProgress(0);
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        progressBar.setDisable(true);
        progressBar.setStyle("-fx-accent: #9AA0A6;");
        lblEstado.setStyle("-fx-text-fill: #555; -fx-font-size: 11px;");
        vista.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    }

    public HBox getVista() {
        return vista;
    }

    public boolean isEnCurso() {
        return enCurso;
    }

    public void iniciar(String mensaje) {
        enCurso = true;
        ejecutarEnFx(() -> aplicarEstado(true, mensaje));
    }

    public void completar(String mensaje) {
        enCurso = false;
        ejecutarEnFx(() -> aplicarEstado(false, mensaje));
    }

    public void error(String mensaje) {
        enCurso = false;
        ejecutarEnFx(() -> aplicarEstado(false, mensaje));
    }

    private void ejecutarEnFx(Runnable accion) {
        if (Platform.isFxApplicationThread()) {
            accion.run();
            return;
        }
        Platform.runLater(accion);
    }

    private void aplicarEstado(boolean enProceso, String mensaje) {
        if (botonCargar != null) {
            botonCargar.setDisable(enProceso);
        }
        if (comboBrokers != null) {
            comboBrokers.setDisable(enProceso);
        }

        progressBar.setDisable(!enProceso);
        progressBar.setProgress(enProceso ? ProgressIndicator.INDETERMINATE_PROGRESS : 0);
        progressBar.setStyle(enProceso ? "-fx-accent: #0A84FF;" : "-fx-accent: #9AA0A6;");
        lblEstado.setStyle(enProceso
            ? "-fx-text-fill: #0A84FF; -fx-font-size: 11px; -fx-font-weight: bold;"
            : "-fx-text-fill: #555; -fx-font-size: 11px;");
        lblEstado.setText(mensaje);
    }
}
