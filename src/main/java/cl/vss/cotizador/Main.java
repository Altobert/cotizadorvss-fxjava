package cl.vss.cotizador;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import cl.vss.cotizador.demo.LoginController;
import cl.vss.cotizador.model.Familia;
import cl.vss.cotizador.model.ItemCotizacionExcel;
import cl.vss.cotizador.service.AuditoriaDAO;
import cl.vss.cotizador.service.AuditoriaRegistro;
import cl.vss.cotizador.service.CotizacionService;
import cl.vss.cotizador.service.ParametrosDAO;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
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
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import cl.vss.cotizador.model.Producto;
import cl.vss.cotizador.service.ProductoService;
import cl.vss.cotizador.service.UsuarioDAO;
import cl.vss.cotizador.util.DBConnection;
import cl.vss.cotizador.util.Sesion;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import cl.vss.cotizador.view.UserCrudView;


public class Main extends Application {
    private static final Logger logger = LogManager.getLogger(Main.class);
    
    // Cotizador
    private final CotizacionService cotizacionService = new CotizacionService();
    private final TableView<ItemCotizacionExcel> tabla = new TableView<>();
    private cl.vss.cotizador.model.CabeceraCotizacion cabeceraActual;
    
    // Campos de texto para la cabecera
    private TextField txtNombreCliente;
    private TextField txtIdCliente;
    private TextField txtEmpresa;
    private TextField txtNumeroCotizacion;
    private TextField txtReferencia;
    private Label lblFecha;
    private TextArea txtObservaciones;
    
    // Productos
    private final ProductoService productoService = new ProductoService();
    private final TableView<Producto> tablaProductos = new TableView<>();
    private ObservableList<Producto> productos;   // 👉 lista compartida para filtro y recarga
    private TableColumn<Producto, String> colDescEs;
    private TableColumn<Producto, String> colDescEn;
    private TableColumn<Producto, String> colUnidad;
    private TableColumn<Producto, Double> colValor;

        // 👉 Columnas nuevas con precios calculados
    private TableColumn<Producto, Double> colPrecioUSD;
    private TableColumn<Producto, Double> colPrecioUtil;
    private TableColumn<Producto, Double> colPrecioFinalCLP;

    // 👉 Formatos numéricos
    private final DecimalFormat formatoUSD = new DecimalFormat("#,##0.00");
    private final DecimalFormat formatoCLP = new DecimalFormat("#,###");

    // 👉 Filtros y búsqueda para Productos
    private FilteredList<Producto> filteredProductos;

    // 👉 Controles usados en el filtrado (se inicializan en la UI)
    private TextField txtBuscar;
    private ComboBox<String> comboFamilias;
 
    // 👉 Variables para parámetros comerciales en productos
    private double tipoCambioActual = 1.0;
    private double utilidadActual = 0.0;
    private Label lblTipoCambioUsado;
    private Label lblUtilidadUsada;

    // ✅ Mapa en memoria para filtros instantáneos
    private Map<String, Integer> mapaFamilias;

