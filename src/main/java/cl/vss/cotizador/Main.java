package cl.vss.cotizador;

import cl.vss.cotizador.model.ItemCotizacionExcel;
import cl.vss.cotizador.service.CotizacionService;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.sql.Connection;
import java.util.List;
import cl.vss.cotizador.model.Producto;
import cl.vss.cotizador.service.ProductoService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;


public class Main extends Application {
    // Cotizador
    private final CotizacionService cotizacionService = new CotizacionService();
    private final TableView<ItemCotizacionExcel> tabla = new TableView<>();
    // Productos
    private final ProductoService productoService = new ProductoService();
    private final TableView<Producto> tablaProductos = new TableView<>();

    @Override
public void start(Stage stage) {
    probarConexion();

    // Tab Cotizador
    BorderPane rootCotizador = new BorderPane();
    Button btnCargar = new Button("📂 Cargar Excel");
    btnCargar.setOnAction(e -> cargarArchivo(stage));

    Button btnLimpiar = new Button("🗑️ Limpiar Tabla");
    btnLimpiar.setOnAction(e -> limpiarTabla());

    Button btnExportar = new Button("💾 Exportar Cotización");
    btnExportar.setOnAction(e -> exportarCotizacion(stage));

    ToolBar barraCotizador = new ToolBar(btnCargar, new Separator(), btnLimpiar, btnExportar);
    rootCotizador.setTop(barraCotizador);
    rootCotizador.setCenter(tabla);
    configurarTabla();

    // Tab Productos
    BorderPane rootProductos = new BorderPane();
    ToolBar barraProductos = new ToolBar(
        new Button("➕ Agregar"),
        new Button("✏️ Editar"),
        new Button("🗑️ Eliminar")
    );
    rootProductos.setTop(barraProductos);
    rootProductos.setCenter(tablaProductos);

    configurarTablaProductos();
    cargarProductos();

    // TabPane principal
    TabPane tabs = new TabPane();
    tabs.getTabs().add(new Tab("Cotizador", rootCotizador));
    tabs.getTabs().add(new Tab("Productos", rootProductos));

    Scene scene = new Scene(tabs, 1200, 600);
    stage.setTitle("Cotizador VSS");
    stage.setScene(scene);
    stage.show();
}


    
    private void configurarTablaProductos() {
    TableColumn<Producto, String> colDescEs = new TableColumn<>("Descripción ES");
    colDescEs.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescripcionEs()));

    TableColumn<Producto, String> colDescEn = new TableColumn<>("Descripción EN");
    colDescEn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescripcionEn()));

    TableColumn<Producto, String> colUnidad = new TableColumn<>("Unidad");
    colUnidad.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUnidadMedida()));

    TableColumn<Producto, Double> colValor = new TableColumn<>("Valor Pesos");
    colValor.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getValorPesos()).asObject());

    tablaProductos.getColumns().addAll(colDescEs, colDescEn, colUnidad, colValor);
}

