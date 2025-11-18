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
import javafx.scene.image.Image;
import java.io.File;
import java.io.InputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
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
    
    // Configurar iconos de la aplicación (múltiples tamaños para mejor compatibilidad)
    configurarIconosAplicacion(stage);
    
    stage.setScene(scene);
    
    // En macOS, a veces es necesario configurar el icono después de mostrar la ventana
    stage.show();
    
    // Configurar icono del dock en macOS si es posible
    try {
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Cotizador VSS");
        System.setProperty("apple.awt.application.name", "Cotizador VSS");
        
        // Intentar configurar el icono del dock usando AWT si está disponible
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
            java.net.URL imageURL = getClass().getResource("/images/cotizador-icon-64.png");
            if (imageURL != null) {
                java.awt.Image awtImage = toolkit.getImage(imageURL);
                try {
                    // Usar reflexión para establecer el icono del dock sin dependencias directas
                    Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
                    Object application = applicationClass.getMethod("getApplication").invoke(null);
                    applicationClass.getMethod("setDockIconImage", java.awt.Image.class).invoke(application, awtImage);
                    System.out.println("🍎 Icono del dock configurado para macOS");
                } catch (Exception e) {
                    System.out.println("⚠️ No se pudo configurar el icono del dock: " + e.getMessage());
                }
            }
        }
    } catch (Exception e) {
        System.out.println("⚠️ Error configurando propiedades de macOS: " + e.getMessage());
    }
}

    private void configurarIconosAplicacion(Stage primaryStage) {
        System.out.println("🎨 Configurando iconos de la aplicación...");
        
        // Lista de tamaños de iconos a cargar
        String[] iconSizes = {"icon-16.png", "icon-32.png", "icon-48.png", "icon-64.png", "icon.png"};
        int iconosConfigurados = 0;
        
        for (String iconFile : iconSizes) {
            try {
                // Cargar imagen desde recursos
                InputStream iconStream = getClass().getResourceAsStream("/images/" + iconFile);
                if (iconStream != null) {
                    Image icon = new Image(iconStream);
                    if (!icon.isError()) {
                        primaryStage.getIcons().add(icon);
                        iconosConfigurados++;
                        System.out.println("✅ Icono cargado: " + iconFile + " (" + 
                                         (int)icon.getWidth() + "x" + (int)icon.getHeight() + ")");
                    } else {
                        System.err.println("❌ Error en imagen: " + iconFile);
                    }
                    iconStream.close();
                } else {
                    System.err.println("❌ No se encontró: /images/" + iconFile);
                }
            } catch (Exception e) {
                System.err.println("❌ Error cargando " + iconFile + ": " + e.getMessage());
            }
        }
        
        System.out.println("✅ " + iconosConfigurados + " iconos de aplicación configurados correctamente");
        System.out.println("📋 Total de iconos en stage: " + primaryStage.getIcons().size());
        
        // Configurar propiedades del sistema para identificación de la aplicación
        System.setProperty("apple.awt.application.name", "Cotizador VSS");
        
        // Intentar configurar icono usando AWT Toolkit (alternativa más compatible)
        try {
            InputStream iconStream = getClass().getResourceAsStream("/images/icon.png");
            if (iconStream != null) {
                BufferedImage iconImage = ImageIO.read(iconStream);
                
                // Método 1: Configurar usando AWT Toolkit
                java.awt.Toolkit.getDefaultToolkit().setDynamicLayout(true);
                
                // Método 2: Si estamos en macOS, intentar configurar dock
                String osName = System.getProperty("os.name").toLowerCase();
                if (osName.contains("mac")) {
                    try {
                        // Configurar usando reflexión para evitar problemas de módulos
                        Class<?> taskbarClass = Class.forName("java.awt.Taskbar");
                        if ((Boolean) taskbarClass.getMethod("isTaskbarSupported").invoke(null)) {
                            Object taskbar = taskbarClass.getMethod("getTaskbar").invoke(null);
                            taskbarClass.getMethod("setIconImage", java.awt.Image.class)
                                       .invoke(taskbar, iconImage);
                            System.out.println("✅ Icono del taskbar configurado");
                        }
                    } catch (Exception taskbarEx) {
                        System.out.println("⚠️ Taskbar API no disponible: " + taskbarEx.getMessage());
                    }
                }
                
                iconStream.close();
            }
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo configurar el icono del sistema: " + e.getMessage());
        }
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
                        row.setStyle("-fx-background-color: #eded93ff; -fx-text-fill: black;");
                    } else {
                        // colorear en verde claro si se encontró precio
                        row.setStyle("-fx-background-color: #6de26dff; -fx-text-fill: black;");
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