    @Override
    public void start(Stage stage) {
        // 👉 Si no hay sesión activa, abrir login 
    if (!Sesion.estaLogueado()) {
    Connection conn;
    try {
        conn = DBConnection.getConnection();
        UsuarioDAO usuarioDAO = new UsuarioDAO(conn);
        LoginController login = new LoginController(usuarioDAO);
        login.mostrarLogin(stage);
        return;
    } catch (SQLException ex) {
        logger.error("Error crítico al conectar con la base de datos", ex);
        ex.printStackTrace();
        Alert alert = new Alert(Alert.AlertType.ERROR, "❌ Error de conexión a la BD: " + ex.getMessage());
        alert.showAndWait();
        return;
    }
}

    probarConexion();

    // 👉 Cargar parámetros comerciales ANTES de construir la tabla de productos
    try (Connection conn = DBConnection.getConnection()) {
        ParametrosDAO parametrosDAO = new ParametrosDAO(conn);
        tipoCambioActual = parametrosDAO.getTipoCambioActual();
        utilidadActual = parametrosDAO.getPorcentajeUtilidadActual();
    } catch (SQLException ex) {
        ex.printStackTrace();
        tipoCambioActual = 1.0;
        utilidadActual = 0.0;
    }

    // Tab Cotizador
    BorderPane rootCotizador = new BorderPane();
    Button btnCargar = new Button("📂 Cargar Excel");
    btnCargar.setOnAction(e -> cargarArchivo(stage));
    
    //Button btnAnalizar = new Button("🔍 Analizar Estructura Excel");
    //btnAnalizar.setDisable(true); // Deshabilitado inicialmente
    //btnAnalizar.setOnAction(e -> analizarEstructuraExcel(stage));

    Button btnLimpiar = new Button("🗑️ Limpiar Tabla");
    btnLimpiar.setOnAction(e -> limpiarTabla());

    Button btnExportar = new Button("💾 Exportar Cotización");
    btnExportar.setOnAction(e -> exportarCotizacion(stage));

    // 👉 aplicar estilo corporativo VSS (azul con letras blancas)
    btnCargar.getStyleClass().add("color-primario");
    //btnAnalizar.getStyleClass().add("color-primario");
    btnLimpiar.getStyleClass().add("color-primario");
    btnExportar.getStyleClass().add("color-primario");

    //ToolBar barraCotizador = new ToolBar(btnCargar, btnAnalizar, new Separator(), btnLimpiar, btnExportar);
    ToolBar barraCotizador = new ToolBar(btnCargar, new Separator(), btnLimpiar,new Separator(), btnExportar);
    rootCotizador.setTop(barraCotizador);
    
    // Crear panel con cabecera y tabla
    //VBox panelConCabecera = new VBox(5);
    //panelConCabecera.getChildren().addAll(crearPanelCabecera(), tabla);
    //VBox.setVgrow(tabla, javafx.scene.layout.Priority.ALWAYS);
    
    //rootCotizador.setCenter(panelConCabecera);
    rootCotizador.setCenter(tabla);  // 👈 Solo mostrar la tabla sin cabecera
    configurarTabla();


    // Tab Productos
    BorderPane rootProductos = new BorderPane();

    // Botones con handlers
    Button btnAgregar = new Button("➕ Agregar");
    Button btnEditar  = new Button("✏️ Editar");
    Button btnEliminar = new Button("🗑️ Eliminar");

    
    // Aplicar colores corporativos VSS desde estilos.css
    btnAgregar.getStyleClass().add("color-primario");
    btnEditar.getStyleClass().add("color-primario");
    btnEliminar.getStyleClass().add("color-primario");
    rootProductos.getStyleClass().add("color-fondo");

    // Handlers CRUD
    btnAgregar.setOnAction(e -> abrirDialogAgregarProducto());
    btnEditar.setOnAction(e -> abrirDialogEditarProducto());
    btnEliminar.setOnAction(e -> eliminarProductoSeleccionado());

    // Barra y layout
    // Campo de búsqueda
    txtBuscar = new TextField();
    txtBuscar.setPromptText("Buscar producto...");
    txtBuscar.setPrefWidth(200);

    // ComboBox de familias
    comboFamilias = new ComboBox<>();
    comboFamilias.setPromptText("Todas");


    // Barra superior completa
    ToolBar barraProductos = new ToolBar(
    btnAgregar, btnEditar, btnEliminar,
    new Separator(),
    new Label("Buscar:"), txtBuscar,
    new Separator(),
    new Label("Familia:"), comboFamilias
);

// ===============================
// BANNER DE PARÁMETROS COMERCIALES
// ===============================

// 1️⃣ Crear los labels (ANTES de usarlos)
    lblTipoCambioUsado = new Label();
    lblUtilidadUsada = new Label();

// 2️⃣ Crear el contenedor visual del banner
    HBox bannerParametros = new HBox(20, lblTipoCambioUsado, lblUtilidadUsada);
    bannerParametros.setStyle(
    "-fx-font-size: 16px;" +
    "-fx-font-weight: bold;" +
    "-fx-text-fill: #0A3D91;" +
    "-fx-background-color: #D6E4FF;" +
    "-fx-padding: 12px;" +
    "-fx-border-color: #0A3D91;" +
    "-fx-border-width: 2px;" +
    "-fx-border-radius: 6px;" +
    "-fx-background-radius: 6px;"
);

// 3️⃣ Actualizar los labels con los valores cargados
    lblTipoCambioUsado.setText("💱 Tipo de cambio aplicado: " + tipoCambioActual);
    lblUtilidadUsada.setText("📈 Utilidad aplicada: " + utilidadActual + "%");

// 4️⃣ Agregar banner + barra superior al layout
    VBox topProductos = new VBox(10, bannerParametros, barraProductos);
    rootProductos.setTop(topProductos);


  // 👉 Configurar columnas de la tabla
configurarTablaProductos();

// 👉 Lista base
productos = FXCollections.observableArrayList(productoService.listarProductos());

// 👉 Cargar mapa de familias
mapaFamilias = productoService.obtenerMapaFamilias();

// ============================================================
// 🔵 Lista filtrada (atributo de clase)
// ============================================================
filteredProductos = new FilteredList<>(productos, p -> true);

// ============================================================
// 🔵 Lista ordenada
// ============================================================
SortedList<Producto> ordenados = new SortedList<>(filteredProductos);
ordenados.comparatorProperty().bind(tablaProductos.comparatorProperty());

// 👉 Asignar a la tabla
tablaProductos.setItems(ordenados);

// 👉 Ordenar por descripción automáticamente
tablaProductos.getSortOrder().clear();
colDescEs.setSortType(TableColumn.SortType.ASCENDING);
tablaProductos.getSortOrder().add(colDescEs);

// 👉 Cargar familias
comboFamilias.getItems().add("Todas");
comboFamilias.getItems().addAll(productoService.listarFamiliasNombres());
comboFamilias.setValue("Todas");

// ============================================================
// 🔵 Listeners NUEVOS (usan filtrarProductos())
// ============================================================
txtBuscar.textProperty().addListener((obs, oldValue, newValue) -> filtrarProductos());

comboFamilias.valueProperty().addListener((obs, oldValue, newValue) -> filtrarProductos());

// 👉 Contenedor central solo con la tabla
VBox centroProductos = new VBox(10, tablaProductos);
centroProductos.setStyle("-fx-padding: 10;");
rootProductos.setCenter(centroProductos);


    // TabPane principal
TabPane tabs = new TabPane();

// 👉 Pestaña Cotizador (queda igual)
tabs.getTabs().add(new Tab("Cotizador", rootCotizador));

// 👉 Pestaña Productos (versión corregida con listener)
Tab tabProductos = new Tab("Productos", rootProductos);

tabProductos.setOnSelectionChanged(event -> {
    if (tabProductos.isSelected()) {
        recargarParametrosDesdeBD();
        tablaProductos.refresh();
    }
});

tabs.getTabs().add(tabProductos);


    // ----------------------
    // Tab Parámetros Comerciales
    BorderPane rootParametros = new BorderPane();

    // Campos de entrada
    TextField txtTipoCambio = new TextField();
    txtTipoCambio.setPromptText("Tipo de cambio usado");
    txtTipoCambio.setPrefWidth(120);
    txtTipoCambio.setMaxWidth(150);

    TextField txtUtilidad = new TextField();
    txtUtilidad.setPromptText("Porcentaje de utilidad");
    txtUtilidad.setPrefWidth(120);
    txtUtilidad.setMaxWidth(150);
    txtUtilidad.setPrefColumnCount(5);

    DatePicker dpVigencia = new DatePicker();
    dpVigencia.setPromptText("Fecha de vigencia");
    dpVigencia.setPrefWidth(150);

    Button btnGuardar = new Button("Guardar parámetros");
    Label lblMensaje = new Label();

    // Acción del botón
    btnGuardar.setOnAction(e -> {
        try {
            // ✅ Validaciones básicas de entrada
            if (txtTipoCambio.getText().isBlank()) {
                lblMensaje.setText("❌ Debe ingresar un tipo de cambio");
                return;
            }
            if (txtUtilidad.getText().isBlank()) {
                lblMensaje.setText("❌ Debe ingresar un porcentaje de utilidad");
                return;
            }
            if (dpVigencia.getValue() == null) {
                lblMensaje.setText("❌ Debe seleccionar una fecha de vigencia");
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                ParametrosDAO parametrosDAO = new ParametrosDAO(conn);
                AuditoriaDAO auditoriaDAO = new AuditoriaDAO(conn);

                // 👉 Consultar valores anteriores ANTES de actualizar
                double tipoCambioAnterior = parametrosDAO.getTipoCambioActual();
                double utilidadAnterior = parametrosDAO.getPorcentajeUtilidadActual();
                LocalDate vigenciaAnterior = parametrosDAO.getFechaVigenciaActual();

                // 👉 Nuevos valores desde la UI
                double tipoCambio = Double.parseDouble(txtTipoCambio.getText());
                double utilidad = Double.parseDouble(txtUtilidad.getText());
                LocalDate vigencia = dpVigencia.getValue();

                // 👉 Guardar parámetros en BD
                parametrosDAO.actualizarParametros(tipoCambio, utilidad, vigencia, Sesion.getUsuarioActual().getId());

                // 👉 Registrar auditoría
                auditoriaDAO.insertarCambio("tipo_cambio_usado",
                    String.valueOf(tipoCambioAnterior),
                    String.valueOf(tipoCambio),
                    Sesion.getUsuarioActual().getId());

                auditoriaDAO.insertarCambio("porcentaje_utilidad",
                    String.valueOf(utilidadAnterior),
                    String.valueOf(utilidad),
                    Sesion.getUsuarioActual().getId());

                auditoriaDAO.insertarCambio("fecha_vigencia",
                    vigenciaAnterior != null ? vigenciaAnterior.toString() : "N/A",
                    vigencia.toString(),
                    Sesion.getUsuarioActual().getId());

                // 👉 Actualizar variables en memoria
                tipoCambioActual = tipoCambio;
                utilidadActual = utilidad;

                // 👉 Actualizar banner de productos
                lblTipoCambioUsado.setText("💱 Tipo de cambio aplicado: " + tipoCambioActual);
                lblUtilidadUsada.setText("📈 Utilidad aplicada: " + utilidadActual + "%");

                // 👉 Refrescar tabla de productos
                tablaProductos.refresh();

                lblMensaje.setText("✅ Parámetros guardados y auditoría registrada");
                // 🔄 Limpiar campos después de guardar
                txtTipoCambio.clear();
                txtUtilidad.clear();
                dpVigencia.setValue(null);

                // 🔄 Limpiar mensaje después de unos segundos
                new Thread(() -> {
                    try {
                        Thread.sleep(2500);
                        javafx.application.Platform.runLater(() -> lblMensaje.setText(""));
                    } catch (InterruptedException ignored) {}
                }).start();



            }
        } catch (NumberFormatException ex) {
            logger.error("Error al parsear valores numéricos en parámetros comerciales", ex);
            lblMensaje.setText("❌ Error: valores numéricos inválidos");
        } catch (IllegalArgumentException ex) {
            logger.warn("Validación fallida en parámetros comerciales: {}", ex.getMessage());
            lblMensaje.setText("❌ Validación: " + ex.getMessage());
        } catch (SQLException ex) {
            logger.error("Error SQL al actualizar parámetros comerciales", ex);
            lblMensaje.setText("❌ Error SQL: " + ex.getMessage());
            ex.printStackTrace();
        }
    });

    // Layout de los campos
    VBox centroParametros = new VBox(10, txtTipoCambio, txtUtilidad, dpVigencia, btnGuardar, lblMensaje);
    centroParametros.setStyle("-fx-padding: 20;");
    rootParametros.setCenter(centroParametros);

    // 👉 Pestaña Parámetros Comerciales
    tabs.getTabs().add(new Tab("Parámetros Comerciales", rootParametros));

    // ----------------------
    // Tab Auditoría Parámetros
    TableView<AuditoriaRegistro> tablaAuditoria = new TableView<>();

    TableColumn<AuditoriaRegistro, String> colParametro = new TableColumn<>("Parámetro");
    colParametro.setCellValueFactory(new PropertyValueFactory<>("parametro"));

    TableColumn<AuditoriaRegistro, String> colAnterior = new TableColumn<>("Valor Anterior");
    colAnterior.setCellValueFactory(new PropertyValueFactory<>("valorAnterior"));

    TableColumn<AuditoriaRegistro, String> colNuevo = new TableColumn<>("Valor Nuevo");
    colNuevo.setCellValueFactory(new PropertyValueFactory<>("valorNuevo"));

    TableColumn<AuditoriaRegistro, String> colUsuario = new TableColumn<>("Usuario");
    colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));

    TableColumn<AuditoriaRegistro, String> colCorreo = new TableColumn<>("Correo");
    colCorreo.setCellValueFactory(new PropertyValueFactory<>("correoUsuario"));

    TableColumn<AuditoriaRegistro, LocalDateTime> colFecha = new TableColumn<>("Fecha Cambio");
    colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));

    tablaAuditoria.getColumns().addAll(colParametro, colAnterior, colNuevo, colUsuario, colCorreo, colFecha);

    // 👉 Declarar vistaAuditoria fuera del try
    VBox vistaAuditoria = new VBox(10, tablaAuditoria);
    vistaAuditoria.setStyle("-fx-padding: 20;");

    // Cargar datos desde el DAO
    try (Connection conn = DBConnection.getConnection()) {
    AuditoriaDAO auditoriaDAO = new AuditoriaDAO(conn);
    tablaAuditoria.getItems().addAll(auditoriaDAO.listarCambios());
}   catch (SQLException ex) {
    ex.printStackTrace();

    // Opcional: mostrar un mensaje en la UI
    Label lblError = new Label("❌ Error cargando auditoría: " + ex.getMessage());
    vistaAuditoria.getChildren().add(lblError);
}


   // 👉 Pestaña Auditoría Parámetros con recarga automática
