package cl.vss.cotizador;

import cl.vss.cotizador.model.ItemCotizacionExcel;
import cl.vss.cotizador.service.CotizacionService;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class Main extends Application {

    private final CotizacionService cotizacionService = new CotizacionService();
    private final TableView<ItemCotizacionExcel> tabla = new TableView<>();

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        Button btnCargar = new Button("📂 Cargar Excel");
        btnCargar.setOnAction(e -> cargarArchivo(stage));

        Button btnLimpiar = new Button("🗑️ Limpiar Tabla");
        btnLimpiar.setOnAction(e -> limpiarTabla());

        Button btnExportar = new Button("💾 Exportar Cotización");
        btnExportar.setOnAction(e -> exportarCotizacion(stage));

        ToolBar barra = new ToolBar(btnCargar, new Separator(), btnLimpiar, btnExportar);
        root.setTop(barra);
        root.setCenter(tabla);

        configurarTabla();

        Scene scene = new Scene(root, 1200, 600);
        stage.setTitle("Cotizador VSS");
        stage.setScene(scene);
        stage.show();
    }

    @SuppressWarnings("unchecked")
    private void configurarTabla() {
        TableColumn<ItemCotizacionExcel, String> colCodigo = new TableColumn<>("Código");
        colCodigo.setCellValueFactory(cellData -> cellData.getValue().codigoProperty());

        TableColumn<ItemCotizacionExcel, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(cellData -> cellData.getValue().descripcionProperty());

        TableColumn<ItemCotizacionExcel, Integer> colCantidad = new TableColumn<>("Cantidad");
        colCantidad.setCellValueFactory(cellData -> cellData.getValue().cantidadProperty().asObject());

        TableColumn<ItemCotizacionExcel, Double> colPrecio = new TableColumn<>("Precio");
        colPrecio.setCellValueFactory(cellData -> cellData.getValue().precioProperty().asObject());

        TableColumn<ItemCotizacionExcel, Double> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getTotal()).asObject()
        );

        // Nuevas columnas extendidas
        TableColumn<ItemCotizacionExcel, Double> colDescuento = new TableColumn<>("Descuento");
        colDescuento.setCellValueFactory(cellData -> cellData.getValue().descuentoProperty().asObject());

        TableColumn<ItemCotizacionExcel, Double> colTotalNeto = new TableColumn<>("Total Neto");
        colTotalNeto.setCellValueFactory(cellData -> cellData.getValue().totalNetoProperty().asObject());

        TableColumn<ItemCotizacionExcel, String> colComentarios = new TableColumn<>("Comentarios");
        colComentarios.setCellValueFactory(cellData -> cellData.getValue().comentariosProperty());

        TableColumn<ItemCotizacionExcel, Integer> colDisponibilidad = new TableColumn<>("Disponibilidad");
        colDisponibilidad.setCellValueFactory(cellData -> cellData.getValue().disponibilidadProperty().asObject());

        TableColumn<ItemCotizacionExcel, Double> colTotalBruto = new TableColumn<>("Total Bruto");
        colTotalBruto.setCellValueFactory(cellData -> cellData.getValue().totalBrutoProperty().asObject());

        tabla.getColumns().addAll(
            colCodigo, colDescripcion, colCantidad, colPrecio, colTotal,
            colDescuento, colTotalNeto, colComentarios, colDisponibilidad, colTotalBruto
        );
    }

    private void cargarArchivo(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos Excel", "*.xlsx"));
        File archivo = fileChooser.showOpenDialog(stage);

        if (archivo != null) {
            List<ItemCotizacionExcel> items = cotizacionService.leerItemsDesdeExcel(archivo);
            ObservableList<ItemCotizacionExcel> datos = FXCollections.observableArrayList(items);
            tabla.setItems(datos);
        }
    }

    private void limpiarTabla() {
        tabla.getItems().clear();
        // Opcional: mostrar un mensaje de confirmación
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Tabla Limpiada");
        alert.setHeaderText(null);
        alert.setContentText("La tabla ha sido limpiada exitosamente.");
        alert.showAndWait();
    }

    private void exportarCotizacion(Stage stage) {
        if (tabla.getItems().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Tabla Vacía");
            alert.setHeaderText(null);
            alert.setContentText("No hay datos para exportar. Por favor, carga un archivo Excel primero.");
            alert.showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Cotización");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos Excel", "*.xlsx"));
        fileChooser.setInitialFileName("cotizacion_exportada.xlsx");
        File archivo = fileChooser.showSaveDialog(stage);

        if (archivo != null) {
            try {
                // Aquí llamaremos al servicio de exportación
                cotizacionService.exportarCotizacion(tabla.getItems(), archivo);
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Exportación Exitosa");
                alert.setHeaderText(null);
                alert.setContentText("La cotización ha sido exportada exitosamente a:\n" + archivo.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error de Exportación");
                alert.setHeaderText(null);
                alert.setContentText("Error al exportar la cotización: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}