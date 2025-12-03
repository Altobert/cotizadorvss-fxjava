package cl.vss.cotizador;
import cl.vss.cotizador.model.Familia;

import cl.vss.cotizador.model.ItemCotizacionExcel;
import cl.vss.cotizador.service.CotizacionService;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.Callback;
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

    // Botones con handlers
    Button btnAgregar = new Button("➕ Agregar");
    Button btnEditar  = new Button("✏️ Editar");
    Button btnEliminar = new Button("🗑️ Eliminar");

    // Handlers CRUD
    btnAgregar.setOnAction(e -> abrirDialogAgregarProducto());
    btnEditar.setOnAction(e -> abrirDialogEditarProducto());
    btnEliminar.setOnAction(e -> eliminarProductoSeleccionado());

    // Barra y layout
    ToolBar barraProductos = new ToolBar(btnAgregar, btnEditar, btnEliminar);
    rootProductos.setTop(barraProductos);
    rootProductos.setCenter(tablaProductos);

    // Tabla y datos
    configurarTablaProductos();
    cargarProductos();


    // TabPane principal
    TabPane tabs = new TabPane();
    tabs.getTabs().add(new Tab("Cotizador", rootCotizador));
    tabs.getTabs().add(new Tab("Productos", rootProductos));

    Scene scene = new Scene(tabs, 1400, 1000);
    stage.setTitle("Cotizador VSS");
    
    // Aplicar hoja de estilos CSS
    try {
        String cssPath = getClass().getResource("/estilos.css").toExternalForm();
        scene.getStylesheets().add(cssPath);
        System.out.println("Hoja de estilos cargada: " + cssPath);
    } catch (Exception e) {
        System.out.println("No se pudo cargar la hoja de estilos: " + e.getMessage());
    }
    
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
    tablaProductos.getStylesheets().add(
        getClass().getResource("/productos.css").toExternalForm()
    );
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

        // Columna de Acción con botones
        TableColumn<ItemCotizacionExcel, Void> colAccion = new TableColumn<>("Acción");
        colAccion.setCellFactory(new Callback<TableColumn<ItemCotizacionExcel, Void>, TableCell<ItemCotizacionExcel, Void>>() {
            @Override
            public TableCell<ItemCotizacionExcel, Void> call(final TableColumn<ItemCotizacionExcel, Void> param) {
                final TableCell<ItemCotizacionExcel, Void> cell = new TableCell<ItemCotizacionExcel, Void>() {
                    private final Button btnAccion = new Button("Editar");
                    
                    {
                        btnAccion.setOnAction((event) -> {
                            ItemCotizacionExcel item = getTableView().getItems().get(getIndex());
                            Main.this.manejarAccionItem(item);
                        });
                        btnAccion.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 12px;");
                        btnAccion.setPrefWidth(80);
                    }
                    
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btnAccion);
                        }
                    }
                };
                return cell;
            }
        });
        colAccion.setMinWidth(100);
        colAccion.setSortable(false);

        tabla.getColumns().addAll(
            colCodigo, colDescripcion, colCantidad, colPrecio, colTotal,
            colDescuento, colTotalNeto, colComentarios, colDisponibilidad, colTotalBruto, colAccion
        );
        
        // Aplicar estilos CSS personalizados para la selección de filas
        tabla.setStyle(
            // Estilo para fila seleccionada: texto visible, fondo transparente
            "-fx-selection-bar: transparent; " +
            "-fx-selection-bar-non-focused: transparent; " +
            "-fx-focus-color: transparent; " +
            "-fx-faint-focus-color: transparent; " +
            // Forzar color de texto en celdas seleccionadas
            "-fx-selection-bar-text: black; " +
            "-fx-cell-focus-inner-border: transparent;"
        );
        
        // Crear hoja de estilos CSS más específica para sobrescribir los estilos predeterminados
        String cssOverride = 
            ".table-view .table-row-cell:selected .table-cell { " +
                "-fx-background-color: transparent; " +
                "-fx-text-fill: black !important; " +
                "-fx-font-weight: bold; " +
            "} " +
            ".table-view .table-row-cell:selected:focused .table-cell { " +
                "-fx-background-color: transparent; " +
                "-fx-text-fill: black !important; " +
                "-fx-font-weight: bold; " +
            "} " +
            ".table-view .table-row-cell:selected .text { " +
                "-fx-fill: black !important; " +
                "-fx-font-weight: bold; " +
            "}";
        
        // Crear una escena temporal para aplicar CSS si no existe
        if (tabla.getScene() == null) {
            // Se aplicará cuando la tabla se agregue a la escena
            tabla.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.getRoot().setStyle(cssOverride);
                }
            });
        } else {
            tabla.getScene().getRoot().setStyle(cssOverride);
        }
        
        // Configurar RowFactory para colorear filas cuando no se encuentra precio y manejar selección
        tabla.setRowFactory(tv -> {
            TableRow<ItemCotizacionExcel> row = new TableRow<ItemCotizacionExcel>() {
                @Override
                protected void updateItem(ItemCotizacionExcel item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setStyle("");
                    } else {
                        Main.this.actualizarEstiloFila(this, item);
                    }
                }
                
                @Override
                public void updateSelected(boolean selected) {
                    super.updateSelected(selected);
                    if (!isEmpty()) {
                        Main.this.actualizarEstiloFila(this, getItem());
                    }
                }
            };
            
            return row;
        });
        
        // Agregar listener para cambios de selección y forzar actualización de estilos
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            // Forzar actualización de todas las filas visibles
            tabla.refresh();
        });
    }

    private void actualizarEstiloFila(TableRow<ItemCotizacionExcel> row, ItemCotizacionExcel item) {
        if (item == null) {
            row.setStyle("");
            return;
        }
        
        StringBuilder estilo = new StringBuilder();
        
        // Si la fila está seleccionada, usar estilos más específicos
        if (row.isSelected()) {
            // Fondo más oscuro y texto forzado en negro con negrita
            if (item.isPrecioNoEncontrado()) {
                estilo.append("-fx-background-color: #e6e67a; "); // Amarillo más oscuro
            } else {
                estilo.append("-fx-background-color: #5cb85c; "); // Verde más oscuro
            }
            // Forzar texto negro y negrita - usar CSS más específico
            estilo.append("-fx-text-fill: black; ");
            estilo.append("-fx-font-weight: bold; ");
            
            // Usar Platform.runLater para asegurar que los estilos se apliquen después del renderizado
            javafx.application.Platform.runLater(() -> {
                // Aplicar estilos directamente a cada celda
                row.getChildrenUnmodifiable().forEach(node -> {
                    if (node instanceof javafx.scene.control.TableCell) {
                        TableCell<?, ?> cell = (TableCell<?, ?>) node;
                        cell.setStyle("-fx-text-fill: black; -fx-font-weight: bold; -fx-background-color: transparent;");
                        
                        // También aplicar a los nodos de texto dentro de la celda
                        cell.getChildrenUnmodifiable().forEach(childNode -> {
                            if (childNode instanceof javafx.scene.text.Text) {
                                ((javafx.scene.text.Text) childNode).setStyle("-fx-fill: black; -fx-font-weight: bold;");
                            }
                        });
                    }
                });
            });
        } else {
            // Estilo normal según el estado del precio
            if (item.isPrecioNoEncontrado()) {
                estilo.append("-fx-background-color: #eded93ff; ");
            } else {
                estilo.append("-fx-background-color: #6de26dff; ");
            }
            estilo.append("-fx-text-fill: black; ");
            
            // Limpiar estilos de selección en celdas cuando no está seleccionada
            javafx.application.Platform.runLater(() -> {
                row.getChildrenUnmodifiable().forEach(node -> {
                    if (node instanceof javafx.scene.control.TableCell) {
                        node.setStyle("");
                    }
                });
            });
        }
        
        row.setStyle(estilo.toString());
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

    private void manejarAccionItem(ItemCotizacionExcel item) {
        // Buscar productos similares en la base de datos
        List<cl.vss.cotizador.model.ProductoSimilar> productosSimilares = 
            cotizacionService.buscarProductosSimilares(item.getDescripcion());
        
        // Crear diálogo personalizado con tabla
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Productos Similares");
        dialog.setHeaderText("Productos encontrados para: " + item.getDescripcion());
        
        // Crear la tabla de productos similares
        TableView<cl.vss.cotizador.model.ProductoSimilar> tablaProductosSimilares = new TableView<>();
        
        // Configurar columnas
        TableColumn<cl.vss.cotizador.model.ProductoSimilar, String> colDescEs = new TableColumn<>("Descripción ES");
        colDescEs.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescripcionEs()));
        colDescEs.setPrefWidth(200);
        
        TableColumn<cl.vss.cotizador.model.ProductoSimilar, String> colDescEn = new TableColumn<>("Descripción EN");
        colDescEn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescripcionEn()));
        colDescEn.setPrefWidth(200);
        
        TableColumn<cl.vss.cotizador.model.ProductoSimilar, String> colUnidad = new TableColumn<>("Unidad");
        colUnidad.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUnidadMedida()));
        colUnidad.setPrefWidth(80);
        
        TableColumn<cl.vss.cotizador.model.ProductoSimilar, Double> colPrecio = new TableColumn<>("Precio Neto");
        colPrecio.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getPrecioVentaNeto()).asObject());
        colPrecio.setPrefWidth(120);
        
        // Formatear la columna de precio para mostrar como moneda
        colPrecio.setCellFactory(column -> new TableCell<cl.vss.cotizador.model.ProductoSimilar, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("$%,.2f", item));
                }
            }
        });
        
        // Agregar columnas a la tabla
        tablaProductosSimilares.getColumns().addAll(colDescEs, colDescEn, colUnidad, colPrecio);
        
        // Cargar datos en la tabla
        ObservableList<cl.vss.cotizador.model.ProductoSimilar> datosTabla = 
            FXCollections.observableArrayList(productosSimilares);
        tablaProductosSimilares.setItems(datosTabla);
        
        // Configurar tamaño de la tabla
        tablaProductosSimilares.setPrefSize(650, 300);
        
        // Crear panel de información del item original
        VBox infoPanel = new VBox(10);
        infoPanel.setStyle("-fx-padding: 10; -fx-background-color: #f0f0f0;");
        
        Label lblItemOriginal = new Label("📦 Item Original:");
        lblItemOriginal.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label lblDescripcion = new Label("Descripción: " + item.getDescripcion());
        Label lblCantidad = new Label("Cantidad: " + item.getCantidad());
        Label lblPrecioActual = new Label("Precio Actual: $" + String.format("%,.2f", item.getPrecio()));
        
        infoPanel.getChildren().addAll(lblItemOriginal, lblDescripcion, lblCantidad, lblPrecioActual);
        
        // Panel principal que combina información y tabla
        VBox contenidoPrincipal = new VBox(15);
        contenidoPrincipal.getChildren().addAll(infoPanel, 
                                              new Label("🔍 Productos Similares Encontrados (" + productosSimilares.size() + "):"),
                                              tablaProductosSimilares);
        
        // Configurar el diálogo
        dialog.getDialogPane().setContent(contenidoPrincipal);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(700, 500);
        
        // Mensaje si no se encontraron productos
        if (productosSimilares.isEmpty()) {
            Label sinResultados = new Label("❌ No se encontraron productos similares en la base de datos.");
            sinResultados.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");
            tablaProductosSimilares.setPlaceholder(sinResultados);
        }
        
        // Mostrar el diálogo
        dialog.showAndWait();
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

    // 👉 Métodos CRUD de Productos

    private void abrirDialogAgregarProducto() {
    Dialog<Producto> dialog = new Dialog<>();
    dialog.setTitle("Agregar Producto");

    TextField txtDescEs = new TextField();
    TextField txtDescEn = new TextField();
    TextField txtUnidad = new TextField();
    TextField txtValor = new TextField();

    // 👉 ComboBox de familias
    ComboBox<Familia> comboFamilia = crearComboFamilias();

    VBox content = new VBox(10,
        new Label("Descripción ES:"), txtDescEs,
        new Label("Descripción EN:"), txtDescEn,
        new Label("Unidad:"), txtUnidad,
        new Label("Valor Pesos:"), txtValor,
        new Label("Familia:"), comboFamilia   // 👉 agregado al diálogo
    );
    content.setStyle("-fx-padding: 10;");
    dialog.getDialogPane().setContent(content);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    dialog.setResultConverter(btn -> {
        if (btn == ButtonType.OK) {
            try {
                // Validaciones
                String descEs = txtDescEs.getText().trim();
                String descEn = txtDescEn.getText().trim();
                String unidad  = txtUnidad.getText().trim();
                double valor   = Double.parseDouble(txtValor.getText().trim());

                if (descEs.isEmpty() || unidad.isEmpty()) {
                    throw new IllegalArgumentException("Descripción ES y Unidad son obligatorias.");
                }
                if (valor <= 0) {
                    throw new IllegalArgumentException("El valor debe ser mayor que 0.");
                }
                if (comboFamilia.getValue() == null) {
                    throw new IllegalArgumentException("Debe seleccionar una familia.");
                }

                // Construcción segura del objeto
                Producto nuevo = new Producto();
                nuevo.setDescripcionEs(descEs);
                nuevo.setDescripcionEn(descEn);
                nuevo.setUnidadMedida(unidad);
                nuevo.setValorPesos(valor);
                nuevo.setFamiliaId(comboFamilia.getValue().getId());

                return nuevo;
            } catch (NumberFormatException nfe) {
                mostrarError("Valor inválido", "El campo 'Valor Pesos' debe ser numérico.\n" + nfe.getMessage());
            } catch (Exception ex) {
                mostrarError("Datos inválidos", "Revisa los campos.\n" + ex.getMessage());
            }
        }
        return null;
    });

    dialog.showAndWait().ifPresent(p -> {
        try {
            productoService.agregarProducto(p);
            cargarProductos();
        } catch (Exception e) {
            mostrarError("Error al guardar", e.getMessage());
        }
    });
}


    private void abrirDialogEditarProducto() {
    Producto seleccionado = tablaProductos.getSelectionModel().getSelectedItem();
    if (seleccionado == null) {
        mostrarInfo("Selecciona un producto", "Debes seleccionar un producto para editar.");
        return;
    }

    Dialog<Producto> dialog = new Dialog<>();
    dialog.setTitle("Editar Producto");

    TextField txtDescEs = new TextField(seleccionado.getDescripcionEs());
    TextField txtDescEn = new TextField(seleccionado.getDescripcionEn());
    TextField txtUnidad = new TextField(seleccionado.getUnidadMedida());
    TextField txtValor = new TextField(String.valueOf(seleccionado.getValorPesos()));

    // 👉 ComboBox de familias
    ComboBox<Familia> comboFamilia = crearComboFamilias();
    // Preseleccionar la familia actual del producto
    comboFamilia.setValue(
        productoService.listarFamilias().stream()
            .filter(f -> f.getId() == seleccionado.getFamiliaId())
            .findFirst()
            .orElse(null)
    );

    VBox content = new VBox(10,
        new Label("Descripción ES:"), txtDescEs,
        new Label("Descripción EN:"), txtDescEn,
        new Label("Unidad:"), txtUnidad,
        new Label("Valor Pesos:"), txtValor,
        new Label("Familia:"), comboFamilia   // 👉 agregado al diálogo
    );
    content.setStyle("-fx-padding: 10;");
    dialog.getDialogPane().setContent(content);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    dialog.setResultConverter(btn -> {
        if (btn == ButtonType.OK) {
            try {
                // Validaciones
                String descEs = txtDescEs.getText().trim();
                String descEn = txtDescEn.getText().trim();
                String unidad  = txtUnidad.getText().trim();
                double valor   = Double.parseDouble(txtValor.getText().trim());

                if (descEs.isEmpty() || unidad.isEmpty()) {
                    throw new IllegalArgumentException("Descripción ES y Unidad son obligatorias.");
                }
                if (valor <= 0) {
                    throw new IllegalArgumentException("El valor debe ser mayor que 0.");
                }
                if (comboFamilia.getValue() == null) {
                    throw new IllegalArgumentException("Debe seleccionar una familia.");
                }

                // Asignación segura
                seleccionado.setDescripcionEs(descEs);
                seleccionado.setDescripcionEn(descEn);
                seleccionado.setUnidadMedida(unidad);
                seleccionado.setValorPesos(valor);
                seleccionado.setFamiliaId(comboFamilia.getValue().getId());

                return seleccionado;
            } catch (NumberFormatException nfe) {
                mostrarError("Valor inválido", "El campo 'Valor Pesos' debe ser numérico.\n" + nfe.getMessage());
            } catch (Exception ex) {
                mostrarError("Datos inválidos", "Revisa los campos.\n" + ex.getMessage());
            }
        }
        return null;
    });

    dialog.showAndWait().ifPresent(p -> {
        try {
            productoService.actualizarProducto(p);
            cargarProductos();
        } catch (Exception e) {
            mostrarError("Error al actualizar", e.getMessage());
        }
    });
}


    private void eliminarProductoSeleccionado() {
        Producto seleccionado = tablaProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarInfo("Selecciona un producto", "Debes seleccionar un producto para eliminar.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Seguro que deseas eliminar el producto:\n" + seleccionado.getDescripcionEs() + "?",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmar eliminación");
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                try {
                    productoService.eliminarProducto(seleccionado.getId());
                    cargarProductos();
                } catch (Exception ex) {
                    mostrarError("Error al eliminar", ex.getMessage());
                }
            }
        });
    }
        private ComboBox<Familia> crearComboFamilias() {
        ComboBox<Familia> combo = new ComboBox<>();
        try {
            List<Familia> familias = productoService.listarFamilias();
            combo.getItems().addAll(familias);
            combo.setConverter(new javafx.util.StringConverter<Familia>() {
                @Override
                public String toString(Familia f) {
                    return f != null ? f.getNombre() : "";
                }
                @Override
                public Familia fromString(String string) {
                    return combo.getItems().stream()
                            .filter(f -> f.getNombre().equals(string))
                            .findFirst().orElse(null);
                }
            });
        } catch (Exception e) {
            mostrarError("Error cargando familias", e.getMessage());
        }
        return combo;
    }


    private void mostrarInfo(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }

    private void mostrarError(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }

    // ⚠️ El main siempre al final
    public static void main(String[] args) {
        launch(args);
    }
}