Tab tabAuditoria = new Tab("Auditoría Parámetros", vistaAuditoria);

tabAuditoria.setOnSelectionChanged(e -> {
    if (tabAuditoria.isSelected()) {
        try (Connection conn = DBConnection.getConnection()) {
            AuditoriaDAO auditoriaDAO = new AuditoriaDAO(conn);

            tablaAuditoria.getItems().clear();
            tablaAuditoria.getItems().addAll(auditoriaDAO.listarCambios());

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
});

tabs.getTabs().add(tabAuditoria);

// ===============================
// MENÚ SUPERIOR (Archivo + Administración)
// ===============================

// Crear layout principal que contendrá menú + tabs
BorderPane root = new BorderPane();

// Insertar las pestañas en el centro
root.setCenter(tabs);

// Crear barra de menú
MenuBar menuBar = new MenuBar();

// Menú Archivo
Menu menuArchivo = new Menu("Archivo");
MenuItem salir = new MenuItem("Salir");
salir.setOnAction(e -> stage.close());
menuArchivo.getItems().add(salir);

// Menú Administración (solo admin)
Menu menuAdmin = new Menu("Administración");

if (Sesion.getUsuarioActual() != null &&
    "admin".equalsIgnoreCase(Sesion.getUsuarioActual().getRol())) {

    // ============================
    // 1) Crear usuario (modal)
    // ============================
    MenuItem crearUsuario = new MenuItem("Crear usuario");

    crearUsuario.setOnAction(e -> {
        LoginController login = new LoginController(null);
        login.mostrarFormularioRegistro(stage);
    });

    // ============================
    // 2) Gestionar usuarios (CRUD en pestaña)
    // ============================
    MenuItem gestionarUsuarios = new MenuItem("Gestionar usuarios");

    gestionarUsuarios.setOnAction(e -> {
        Tab tabUsuarios = new Tab("Usuarios");
        tabUsuarios.setClosable(true);

        UserCrudView vista = new UserCrudView(); // ← Clase que crearemos
        tabUsuarios.setContent(vista.getRoot());

        tabs.getTabs().add(tabUsuarios);
        tabs.getSelectionModel().select(tabUsuarios);
    });

    // Agregar ambos al menú Administración
    menuAdmin.getItems().addAll(crearUsuario, gestionarUsuarios);
}

// Agregar menús a la barra
menuBar.getMenus().addAll(menuArchivo, menuAdmin);

// Insertar menú arriba del layout
root.setTop(menuBar);

// 👉 AHORA SÍ crear la escena usando root (NO tabs)
Scene scene = new Scene(root, 1400, 1000);
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

    // Limpiar columnas anteriores para evitar duplicados
    tablaProductos.getColumns().clear();

    // ============================
    // FORMATOS NUMÉRICOS
    // ============================
    DecimalFormat formatoUSD = new DecimalFormat("#,##0.00");
    DecimalFormat formatoCLP = new DecimalFormat("#,###");

    // 👉 Columna: Descripción ES
    colDescEs = new TableColumn<>("Descripción ES");
    colDescEs.setCellValueFactory(cell ->
        new SimpleStringProperty(cell.getValue().getDescripcionEs())
    );

    // 👉 Columna: Descripción EN
    colDescEn = new TableColumn<>("Descripción EN");
    colDescEn.setCellValueFactory(cell ->
        new SimpleStringProperty(cell.getValue().getDescripcionEn())
    );

    // 👉 Columna: Unidad de medida
    colUnidad = new TableColumn<>("Unidad");
    colUnidad.setCellValueFactory(cell ->
        new SimpleStringProperty(cell.getValue().getUnidadMedida())
    );

    // 👉 Columna: Valor en pesos (precio base)
    colValor = new TableColumn<>("Valor Pesos");
    colValor.setCellValueFactory(cell ->
        new SimpleDoubleProperty(cell.getValue().getValorPesos()).asObject()
    );

    // ============================================================
    // 🔥 NUEVAS COLUMNAS CALCULADAS SEGÚN PARÁMETROS COMERCIALES
    // ============================================================

    // 👉 Precio en USD
    colPrecioUSD = new TableColumn<>("Precio USD");
    colPrecioUSD.setCellValueFactory(cell -> {
        double usd = 0.0;
        if (tipoCambioActual != 0) {
            usd = cell.getValue().getValorPesos() / tipoCambioActual;
        }
        return new SimpleDoubleProperty(usd).asObject();
    });
    colPrecioUSD.setCellFactory(col -> new TableCell<Producto, Double>() {
        @Override
        protected void updateItem(Double value, boolean empty) {
            super.updateItem(value, empty);
            if (empty || value == null) {
                setText(null);
            } else {
                setText(formatoUSD.format(value));
            }
        }
    });

    // 👉 Precio USD + utilidad (multiplicador)
    colPrecioUtil = new TableColumn<>("Precio + Utilidad (USD)");
    colPrecioUtil.setCellValueFactory(cell -> {
        double usd = 0.0;
        if (tipoCambioActual != 0) {
            usd = cell.getValue().getValorPesos() / tipoCambioActual;
        }
        double conUtil = usd * utilidadActual; // ✔ multiplicador correcto
        return new SimpleDoubleProperty(conUtil).asObject();
    });
    colPrecioUtil.setCellFactory(col -> new TableCell<Producto, Double>() {
        @Override
        protected void updateItem(Double value, boolean empty) {
            super.updateItem(value, empty);
            if (empty || value == null) {
                setText(null);
            } else {
                setText(formatoUSD.format(value));
            }
        }
    });

    // 👉 Precio final en CLP
    colPrecioFinalCLP = new TableColumn<>("Precio Final CLP");
    colPrecioFinalCLP.setCellValueFactory(cell -> {
        double usd = 0.0;
        if (tipoCambioActual != 0) {
            usd = cell.getValue().getValorPesos() / tipoCambioActual;
        }
        double conUtil = usd * utilidadActual;
        double finalClp = conUtil * tipoCambioActual;
        return new SimpleDoubleProperty(finalClp).asObject();
    });
    colPrecioFinalCLP.setCellFactory(col -> new TableCell<Producto, Double>() {
        @Override
        protected void updateItem(Double value, boolean empty) {
            super.updateItem(value, empty);
            if (empty || value == null) {
                setText(null);
            } else {
                setText("$ " + formatoCLP.format(value));
            }
        }
    });

    // ============================================================
    // 👉 Agregar todas las columnas a la tabla
    // ============================================================
    tablaProductos.getColumns().addAll(
        colDescEs,
        colDescEn,
        colUnidad,
        colValor,
        colPrecioUSD,
        colPrecioUtil,
        colPrecioFinalCLP
    );

    // 👉 Estilos opcionales
    tablaProductos.getStylesheets().add(
        getClass().getResource("/productos.css").toExternalForm()
    );
}




private void cargarProductos() {
    productos.setAll(productoService.listarProductos());
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
            System.out.println("✅ Comentario actualizado: " + event.getNewValue()); // Debug
        });
        colComentarios.setEditable(true); // Asegurar que la columna sea editable
        colComentarios.setPrefWidth(150); // Hacer la columna más ancha para comentarios

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
                        // Limpiar cualquier listener anterior
                        if (getItem() != null) {
                            getItem().precioNoEncontradoProperty().removeListener((obs, oldVal, newVal) -> {});
                        }
                    } else {
                        Main.this.actualizarEstiloFila(this, item);
                        
                        // Agregar listener para detectar cambios en precioNoEncontrado
                        item.precioNoEncontradoProperty().addListener((obs, oldVal, newVal) -> {
                            System.out.println("🐛 DEBUG - Listener activado para item: " + item.getDescripcion());
                            System.out.println("   - Valor anterior: " + oldVal);
                            System.out.println("   - Valor nuevo: " + newVal);
                            
                            if (!newVal.equals(oldVal)) {
                                javafx.application.Platform.runLater(() -> {
                                    System.out.println("🐛 DEBUG - Actualizando estilo de fila...");
                                    Main.this.actualizarEstiloFila(this, item);
                                });
                            }
                        });
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
        System.out.println("🐛 DEBUG - actualizarEstiloFila llamado");
        
        if (item == null) {
            System.out.println("🐛 DEBUG - item es null, limpiando estilo");
            row.setStyle("");
            return;
        }
        
        System.out.println("🐛 DEBUG - Item: " + item.getDescripcion());
        System.out.println("🐛 DEBUG - PrecioNoEncontrado: " + item.isPrecioNoEncontrado());
        System.out.println("🐛 DEBUG - Fila seleccionada: " + row.isSelected());
        
        StringBuilder estilo = new StringBuilder();
        
        // Si la fila está seleccionada, usar estilos más específicos
        if (row.isSelected()) {
            // Fondo más oscuro y texto forzado en negro con negrita
            if (item.isPrecioNoEncontrado()) {
                estilo.append("-fx-background-color: #e6e67a; "); // Amarillo más oscuro
                System.out.println("🐛 DEBUG - Aplicando amarillo oscuro (seleccionado)");
            } else {
                estilo.append("-fx-background-color: #5cb85c; "); // Verde más oscuro
                System.out.println("🐛 DEBUG - Aplicando verde oscuro (seleccionado)");
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
                System.out.println("🐛 DEBUG - Aplicando amarillo normal (no seleccionado)");
            } else {
                estilo.append("-fx-background-color: #6de26dff; ");
                System.out.println("🐛 DEBUG - Aplicando verde normal (no seleccionado)");
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
        
        System.out.println("🐛 DEBUG - Estilo final aplicado: " + estilo.toString());
        row.setStyle(estilo.toString());
    }

    private void probarConexion() {

    try (Connection conn = cl.vss.cotizador.util.DBConnection.getConnection()) {
                  System.out.println("✅ Conexión exitosa a PostgreSQL");
        }   catch (Exception e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
    }
}


    private void analizarEstructuraExcel(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo Excel para analizar");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos Excel", "*.xlsx"));
        File archivo = fileChooser.showOpenDialog(stage);

        if (archivo != null) {
            // Mostrar información de formatos soportados
            cotizacionService.mostrarInformacionFormatos();
            
            // Ejecutar análisis
            cotizacionService.analizarEstructuraExcel(archivo);
            
            // Mostrar diálogo con resumen
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Análisis Completado");
            alert.setHeaderText("Archivo analizado: " + archivo.getName());
            
            StringBuilder contenido = new StringBuilder();
            contenido.append("✅ Análisis completado exitosamente\n\n");
            contenido.append("📊 Formatos soportados:\n");
            for (String formato : cotizacionService.getFormatosSoportados()) {
                contenido.append("  • ").append(formato).append("\n");
            }
            contenido.append("\n🔍 Revise la consola para ver el análisis detallado.");
            
            alert.setContentText(contenido.toString());
            alert.showAndWait();
        }
    }

    private void cargarArchivo(Stage stage) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Seleccionar archivo Excel");
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos Excel", "*.xlsx"));
    File archivo = fileChooser.showOpenDialog(stage);

    if (archivo != null) {
        // Extraer cabecera
        //cabeceraActual = cotizacionService.extraerCabecera(archivo);
        //actualizarCamposCabecera();
        
        // Leer items
        List<ItemCotizacionExcel> items = cotizacionService.leerItemsDesdeExcel(archivo);

        // 👉 aplicar utilidad desde BD con manejo de SQLException
        try (Connection conn = DBConnection.getConnection()) {
            ParametrosDAO parametrosDAO = new ParametrosDAO(conn);
            double utilidad = parametrosDAO.getPorcentajeUtilidadActual();

            for (ItemCotizacionExcel item : items) {
                double precioBase = item.getPrecio();
                double precioFinal = precioBase * (1 + utilidad / 100);
                item.setPrecio(precioFinal);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "❌ Error al obtener utilidad desde BD: " + ex.getMessage());
            alert.showAndWait();
        }

        ObservableList<ItemCotizacionExcel> datos = FXCollections.observableArrayList(items);
        tabla.setItems(datos);
    }
}



    private void actualizarCamposCabecera() {
        if (cabeceraActual == null) {
            return;
        }
        
        // Actualizar campos de texto
        txtNombreCliente.setText(cabeceraActual.getNombreCliente() != null ? cabeceraActual.getNombreCliente() : "");
        txtIdCliente.setText(cabeceraActual.getIdCliente() != null ? cabeceraActual.getIdCliente() : "");
        txtEmpresa.setText(cabeceraActual.getEmpresaCliente() != null ? cabeceraActual.getEmpresaCliente() : "");
        txtNumeroCotizacion.setText(cabeceraActual.getNumeroCotizacion() != null ? cabeceraActual.getNumeroCotizacion() : "");
        txtReferencia.setText(cabeceraActual.getReferencia() != null ? cabeceraActual.getReferencia() : "");
        txtObservaciones.setText(cabeceraActual.getObservaciones() != null ? cabeceraActual.getObservaciones() : "");
        
        if (cabeceraActual.getFecha() != null) {
            lblFecha.setText("📅 " + cabeceraActual.getFecha().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        } else {
            lblFecha.setText("📅 No disponible");
        }
    }

    private VBox crearPanelCabecera() {
        VBox panelCabecera = new VBox(10);
        panelCabecera.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-padding: 15; -fx-background-color: #f9f9f9;");
        
        // Título
        //Label titulo = new Label("📋 Datos de Cabecera");
        //titulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333333;");
        
        // Primer fila: Cliente y ID
        HBox fila1 = new HBox(15);
        fila1.setPrefHeight(60);
        
        VBox campoNombre = new VBox(3);
        //Label lblNombre = new Label("Cliente:");
        //lblNombre.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #555;");
        //txtNombreCliente = new TextField();
        //txtNombreCliente.setPromptText("Nombre del cliente...");
        //txtNombreCliente.setStyle("-fx-font-size: 11px;");
        //campoNombre.getChildren().addAll(lblNombre, txtNombreCliente);
        
        VBox campoId = new VBox(3);
        //Label lblId = new Label("ID Cliente:");
        //lblId.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #555;");
        //txtIdCliente = new TextField();
        //txtIdCliente.setPromptText("ID o código...");
        //txtIdCliente.setStyle("-fx-font-size: 11px;");
        //campoId.getChildren().addAll(lblId, txtIdCliente);
        //campoId.setPrefWidth(150);
        
        HBox.setHgrow(campoNombre, javafx.scene.layout.Priority.ALWAYS);
        fila1.getChildren().addAll(campoNombre, campoId);
        
        // Segunda fila: Empresa
        HBox fila2 = new HBox(15);
        fila2.setPrefHeight(50);
        
        //VBox campoEmpresa = new VBox(3);
        //Label lblEmpresa = new Label("Empresa/Razón Social:");
        //lblEmpresa.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #555;");
        //txtEmpresa = new TextField();
        //txtEmpresa.setPromptText("Empresa...");
        //txtEmpresa.setStyle("-fx-font-size: 11px;");
        //campoEmpresa.getChildren().addAll(lblEmpresa, txtEmpresa);
        
        //HBox.setHgrow(campoEmpresa, javafx.scene.layout.Priority.ALWAYS);
        //fila2.getChildren().add(campoEmpresa);
        
        // Tercera fila: Cotización, Referencia y Fecha
        /*HBox fila3 = new HBox(15);
        fila3.setPrefHeight(60);
        
        VBox campoCotizacion = new VBox(3);
        Label lblCotizacion = new Label("Nº Cotización:");
        lblCotizacion.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #555;");
        txtNumeroCotizacion = new TextField();
        txtNumeroCotizacion.setPromptText("Número...");
        txtNumeroCotizacion.setStyle("-fx-font-size: 11px;");
        campoCotizacion.getChildren().addAll(lblCotizacion, txtNumeroCotizacion);
        
        VBox campoReferencia = new VBox(3);
        Label lblReferencia = new Label("Referencia:");
        lblReferencia.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #555;");
        txtReferencia = new TextField();
        txtReferencia.setPromptText("PO, RFQ, etc...");
        txtReferencia.setStyle("-fx-font-size: 11px;");
        campoReferencia.getChildren().addAll(lblReferencia, txtReferencia);
        
        VBox campoFecha = new VBox(3);
        Label lblFechaLabel = new Label("Fecha:");
        lblFechaLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #555;");
        lblFecha = new Label("📅 No disponible");
        lblFecha.setStyle("-fx-font-size: 11px; -fx-text-fill: #2196F3;");
        campoFecha.getChildren().addAll(lblFechaLabel, lblFecha);*/
        
        //fila3.getChildren().addAll(campoCotizacion, campoReferencia, campoFecha);
        
        // Cuarta fila: Observaciones (ancho completo)
        /*HBox fila4 = new HBox(10);
        fila4.setPrefHeight(80);
        
        VBox campoObservaciones = new VBox(3);
        Label lblObservaciones = new Label("Observaciones/Comentarios:");
        lblObservaciones.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #555;");
        txtObservaciones = new TextArea();
        txtObservaciones.setPromptText("Notas, comentarios especiales...");
        txtObservaciones.setStyle("-fx-font-size: 11px; -fx-control-inner-background: #ffffff;");
        txtObservaciones.setWrapText(true);
        txtObservaciones.setPrefRowCount(3);
        //campoObservaciones.getChildren().addAll(lblObservaciones, txtObservaciones);*/
        
        //HBox.setHgrow(campoObservaciones, javafx.scene.layout.Priority.ALWAYS);
        //fila4.getChildren().add(campoObservaciones);
        
        // Botones de acción
        HBox filaAcciones = new HBox(10);
        filaAcciones.setStyle("-fx-alignment: center-left;");
        
        /*Button btnLimpiarCabecera = new Button("🗑️ Limpiar Cabecera");
        btnLimpiarCabecera.setStyle("-fx-font-size: 11px; -fx-padding: 5px 10px;");
        btnLimpiarCabecera.setOnAction(e -> limpiarCabecera());*/
        
        /*Button btnCopiarCliente = new Button("📋 Copiar Datos");
        btnCopiarCliente.setStyle("-fx-font-size: 11px; -fx-padding: 5px 10px;");
        btnCopiarCliente.setOnAction(e -> copiarDatosCabecera());*/
        
        //filaAcciones.getChildren().addAll(btnLimpiarCabecera, btnCopiarCliente);
        //filaAcciones.getChildren().addAll(btnLimpiarCabecera, btnCopiarCliente);
        
        // Agregar todas las filas al panel
        //panelCabecera.getChildren().addAll(titulo, fila1, fila2, fila3, fila4, filaAcciones);
        //panelCabecera.getChildren().addAll(fila1, fila2, fila3, fila4, filaAcciones);
        panelCabecera.getChildren().addAll(fila1, fila2, filaAcciones);
        
        return panelCabecera;
    }
    
    private void limpiarCabecera() {
        txtNombreCliente.clear();
        txtIdCliente.clear();
        txtEmpresa.clear();
        txtNumeroCotizacion.clear();
        txtReferencia.clear();
        txtObservaciones.clear();
        lblFecha.setText("📅 No disponible");
        cabeceraActual = null;
    }
    
    private void copiarDatosCabecera() {
        StringBuilder datos = new StringBuilder();
        datos.append("Cliente: ").append(txtNombreCliente.getText()).append("\n");
        datos.append("ID: ").append(txtIdCliente.getText()).append("\n");
        datos.append("Empresa: ").append(txtEmpresa.getText()).append("\n");
        datos.append("Cotización: ").append(txtNumeroCotizacion.getText()).append("\n");
        datos.append("Referencia: ").append(txtReferencia.getText()).append("\n");
        datos.append("Observaciones: ").append(txtObservaciones.getText());
        
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(datos.toString());
        clipboard.setContent(content);
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Datos Copiados");
        alert.setHeaderText(null);
        alert.setContentText("Los datos de la cabecera han sido copiados al portapapeles.");
        alert.showAndWait();
    }

    private void manejarAccionItem(ItemCotizacionExcel item) {
        // Buscar productos similares en la base de datos
        List<cl.vss.cotizador.model.ProductoSimilar> productosSimilares = 
            cotizacionService.buscarProductosSimilares(item.getDescripcion());
        
        // Si no se encuentran productos, ejecutar diagnóstico
        if (productosSimilares.isEmpty()) {
            System.out.println("🔍 No se encontraron productos similares, ejecutando diagnóstico...");
            cotizacionService.diagnosticarTablaProducto();
            cotizacionService.diagnosticarVistaProductoPrecio();
        }
        
        // Crear diálogo personalizado con tabla
        Dialog<Void> dialogProductosSimilares = new Dialog<>();
        dialogProductosSimilares.setTitle("Productos Similares");
        dialogProductosSimilares.setHeaderText("Productos encontrados para: " + item.getDescripcion());
        
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
        
        // Nueva columna para Precio Venta Neto en dólares
        TableColumn<cl.vss.cotizador.model.ProductoSimilar, Double> colPrecioVentaNeto = new TableColumn<>("Precio Venta Neto");
        colPrecioVentaNeto.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getPrecioVentaNetoDolares()).asObject());
        colPrecioVentaNeto.setPrefWidth(140);
        
        // Formatear la columna de precio venta neto para mostrar como moneda en dólares
        colPrecioVentaNeto.setCellFactory(column -> new TableCell<cl.vss.cotizador.model.ProductoSimilar, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("US$%,.2f", item));
                }
            }
        });
        
        // Agregar columnas a la tabla
        tablaProductosSimilares.getColumns().add(colDescEs);
        tablaProductosSimilares.getColumns().add(colDescEn);
        tablaProductosSimilares.getColumns().add(colUnidad);
        tablaProductosSimilares.getColumns().add(colPrecio);
        tablaProductosSimilares.getColumns().add(colPrecioVentaNeto);
        
        // Cargar datos en la tabla
        ObservableList<cl.vss.cotizador.model.ProductoSimilar> datosTabla = 
            FXCollections.observableArrayList(productosSimilares);
        tablaProductosSimilares.setItems(datosTabla);
        
        // Configurar tamaño de la tabla (aumentado para nueva columna)
        tablaProductosSimilares.setPrefSize(800, 300);
        
        // Crear panel de información del item original
        VBox infoPanel = new VBox(10);
        infoPanel.setStyle("-fx-padding: 10; -fx-background-color: #f0f0f0;");
        
        Label lblItemOriginal = new Label("📦 Item Original:");
        lblItemOriginal.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label lblDescripcion = new Label("Descripción: " + item.getDescripcion());
        Label lblCantidad = new Label("Cantidad: " + item.getCantidad());
        Label lblPrecioActual = new Label("Precio Actual: $" + String.format("%,.2f", item.getPrecio()));
        
        infoPanel.getChildren().addAll(lblItemOriginal, lblDescripcion, lblCantidad, lblPrecioActual);
        
        // Crear panel de comentarios
        VBox panelComentarios = new VBox(10);
        panelComentarios.setStyle("-fx-padding: 10; -fx-background-color: #e8f4f8;");
        
        Label lblComentarios = new Label("💬 Comentarios del Item:");
        lblComentarios.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        TextArea txtComentarios = new TextArea();
        txtComentarios.setPromptText("Ingrese comentarios adicionales para este item...");
        txtComentarios.setPrefRowCount(3);
        txtComentarios.setWrapText(true);
        txtComentarios.setText(item.getComentarios() != null ? item.getComentarios() : "");
        
        Button btnGuardarComentario = new Button("💾 Guardar Comentario");
        btnGuardarComentario.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        btnGuardarComentario.setPrefWidth(150);
        
        // Acción para guardar el comentario
        btnGuardarComentario.setOnAction(e -> {
            String nuevoComentario = txtComentarios.getText().trim();
            item.setComentarios(nuevoComentario);
            
            // Mostrar confirmación
            Alert confirmacion = new Alert(Alert.AlertType.INFORMATION);
            confirmacion.setTitle("Comentario Guardado");
            confirmacion.setHeaderText(null);
            confirmacion.setContentText("El comentario ha sido guardado para el item:\n" + item.getDescripcion());
            confirmacion.showAndWait();
            
            System.out.println("🔄 Comentario actualizado para " + item.getDescripcion() + ": " + nuevoComentario);
        });
        
        Button btnUtilizarDatosProducto = new Button("🔄 Utilizar Datos Producto");
        btnUtilizarDatosProducto.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");
        btnUtilizarDatosProducto.setPrefWidth(180);
        
        // Acción para utilizar datos del producto seleccionado
        btnUtilizarDatosProducto.setOnAction(e -> {
            // Obtener el producto seleccionado de la tabla
            cl.vss.cotizador.model.ProductoSimilar productoSeleccionado = 
                tablaProductosSimilares.getSelectionModel().getSelectedItem();
            
            if (productoSeleccionado != null) {
                // Actualizar los datos del item con los del producto seleccionado
                // vitem.setDescripcion(productoSeleccionado.getDescripcionEs());
                item.setPrecio(productoSeleccionado.getPrecioVentaNeto());
                
                // Actualizar el precio del item primero
                item.setPrecio(productoSeleccionado.getPrecioVentaNeto());
                
                // SOLUCION COMPLETA: Marcar el item como actualizado con precio encontrado
                item.setPrecioNoEncontrado(false);
                
                System.out.println("✅ ITEM ACTUALIZADO - Precio: $" + item.getPrecio() + ", PrecioNoEncontrado: " + item.isPrecioNoEncontrado());
                
                // MÉTODO MEJORADO: Forzar actualización completa de la UI
                javafx.application.Platform.runLater(() -> {
                    // 1. Refrescar la tabla completa
                    tabla.refresh();
                    
                    // 2. Encontrar el índice del item y seleccionarlo
                    int itemIndex = tabla.getItems().indexOf(item);
                    if (itemIndex >= 0) {
                        // 3. Limpiar selección actual y seleccionar el item actualizado
                        tabla.getSelectionModel().clearSelection();
                        tabla.getSelectionModel().select(itemIndex);
                        tabla.scrollTo(itemIndex);
                        tabla.getFocusModel().focus(itemIndex);
                        
                        // 4. Forzar segunda actualización para asegurar el pintado
                        javafx.application.Platform.runLater(() -> {
                            tabla.refresh();
                            System.out.println("🎨 PINTADO VERDE APLICADO - Fila " + itemIndex + " debería estar verde");
                        });
                    }
                });
                
                // Mostrar confirmación
                Alert confirmacion = new Alert(Alert.AlertType.INFORMATION);
                confirmacion.setTitle("Datos Actualizados");
                confirmacion.setHeaderText(null);
                confirmacion.setContentText("Los datos del item han sido actualizados con:\n" +
                                          //"Descripción: " + productoSeleccionado.getDescripcionEs() + "\n" +
                                          "Precio: $" + String.format("%,.2f", productoSeleccionado.getPrecioVentaNeto()) + "\n" +
                                          "Estado: Precio encontrado ✅");
                confirmacion.showAndWait();
                
                System.out.println("🔄 Datos del producto utilizados para " + item.getDescripcion() + 
                                 ": " + productoSeleccionado.getDescripcionEs() + 
                                 " - $" + productoSeleccionado.getPrecioVentaNeto());
                                 
                // Cerrar el diálogo después de actualizar
                dialogProductosSimilares.close();
            } else {
                // Mostrar advertencia si no hay producto seleccionado
                Alert advertencia = new Alert(Alert.AlertType.WARNING);
                advertencia.setTitle("Producto No Seleccionado");
                advertencia.setHeaderText(null);
                advertencia.setContentText("Por favor, seleccione un producto de la tabla antes de utilizar sus datos.");
                advertencia.showAndWait();
            }
        });
        
        HBox panelBotonComentario = new HBox(10);
        panelBotonComentario.setStyle("-fx-alignment: center-left;");
        panelBotonComentario.getChildren().addAll(btnGuardarComentario, btnUtilizarDatosProducto);
        
        panelComentarios.getChildren().addAll(lblComentarios, txtComentarios, panelBotonComentario);
        
        // Crear panel de búsqueda personalizada
        javafx.scene.layout.HBox panelBusqueda = new javafx.scene.layout.HBox(10);
        panelBusqueda.setStyle("-fx-padding: 10; -fx-alignment: center-left;");
        
        Label lblBuscar = new Label("🔍 Buscar otros productos:");
        lblBuscar.setStyle("-fx-font-weight: bold;");
        
        TextField txtBusqueda = new TextField();
        txtBusqueda.setPromptText("Ingrese términos de búsqueda...");
        txtBusqueda.setPrefWidth(300);
        txtBusqueda.setText(item.getDescripcion()); // Prellenar con la descripción actual
        
        Button btnBuscar = new Button("Buscar");
        btnBuscar.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        btnBuscar.setPrefWidth(80);
        
        // Acción del botón buscar
        btnBuscar.setOnAction(e -> {
            String terminoBusqueda = txtBusqueda.getText().trim();
            if (!terminoBusqueda.isEmpty()) {
                // Realizar nueva búsqueda
                List<cl.vss.cotizador.model.ProductoSimilar> nuevosResultados = 
                    cotizacionService.buscarProductosSimilares(terminoBusqueda);
                
                // Actualizar la tabla con los nuevos resultados  
                ObservableList<cl.vss.cotizador.model.ProductoSimilar> nuevosdatos = 
                    FXCollections.observableArrayList(nuevosResultados);
                tablaProductosSimilares.setItems(nuevosdatos);
                
                // Actualizar el header del diálogo
                dialogProductosSimilares.setHeaderText("Productos encontrados para: " + terminoBusqueda + 
                                                      " (" + nuevosResultados.size() + " resultados)");
                
                // Actualizar mensaje si no hay resultados
                if (nuevosResultados.isEmpty()) {
                    Label sinResultados = new Label("❌ No se encontraron productos para: " + terminoBusqueda);
                    sinResultados.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");
                    tablaProductosSimilares.setPlaceholder(sinResultados);
                }
            }
        });
        
        // Permitir búsqueda con Enter
        txtBusqueda.setOnAction(e -> btnBuscar.fire());
        
        panelBusqueda.getChildren().addAll(lblBuscar, txtBusqueda, btnBuscar);
        
        // Panel principal que combina información, comentarios, búsqueda y tabla
        VBox contenidoPrincipal = new VBox(15);
        contenidoPrincipal.getChildren().addAll(infoPanel,
                                              panelComentarios,
                                              panelBusqueda,
                                              new Label("🔍 Productos Similares Encontrados (" + productosSimilares.size() + "):"),
                                              tablaProductosSimilares);
        
        // Configurar el diálogo (aumentado para panel de comentarios)
        dialogProductosSimilares.getDialogPane().setContent(contenidoPrincipal);
        dialogProductosSimilares.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialogProductosSimilares.getDialogPane().setPrefSize(850, 600);
        
        // Mensaje si no se encontraron productos
        if (productosSimilares.isEmpty()) {
            Label sinResultados = new Label("❌ No se encontraron productos similares en la base de datos.");
            sinResultados.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");
            tablaProductosSimilares.setPlaceholder(sinResultados);
        }
        
        // Mostrar el diálogo
        dialogProductosSimilares.showAndWait();
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

    // 👉 Métodos CRUD de Productos *****************************************************

    private void abrirDialogAgregarProducto() {
    Dialog<Producto> dialog = new Dialog<>();
    dialog.setTitle("Agregar Producto");

    TextField txtDescEs = new TextField();
    TextField txtDescEn = new TextField();
    TextField txtUnidad = new TextField();
    TextField txtValor = new TextField();

    // 👉 TextFormatter para permitir solo números positivos en Valor
    UnaryOperator<TextFormatter.Change> filtroNumerico = change -> {
        String nuevoTexto = change.getControlNewText();
        if (nuevoTexto.matches("\\d*(\\.\\d*)?")) { // solo números y decimales
            return change;
        }
        return null;
    };
    txtValor.setTextFormatter(new TextFormatter<>(filtroNumerico));

    // 👉 ComboBox de familias
    ComboBox<Familia> comboFamilia = crearComboFamilias();

    VBox content = new VBox(10,
        new Label("Descripción ES:"), txtDescEs,
        new Label("Descripción EN:"), txtDescEn,
        new Label("Unidad:"), txtUnidad,
        new Label("Valor Pesos:"), txtValor,
        new Label("Familia:"), comboFamilia
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
                String valorStr = txtValor.getText().trim();

                // Reset estilos antes de validar
                txtDescEs.setStyle("");
                txtUnidad.setStyle("");
                txtValor.setStyle("");

                if (descEs.isEmpty()) {
                    txtDescEs.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                    throw new IllegalArgumentException("Descripción ES es obligatoria.");
                }
                if (unidad.isEmpty()) {
                    txtUnidad.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                    throw new IllegalArgumentException("Unidad es obligatoria.");
                }
                if (valorStr.isEmpty()) {
                    txtValor.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                    throw new IllegalArgumentException("El valor es obligatorio.");
                }

                double valor = Double.parseDouble(valorStr);
                if (valor <= 0) {
                    txtValor.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                    throw new IllegalArgumentException("El valor debe ser mayor que 0.");
                }

                if (comboFamilia.getValue() == null) {
                    throw new IllegalArgumentException("Debe seleccionar una familia.");
                }

                // 👉 Prevenir duplicados por descripción ES
                boolean existe = productoService.listarProductos().stream()
                    .anyMatch(p -> p.getDescripcionEs().equalsIgnoreCase(descEs));
                if (existe) {
                    throw new IllegalArgumentException("Ya existe un producto con esa descripción ES.");
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
                logger.error("Error al parsear valor numérico en agregar producto", nfe);
                mostrarError("Valor inválido", "El campo 'Valor Pesos' debe ser numérico.\n" + nfe.getMessage());
            } catch (Exception ex) {
                logger.error("Error al validar datos de nuevo producto", ex);
                mostrarError("Datos inválidos", "Revisa los campos.\n" + ex.getMessage());
            }
        }
        return null;
    });

    dialog.showAndWait().ifPresent(p -> {
        try {
            productoService.agregarProducto(p);
            cargarProductos();

            // 👉 Mensaje de confirmación
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Acción exitosa");
            alert.setHeaderText(null);
            alert.setContentText("Producto agregado correctamente.");
            alert.showAndWait();

        } catch (Exception e) {
            logger.error("Error al guardar producto en base de datos", e);
            mostrarError("Error al guardar", e.getMessage());
        }
    });
}



   private void abrirDialogEditarProducto() {
    Producto seleccionado = tablaProductos.getSelectionModel().getSelectedItem();
    if (seleccionado == null) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Sin selección");
        alert.setHeaderText(null);
        alert.setContentText("Debes seleccionar un producto para editar.");
        alert.showAndWait();
        return;
    }

    Dialog<Producto> dialog = new Dialog<>();
    dialog.setTitle("Editar Producto");

    TextField txtDescEs = new TextField(seleccionado.getDescripcionEs());
    TextField txtDescEn = new TextField(seleccionado.getDescripcionEn());
    TextField txtUnidad = new TextField(seleccionado.getUnidadMedida());
    TextField txtValor = new TextField(String.valueOf(seleccionado.getValorPesos()));

    // 👉 TextFormatter para permitir solo números positivos en Valor
    UnaryOperator<TextFormatter.Change> filtroNumerico = change -> {
        String nuevoTexto = change.getControlNewText();
        if (nuevoTexto.matches("\\d*(\\.\\d*)?")) {
            return change;
        }
        return null;
    };
    txtValor.setTextFormatter(new TextFormatter<>(filtroNumerico));

    // 👉 ComboBox de familias
    ComboBox<Familia> comboFamilia = crearComboFamilias();
    comboFamilia.setValue(buscarFamiliaPorId(seleccionado.getFamiliaId()));

    VBox content = new VBox(10,
        new Label("Descripción ES:"), txtDescEs,
        new Label("Descripción EN:"), txtDescEn,
        new Label("Unidad:"), txtUnidad,
        new Label("Valor Pesos:"), txtValor,
        new Label("Familia:"), comboFamilia
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
                String valorStr = txtValor.getText().trim();

                // Reset estilos antes de validar
                txtDescEs.setStyle("");
                txtUnidad.setStyle("");
                txtValor.setStyle("");

                if (descEs.isEmpty()) {
                    txtDescEs.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                    throw new IllegalArgumentException("Descripción ES es obligatoria.");
                }
                if (unidad.isEmpty()) {
                    txtUnidad.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                    throw new IllegalArgumentException("Unidad es obligatoria.");
                }
                if (valorStr.isEmpty()) {
                    txtValor.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                    throw new IllegalArgumentException("El valor es obligatorio.");
                }

                double valor = Double.parseDouble(valorStr);
                if (valor <= 0) {
                    txtValor.setStyle("-fx-border-color: red; -fx-border-width: 2;");
                    throw new IllegalArgumentException("El valor debe ser mayor que 0.");
                }

                if (comboFamilia.getValue() == null) {
                    throw new IllegalArgumentException("Debe seleccionar una familia.");
                }

                // 👉 Prevenir duplicados por descripción ES (excepto el mismo producto)
                boolean existe = productoService.listarProductos().stream()
                    .anyMatch(p -> p.getDescripcionEs().equalsIgnoreCase(descEs)
                                && p.getId() != seleccionado.getId());
                if (existe) {
                    throw new IllegalArgumentException("Ya existe otro producto con esa descripción ES.");
                }

                // Actualizar objeto seleccionado
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

            // 👉 Mensaje de confirmación
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Acción exitosa");
            alert.setHeaderText(null);
            alert.setContentText("Producto editado correctamente.");
            alert.showAndWait();

        } catch (Exception e) {
            mostrarError("Error al actualizar", e.getMessage());
        }
    });
}


   private void eliminarProductoSeleccionado() {
    Producto seleccionado = tablaProductos.getSelectionModel().getSelectedItem();
    if (seleccionado == null) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Sin selección");
        alert.setHeaderText(null);
        alert.setContentText("Debes seleccionar un producto para eliminar.");
        alert.showAndWait();
        return;
    }

    // 👉 Confirmación antes de eliminar
    Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
    confirmacion.setTitle("Confirmar eliminación");
    confirmacion.setHeaderText("¿Eliminar producto?");
    confirmacion.setContentText("¿Estás seguro de que quieres eliminar el producto:\n\n" 
                                + seleccionado.getDescripcionEs() + "?");

    Optional<ButtonType> resultado = confirmacion.showAndWait();
    if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
        try {
            productoService.eliminarProducto(seleccionado.getId());
            cargarProductos();

            // 👉 Mensaje de éxito
            Alert exito = new Alert(Alert.AlertType.INFORMATION);
            exito.setTitle("Acción exitosa");
            exito.setHeaderText(null);
            exito.setContentText("Producto eliminado correctamente.");
            exito.showAndWait();

        } catch (Exception e) {
            mostrarError("Error al eliminar", e.getMessage());
        }
    }
}

