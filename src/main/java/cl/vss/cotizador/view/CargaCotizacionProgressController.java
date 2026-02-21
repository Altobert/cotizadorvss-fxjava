package cl.vss.cotizador.view;

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
        this.botonCargar = botonCargar;
        this.comboBrokers = comboBrokers;
        this.progressBar = new ProgressBar();
        this.lblEstado = new Label("Listo");
        this.vista = new HBox(6, new Label("Carga:"), progressBar, lblEstado);
        this.enCurso = false;

        configurarControles();
    }

    private void configurarControles() {
        progressBar.setPrefWidth(240);
        progressBar.setPrefHeight(20);
        progressBar.setMinHeight(20);
        progressBar.setMaxHeight(20);
        progressBar.setProgress(0);
        progressBar.setVisible(false);
        progressBar.setManaged(false);
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
        aplicarEstado(true, mensaje);
    }

    public void completar(String mensaje) {
        enCurso = false;
        aplicarEstado(false, mensaje);
    }

    public void error(String mensaje) {
        enCurso = false;
        aplicarEstado(false, mensaje);
    }

    private void aplicarEstado(boolean enProceso, String mensaje) {
        if (botonCargar != null) {
            botonCargar.setDisable(enProceso);
        }
        if (comboBrokers != null) {
            comboBrokers.setDisable(enProceso);
        }

        progressBar.setVisible(enProceso);
        progressBar.setManaged(enProceso);
        progressBar.setProgress(enProceso ? ProgressIndicator.INDETERMINATE_PROGRESS : 0);
        lblEstado.setText(mensaje);
    }
}