private void cargarProductos() {
    tablaProductos.setItems(FXCollections.observableArrayList(productoService.listarProductos()));
}



    @SuppressWarnings("unchecked")
    private void configurarTabla() {
        // Habilitar edición en la tabla
        tabla.setEditable(true);
        
        TableColumn<ItemCotizacionExcel, String> colCodigo = new TableColumn<>("Código");
        colCodigo.setCellValueFactory(cellData -> cellData.getValue().codigoProperty());
        colCodigo.setCellFactory(TextFieldTableCell.forTableColumn());
        colCodigo.setOnEditCommit(event -> {
            ItemCotizacionExcel item = event.getRowValue();
            item.setCodigo(event.getNewValue());
        });

        TableColumn<ItemCotizacionExcel, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(cellData -> cellData.getValue().descripcionProperty());
        colDescripcion.setCellFactory(TextFieldTableCell.forTableColumn());
        colDescripcion.setOnEditCommit(event -> {
            ItemCotizacionExcel item = event.getRowValue();
            item.setDescripcion(event.getNewValue());
        });

        TableColumn<ItemCotizacionExcel, Integer> colCantidad = new TableColumn<>("Cantidad");
        colCantidad.setCellValueFactory(cellData -> cellData.getValue().cantidadProperty().asObject());
        colCantidad.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colCantidad.setOnEditCommit(event -> {
            ItemCotizacionExcel item = event.getRowValue();
            item.setCantidad(event.getNewValue());
        });

        TableColumn<ItemCotizacionExcel, Double> colPrecio = new TableColumn<>("Precio");
        colPrecio.setCellValueFactory(cellData -> cellData.getValue().precioProperty().asObject());
        colPrecio.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colPrecio.setOnEditCommit(event -> {
            ItemCotizacionExcel item = event.getRowValue();
            item.setPrecio(event.getNewValue());
        });

        TableColumn<ItemCotizacionExcel, Double> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getTotal()).asObject()
        );

        // Nuevas columnas extendidas
        TableColumn<ItemCotizacionExcel, Double> colDescuento = new TableColumn<>("Descuento");
        colDescuento.setCellValueFactory(cellData -> cellData.getValue().descuentoProperty().asObject());
        colDescuento.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colDescuento.setOnEditCommit(event -> {
            ItemCotizacionExcel item = event.getRowValue();
            item.setDescuento(event.getNewValue());
        });

        TableColumn<ItemCotizacionExcel, Double> colTotalNeto = new TableColumn<>("Total Neto");
        colTotalNeto.setCellValueFactory(cellData -> cellData.getValue().totalNetoProperty().asObject());
        colTotalNeto.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colTotalNeto.setOnEditCommit(event -> {
            ItemCotizacionExcel item = event.getRowValue();
            item.setTotalNeto(event.getNewValue());
        });

        TableColumn<ItemCotizacionExcel, String> colComentarios = new TableColumn<>("Comentarios");
        colComentarios.setCellValueFactory(cellData -> cellData.getValue().comentariosProperty());
        colComentarios.setCellFactory(TextFieldTableCell.forTableColumn());
        colComentarios.setOnEditCommit(event -> {
            ItemCotizacionExcel item = event.getRowValue();
            item.setComentarios(event.getNewValue());
        });

        TableColumn<ItemCotizacionExcel, Integer> colDisponibilidad = new TableColumn<>("Disponibilidad");
        colDisponibilidad.setCellValueFactory(cellData -> cellData.getValue().disponibilidadProperty().asObject());
        colDisponibilidad.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colDisponibilidad.setOnEditCommit(event -> {
            ItemCotizacionExcel item = event.getRowValue();
            item.setDisponibilidad(event.getNewValue());
        });

        TableColumn<ItemCotizacionExcel, Double> colTotalBruto = new TableColumn<>("Total Bruto");
        colTotalBruto.setCellValueFactory(cellData -> cellData.getValue().totalBrutoProperty().asObject());
        colTotalBruto.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colTotalBruto.setOnEditCommit(event -> {
            ItemCotizacionExcel item = event.getRowValue();
            item.setTotalBruto(event.getNewValue());
        });

        tabla.getColumns().addAll(
            colCodigo, colDescripcion, colCantidad, colPrecio, colTotal,
            colDescuento, colTotalNeto, colComentarios, colDisponibilidad, colTotalBruto
        );
        
        // Configurar RowFactory para colorear filas cuando no se encuentra precio
        tabla.setRowFactory(tv -> {
            TableRow<ItemCotizacionExcel> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem == null) {
                    row.setStyle("");
                } else {
                    // Colorear en amarillo si no se encontró precio en la base de datos
                    if (newItem.isPrecioNoEncontrado()) {
                        row.setStyle("-fx-background-color: #FFFF99; -fx-text-fill: black;");
                    } else {
                        row.setStyle("");
                    }
                }
            });
            return row;
        });
    }
        private void probarConexion() {
             try (Connection conn = cl.vss.cotizador.util.DBConnection.getConnection()) {
                  System.out.println("✅ Conexión exitosa a PostgreSQL");
        }   catch (Exception e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
    }
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