private Familia buscarFamiliaPorId(int idFamilia) {
    List<Familia> familias = productoService.listarFamilias();
    for (Familia f : familias) {
        if (f.getId() == idFamilia) {
            return f;
        }
    }
    return null;
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

    private void aplicarFiltros(FilteredList<Producto> filtrados, String texto, String familiaNombre) {

    filtrados.setPredicate(producto -> {

        // Normalizar familia
        String familia = (familiaNombre == null) ? "" : familiaNombre.trim();
        // 👉 Filtro por familia
        if (!familia.equalsIgnoreCase("Todas") && !familia.isEmpty()) {

            // Proteger mapaFamilias
            if (mapaFamilias == null) {
                System.out.println("⚠️ mapaFamilias es NULL, cargándolo...");
                mapaFamilias = productoService.obtenerMapaFamilias();
            }

            int idFamiliaFiltro = mapaFamilias.getOrDefault(familia, -1);

            System.out.println("   → ID familia filtro: " + idFamiliaFiltro + " | Evaluando producto ID familia: " + producto.getFamiliaId());

            if (producto.getFamiliaId() != idFamiliaFiltro) {
                return false;
            }
        }

        // 👉 Filtro por texto
        if (texto == null || texto.isEmpty()) {
            return true; // si no hay texto, ya filtramos por familia arriba
        }

        String filtro = texto.toLowerCase();

        // 👉 Protección contra nulls
        String descEs = producto.getDescripcionEs() == null ? "" : producto.getDescripcionEs().toLowerCase();
        String descEn = producto.getDescripcionEn() == null ? "" : producto.getDescripcionEn().toLowerCase();
        String unidad = producto.getUnidadMedida() == null ? "" : producto.getUnidadMedida().toLowerCase();
        String id = String.valueOf(producto.getId());
        String valor = String.valueOf(producto.getValorPesos());

        return descEs.contains(filtro)
            || descEn.contains(filtro)
            || unidad.contains(filtro)
            || id.contains(filtro)
            || valor.contains(filtro);
    });
}
 // ============================================================
// 🔵 RECARGAR PARÁMETROS COMERCIALES DESDE LA BD
// ============================================================
private void recargarParametrosDesdeBD() {
    try {
        // Obtener conexión desde tu clase real
        Connection conn = DBConnection.getConnection();

        ParametrosDAO dao = new ParametrosDAO(conn);

        // Cargar valores reales desde la BD
        tipoCambioActual = dao.getTipoCambioActual();
        utilidadActual = dao.getPorcentajeUtilidadActual();

        // Actualizar banner y tabla
        actualizarBannerParametros();
        tablaProductos.refresh();

    } catch (Exception e) {
        logger.error("Error al cargar parámetros comerciales", e);
    }
}


// ============================================================
// 🔵 ACTUALIZAR BANNER DE PARÁMETROS
// ============================================================
private void actualizarBannerParametros() {
    if (lblTipoCambioUsado != null) {
        lblTipoCambioUsado.setText("💱 Tipo de cambio aplicado: " + tipoCambioActual);
    }
    if (lblUtilidadUsada != null) {
        lblUtilidadUsada.setText("📈 Utilidad aplicada: " + utilidadActual + "%");
    }
}

// ============================================================
// 🔵 FILTRADO AVANZADO DE PRODUCTOS
// ============================================================
private void filtrarProductos() {
    if (filteredProductos == null) return;

    filteredProductos.setPredicate(prod -> {
        if (prod == null) return false;

        String texto = txtBuscar.getText() != null ? txtBuscar.getText().toLowerCase() : "";
        String familiaSeleccionada = comboFamilias.getValue();

        boolean coincideTexto =
                prod.getDescripcionEs().toLowerCase().contains(texto) ||
                prod.getDescripcionEn().toLowerCase().contains(texto);

        boolean coincideFamilia = true;
        if (familiaSeleccionada != null && !familiaSeleccionada.equals("Todas")) {

            Integer idFamiliaSeleccionada = mapaFamilias.get(familiaSeleccionada);

            coincideFamilia =
                    idFamiliaSeleccionada != null &&
                    idFamiliaSeleccionada == prod.getFamiliaId();
        }

        return coincideTexto && coincideFamilia;
    });
}






    // ⚠️ El main siempre al final
    public static void main(String[] args) {
        launch(args);
    }

}
