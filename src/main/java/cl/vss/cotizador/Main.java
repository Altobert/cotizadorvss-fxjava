package cl.vss.cotizador;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;

import javafx.stage.FileChooser;



import cl.vss.cotizador.demo.LoginController;
import cl.vss.cotizador.model.Broker;
import cl.vss.cotizador.model.BrokerFormato;
import cl.vss.cotizador.model.BrokerMetadata;
import cl.vss.cotizador.model.Familia;
import cl.vss.cotizador.model.FormatoColumna;
import cl.vss.cotizador.model.ItemCotizacionExcel;
import cl.vss.cotizador.model.RowData;
import cl.vss.cotizador.service.AuditoriaDAO;
import cl.vss.cotizador.service.BrokerDAO;
import cl.vss.cotizador.service.BrokerMetadataDAO;
import cl.vss.cotizador.service.FormatoDAO;
import cl.vss.cotizador.service.AuditoriaRegistro;
import cl.vss.cotizador.service.AsposeExcelService;
import cl.vss.cotizador.service.CotizacionService;
import cl.vss.cotizador.service.ParametrosDAO;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
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
import cl.vss.cotizador.view.CargaCotizacionProgressController;
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
    
    // 👉 ComboBox para brokers en la pestaña Cotización
    private ComboBox<Broker> comboBrokers;
    private ObservableList<Broker> listaBrokers;
    private Button btnCargarCotizacion;
    private Button btnExportarCotizacion;
    private CargaCotizacionProgressController cargaProgressController;
    private boolean exportacionEnCurso = false;
    
    // 👉 Tabla dinámica para cotizaciones con formato de broker
    private final TableView<RowData> tablaDinamica = new TableView<>();
    private BrokerFormato formatoActual;
    
    // Constructor o inicialización
    {
        tablaDinamica.setEditable(true); // Hacer la tabla editable
    }
    
    // 👉 Panel de metadata del broker
    private VBox panelMetadata;
    private Map<String, List<BrokerMetadata>> metadataActual;
 
    // 👉 Variables para parámetros comerciales en productos
    private double tipoCambioActual = 1.0;
    private double utilidadActual = 0.0;
    private Label lblTipoCambioUsado;
    private Label lblUtilidadUsada;

    // ✅ Mapa en memoria para filtros instantáneos
    private Map<String, Integer> mapaFamilias;
    
    // 🔷 Aspose: Servicio para manejo de Excel con macros
    private final AsposeExcelService asposeService = AsposeExcelService.getInstance();
    private String rutaArchivoConMacros = null; // Ruta del archivo .xlsm cargado

    // 🔷 CMA CGM: filas especiales detectadas durante la carga (fila 0-based → fórmula original)
    // Contiene TODAS las filas "Item Sub Total", "Total Price" y "Grand Total" con sus fórmulas
    private java.util.TreeMap<Integer, String> cmaCgmSpecialRows = new java.util.TreeMap<>();

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
    
    // 👉 Botón para cargar cotizaciones
    btnCargarCotizacion = new Button("📤 Cargar Cotización");
    btnCargarCotizacion.setOnAction(e -> cargarArchivo(stage));
    
    //Button btnAnalizar = new Button("🔍 Analizar Estructura Excel");
    //btnAnalizar.setDisable(true); // Deshabilitado inicialmente
    //btnAnalizar.setOnAction(e -> analizarEstructuraExcel(stage));

    Button btnLimpiar = new Button("🗑️ Limpiar Tabla");
    btnLimpiar.setOnAction(e -> limpiarTabla());

    btnExportarCotizacion = new Button("💾 Exportar Cotización");
    btnExportarCotizacion.setOnAction(e -> exportarCotizacion(stage));

    // 👉 ComboBox de Brokers
    comboBrokers = new ComboBox<>();
    comboBrokers.setPromptText("Seleccionar Broker...");
    comboBrokers.setPrefWidth(200);
    cargarBrokers(); // Cargar brokers desde la BD
    
    // 👉 Listener para detectar formato al seleccionar broker
    comboBrokers.valueProperty().addListener((obs, oldBroker, newBroker) -> {
        if (newBroker != null) {
            cargarFormatoBroker(newBroker);
        } else {
            formatoActual = null;
        }
    });

    // 👉 aplicar estilo corporativo VSS (azul con letras blancas)
    btnCargarCotizacion.getStyleClass().add("color-primario");
    //btnAnalizar.getStyleClass().add("color-primario");
    btnLimpiar.getStyleClass().add("color-primario");
    btnExportarCotizacion.getStyleClass().add("color-primario");

    cargaProgressController = new CargaCotizacionProgressController(btnCargarCotizacion, comboBrokers);
    HBox estadoCarga = cargaProgressController.getVista();

    //ToolBar barraCotizador = new ToolBar(btnCargar, btnAnalizar, new Separator(), btnLimpiar, btnExportar);
    ToolBar barraCotizador = new ToolBar(
        btnCargarCotizacion,
        new Separator(),        
        btnLimpiar,
        new Separator(), 
        btnExportarCotizacion,
        new Separator(),        
        new Label("Broker:"), comboBrokers,
        estadoCarga,
        new Separator() 
    );
    rootCotizador.setTop(barraCotizador);
    
    // TODO: Descomentar cuando se necesite usar el panel de metadata
    /*
    // Crear panel con metadata dentro de un ScrollPane con altura limitada
    panelMetadata = crearPanelMetadata(); // Inicializar panel de metadata vacío
    ScrollPane scrollMetadata = new ScrollPane(panelMetadata);
    scrollMetadata.setFitToWidth(true);
    scrollMetadata.setMaxHeight(50); // Altura máxima del panel de metadata (reducida)
    scrollMetadata.setMinHeight(50); // Altura mínima (reducida)
    scrollMetadata.setStyle("-fx-background-color: transparent;");
    
    // Panel principal con metadata arriba y tabla abajo
    VBox panelConMetadata = new VBox(10);
    panelConMetadata.getChildren().addAll(scrollMetadata, tablaDinamica);
    VBox.setVgrow(tablaDinamica, javafx.scene.layout.Priority.ALWAYS);
    panelConMetadata.setStyle("-fx-padding: 10;");
    
    rootCotizador.setCenter(panelConMetadata);
    */
    
    // Panel simplificado solo con la tabla dinámica
    VBox panelConMetadata = new VBox(10);
    panelConMetadata.getChildren().add(tablaDinamica);
    VBox.setVgrow(tablaDinamica, javafx.scene.layout.Priority.ALWAYS);
    panelConMetadata.setStyle("-fx-padding: 10;");
    
    rootCotizador.setCenter(panelConMetadata);


    // Tab Productos
    BorderPane rootProductos = new BorderPane();

    // Botones con handlers
    Button btnAgregar = new Button("➕ Agregar");
    Button btnEditar  = new Button("✏️ Editar");
    Button btnEliminar = new Button("🗑️ Eliminar");

    // Nuevo botón Exportar (Maestra producto)
    Button btnExportarExcel = new Button("📤 Exportar Excel");
    btnExportarExcel.getStyleClass().add("color-primario");
    btnExportarExcel.setOnAction(e -> exportarExcelTodasLasFamilias());

    
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
    btnExportarExcel,   // ← AQUÍ LO AGREGAMOS
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

// 👉 Pestaña Cotizador
Tab tabCotizador = new Tab("Cotizador", rootCotizador);
tabCotizador.setClosable(false);
tabs.getTabs().add(tabCotizador);

// 👉 Pestaña Productos (versión corregida con listener)
Tab tabProductos = new Tab("Productos", rootProductos);
tabProductos.setClosable(false);

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
    Tab tabParametros = new Tab("Parámetros Comerciales", rootParametros);
    tabParametros.setClosable(false);
    tabs.getTabs().add(tabParametros);

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
tabAuditoria.setClosable(false);

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

// Menú Ayuda
Menu menuAyuda = new Menu("Ayuda");
MenuItem acercaDe = new MenuItem("Acerca de...");
acercaDe.setOnAction(e -> cl.vss.cotizador.util.Version.mostrarDialogoVersion(stage));
menuAyuda.getItems().add(acercaDe);

// Agregar menús a la barra
menuBar.getMenus().addAll(menuArchivo, menuAdmin, menuAyuda);

// Insertar menú arriba del layout

//root.setTop(menuBar);

// 👉 Crear label con el nombre del usuario
Label lblUsuario = new Label("👤 " + Sesion.getUsuarioActual().getNombre());
lblUsuario.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

// 👉 Contenedor alineado a la derecha
HBox barraUsuario = new HBox(lblUsuario);
barraUsuario.setAlignment(Pos.CENTER_RIGHT);
barraUsuario.setPadding(new Insets(5));

// 👉 Combinar menú + usuario en un VBox
VBox topLayout = new VBox(menuBar, barraUsuario);

// 👉 Insertar en el top del layout principal
root.setTop(topLayout);




// 👉 AHORA SÍ crear la escena usando root (NO tabs)
Scene scene = new Scene(root, 1200, 800);
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
        
        /**
         * // 💰 Nueva columna: Precio VSS Calculado (precio_venta_neto * cantidad)
        TableColumn<ItemCotizacionExcel, Double> colPrecioVSS = new TableColumn<>("Precio VSS");
        colPrecioVSS.setCellValueFactory(cellData -> cellData.getValue().precioVSSCalculadoProperty().asObject());
        colPrecioVSS.setCellFactory(col -> new TableCell<ItemCotizacionExcel, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("$%.2f", value));
                    // Resaltar en verde si hay precio calculado
                    if (value > 0.0) {
                        setStyle("-fx-background-color: #c8e6c9; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-background-color: #ffcccc;");
                    }
                }
            }
        });
        colPrecioVSS.setPrefWidth(120);
        colPrecioVSS.setEditable(false); // No editable, es un cálculo automático
         */

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
            colDescuento, colTotalNeto, colComentarios, colDisponibilidad, colTotalBruto, 
            /*colPrecioVSS,*/ colAccion
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
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos Excel", "*.xlsx", "*.xlsm"));
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

        if (cargaProgressController != null && cargaProgressController.isEnCurso()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Carga en proceso");
            alert.setHeaderText("Ya hay una cotización cargándose");
            alert.setContentText("Espera a que termine la carga actual para iniciar otra.");
            alert.showAndWait();
            return;
        }

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Seleccionar archivo Excel");
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos Excel", "*.xlsx", "*.xlsm"));
    File archivo = fileChooser.showOpenDialog(stage);

    if (archivo == null) {
        return;
    }

    if (cargaProgressController != null) {
        cargaProgressController.iniciar("Procesando " + archivo.getName() + "...");
    }

    Task<Void> taskCarga = new Task<Void>() {
        @Override
        protected Void call() {
            leerExcelConFormato(archivo);
            return null;
        }
    };

    taskCarga.setOnSucceeded(e -> {
        if (cargaProgressController != null) {
            cargaProgressController.completar("Carga completada");
        }
    });

    taskCarga.setOnFailed(e -> {
        if (cargaProgressController != null) {
            cargaProgressController.error("Error en la carga");
        }

        Throwable error = taskCarga.getException();
        if (error != null) {
            logger.error("Error no controlado en carga de cotización", error);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error al cargar cotización");
            alert.setContentText(error.getMessage() != null ? error.getMessage() : "Error desconocido.");
            alert.showAndWait();
        }
    });

    Thread hiloCarga = new Thread(taskCarga, "carga-cotizacion-thread");
    hiloCarga.setDaemon(true);
    hiloCarga.start();
}

    private void ejecutarEnHiloFX(Runnable accion) {
        if (Platform.isFxApplicationThread()) {
            accion.run();
            return;
        }
        Platform.runLater(accion);
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
        tablaDinamica.getItems().clear();
        tablaDinamica.getColumns().clear();
        formatoActual = null;
        comboBrokers.getSelectionModel().clearSelection();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Tabla Limpiada");
        alert.setHeaderText(null);
        alert.setContentText("La tabla ha sido limpiada exitosamente.");
        alert.showAndWait();
    }

    private void exportarCotizacion(Stage stage) {

        if (exportacionEnCurso) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Exportación en proceso");
                alert.setHeaderText("Ya hay una exportación en curso");
                alert.setContentText("Espera a que termine la exportación actual para iniciar otra.");
                alert.showAndWait();
                return;
        }

        // Validar que hay datos para exportar
        if (tablaDinamica.getItems().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Sin datos");
            alert.setHeaderText("No hay datos para exportar");
            alert.setContentText("Primero debes cargar un archivo Excel con cotizaciones.");
            alert.showAndWait();
            return;
        }
        
        // Validar que hay un broker seleccionado
        if (formatoActual == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Sin broker seleccionado");
            alert.setHeaderText("Debes seleccionar un broker");
            alert.setContentText("Por favor, selecciona un broker antes de exportar.");
            alert.showAndWait();
            return;
        }
        
        // 🔷 Verificar si hay workbook Aspose cargado (archivo con macros)
        boolean usarAspose = asposeService.hayWorkbookCargado() && rutaArchivoConMacros != null;
        
        // Verificar si hay plantilla disponible para este broker
        String rutaPlantilla = formatoActual.getRutaPlantilla();
        boolean usarPlantilla = rutaPlantilla != null && !rutaPlantilla.isEmpty();
        
        logger.info("📤 Exportando cotización - Broker: {}, Plantilla: {}, Aspose: {}", 
            formatoActual.getBrokerName(), 
            usarPlantilla ? rutaPlantilla : "NO",
            usarAspose ? "✅ SÍ (macros preservadas)" : "❌ NO");
        
        // FileChooser para seleccionar ubicación
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Cotización");
        
        // Si hay Aspose o plantilla, usar extensión .xlsm para preservar macros
        String extension = (usarAspose || usarPlantilla) ? ".xlsm" : ".xlsx";
        fileChooser.setInitialFileName("Cotizacion_" + formatoActual.getBrokerName() + "_" + 
            LocalDate.now().toString() + extension);
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*" + extension)
        );
        
        File archivo = fileChooser.showSaveDialog(stage);
        if (archivo == null) {
            return; // Usuario canceló
        }
        final List<RowData> datosExportar = new java.util.ArrayList<>(tablaDinamica.getItems());

        // Capturar UNIT_PRICE en el hilo FX (garantiza visibilidad de memoria entre hilos)
        // SimpleStringProperty no es volatile; el background thread podría ver valores stale.
        final java.util.List<String> unitPriceSnapshotFX = new java.util.ArrayList<>();
        for (RowData rd : datosExportar) {
            unitPriceSnapshotFX.add(rd.get("UNIT_PRICE"));
        }
        // 🔍 DIAGNÓSTICO: Mostrar primeros 5 valores capturados del snapshot
        logger.info("🔍 SNAPSHOT UNIT_PRICE capturado en hilo FX ({} items):", unitPriceSnapshotFX.size());
        for (int i = 0; i < Math.min(5, unitPriceSnapshotFX.size()); i++) {
            RowData rd = datosExportar.get(i);
            String desc = rd.get("ITEM_NAME") != null ? rd.get("ITEM_NAME") : rd.get("DESCRIPTION");
            if (desc == null) desc = rd.get("ITEM_DESCRIPTION");
            logger.info("  [{}] UNIT_PRICE='{}' desc='{}'", i, unitPriceSnapshotFX.get(i), 
                desc != null ? desc.substring(0, Math.min(40, desc.length())) : "NULL");
        }

        Task<Void> taskExportacion = new Task<Void>() {
            @Override
            protected Void call() {
                if (usarAspose) {
                    exportarConAspose(archivo, datosExportar, unitPriceSnapshotFX);
                } else if (usarPlantilla) {
                    exportarConPlantilla(archivo, rutaPlantilla, datosExportar);
                } else {
                    exportarSinPlantilla(archivo, datosExportar);
                }
                return null;
            }
        };

        taskExportacion.setOnRunning(e -> {
            exportacionEnCurso = true;
            if (btnExportarCotizacion != null) {
                btnExportarCotizacion.setDisable(true);
            }
        });

        taskExportacion.setOnSucceeded(e -> {
            exportacionEnCurso = false;
            if (btnExportarCotizacion != null) {
                btnExportarCotizacion.setDisable(false);
            }
        });

        taskExportacion.setOnFailed(e -> {
            exportacionEnCurso = false;
            if (btnExportarCotizacion != null) {
                btnExportarCotizacion.setDisable(false);
            }
            Throwable error = taskExportacion.getException();
            logger.error("❌ Error no controlado al exportar cotización", error);
            ejecutarEnHiloFX(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Error al exportar cotización");
                alert.setContentText(error != null && error.getMessage() != null
                    ? error.getMessage()
                    : "Error desconocido.");
                alert.showAndWait();
            });
        });

        Thread hiloExportacion = new Thread(taskExportacion, "exportacion-cotizacion-thread");
        hiloExportacion.setDaemon(true);
        hiloExportacion.start();
    }

    private void exportarSinPlantilla(File archivo, List<RowData> datosExportar) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Cotización");
            
            logger.info("🚀 Exportando cotización con formato específico de {}", formatoActual.getBrokerName());
            
            if (metadataActual != null && !metadataActual.isEmpty()) {
                logger.info("📋 Colocando {} secciones de metadata en posiciones específicas", metadataActual.size());
                
                for (Map.Entry<String, List<BrokerMetadata>> entry : metadataActual.entrySet()) {
                    for (BrokerMetadata metadata : entry.getValue()) {
                        int filaExcel = metadata.getFilaOrigen() - 1;
                        int colExcel = metadata.getColumnaOrigen();
                        if (filaExcel < 0) {
                            logger.warn("⚠️ Metadata con fila inválida ({}), saltando: {}", 
                                metadata.getFilaOrigen(), metadata.getCampoNombre());
                            continue;
                        }
                        Row row = sheet.getRow(filaExcel);
                        if (row == null) {
                            row = sheet.createRow(filaExcel);
                        }
                        Cell cell = row.createCell(colExcel);
                        String campoNombre = metadata.getCampoNombre();
                        String valor = metadata.getCampoValor();
                        if (valor != null && !valor.isEmpty()) {
                            String valorFormateado = campoNombre + "=" + valor;
                            cell.setCellValue(valorFormateado);
                        }
                    }
                }
            }

            int headerRowIndex = formatoActual.getHeaderRow();
            if (headerRowIndex < 0) {
                headerRowIndex = 0;
                logger.warn("⚠️ HeaderRow era negativo, usando fila 0");
            }

            Row headerRow = sheet.getRow(headerRowIndex);
            if (headerRow == null) {
                headerRow = sheet.createRow(headerRowIndex);
            }

            Map<Integer, CellStyle> estilosColumnas = new HashMap<>();
            for (FormatoColumna columna : formatoActual.getColumnas()) {
                int colIndex = columna.getIndiceColumna();
                Cell headerCell = headerRow.createCell(colIndex);
                headerCell.setCellValue(columna.getNombreColumnaOriginal());
                CellStyle estiloColumna = crearEstiloColumna(workbook, columna);
                headerCell.setCellStyle(estiloColumna);
                estilosColumnas.put(colIndex, estiloColumna);
            }

            int dataStartRow = headerRowIndex + 1;
            logger.info("📦 Insertando {} productos a partir de la fila {}", 
                datosExportar.size(), dataStartRow + 1);

            Map<Integer, CellStyle> estilosDatos = new HashMap<>();
            Map<Integer, CellStyle> estilosDatosDecimal = new HashMap<>();
            DataFormat formatoNumerico = workbook.createDataFormat();

            for (FormatoColumna columna : formatoActual.getColumnas()) {
                int colIndex = columna.getIndiceColumna();
                CellStyle estiloDatos = workbook.createCellStyle();
                if (estilosColumnas.containsKey(colIndex)) {
                    CellStyle estiloOriginal = estilosColumnas.get(colIndex);
                    estiloDatos.setBorderBottom(estiloOriginal.getBorderBottom());
                    estiloDatos.setBorderTop(estiloOriginal.getBorderTop());
                    estiloDatos.setBorderLeft(estiloOriginal.getBorderLeft());
                    estiloDatos.setBorderRight(estiloOriginal.getBorderRight());
                }
                estilosDatos.put(colIndex, estiloDatos);

                if ("DECIMAL".equalsIgnoreCase(columna.getTipoDato())) {
                    CellStyle estiloDecimal = workbook.createCellStyle();
                    estiloDecimal.cloneStyleFrom(estiloDatos);
                    estiloDecimal.setDataFormat(formatoNumerico.getFormat("#,##0.00"));
                    estilosDatosDecimal.put(colIndex, estiloDecimal);
                }
            }

            int currentRow = dataStartRow;
            for (RowData rowData : datosExportar) {
                Row row = sheet.createRow(currentRow++);
                for (FormatoColumna columna : formatoActual.getColumnas()) {
                    int colIndex = columna.getIndiceColumna();
                    String campoEstandar = columna.getCampoEstandar();
                    String valor = rowData.get(campoEstandar);
                    if ("PRECIO_VSS".equals(campoEstandar)) {
                        valor = rowData.get("precio_vss_calculado");
                        if (valor == null || valor.isEmpty()) {
                            valor = "0.0";
                        }
                    }

                    if (valor != null && !valor.isEmpty()) {
                        Cell cell = row.createCell(colIndex);
                        String valorStr = valor;
                        if ("DECIMAL".equalsIgnoreCase(columna.getTipoDato()) || 
                            "INTEGER".equalsIgnoreCase(columna.getTipoDato()) ||
                            "PRECIO_VSS".equals(campoEstandar)) {
                            try {
                                double numValue = Double.parseDouble(valorStr.replace("$", "").replace(",", ""));
                                cell.setCellValue(numValue);
                                if (estilosDatosDecimal.containsKey(colIndex)) {
                                    cell.setCellStyle(estilosDatosDecimal.get(colIndex));
                                } else if (estilosDatos.containsKey(colIndex)) {
                                    cell.setCellStyle(estilosDatos.get(colIndex));
                                }
                            } catch (NumberFormatException e) {
                                cell.setCellValue(valorStr);
                                if (estilosDatos.containsKey(colIndex)) {
                                    cell.setCellStyle(estilosDatos.get(colIndex));
                                }
                            }
                        } else {
                            cell.setCellValue(valorStr);
                            if (estilosDatos.containsKey(colIndex)) {
                                cell.setCellStyle(estilosDatos.get(colIndex));
                            }
                        }
                    }
                }
            }

            final int MAX_COLUMN_WIDTH = 255 * 256;
            for (FormatoColumna columna : formatoActual.getColumnas()) {
                try {
                    sheet.autoSizeColumn(columna.getIndiceColumna());
                    int currentWidth = sheet.getColumnWidth(columna.getIndiceColumna());
                    int newWidth = Math.min(currentWidth + 1000, MAX_COLUMN_WIDTH);
                    sheet.setColumnWidth(columna.getIndiceColumna(), newWidth);
                } catch (IllegalArgumentException e) {
                    logger.warn("⚠️ No se pudo ajustar ancho de columna {}", columna.getLetraColumna());
                    sheet.setColumnWidth(columna.getIndiceColumna(), 8000);
                }
            }

            try (FileOutputStream outputStream = new FileOutputStream(archivo)) {
                workbook.write(outputStream);
            }

            logger.info("✅ Cotización exportada exitosamente con formato de {}: {}", 
                formatoActual.getBrokerName(), archivo.getAbsolutePath());

            final String ruta = archivo.getAbsolutePath();
            final String broker = formatoActual.getBrokerName();
            final int formatoId = formatoActual.getFormatoId();
            final int totalCols = formatoActual.getColumnas().size();
            final int totalItems = datosExportar.size();
            final String metadataInfo = metadataActual != null ? "\n✓ Metadata: " + contarMetadataTotal() + " campos" : "";

            ejecutarEnHiloFX(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Exportación Exitosa");
                alert.setHeaderText("Cotización exportada en formato " + broker);
                alert.setContentText("Archivo guardado en:\n" + ruta + 
                    "\n\n✓ Broker: " + broker +
                    "\n✓ Formato ID: " + formatoId +
                    "\n✓ Columnas: " + totalCols +
                    "\n✓ Productos: " + totalItems +
                    metadataInfo);
                alert.showAndWait();
            });

        } catch (Exception e) {
            logger.error("❌ Error al exportar cotización", e);
            final String mensaje = e.getMessage();
            ejecutarEnHiloFX(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Error al exportar cotización");
                alert.setContentText("No se pudo exportar el archivo:\n" + mensaje);
                alert.showAndWait();
            });
        }
    }
    
    /**
     * Exporta la cotización usando una plantilla existente.
     * Preserva el formato original, logo, macros y estilos.
     * Solo modifica las filas de datos a partir de header_row + 1.
     * 
     * @param archivoDestino Archivo donde guardar la exportación
     * @param rutaPlantilla Ruta del recurso de la plantilla
     */
    private void exportarConPlantilla(File archivoDestino, String rutaPlantilla, List<RowData> datosExportar) {
        logger.info("📄 Exportando con plantilla: {}", rutaPlantilla);
        
        try (InputStream plantillaStream = getClass().getResourceAsStream(rutaPlantilla)) {
            if (plantillaStream == null) {
                throw new RuntimeException("No se encontró la plantilla: " + rutaPlantilla);
            }
            
            // Abrir la plantilla (soporta .xlsm con macros)
            try (XSSFWorkbook workbook = new XSSFWorkbook(plantillaStream)) {
                XSSFSheet sheet = workbook.getSheetAt(0);
                
                // Fila donde empiezan los datos (después del header)
                int headerRowIndex = formatoActual.getHeaderRow() - 1; // 0-indexed
                int dataStartRow = headerRowIndex + 1;
                
                logger.info("📦 Insertando {} productos a partir de la fila {} (preservando formato original)", 
                    datosExportar.size(), dataStartRow + 1);
                
                // Limpiar filas de datos existentes en la plantilla (si las hay)
                int lastRowNum = sheet.getLastRowNum();
                for (int i = lastRowNum; i >= dataStartRow; i--) {
                    Row row = sheet.getRow(i);
                    if (row != null) {
                        sheet.removeRow(row);
                    }
                }
                
                // 📝 Escribir nombres de columnas (encabezados) en la fila de header
                Row headerRow = sheet.getRow(headerRowIndex);
                if (headerRow == null) {
                    headerRow = sheet.createRow(headerRowIndex);
                }
                
                logger.info("📊 Escribiendo encabezados en fila {}", headerRowIndex + 1);
                
                // Determinar índice de columna PRECIO_VSS (siguiente a la última columna del formato)
                int maxColIndex = 0;
                for (FormatoColumna columna : formatoActual.getColumnas()) {
                    if (columna.getIndiceColumna() > maxColIndex) {
                        maxColIndex = columna.getIndiceColumna();
                    }
                }
                int precioVssColIndex = maxColIndex + 1;
                
                logger.info("🔍 DEBUG: maxColIndex={}, precioVssColIndex={}, columna={}", 
                    maxColIndex, precioVssColIndex, (char)('A' + precioVssColIndex));
                logger.info("🔍 DEBUG: Total columnas del formato: {}", formatoActual.getColumnas().size());
                
                // 🎨 Crear estilo para encabezados (basado en el formato del broker)
                XSSFCellStyle estiloHeader = workbook.createCellStyle();
                XSSFFont fontHeader = workbook.createFont();
                fontHeader.setBold(true);
                fontHeader.setFontHeightInPoints((short) 11);
                
                // Usar colores del formato si están definidos (tomar de la primera columna con estilo)
                String colorFondoHeader = "#9DBEC3"; // Color por defecto BSM
                String colorTextoHeader = "#1F497D"; // Color por defecto BSM
                for (FormatoColumna col : formatoActual.getColumnas()) {
                    if (col.getColorFondo() != null && !col.getColorFondo().isEmpty()) {
                        colorFondoHeader = col.getColorFondo();
                        break;
                    }
                }
                for (FormatoColumna col : formatoActual.getColumnas()) {
                    if (col.getColorTexto() != null && !col.getColorTexto().isEmpty()) {
                        colorTextoHeader = col.getColorTexto();
                        break;
                    }
                }
                
                // Aplicar color de fondo
                if (colorFondoHeader.startsWith("#")) {
                    String hex = colorFondoHeader.substring(1);
                    byte[] rgb = new byte[] {
                        (byte) Integer.parseInt(hex.substring(0, 2), 16),
                        (byte) Integer.parseInt(hex.substring(2, 4), 16),
                        (byte) Integer.parseInt(hex.substring(4, 6), 16)
                    };
                    XSSFColor bgColor = new XSSFColor(rgb, null);
                    estiloHeader.setFillForegroundColor(bgColor);
                    estiloHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                }
                
                // Aplicar color de texto
                if (colorTextoHeader.startsWith("#")) {
                    String hex = colorTextoHeader.substring(1);
                    byte[] rgb = new byte[] {
                        (byte) Integer.parseInt(hex.substring(0, 2), 16),
                        (byte) Integer.parseInt(hex.substring(2, 4), 16),
                        (byte) Integer.parseInt(hex.substring(4, 6), 16)
                    };
                    XSSFColor textColor = new XSSFColor(rgb, null);
                    fontHeader.setColor(textColor);
                }
                
                estiloHeader.setFont(fontHeader);
                
                // Aplicar bordes
                estiloHeader.setBorderBottom(BorderStyle.THIN);
                estiloHeader.setBorderTop(BorderStyle.THIN);
                estiloHeader.setBorderLeft(BorderStyle.THIN);
                estiloHeader.setBorderRight(BorderStyle.THIN);
                
                // Alineación
                estiloHeader.setAlignment(HorizontalAlignment.CENTER);
                estiloHeader.setVerticalAlignment(VerticalAlignment.CENTER);
                
                logger.info("🎨 Estilo de cabecera creado - Fondo: {}, Texto: {}", colorFondoHeader, colorTextoHeader);
                
                // Escribir encabezados de las columnas del formato CON ESTILO
                for (FormatoColumna columna : formatoActual.getColumnas()) {
                    int colIndex = columna.getIndiceColumna();
                    Cell headerCell = headerRow.getCell(colIndex);
                    if (headerCell == null) {
                        headerCell = headerRow.createCell(colIndex);
                    }
                    headerCell.setCellValue(columna.getNombreColumnaOriginal());
                    headerCell.setCellStyle(estiloHeader);
                }
                
                // 💰 Agregar encabezado "Precio VSS" en la última columna CON ESTILO
                // COMENTADO: El usuario solicitó no incluir esta columna en la exportación
                // Cell precioVssHeaderCell = headerRow.createCell(precioVssColIndex);
                // precioVssHeaderCell.setCellValue("Precio VSS");
                // precioVssHeaderCell.setCellStyle(estiloHeader);
                // logger.info("💰 ENCABEZADO 'Precio VSS' escrito en celda [{},{}] = columna {} con formato", 
                //     headerRowIndex, precioVssColIndex, (char)('A' + precioVssColIndex));
                
                // Insertar datos de tablaDinámica
                int currentRowNum = dataStartRow;
                int filasConPrecioVss = 0;
                for (RowData rowData : datosExportar) {
                    Row row = sheet.createRow(currentRowNum++);
                    
                    // Mapear cada columna según el formato
                    for (FormatoColumna columna : formatoActual.getColumnas()) {
                        int colIndex = columna.getIndiceColumna();
                        String campoEstandar = columna.getCampoEstandar();
                        
                        // Obtener valor del RowData
                        String valor = rowData.get(campoEstandar);
                        
                        if (valor != null && !valor.isEmpty()) {
                            Cell cell = row.createCell(colIndex);
                            
                            // Intentar parsear como número según tipo de dato
                            if ("DECIMAL".equalsIgnoreCase(columna.getTipoDato()) || 
                                "INTEGER".equalsIgnoreCase(columna.getTipoDato())) {
                                try {
                                    double numValue = Double.parseDouble(
                                        valor.replace("$", "").replace(",", "")
                                    );
                                    cell.setCellValue(numValue);
                                } catch (NumberFormatException e) {
                                    cell.setCellValue(valor);
                                }
                            } else {
                                cell.setCellValue(valor);
                            }
                        }
                    }
                    
                    // 💰 Agregar valor de Precio VSS (calculado en frontend)
                    // COMENTADO: El usuario solicitó no incluir esta columna en la exportación
                    // String precioVssValor = rowData.get("precio_vss_calculado");
                    // if (precioVssValor == null || precioVssValor.isEmpty()) {
                    //     precioVssValor = "0.0";
                    // }
                    // Cell precioVssCell = row.createCell(precioVssColIndex);
                    // try {
                    //     double precioVss = Double.parseDouble(
                    //         precioVssValor.replace("$", "").replace(",", "")
                    //     );
                    //     precioVssCell.setCellValue(precioVss);
                    //     if (precioVss > 0) filasConPrecioVss++;
                    // } catch (NumberFormatException e) {
                    //     precioVssCell.setCellValue(precioVssValor);
                    // }
                    // 
                    // // Log de la primera fila para verificar
                    // if (currentRowNum == dataStartRow + 1) {
                    //     logger.info("🔍 DEBUG primera fila: precio_vss_calculado='{}', escrito en columna {}", 
                    //         precioVssValor, precioVssColIndex);
                    // }
                }
                
                // logger.info("💰 Total filas con Precio VSS > 0: {} de {}", 
                //     filasConPrecioVss, tablaDinamica.getItems().size());
                // 
                // // 📏 Ajustar ancho de la columna Precio VSS para que sea visible
                // sheet.setColumnWidth(precioVssColIndex, 4000); // ~14 caracteres
                // 
                // // 👁️ Asegurar que la columna NO esté oculta
                // sheet.setColumnHidden(precioVssColIndex, false);
                // 
                // logger.info("📏 Ancho de columna {} (Precio VSS) ajustado a 4000, visible=true", (char)('A' + precioVssColIndex));
                // 
                // // 🔍 Verificación final: leer lo que se escribió en la primera fila de datos
                // Row primeraFilaDatos = sheet.getRow(dataStartRow);
                // if (primeraFilaDatos != null) {
                //     Cell celdaVerificacion = primeraFilaDatos.getCell(precioVssColIndex);
                //     if (celdaVerificacion != null) {
                //         logger.info("✅ VERIFICACIÓN FINAL: Celda [{},{}] contiene: {}", 
                //             dataStartRow, precioVssColIndex, 
                //             celdaVerificacion.getCellType().toString() + " = " + 
                //             (celdaVerificacion.getCellType().toString().equals("NUMERIC") ? 
                //                 celdaVerificacion.getNumericCellValue() : celdaVerificacion.toString()));
                //     } else {
                //         logger.error("❌ VERIFICACIÓN FINAL: La celda [{},{}] es NULL!", dataStartRow, precioVssColIndex);
                //     }
                // }
                
                // Guardar archivo con el nuevo contenido
                try (FileOutputStream outputStream = new FileOutputStream(archivoDestino)) {
                    workbook.write(outputStream);
                    logger.info("💾 Archivo guardado exitosamente en: {}", archivoDestino.getAbsolutePath());
                }
                
                logger.info("✅ Cotización exportada con plantilla exitosamente: {}", 
                    archivoDestino.getAbsolutePath());
                
                // Mensaje de confirmación
                final String brokerNombre = formatoActual.getBrokerName();
                final String rutaDestino = archivoDestino.getAbsolutePath();
                final int totalProductos = datosExportar.size();
                ejecutarEnHiloFX(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Exportación Exitosa");
                    alert.setHeaderText("Cotización exportada con formato " + brokerNombre);
                    alert.setContentText("Archivo guardado en:\n" + rutaDestino + 
                        "\n\n✓ Broker: " + brokerNombre +
                        "\n✓ Plantilla: SÍ (formato original preservado)" +
                        "\n✓ Logo: Preservado" +
                        "\n✓ Macros: Preservadas" +
                        "\n✓ Productos: " + totalProductos);
                    alert.showAndWait();
                });
            }
            
        } catch (Exception e) {
            logger.error("❌ Error al exportar con plantilla", e);
            final String mensajeError = e.getMessage();
            ejecutarEnHiloFX(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Error al exportar con plantilla");
                alert.setContentText("No se pudo exportar el archivo:\n" + mensajeError);
                alert.showAndWait();
            });
        }
    }
    
    // ============================================================
    // 🔷 EXPORTAR CON ASPOSE (PRESERVA MACROS VBA DEL ARCHIVO ORIGINAL)
    // ============================================================
    /**
     * Exporta la cotización usando el workbook Aspose cargado.
     * Preserva todas las macros VBA, fórmulas, formatos y estructura del archivo original.
     * Solo modifica las celdas de datos.
     * 
     * @param archivoDestino Archivo donde guardar la exportación
     */
    private void exportarConAspose(File archivoDestino, List<RowData> datosExportar, java.util.List<String> unitPriceSnapshotFX) {
        logger.info("🔷 Exportando con Aspose (macros preservadas): {}", archivoDestino.getName());
        
        try {
            com.aspose.cells.Workbook workbook = asposeService.getWorkbookActual();
            if (workbook == null) {
                throw new IllegalStateException("No hay workbook Aspose cargado");
            }
            
            com.aspose.cells.Worksheet sheet = workbook.getWorksheets().get(0);
            com.aspose.cells.Cells cells = sheet.getCells();
            
            // Fila donde están las cabeceras (headerRow ya está en 0-indexed)
            int headerRowIndex = formatoActual.getHeaderRow();
            int dataStartRow = headerRowIndex + 1;
            
            // ============================
            // DETECTAR FILA ESPECIAL (ej: "PROVISIONS" con fondo amarillo)
            // ============================
            int filaEspecialIndex = -1; // -1 significa que no hay fila especial
            boolean filaEspecialDetectada = false;
            
            logger.info("🔍 Verificando fila {} (Excel) para detectar fila especial...", dataStartRow + 1);
            
            // ESTRATEGIA 1: Buscar texto "PROVISIONS" en cualquier columna de la fila
            for (int colCheck = 0; colCheck <= 20 && !filaEspecialDetectada; colCheck++) {
                com.aspose.cells.Cell celdaCheck = cells.get(dataStartRow, colCheck);
                if (celdaCheck != null && celdaCheck.getValue() != null) {
                    String valorCelda = celdaCheck.getStringValue().trim().toUpperCase();
                    if (valorCelda.equals("PROVISIONS") || valorCelda.equals("PROVISION") ||
                        valorCelda.equals("ITEMS") || valorCelda.equals("PRODUCTS") ||
                        valorCelda.equals("PRODUCTOS") || valorCelda.equals("LISTA")) {
                        filaEspecialIndex = dataStartRow;
                        filaEspecialDetectada = true;
                        logger.info("🟡 Fila especial '{}' detectada por texto en columna {} de fila {}", 
                            valorCelda, colCheck, filaEspecialIndex + 1);
                    }
                }
            }
            
            // ESTRATEGIA 2: Verificar si la fila tiene fondo amarillo (color RGB)
            if (!filaEspecialDetectada) {
                com.aspose.cells.Cell celdaPrimeraCol = cells.get(dataStartRow, 0);
                if (celdaPrimeraCol != null) {
                    com.aspose.cells.Style estilo = celdaPrimeraCol.getStyle();
                    if (estilo != null) {
                        com.aspose.cells.Color bgColor = estilo.getBackgroundColor();
                        com.aspose.cells.Color fgColor = estilo.getForegroundColor();
                        // Amarillo típico: R=255, G=255, B=0 o similar
                        if (bgColor != null && bgColor.getR() == (byte)255 && bgColor.getG() == (byte)255 && bgColor.getB() == (byte)0) {
                            filaEspecialIndex = dataStartRow;
                            filaEspecialDetectada = true;
                            logger.info("🟡 Fila especial detectada por color amarillo en fila {}", filaEspecialIndex + 1);
                        } else if (fgColor != null && fgColor.getR() == (byte)255 && fgColor.getG() == (byte)255 && fgColor.getB() == (byte)0) {
                            filaEspecialIndex = dataStartRow;
                            filaEspecialDetectada = true;
                            logger.info("🟡 Fila especial detectada por color amarillo (foreground) en fila {}", filaEspecialIndex + 1);
                        }
                    }
                }
            }
            
            // ESTRATEGIA 3: Para BSM CATERING específicamente - verificar si la fila 20 no tiene datos
            // y la fila 21 sí tiene datos (asumiendo que headerRow=19 para BSM)
            if (!filaEspecialDetectada && formatoActual.getBrokerName() != null && 
                formatoActual.getBrokerName().toUpperCase().contains("BSM")) {
                // Para BSM, la fila después de las cabeceras es siempre la fila PROVISIONS
                filaEspecialIndex = dataStartRow;
                filaEspecialDetectada = true;
                logger.info("🟡 Fila especial detectada para BSM CATERING en fila {} (forzado)", filaEspecialIndex + 1);
            }
            
            // Si se detectó fila especial, incrementar dataStartRow
            if (filaEspecialDetectada) {
                dataStartRow = dataStartRow + 1;
                logger.info("📦 Los datos empezarán desde la fila {} (saltando fila especial {})", 
                    dataStartRow + 1, filaEspecialIndex + 1);
            }
            
            logger.info("📊 Header row del formato: {} (Excel) -> {} (Aspose 0-indexed)", 
                formatoActual.getHeaderRow(), headerRowIndex);
            logger.info("📦 Escribiendo {} productos a partir de la fila {} (preservando macros)", 
                datosExportar.size(), dataStartRow + 1);
            
            // ============================
            // ENCONTRAR ÍNDICES DE COLUMNAS PARA FÓRMULAS
            // ============================
            int colQuantity = -1;
            int colUnitPrice = -1;
            int colTotal = -1;
            String letraQuantity = "";
            String letraUnitPrice = "";
            String letraTotal = "";
            
            for (FormatoColumna columna : formatoActual.getColumnas()) {
                String campo = columna.getCampoEstandar() != null ? columna.getCampoEstandar().toUpperCase() : "";
                String nombreCol = columna.getNombreColumnaOriginal() != null ? columna.getNombreColumnaOriginal().toUpperCase() : "";
                
                // Detectar columna QUANTITY (ej: "Quantity")
                if (campo.contains("QUANTITY") || campo.equals("QTY") || 
                    nombreCol.contains("QUANTITY") || nombreCol.equals("QTY") ||
                    nombreCol.contains("CANTIDAD")) {
                    colQuantity = columna.getIndiceColumna();
                    letraQuantity = columna.getLetraColumna();
                    logger.info("🔍 Columna QUANTITY detectada: {} ({})", letraQuantity, columna.getNombreColumnaOriginal());
                } 
                // Detectar columna UNIT_PRICE (ej: "Unit Price ( USD )")
                // IMPORTANTE: verificar UNIT PRICE antes que TOTAL PRICE
                else if (campo.contains("UNIT_PRICE") || campo.contains("UNIT PRICE") ||
                         nombreCol.contains("UNIT PRICE") || 
                         (nombreCol.contains("PRICE") && nombreCol.contains("USD") && !nombreCol.contains("TOTAL"))) {
                    colUnitPrice = columna.getIndiceColumna();
                    letraUnitPrice = columna.getLetraColumna();
                    logger.info("🔍 Columna UNIT_PRICE detectada: {} ({})", letraUnitPrice, columna.getNombreColumnaOriginal());
                } 
                // Detectar columna TOTAL (ej: "Total Price")
                else if (campo.contains("TOTAL") || campo.equals("AMOUNT") ||
                         nombreCol.contains("TOTAL PRICE") || nombreCol.contains("TOTAL")) {
                    colTotal = columna.getIndiceColumna();
                    letraTotal = columna.getLetraColumna();
                    logger.info("🔍 Columna TOTAL detectada: {} ({})", letraTotal, columna.getNombreColumnaOriginal());
                }
            }
            
            logger.info("📝 Columnas para fórmula: QUANTITY={} ({}), UNIT_PRICE={} ({}), TOTAL={} ({})",
                colQuantity, letraQuantity, colUnitPrice, letraUnitPrice, colTotal, letraTotal);
            
            boolean puedeCrearFormula = colQuantity >= 0 && colUnitPrice >= 0 && colTotal >= 0;
            if (puedeCrearFormula) {
                logger.info("✅ Columna TOTAL detectada en {} - se copiará fórmula original", letraTotal);
            } else {
                logger.warn("⚠️ No se puede crear fórmula TOTAL - faltan columnas");
            }
            
            // 📝 PROCURESHIP: Detectar columna "Supplier Notes" en el header (no está en formato BD)
            int supplierNotesExportColIndex = -1;
            boolean esProcureshipExport = formatoActual.getBrokerName() != null && 
                formatoActual.getBrokerName().toUpperCase().contains("PROCURE");
            if (esProcureshipExport) {
                int maxColHeader = cells.getMaxDataColumn();
                for (int colScan = 0; colScan <= maxColHeader; colScan++) {
                    com.aspose.cells.Cell headerCell = cells.get(headerRowIndex, colScan);
                    if (headerCell != null && headerCell.getStringValue() != null) {
                        String headerValue = headerCell.getStringValue().trim();
                        if (headerValue.toUpperCase().contains("SUPPLIER") && 
                            headerValue.toUpperCase().contains("NOTE")) {
                            supplierNotesExportColIndex = colScan;
                            logger.info("📝 PROCURESHIP EXPORT: Columna 'Supplier Notes' detectada en col {} ({})", 
                                colScan, headerValue);
                            break;
                        }
                    }
                }
            }
            
            // ============================
            // GUARDAR FÓRMULA ORIGINAL DE TOTAL PRICE
            // ============================
            String formulaOriginalTotal = null;
            int filaFormulaOriginal = -1; // Fila exacta donde se encontró la fórmula (0-indexed)
            
            if (colTotal >= 0) {
                // Buscar fórmula en las primeras filas de datos
                logger.info("🔍 Buscando fórmula en columna TOTAL (col {}), desde fila {}", colTotal, dataStartRow + 1);
                
                // Revisar varias filas por si la primera no tiene fórmula
                for (int searchRow = dataStartRow; searchRow <= Math.min(dataStartRow + 10, cells.getMaxDataRow()); searchRow++) {
                    com.aspose.cells.Cell celdaFormulaOriginal = cells.get(searchRow, colTotal);
                    if (celdaFormulaOriginal != null) {
                        logger.debug("  Fila {}: tipo={}, isFormula={}, valor={}", 
                            searchRow + 1, 
                            celdaFormulaOriginal.getType(),
                            celdaFormulaOriginal.isFormula(),
                            celdaFormulaOriginal.getValue());
                        
                        if (celdaFormulaOriginal.isFormula()) {
                            formulaOriginalTotal = celdaFormulaOriginal.getFormula();
                            filaFormulaOriginal = searchRow; // Guardar la fila exacta (0-indexed)
                            logger.info("📝 Fórmula original de Total Price encontrada en fila {} (0-idx: {}): {}", 
                                searchRow + 1, searchRow, formulaOriginalTotal);
                            break;
                        }
                    }
                }
                
                if (formulaOriginalTotal == null) {
                    logger.warn("⚠️ No se encontró fórmula en columna TOTAL. Las celdas pueden contener valores estáticos.");
                }
            } else {
                logger.warn("⚠️ Columna TOTAL no detectada (colTotal={})", colTotal);
            }
            
            // ============================
            // DETECTAR COLUMNAS DESDE LA FÓRMULA (para escribir datos en posiciones correctas)
            // ============================
            int colQuantityFormula = -1;   // Columna donde la fórmula espera Quantity
            int colUnitPriceFormula = -1;  // Columna donde la fórmula espera Unit Price
            int colDiscountFormula = -1;   // Columna donde la fórmula espera Discount
            int colVatFormula = -1;        // Columna donde la fórmula espera VAT
            
            if (formulaOriginalTotal != null) {
                // Extraer todas las referencias de columna de la fórmula
                java.util.regex.Pattern patronColumna = java.util.regex.Pattern.compile("([A-Z]+)\\d+");
                java.util.regex.Matcher matcherColumna = patronColumna.matcher(formulaOriginalTotal);
                
                java.util.List<String> columnasEnFormula = new java.util.ArrayList<>();
                java.util.Set<String> columnasUnicas = new java.util.LinkedHashSet<>();
                
                while (matcherColumna.find()) {
                    String letraCol = matcherColumna.group(1);
                    columnasEnFormula.add(letraCol);
                    columnasUnicas.add(letraCol);
                }
                
                logger.info("🔍 Columnas detectadas en fórmula: {}", columnasUnicas);
                
                // Convertir a lista ordenada para asignar roles
                java.util.List<String> listaColumnas = new java.util.ArrayList<>(columnasUnicas);
                
                // Asignar columnas según la estructura típica de fórmulas:
                // Primera columna = Quantity, Segunda = Unit Price, Tercera = Discount, Cuarta = VAT
                if (listaColumnas.size() >= 1) {
                    colQuantityFormula = letraAIndice(listaColumnas.get(0));
                    logger.info("📌 Fórmula usa columna {} para QUANTITY (col {})", listaColumnas.get(0), colQuantityFormula);
                }
                if (listaColumnas.size() >= 2) {
                    colUnitPriceFormula = letraAIndice(listaColumnas.get(1));
                    logger.info("📌 Fórmula usa columna {} para UNIT_PRICE (col {})", listaColumnas.get(1), colUnitPriceFormula);
                }
                if (listaColumnas.size() >= 3) {
                    colDiscountFormula = letraAIndice(listaColumnas.get(2));
                    logger.info("📌 Fórmula usa columna {} para DISCOUNT (col {})", listaColumnas.get(2), colDiscountFormula);
                }
                if (listaColumnas.size() >= 4) {
                    colVatFormula = letraAIndice(listaColumnas.get(3));
                    logger.info("📌 Fórmula usa columna {} para VAT (col {})", listaColumnas.get(3), colVatFormula);
                }
            }
            
            // ============================
            // DETECTAR FILA DE SUBTOTAL (ej: "Item Sub Total")
            // ============================
            int filaSubtotalIndex = -1;
            String formulaSubtotalOriginal = null;
            int colSubtotalFormula = -1;
            
            int lastDataRow = cells.getMaxDataRow();
            logger.info("🔍 Buscando fila de subtotal entre filas {} y {} (lastDataRow: {})...", dataStartRow + 1, lastDataRow + 1, lastDataRow);
            
            // Buscar desde el final hacia arriba
            for (int searchRow = lastDataRow; searchRow >= dataStartRow; searchRow--) {
                // Primero, verificar si la fila tiene una fórmula en colTotal que sea de SUBTOTAL
                // Una fórmula de subtotal suma un rango grande desde dataStartRow
                boolean esFilaSubtotal = false;
                if (colTotal >= 0) {
                    com.aspose.cells.Cell celdaTotalCheck = cells.get(searchRow, colTotal);
                    if (celdaTotalCheck != null && celdaTotalCheck.isFormula()) {
                        String formulaCheck = celdaTotalCheck.getFormula().toUpperCase();
                        logger.info("🔍 Fila {} tiene fórmula: {}", searchRow + 1, formulaCheck);
                        
                        // Verificar si es una fórmula SUM/SUMA que suma desde cerca de dataStartRow
                        if (formulaCheck.contains("SUM") || formulaCheck.contains("SUMA")) {
                            // Extraer el rango de la fórmula: =SUM(P21:P234) -> P21:P234
                            java.util.regex.Pattern patronRango = java.util.regex.Pattern.compile("([A-Z]+)(\\d+):([A-Z]+)(\\d+)");
                            java.util.regex.Matcher matcherRango = patronRango.matcher(formulaCheck);
                            
                            if (matcherRango.find()) {
                                int filaInicio = Integer.parseInt(matcherRango.group(2));
                                int filaFin = Integer.parseInt(matcherRango.group(4));
                                
                                // Si la fórmula suma desde cerca de dataStartRow (21) hasta cerca de searchRow,
                                // entonces ES la fila de subtotal
                                int dataStartExcel = dataStartRow + 1;  // Convertir a 1-based
                                int rangoEsperado = searchRow - dataStartRow;
                                int rangoReal = filaFin - filaInicio + 1;
                                
                                logger.info("  → Rango: {}-{} ({} filas), esperado desde fila {} ({} filas desde dataStart)",
                                    filaInicio, filaFin, rangoReal, dataStartExcel, rangoEsperado);
                                
                                // Si empieza cerca de dataStartRow (dentro de 5 filas) y suma un rango grande
                                if (Math.abs(filaInicio - dataStartExcel) <= 5 && rangoReal >= 10) {
                                    esFilaSubtotal = true;
                                    filaSubtotalIndex = searchRow;
                                    formulaSubtotalOriginal = celdaTotalCheck.getFormula();
                                    colSubtotalFormula = colTotal;
                                    logger.info("📊 ✅ FILA DE SUBTOTAL DETECTADA por fórmula: fila {} (0-idx: {}) con fórmula: {}",
                                        searchRow + 1, searchRow, formulaSubtotalOriginal);
                                    break;  // Salir del bucle, ya encontramos el subtotal
                                }
                            }
                        }
                    }
                }
                
                // Si ya encontramos el subtotal, salir
                if (esFilaSubtotal) break;
            }
            
            // CMA CGM: usar primera fila especial detectada en carga como subtotal
            if (formatoActual.getBrokerName() != null) {
                String brokerUpper = formatoActual.getBrokerName().toUpperCase();
                if (brokerUpper.contains("CMA") && brokerUpper.contains("CGM") && !cmaCgmSpecialRows.isEmpty()) {
                    filaSubtotalIndex = cmaCgmSpecialRows.firstKey(); // Primera fila especial (más alta)
                    logger.info("📌 CMA CGM: Usando primera fila especial detectada en carga: {}", filaSubtotalIndex + 1);
                }
            }
            
            if (filaSubtotalIndex < 0) {
                logger.warn("⚠️ NO SE DETECTÓ FILA DE SUBTOTAL. Se buscará en el archivo original...");
            }
            
            // ============================
            // DETECTAR FILAS DE RESUMEN DE PROCURESHIP
            // ============================
            // ProcureShip tiene múltiples filas de resumen después del Subtotal:
            // Subtotal, Transportation Cost, Subtotal (2do), Other (Taxes/Charges), Grand Total
            // Estas filas deben preservarse durante la limpieza.
            java.util.Set<Integer> filasResumenProcureship = new java.util.HashSet<>();
            boolean esProcureship = formatoActual.getBrokerName() != null && 
                formatoActual.getBrokerName().toUpperCase().contains("PROCURESHIP");
            
            if (esProcureship && filaSubtotalIndex >= 0) {
                logger.info("🏛️ PROCURESHIP: Buscando filas de resumen después del subtotal (fila {})...", filaSubtotalIndex + 1);
                
                // Escanear desde filaSubtotalIndex hasta lastDataRow buscando filas de resumen
                for (int searchRow = filaSubtotalIndex; searchRow <= lastDataRow; searchRow++) {
                    for (int colCheck = 0; colCheck <= 20; colCheck++) {
                        com.aspose.cells.Cell celdaCheck = cells.get(searchRow, colCheck);
                        if (celdaCheck != null && celdaCheck.getValue() != null) {
                            String valorCelda = "";
                            try {
                                valorCelda = celdaCheck.getStringValue().trim().toUpperCase();
                            } catch (Exception e) {
                                continue;
                            }
                            
                            if (valorCelda.contains("SUBTOTAL") || valorCelda.contains("SUB TOTAL") ||
                                valorCelda.contains("TRANSPORTATION") || valorCelda.contains("TRANSPORT") ||
                                valorCelda.contains("GRAND TOTAL") || 
                                valorCelda.contains("CHARGES") || valorCelda.contains("CUSTOMS") ||
                                valorCelda.contains("TAXES") || valorCelda.contains("OTHER")) {
                                
                                filasResumenProcureship.add(searchRow);
                                
                                // Log de fórmula/valor en colTotal para esta fila
                                String infoFormula = "";
                                if (colTotal >= 0) {
                                    com.aspose.cells.Cell celdaTotal = cells.get(searchRow, colTotal);
                                    if (celdaTotal != null && celdaTotal.isFormula()) {
                                        infoFormula = " [Fórmula: " + celdaTotal.getFormula() + "]";
                                    } else if (celdaTotal != null && celdaTotal.getValue() != null) {
                                        infoFormula = " [Valor: " + celdaTotal.getValue() + "]";
                                    }
                                }
                                
                                logger.info("🏛️ PROCURESHIP: Fila de resumen detectada: fila {} = '{}'{}" , 
                                    searchRow + 1, valorCelda, infoFormula);
                                break; // Ya encontramos la etiqueta en esta fila
                            }
                        }
                    }
                }
                
                logger.info("🏛️ PROCURESHIP: {} filas de resumen detectadas: {}", 
                    filasResumenProcureship.size(), filasResumenProcureship);
            }
            
            // ============================
            // LIMPIAR ESPECÍFICAMENTE LA CELDA DE SUBTOTAL
            // ============================
            // IMPORTANTE: Limpiar la celda del subtotal ANTES de escribir datos
            // porque tiene una fórmula incorrecta (fórmula de fila de datos) que será reemplazada
            if (filaSubtotalIndex >= 0 && colTotal >= 0) {
                com.aspose.cells.Cell celdaSubtotalLimpiar = cells.get(filaSubtotalIndex, colTotal);
                if (celdaSubtotalLimpiar != null) {
                    String formulaAnterior = celdaSubtotalLimpiar.isFormula() ? celdaSubtotalLimpiar.getFormula() : "(sin fórmula)";
                    celdaSubtotalLimpiar.putValue("");  // Limpiar completamente la celda
                    logger.info("🧹 LIMPIADA celda de subtotal en fila {}, col {} ({}). Fórmula anterior: {}", 
                        filaSubtotalIndex + 1, colTotal, letraTotal, formulaAnterior);
                }
            }
            
            // Limpiar filas de datos existentes (preservar estructura y formato)
            // NOTA: No limpiar la fila especial (ej: PROVISIONS)
            for (int rowIdx = lastDataRow; rowIdx >= dataStartRow; rowIdx--) {
                // Saltar la fila especial si existe
                if (filaEspecialIndex >= 0 && rowIdx == filaEspecialIndex) {
                    logger.debug("🟡 Preservando fila especial {} sin limpiar", filaEspecialIndex + 1);
                    continue;
                }
                
                // Saltar la fila de subtotal (ya fue limpiada arriba específicamente)
                if (filaSubtotalIndex >= 0 && rowIdx == filaSubtotalIndex) {
                    logger.debug("📊 Saltando fila de subtotal {} (ya limpiada)", filaSubtotalIndex + 1);
                    continue;
                }

                // CMA CGM: preservar TODAS las filas especiales (Item Sub Total, Total Price, Grand Total)
                if (!cmaCgmSpecialRows.isEmpty() && cmaCgmSpecialRows.containsKey(rowIdx)) {
                    logger.debug("📊 CMA CGM: Preservando fila especial {} sin limpiar", rowIdx + 1);
                    continue;
                }
                
                // Saltar filas de resumen de ProcureShip (Transportation, Charges, Grand Total, etc.)
                if (!filasResumenProcureship.isEmpty() && filasResumenProcureship.contains(rowIdx)) {
                    logger.info("🏛️ PROCURESHIP: Preservando fila de resumen {} sin limpiar", rowIdx + 1);
                    continue;
                }
                
                // Limpiar filas de datos normales
                for (FormatoColumna columna : formatoActual.getColumnas()) {
                    com.aspose.cells.Cell cell = cells.get(rowIdx, columna.getIndiceColumna());
                    if (cell != null) {
                        cell.putValue(""); // Limpiar contenido, preservar formato
                    }
                }
            }
            
            // ============================
            // ESCRIBIR CABECERAS DE COLUMNAS
            // ============================
            logger.info("📊 Escribiendo cabeceras en fila {} (Aspose index: {})", 
                formatoActual.getHeaderRow(), headerRowIndex);
            
            for (FormatoColumna columna : formatoActual.getColumnas()) {
                int colIdx = columna.getIndiceColumna();
                
                // Omitir columna PRECIO_VSS (es columna interna, no debe exportarse)
                if ("PRECIO_VSS".equals(columna.getCampoEstandar())) {
                    logger.info("🚫 Omitiendo encabezado PRECIO_VSS (columna interna)");
                    continue;
                }
                
                com.aspose.cells.Cell headerCell = cells.get(headerRowIndex, colIdx);
                
                // Escribir nombre de columna original
                String nombreColumna = columna.getNombreColumnaOriginal();
                if (nombreColumna != null && !nombreColumna.isEmpty()) {
                    headerCell.putValue(nombreColumna);
                    
                    // Aplicar estilo de cabecera (negrita)
                    com.aspose.cells.Style headerStyle = headerCell.getStyle();
                    if (headerStyle == null) {
                        headerStyle = workbook.createStyle();
                    }
                    com.aspose.cells.Font headerFont = headerStyle.getFont();
                    headerFont.setBold(true);
                    headerCell.setStyle(headerStyle);
                    
                    logger.debug("  └─ Col {} [{}]: '{}'", 
                        columna.getLetraColumna(), 
                        columna.getCampoEstandar(),
                        nombreColumna);
                }
            }
            
            // ============================
            // ESCRIBIR DATOS DE LA TABLA
            // ============================
            
            // Log de mapeo de columnas para verificar posiciones
            logger.info("📊 MAPEO DE COLUMNAS DEL FORMATO:");
            for (FormatoColumna col : formatoActual.getColumnas()) {
                logger.info("  {} (col {}) = {} [{}]", 
                    col.getLetraColumna(), col.getIndiceColumna(), 
                    col.getCampoEstandar(), col.getNombreColumnaOriginal());
            }
            
            // Verificar que las columnas de la fórmula coincidan
            logger.info("🔍 VERIFICACIÓN DE COLUMNAS PARA FÓRMULA:");
            logger.info("  Fórmula original: {}", formulaOriginalTotal);
            logger.info("  Columna QUANTITY detectada: {} (col {})", letraQuantity, colQuantity);
            logger.info("  Columna UNIT_PRICE detectada: {} (col {})", letraUnitPrice, colUnitPrice);
            logger.info("  Columna TOTAL detectada: {} (col {})", letraTotal, colTotal);
            logger.info("📌 COLUMNAS DETECTADAS DESDE FÓRMULA (donde escribir datos):");
            logger.info("  QUANTITY en col {}, UNIT_PRICE en col {}, DISCOUNT en col {}, VAT en col {}",
                colQuantityFormula, colUnitPriceFormula, colDiscountFormula, colVatFormula);
            
            // Mapa para guardar valores UNIT_PRICE escritos por fila (snapshot del hilo FX)
            // Se usa para forzar escritura después de calculateFormula() y justo antes de guardar
            java.util.Map<Integer, String> unitPricesPorFila = new java.util.LinkedHashMap<>();
            
            // 🔍 DIAGNÓSTICO: Verificar colUnitPrice vs colUnitPriceFormula
            logger.info("🔍 DIAG UNIT_PRICE: colUnitPrice(mapeo)={}, colUnitPriceFormula(fórmula)={}", 
                colUnitPrice, colUnitPriceFormula);
            
            int currentRow = dataStartRow;
            boolean primeraFila = true;
            int idxRowData = 0;
            for (RowData rowData : datosExportar) {
                // IMPORTANTE: Saltar la fila de subtotal al escribir datos
                if (filaSubtotalIndex >= 0 && currentRow == filaSubtotalIndex) {
                    logger.info("🚫 Saltando fila de subtotal {} al escribir datos", filaSubtotalIndex + 1);
                    currentRow++;  // Saltar a la siguiente fila
                }
                
                // También saltar la fila especial si existe
                if (filaEspecialIndex >= 0 && currentRow == filaEspecialIndex) {
                    logger.info("🚫 Saltando fila especial {} al escribir datos", filaEspecialIndex + 1);
                    currentRow++;  // Saltar a la siguiente fila
                }

                // CMA CGM: Saltar TODAS las filas especiales al escribir datos
                while (!cmaCgmSpecialRows.isEmpty() && cmaCgmSpecialRows.containsKey(currentRow)) {
                    logger.info("🚫 CMA CGM: Saltando fila especial {} al escribir datos", currentRow + 1);
                    currentRow++;
                }
                
                // Saltar filas de resumen de ProcureShip
                while (!filasResumenProcureship.isEmpty() && filasResumenProcureship.contains(currentRow)) {
                    logger.info("🏛️ PROCURESHIP: Saltando fila de resumen {} al escribir datos", currentRow + 1);
                    currentRow++;
                }
                
                for (FormatoColumna columna : formatoActual.getColumnas()) {
                    int colIdx = columna.getIndiceColumna();
                    String campoEstandar = columna.getCampoEstandar();
                    
                    // ============================
                    // OMITIR PRECIO_VSS (columna interna, no debe exportarse)
                    // ============================
                    if ("PRECIO_VSS".equals(campoEstandar)) {
                        if (primeraFila) {
                            logger.info("🚫 Omitiendo columna PRECIO_VSS en exportación (columna interna)");
                        }
                        continue; // Saltar esta columna
                    }
                    
                    com.aspose.cells.Cell cell = cells.get(currentRow, colIdx);
                    
                    // ============================
                    // CASO ESPECIAL: COLUMNA TOTAL - COPIAR FÓRMULA ORIGINAL
                    // ============================
                    String campoUpper = campoEstandar != null ? campoEstandar.toUpperCase() : "";
                    String nombreColUpper = columna.getNombreColumnaOriginal() != null ? 
                                           columna.getNombreColumnaOriginal().toUpperCase() : "";
                    
                    boolean esColumnaTOTAL = !campoUpper.equals("UNIT_PRICE") &&
                                            (campoUpper.contains("TOTAL") || campoUpper.equals("AMOUNT") ||
                                            nombreColUpper.contains("TOTAL") || nombreColUpper.contains("AMOUNT"));
                    
                    if (esColumnaTOTAL && formulaOriginalTotal != null && filaFormulaOriginal >= 0) {
                        // Copiar la fórmula original ajustando las referencias de fila
                        // Calcular el desplazamiento desde la fila EXACTA donde estaba la fórmula
                        int filaExcelOriginal = filaFormulaOriginal + 1; // Fila Excel donde estaba la fórmula (1-indexed)
                        int filaExcelActual = currentRow + 1;            // Fila Excel actual (1-indexed)
                        int desplazamiento = filaExcelActual - filaExcelOriginal;
                        
                        // Ajustar las referencias de fila en la fórmula
                        String formulaAjustada = ajustarReferenciasFormula(formulaOriginalTotal, desplazamiento);
                        cell.setFormula(formulaAjustada);
                        
                        logger.debug("📝 Fórmula copiada a {}{}: {} (desplazamiento={} desde fila {})", 
                            columna.getLetraColumna(), filaExcelActual, formulaAjustada, 
                            desplazamiento, filaExcelOriginal);
                        continue;
                    } else if (esColumnaTOTAL) {
                        // Si no hay fórmula original, escribir el valor del campo TOTAL
                        String valorTotal = rowData.get(campoEstandar);
                        if (valorTotal != null && !valorTotal.isEmpty()) {
                            try {
                                String valorLimpio = valorTotal.replace(",", "").replace("$", "").trim();
                                double numValue = Double.parseDouble(valorLimpio);
                                cell.putValue(numValue);
                            } catch (NumberFormatException e) {
                                cell.putValue(valorTotal);
                            }
                        }
                        continue;
                    }
                    
                    String valor = rowData.get(campoEstandar);
                    
                    // Para UNIT_PRICE: usar snapshot capturado en hilo FX (thread-safe)
                    if ("UNIT_PRICE".equals(campoEstandar) && unitPriceSnapshotFX != null && idxRowData < unitPriceSnapshotFX.size()) {
                        String valorFX = unitPriceSnapshotFX.get(idxRowData);
                        // 🔍 DIAGNÓSTICO: Log para CADA fila de UNIT_PRICE
                        if (idxRowData < 5) {
                            logger.info("🔍 DIAG fila {} idx {}: campoEstandar='{}', RowData.get='{}', snapshot='{}', colIdx={}",
                                currentRow + 1, idxRowData, campoEstandar, valor, valorFX, colIdx);
                        }
                        if (valorFX != null && !valorFX.isEmpty()) {
                            if (!valorFX.equals(valor)) {
                                logger.warn("⚠️ UNIT_PRICE discrepancia hilo: RowData='{}', SnapshotFX='{}' - usando snapshot", valor, valorFX);
                            }
                            valor = valorFX;
                        }
                    }
                    
                    // Si no se encontró valor con campoEstandar, buscar con nombre de columna original
                    // Esto es necesario para campos como Supplier Notes, Supplier Comments, etc.
                    if (valor == null || valor.isEmpty()) {
                        String nombreColumnaOriginal = columna.getNombreColumnaOriginal();
                        if (nombreColumnaOriginal != null && !nombreColumnaOriginal.isEmpty()) {
                            valor = rowData.get(nombreColumnaOriginal);
                        }
                    }
                    
                    if (valor == null || valor.isEmpty()) {
                        continue;
                    }
                    
                    // Guardar valor de UNIT_PRICE (del snapshot FX) para forzar escritura post-cálculo
                    if ("UNIT_PRICE".equals(campoEstandar) && colUnitPrice >= 0) {
                        unitPricesPorFila.put(currentRow, valor);
                    }
                    
                    // Log detallado para primera fila
                    if (primeraFila) {
                        logger.info("📝 Fila {}: {}{}='{}' (tipo={})", 
                            currentRow + 1, columna.getLetraColumna(), currentRow + 1, 
                            valor, columna.getTipoDato());
                    }
                    
                    // Escribir valor - SIEMPRE intentar como número para columnas numéricas
                    String tipoDato = columna.getTipoDato();
                    String campoUpperCheck = campoEstandar != null ? campoEstandar.toUpperCase() : "";
                    
                    // Forzar escritura numérica para columnas de cantidad, precio, descuento, etc.
                    boolean esColumnaNum = tipoDato != null && 
                        (tipoDato.equalsIgnoreCase("DECIMAL") || 
                         tipoDato.equalsIgnoreCase("INTEGER") ||
                         tipoDato.equalsIgnoreCase("NUMERIC"));
                    boolean esColumnaQueDebeSerNum = campoUpperCheck.contains("QUANTITY") ||
                        campoUpperCheck.contains("PRICE") || campoUpperCheck.contains("DISCOUNT") ||
                        campoUpperCheck.contains("VAT") || campoUpperCheck.contains("AMOUNT") ||
                        campoUpperCheck.contains("QTY") || campoUpperCheck.contains("TOTAL");
                    
                    if (esColumnaNum || esColumnaQueDebeSerNum) {
                        try {
                            // Limpiar valor: remover comas de miles, símbolos de moneda, espacios
                            String valorLimpio = valor
                                .replace(" ", "")
                                .replace("$", "")
                                .replace("€", "")
                                .trim();
                            
                            // Manejar formato europeo (coma decimal) vs americano (punto decimal)
                            // Si tiene coma pero no punto, asumir que la coma es decimal
                            if (valorLimpio.contains(",") && !valorLimpio.contains(".")) {
                                valorLimpio = valorLimpio.replace(",", ".");
                            } else {
                                // Si tiene ambos, asumir formato americano (coma = miles)
                                valorLimpio = valorLimpio.replace(",", "");
                            }
                            
                            double numValue = Double.parseDouble(valorLimpio);
                            cell.putValue(numValue);
                            
                            // ============================
                            // ESCRIBIR TAMBIÉN EN COLUMNA DETECTADA DE LA FÓRMULA (si es diferente)
                            // ============================
                            int colFormula = -1;
                            String nombreCampo = "";
                            
                            if (campoUpperCheck.contains("QUANTITY") || campoUpperCheck.contains("QTY")) {
                                colFormula = colQuantityFormula;
                                nombreCampo = "QUANTITY";
                            } else if (campoUpperCheck.contains("UNIT_PRICE") || campoUpperCheck.contains("UNIT PRICE") ||
                                       (campoUpperCheck.contains("PRICE") && !campoUpperCheck.contains("TOTAL"))) {
                                colFormula = colUnitPriceFormula;
                                nombreCampo = "UNIT_PRICE";
                            } else if (campoUpperCheck.contains("DISCOUNT")) {
                                colFormula = colDiscountFormula;
                                nombreCampo = "DISCOUNT";
                            } else if (campoUpperCheck.contains("VAT") || campoUpperCheck.contains("TAX") ||
                                       campoUpperCheck.contains("IVA")) {
                                colFormula = colVatFormula;
                                nombreCampo = "VAT";
                            }
                            
                            // Si la columna de la fórmula es diferente a la del mapeo, escribir también ahí
                            if (colFormula >= 0 && colFormula != colIdx) {
                                com.aspose.cells.Cell cellFormula = cells.get(currentRow, colFormula);
                                cellFormula.putValue(numValue);
                                if (primeraFila) {
                                    logger.info("  📌 {} también escrito en col {} (fórmula) además de col {} (mapeo)", 
                                        nombreCampo, colFormula, colIdx);
                                }
                            }
                            
                            if (primeraFila) {
                                logger.info("  ✔️ Escrito como número: {}", numValue);
                            }
                        } catch (NumberFormatException e) {
                            cell.putValue(valor);
                            if (primeraFila) {
                                logger.warn("  ⚠️ No se pudo convertir a número, escrito como texto: {}", valor);
                            }
                        }
                    } else {
                        cell.putValue(valor);
                    }
                }
                
                // 📝 PROCURESHIP: Escribir columna "Supplier Notes" (no está en formato BD)
                if (supplierNotesExportColIndex >= 0) {
                    String supplierNotesVal = rowData.get("SUPPLIER_NOTES");
                    if (supplierNotesVal != null && !supplierNotesVal.isEmpty()) {
                        com.aspose.cells.Cell supplierNotesCell = cells.get(currentRow, supplierNotesExportColIndex);
                        supplierNotesCell.putValue(supplierNotesVal);
                        if (primeraFila) {
                            logger.info("📝 PROCURESHIP: Supplier Notes escrito en col {}: '{}'", 
                                supplierNotesExportColIndex, supplierNotesVal);
                        }
                    }
                }
                
                // ============================
                // ESCRIBIR 0 EN COLUMNAS DE FÓRMULA QUE QUEDARON VACÍAS
                // ============================
                // Las fórmulas de Total Price esperan valores numéricos en las columnas de
                // Quantity, Unit Price, Discount y VAT. Si alguna está vacía, escribir 0.
                if (colQuantityFormula >= 0) {
                    com.aspose.cells.Cell cellQty = cells.get(currentRow, colQuantityFormula);
                    if (cellQty.getValue() == null || cellQty.getStringValue().isEmpty()) {
                        cellQty.putValue(0.0);
                        if (primeraFila) {
                            logger.info("  📌 QUANTITY: escrito 0 en col {} (fórmula) - celda estaba vacía", colQuantityFormula);
                        }
                    }
                }
                if (colUnitPriceFormula >= 0) {
                    com.aspose.cells.Cell cellPrice = cells.get(currentRow, colUnitPriceFormula);
                    if (cellPrice.getValue() == null || cellPrice.getStringValue().isEmpty()) {
                        cellPrice.putValue(0.0);
                        if (primeraFila) {
                            logger.info("  📌 UNIT_PRICE: escrito 0 en col {} (fórmula) - celda estaba vacía", colUnitPriceFormula);
                        }
                    }
                }
                if (colDiscountFormula >= 0) {
                    com.aspose.cells.Cell cellDiscount = cells.get(currentRow, colDiscountFormula);
                    if (cellDiscount.getValue() == null || cellDiscount.getStringValue().isEmpty()) {
                        cellDiscount.putValue(0.0);
                        if (primeraFila) {
                            logger.info("  📌 DISCOUNT: escrito 0 en col {} (fórmula) - celda estaba vacía", colDiscountFormula);
                        }
                    }
                }
                if (colVatFormula >= 0) {
                    com.aspose.cells.Cell cellVat = cells.get(currentRow, colVatFormula);
                    if (cellVat.getValue() == null || cellVat.getStringValue().isEmpty()) {
                        cellVat.putValue(0.0);
                        if (primeraFila) {
                            logger.info("  📌 VAT: escrito 0 en col {} (fórmula) - celda estaba vacía", colVatFormula);
                        }
                    }
                }
                
                primeraFila = false;
                currentRow++;
                idxRowData++;
            }
            
            // ============================
            // PROCURESHIP: ASEGURAR QUE FILAS NO USADAS TENGAN VALORES NUMÉRICOS
            // ============================
            // Para ProcureShip, las filas entre el último producto escrito y el subtotal
            // quedaron con putValue("") (texto). Las fórmulas del TOTAL en esas filas
            // referencian QUANTITY y UNIT_PRICE que contienen "" (texto), causando #VALOR!.
            // Solución: escribir 0.0 en las columnas numéricas de esas filas vacías.
            if (esProcureship && filaSubtotalIndex >= 0) {
                int filaLimite = filaSubtotalIndex;
                // Si hay filas de resumen antes del subtotal, ajustar el límite
                for (int resumenRow : filasResumenProcureship) {
                    if (resumenRow < filaLimite) {
                        filaLimite = resumenRow;
                    }
                }
                
                logger.info("🏛️ PROCURESHIP: Limpiando filas no usadas entre {} y {} con valores numéricos...", 
                    currentRow + 1, filaLimite);
                
                for (int emptyRow = currentRow; emptyRow < filaLimite; emptyRow++) {
                    // Saltar filas especiales
                    if (filaEspecialIndex >= 0 && emptyRow == filaEspecialIndex) continue;
                    if (filasResumenProcureship.contains(emptyRow)) continue;
                    
                    // Escribir 0.0 en columnas numéricas que las fórmulas referencian
                    if (colQuantityFormula >= 0) {
                        com.aspose.cells.Cell c = cells.get(emptyRow, colQuantityFormula);
                        if (c != null) c.putValue(0.0);
                    }
                    if (colUnitPriceFormula >= 0) {
                        com.aspose.cells.Cell c = cells.get(emptyRow, colUnitPriceFormula);
                        if (c != null) c.putValue(0.0);
                    }
                    if (colDiscountFormula >= 0) {
                        com.aspose.cells.Cell c = cells.get(emptyRow, colDiscountFormula);
                        if (c != null) c.putValue(0.0);
                    }
                    if (colVatFormula >= 0) {
                        com.aspose.cells.Cell c = cells.get(emptyRow, colVatFormula);
                        if (c != null) c.putValue(0.0);
                    }
                    // También limpiar la columna TOTAL con 0.0 en lugar de texto
                    if (colTotal >= 0) {
                        com.aspose.cells.Cell c = cells.get(emptyRow, colTotal);
                        if (c != null) c.putValue(0.0);
                    }
                }
                
                logger.info("🏛️ PROCURESHIP: {} filas no usadas limpiadas con valores numéricos", 
                    Math.max(0, filaLimite - currentRow));
            }
            
            // ============================
            // ACTUALIZAR FÓRMULA DE SUBTOTAL
            // ============================
            if (filaSubtotalIndex >= 0 && colTotal >= 0) {
                // IMPORTANTE: Aspose usa índices 0-based, Excel usa 1-based
                // dataStartRow es 0-based (Aspose)
                // currentRow es 0-based (Aspose)
                // La fórmula de Excel necesita filas 1-based
                
                // Calcular el rango de filas de datos (en formato Excel 1-based)
                int primeraFilaDatosExcel = dataStartRow + 1;  // Convertir a 1-based
                int ultimaFilaDatosExcel = currentRow;         // currentRow-1 (0-based) + 1 (para Excel) = currentRow
                
                logger.info("🔍 Calculando subtotal: dataStartRow(0-based)={}, currentRow(0-based)={}, primeraFilaDatosExcel(1-based)={}, ultimaFilaDatosExcel(1-based)={}",
                    dataStartRow, currentRow, primeraFilaDatosExcel, ultimaFilaDatosExcel);
                
                // IMPORTANTE: Excluir la fila de subtotal del rango de suma
                // La fila de subtotal está en filaSubtotalIndex (0-based), que corresponde a filaSubtotalIndex+1 (1-based)
                // El rango debe terminar ANTES de la fila de subtotal
                if (filaSubtotalIndex >= dataStartRow && filaSubtotalIndex < currentRow) {
                    ultimaFilaDatosExcel = filaSubtotalIndex;  // filaSubtotalIndex (0-based) = filaSubtotalIndex (1-based - 1)
                    logger.info("⚠️ Fila de subtotal en índice {} (1-based: {}), terminando rango en fila {}", 
                        filaSubtotalIndex, filaSubtotalIndex + 1, ultimaFilaDatosExcel);
                }
                
                // Si hay fila especial, ajustar el rango para excluirla también
                if (filaEspecialIndex >= 0 && filaEspecialIndex >= dataStartRow && filaEspecialIndex < ultimaFilaDatosExcel) {
                    logger.info("⚠️ Hay fila especial en índice {} (1-based: {}), ajustando rango", 
                        filaEspecialIndex, filaEspecialIndex + 1);
                    // Terminar antes de la fila especial si está dentro del rango actual
                    ultimaFilaDatosExcel = filaEspecialIndex;  // filaEspecialIndex (0-based) = fila anterior en 1-based
                }
                
                // Validar que hay filas de datos para sumar
                if (ultimaFilaDatosExcel < primeraFilaDatosExcel) {
                    logger.warn("⚠️ No hay filas de datos válidas para el subtotal. Primera: {}, Última: {}", 
                        primeraFilaDatosExcel, ultimaFilaDatosExcel);
                    // Poner 0 directamente en lugar de fórmula inválida
                    com.aspose.cells.Cell celdaSubtotal = cells.get(filaSubtotalIndex, colTotal);
                    if (celdaSubtotal != null) {
                        celdaSubtotal.putValue(0.0);
                        logger.info("📊 Subtotal establecido a 0 (sin datos para sumar)");
                    }
                } else {
                    // Crear nueva fórmula de suma
                    String letraCol = letraTotal;
                    if (letraCol == null || letraCol.isEmpty()) {
                        // Calcular letra de columna si no está disponible
                        letraCol = String.valueOf((char)('A' + colTotal));
                        if (colTotal >= 26) {
                            letraCol = String.valueOf((char)('A' + colTotal / 26 - 1)) + 
                                       String.valueOf((char)('A' + colTotal % 26));
                        }
                    }
                    
                    // Usar SUM (nombre universal de la función que funciona en todas las versiones de Excel)
                    String nuevaFormulaSubtotal = "=SUM(" + letraCol + primeraFilaDatosExcel + ":" + letraCol + ultimaFilaDatosExcel + ")";
                    
                    com.aspose.cells.Cell celdaSubtotal = cells.get(filaSubtotalIndex, colTotal);
                    if (celdaSubtotal != null) {
                        // Verificar estado actual de la celda ANTES de establecer fórmula
                        String estadoAntes = celdaSubtotal.isFormula() ? 
                            "Fórmula: " + celdaSubtotal.getFormula() : 
                            "Valor: " + celdaSubtotal.getValue();
                        
                        // Establecer estilo numérico antes de poner la fórmula
                        com.aspose.cells.Style style = celdaSubtotal.getStyle();
                        style.setNumber(2);  // Formato numérico con 2 decimales
                        celdaSubtotal.setStyle(style);
                        
                        // SIEMPRE establecer la fórmula (no verificar valores porque las fórmulas aún no se han calculado)
                        celdaSubtotal.setFormula(nuevaFormulaSubtotal);
                        
                        // Verificar que se estableció correctamente
                        String estadoDespues = celdaSubtotal.isFormula() ? 
                            "Fórmula: " + celdaSubtotal.getFormula() : 
                            "Valor: " + celdaSubtotal.getValue();
                        
                        logger.info("📊 ✅ SUBTOTAL ACTUALIZADO:");
                        logger.info("   Celda: {}{} (fila 0-based: {}, col: {})", letraCol, filaSubtotalIndex + 1, filaSubtotalIndex, colTotal);
                        logger.info("   Antes: {}", estadoAntes);
                        logger.info("   Después: {}", estadoDespues);
                        logger.info("   Rango de suma: {}{} hasta {}{}", letraCol, primeraFilaDatosExcel, letraCol, ultimaFilaDatosExcel);
                    }
                }
            }
            
            // CMA CGM: aplicar fórmulas detectadas en carga (Grand Total) y asegurar 'Item Sub Total'
            aplicarFormulasCMACGMDespuesDeEscritura(cells, dataStartRow, currentRow, colTotal, letraTotal);

            // ============================
            // RECALCULAR FÓRMULAS
            // ============================
            logger.info("📊 Recalculando fórmulas...");
            workbook.calculateFormula();
            logger.info("✅ Fórmulas recalculadas");
            
            // ============================
            // FORZAR UNIT_PRICE DESPUÉS DE calculateFormula()
            // ============================
            // calculateFormula() y save() pueden restaurar fórmulas originales del template.
            // Forzamos la escritura limpiando fórmulas y estableciendo formato numérico.
            // IMPORTANTE: Escribir en AMBAS columnas (mapeo BD y fórmula) porque pueden diferir
            // Ej: CMA CGM mapea UNIT_PRICE a K(10) pero la fórmula TOTAL usa L(11) para Unit Price.
            if (colUnitPrice >= 0 && !unitPricesPorFila.isEmpty()) {
                boolean dosColumnas = colUnitPriceFormula >= 0 && colUnitPriceFormula != colUnitPrice;
                logger.info("🔧 Forzando {} valores de UNIT_PRICE en col {} (mapeo){}", 
                    unitPricesPorFila.size(), colUnitPrice,
                    dosColumnas ? " + col " + colUnitPriceFormula + " (fórmula)" : "");
                int reparados = 0;
                for (java.util.Map.Entry<Integer, String> entry : unitPricesPorFila.entrySet()) {
                    int fila = entry.getKey();
                    String val = entry.getValue();
                    
                    try {
                        String valorLimpio = val.replace(" ", "").replace("$", "").replace("€", "").trim();
                        if (valorLimpio.contains(",") && !valorLimpio.contains(".")) {
                            valorLimpio = valorLimpio.replace(",", ".");
                        } else {
                            valorLimpio = valorLimpio.replace(",", "");
                        }
                        double numVal = Double.parseDouble(valorLimpio);
                        
                        // Escribir en columna del mapeo BD
                        com.aspose.cells.Cell cellUP = cells.get(fila, colUnitPrice);
                        if (cellUP.isFormula()) {
                            try { cellUP.setFormula(null); } catch (Exception ex) { /* ignore */ }
                            reparados++;
                        }
                        cellUP.putValue(numVal);
                        com.aspose.cells.Style st = cellUP.getStyle();
                        st.setNumber(2);
                        cellUP.setStyle(st);
                        
                        // Escribir TAMBIÉN en columna de fórmula si es diferente
                        // (ej: CMA CGM: mapeo=K, fórmula=L)
                        if (dosColumnas) {
                            com.aspose.cells.Cell cellFormula = cells.get(fila, colUnitPriceFormula);
                            if (cellFormula.isFormula()) {
                                try { cellFormula.setFormula(null); } catch (Exception ex) { /* ignore */ }
                                reparados++;
                            }
                            cellFormula.putValue(numVal);
                            com.aspose.cells.Style stF = cellFormula.getStyle();
                            stF.setNumber(2);
                            cellFormula.setStyle(stF);
                        }
                    } catch (NumberFormatException e) {
                        com.aspose.cells.Cell cellUP = cells.get(fila, colUnitPrice);
                        cellUP.putValue(val);
                        if (dosColumnas) {
                            com.aspose.cells.Cell cellFormula = cells.get(fila, colUnitPriceFormula);
                            cellFormula.putValue(val);
                        }
                    }
                }
                logger.info("✅ UNIT_PRICE forzado en {} filas ({} fórmulas limpiadas)", 
                    unitPricesPorFila.size(), reparados);
                
                // Recalcular fórmulas DESPUÉS del force-write para que TOTAL (O) use el nuevo UNIT_PRICE (L)
                // Necesario cuando colUnitPriceFormula != colUnitPrice (ej: CMA CGM: mapeo=K, fórmula=L)
                if (dosColumnas) {
                    logger.info("📊 Recalculando fórmulas post force-write (UNIT_PRICE en col {} actualizado)...", colUnitPriceFormula);
                    workbook.calculateFormula();
                    logger.info("✅ Fórmulas recalculadas con UNIT_PRICE correcto");
                }
            }

            // CMA CGM: Verificar que las fórmulas sobrevivieron el recálculo
            if (!cmaCgmSpecialRows.isEmpty() && colTotal >= 0) {
                for (java.util.Map.Entry<Integer, String> entry : cmaCgmSpecialRows.entrySet()) {
                    int specialRow = entry.getKey();
                    com.aspose.cells.Cell cVerif = cells.get(specialRow, colTotal);
                    logger.info("✨ VERIF POST-CALC fila especial {}: isFormula={}, formula={}, value={}",
                        specialRow + 1,
                        cVerif != null && cVerif.isFormula(),
                        cVerif != null && cVerif.isFormula() ? cVerif.getFormula() : "N/A",
                        cVerif != null ? cVerif.getValue() : "NULL");
                }
            }
            
            // ============================
            // VERIFICAR SUBTOTAL DESPUÉS DEL RECÁLCULO
            // ============================
            if (filaSubtotalIndex >= 0 && colTotal >= 0) {
                com.aspose.cells.Cell celdaSubtotalVerificar = cells.get(filaSubtotalIndex, colTotal);
                if (celdaSubtotalVerificar != null) {
                    String estadoFinal = celdaSubtotalVerificar.isFormula() ? 
                        "Fórmula: " + celdaSubtotalVerificar.getFormula() : 
                        "Valor: " + celdaSubtotalVerificar.getValue();
                    logger.info("⚠️ 🔍 VERIFICACIÓN DESPUÉS DEL RECÁLCULO:");
                    logger.info("   Celda del subtotal: {}{}", letraTotal, filaSubtotalIndex + 1);
                    logger.info("   Estado final: {}", estadoFinal);
                    
                    // Si la fórmula cambió, registrar ALERTA
                    if (celdaSubtotalVerificar.isFormula()) {
                        String formulaActual = celdaSubtotalVerificar.getFormula();
                        if (!formulaActual.toUpperCase().contains("SUMA") && !formulaActual.toUpperCase().contains("SUM")) {
                            logger.error("❌ ❌ ❌ PROBLEMA DETECTADO: La fórmula del subtotal fue SOBRESCRITA después del recálculo!");
                            logger.error("   Fórmula incorrecta: {}", formulaActual);
                            logger.error("   Debería ser: =SUMA({}{}:{}{})", letraTotal, dataStartRow + 1, letraTotal, filaSubtotalIndex);
                        }
                    }
                }
            }
            
            // 🔍 DIAGNÓSTICO PRE-SAVE: Verificar UNIT_PRICE justo antes de guardar
            if (colUnitPrice >= 0 && !unitPricesPorFila.isEmpty()) {
                logger.info("🔍 PRE-SAVE: Verificando UNIT_PRICE en col {} para {} filas...", colUnitPrice, unitPricesPorFila.size());
                int filasDiag = 0;
                for (java.util.Map.Entry<Integer, String> entry : unitPricesPorFila.entrySet()) {
                    if (filasDiag >= 5) break;
                    int fila = entry.getKey();
                    String valEsperado = entry.getValue();
                    com.aspose.cells.Cell cellVerif = cells.get(fila, colUnitPrice);
                    logger.info("🔍 PRE-SAVE fila {}: esperado='{}', isFormula={}, type={}, value='{}', stringValue='{}'",
                        fila + 1, valEsperado,
                        cellVerif.isFormula(),
                        cellVerif.getType(),
                        cellVerif.getValue(),
                        cellVerif.getStringValue());
                    filasDiag++;
                }
            }
            
            // Guardar con Aspose (preserva macros automáticamente)
            String rutaDestino = archivoDestino.getAbsolutePath();
            
            // Asegurar extensión .xlsm si tiene macros
            if (workbook.getVbaProject() != null && !rutaDestino.toLowerCase().endsWith(".xlsm")) {
                rutaDestino = rutaDestino.replaceAll("\\.[^.]+$", ".xlsm");
            }
            
            asposeService.guardarWorkbook(rutaDestino);
            
            // Verificar macros preservadas
            String infoMacros = "";
            if (workbook.getVbaProject() != null) {
                int modulosVba = workbook.getVbaProject().getModules().getCount();
                infoMacros = "\n✅ Macros VBA: " + modulosVba + " módulos preservados";
                logger.info("✅ Macros VBA preservadas: {} módulos", modulosVba);
            }
            
            logger.info("✅ Cotización exportada exitosamente con Aspose: {}", rutaDestino);
            
            // Mensaje de confirmación
            final String brokerNombre = formatoActual.getBrokerName();
            final String rutaFinal = rutaDestino;
            final String infoMacrosFinal = infoMacros;
            final int totalProductos = datosExportar.size();
            ejecutarEnHiloFX(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Exportación Exitosa (Aspose)");
                alert.setHeaderText("✅ Cotización exportada con macros preservadas");
                alert.setContentText("Archivo guardado en:\n" + rutaFinal + 
                    "\n\n✔ Broker: " + brokerNombre +
                    "\n✔ Motor: Aspose.Cells (preserva macros)" +
                    "\n✔ Productos: " + totalProductos +
                    infoMacrosFinal +
                    "\n\n💡 Las macros VBA del archivo original están intactas.");
                alert.showAndWait();
            });
            
        } catch (Exception e) {
            logger.error("❌ Error al exportar con Aspose", e);
            final String mensajeError = e.getMessage();
            ejecutarEnHiloFX(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Error al exportar con Aspose");
                alert.setContentText("No se pudo exportar el archivo:\n" + mensajeError);
                alert.showAndWait();
            });
        }
    }
    
    // ============================================================
    // 🏛️ MÉTODOS ESPECÍFICOS POR BROKER
    // ============================================================
    
    /**
     * Clase interna para almacenar información del broker durante la exportación
     */
    private static class BrokerExportContext {
        public int filaEspecialIndex = -1;
        public int filaSubtotalIndex = -1;      // "Item Sub Total"
        public int filaTotalPriceIndex = -1;    // "Total Price" (para CMA CGM y otros)
        public int dataStartRow;
        public int colTotal = -1;
        public String letraTotal = "";
        
        public BrokerExportContext(int dataStartRow) {
            this.dataStartRow = dataStartRow;
        }
    }
    
    /**
     * Aplica lógica específica del broker para detectar y procesar particularidades.
     * Delega a métodos específicos según el broker.
     * 
     * @param brokerName Nombre del broker
     * @param cells Celdas del workbook Aspose
     * @param context Contexto de exportación del broker
     */
    private void aplicarLogicaEspecificaBroker(String brokerName, com.aspose.cells.Cells cells, BrokerExportContext context) {
        if (brokerName == null) {
            aplicarLogicaGenerica(cells, context);
            return;
        }
        
        String brokerUpper = brokerName.toUpperCase();
        
        if (brokerUpper.contains("BSM") || brokerUpper.contains("CATERING")) {
            aplicarLogicaBSM(cells, context);
        } else if (brokerUpper.contains("CMA") && brokerUpper.contains("CGM")) {
            aplicarLogicaCMAGM(cells, context);
        } else if (brokerUpper.contains("MCTC") || brokerUpper.contains("MARINE")) {
            aplicarLogicaMCTC(cells, context);
        } else if (brokerUpper.contains("OCEANIC")) {
            aplicarLogicaOceanic(cells, context);
        } else if (brokerUpper.contains("GARRETS")) {
            aplicarLogicaGarrets(cells, context);
        } else if (brokerUpper.contains("PROCURESHIP")) {
            aplicarLogicaProcureship(cells, context);
        } else {
            aplicarLogicaGenerica(cells, context);
        }
    }
    
    /**
     * Lógica genérica para brokers sin configuración específica.
     * Intenta detectar automáticamente filas especiales y subtotales.
     */
    private void aplicarLogicaGenerica(com.aspose.cells.Cells cells, BrokerExportContext context) {
        logger.info("🔧 Aplicando lógica genérica de detección");
        
        // Intentar detectar fila especial por texto común
        detectarFilaEspecialPorTexto(cells, context, new String[]{
            "PROVISIONS", "PROVISION", "ITEMS", "PRODUCTS", "PRODUCTOS", "LISTA"
        });
        
        // Intentar detectar subtotal automáticamente
        detectarFilaSubtotalAutomatica(cells, context);
    }
    
    /**
     * Lógica específica para BSM CATERING.
     * - Fila especial "PROVISIONS" después de las cabeceras
     * - Subtotal con fórmula SUM desde dataStartRow
     */
    private void aplicarLogicaBSM(com.aspose.cells.Cells cells, BrokerExportContext context) {
        logger.info("🏛️ Aplicando lógica específica de BSM CATERING");
        
        // FILA ESPECIAL: Para BSM, la fila después de las cabeceras es siempre "PROVISIONS"
        context.filaEspecialIndex = context.dataStartRow;
        context.dataStartRow = context.dataStartRow + 1;
        logger.info("🟡 BSM: Fila especial 'PROVISIONS' en fila {} (forzado)", context.filaEspecialIndex + 1);
        logger.info("📦 BSM: Datos empezarán desde fila {}", context.dataStartRow + 1);
        
        // SUBTOTAL: Detectar automáticamente por fórmula SUM
        detectarFilaSubtotalAutomatica(cells, context);
    }
    
    /**
     * Lógica específica para CMA CGM.
     * Particularidades:
     * - Puede tener fila especial
     * - Tiene DOS filas de totales:
     *   1. "Item Sub Total" - Suma de todos los productos
     *   2. "Total Price" - Total final (con impuestos/descuentos)
     */
    private void aplicarLogicaCMAGM(com.aspose.cells.Cells cells, BrokerExportContext context) {
        logger.info("🏛️ Aplicando lógica específica de CMA CGM");
        
        // PASO 1: Detectar fila especial si existe
        detectarFilaEspecialPorTexto(cells, context, new String[]{
            "PROVISIONS", "PROVISION", "ITEMS", "PRODUCTS", "SUPPLY LIST"
        });
        
        // PASO 2: Detectar las DOS filas de totales de CMA CGM
        detectarFilasTotalesCMAGM(cells, context);
    }
    
    /**
     * Detecta las dos filas de totales específicas de CMA CGM:
     * 1. "Item Sub Total" - Suma de productos
     * 2. "Total Price" - Total final
     */
    private void detectarFilasTotalesCMAGM(com.aspose.cells.Cells cells, BrokerExportContext context) {
        int lastDataRow = cells.getMaxDataRow();
        logger.info("🔍 CMA CGM: Buscando filas de totales entre {} y {}...", 
            context.dataStartRow + 1, lastDataRow + 1);
        
        // Buscar desde el final hacia arriba
        for (int searchRow = lastDataRow; searchRow >= context.dataStartRow; searchRow--) {
            // Buscar texto "Item Sub Total" o "Total Price" en las primeras columnas
            for (int colCheck = 0; colCheck <= 10; colCheck++) {
                com.aspose.cells.Cell celdaCheck = cells.get(searchRow, colCheck);
                if (celdaCheck != null && celdaCheck.getValue() != null) {
                    String valorCelda = celdaCheck.getStringValue().trim().toUpperCase();
                    
                    // Detectar "Item Sub Total"
                    if ((valorCelda.contains("ITEM") && valorCelda.contains("SUB") && valorCelda.contains("TOTAL")) ||
                        valorCelda.equals("ITEM SUB TOTAL") || valorCelda.equals("SUB TOTAL")) {
                        
                        context.filaSubtotalIndex = searchRow;
                        logger.info("📊 CMA CGM: 'Item Sub Total' detectado en fila {} (0-idx: {})", 
                            searchRow + 1, searchRow);
                        
                        // Verificar si tiene fórmula SUM en colTotal
                        if (context.colTotal >= 0) {
                            com.aspose.cells.Cell celdaTotal = cells.get(searchRow, context.colTotal);
                            if (celdaTotal != null && celdaTotal.isFormula()) {
                                logger.info("📝   Fórmula actual: {}", celdaTotal.getFormula());
                            }
                        }
                    }
                    
                    // Detectar "Total Price"
                    else if ((valorCelda.contains("TOTAL") && valorCelda.contains("PRICE")) ||
                             valorCelda.equals("TOTAL PRICE") || 
                             (valorCelda.equals("TOTAL") && context.filaSubtotalIndex >= 0 && searchRow > context.filaSubtotalIndex)) {
                        
                        context.filaTotalPriceIndex = searchRow;
                        logger.info("📊 CMA CGM: 'Total Price' detectado en fila {} (0-idx: {})", 
                            searchRow + 1, searchRow);
                        
                        // Verificar si tiene fórmula en colTotal
                        if (context.colTotal >= 0) {
                            com.aspose.cells.Cell celdaTotal = cells.get(searchRow, context.colTotal);
                            if (celdaTotal != null && celdaTotal.isFormula()) {
                                logger.info("📝   Fórmula actual: {}", celdaTotal.getFormula());
                            }
                        }
                    }
                }
            }
        }
        
        // Resumen de lo detectado
        if (context.filaSubtotalIndex >= 0) {
            logger.info("✅ CMA CGM: Item Sub Total en fila {}", context.filaSubtotalIndex + 1);
        } else {
            logger.warn("⚠️ CMA CGM: NO se detectó 'Item Sub Total'");
        }
        
        if (context.filaTotalPriceIndex >= 0) {
            logger.info("✅ CMA CGM: Total Price en fila {}", context.filaTotalPriceIndex + 1);
        } else {
            logger.warn("⚠️ CMA CGM: NO se detectó 'Total Price'");
        }
    }
    
    /**
     * Actualiza las fórmulas de las dos filas de totales de CMA CGM.
     * Debe llamarse DESPUÉS de escribir los datos.
     * 
     * @param cells Celdas del workbook
     * @param context Contexto con las filas detectadas
     * @param currentRow Fila actual después de escribir todos los productos (0-based)
     */
    private void actualizarFormulasTotalesCMAGM(com.aspose.cells.Cells cells, BrokerExportContext context, int currentRow) {
        if (context.colTotal < 0) {
            logger.warn("⚠️ CMA CGM: No se puede actualizar totales - columna Total no detectada");
            return;
        }
        
        String letraCol = context.letraTotal;
        int primeraFilaDatosExcel = context.dataStartRow + 1;  // Convertir a 1-based
        int ultimaFilaDatosExcel = currentRow;  // Última fila escrita (ya en formato para Excel)
        
        // Ajustar rango excluyendo filas especiales
        if (context.filaSubtotalIndex >= 0 && context.filaSubtotalIndex < currentRow) {
            ultimaFilaDatosExcel = context.filaSubtotalIndex;  // Terminar antes del subtotal
        }
        
        logger.info("🔧 CMA CGM: Actualizando fórmulas de totales...");
        logger.info("   Rango de datos: {}{} hasta {}{}", letraCol, primeraFilaDatosExcel, letraCol, ultimaFilaDatosExcel);
        
        // ============================
        // 1. ACTUALIZAR "Item Sub Total"
        // ============================
        if (context.filaSubtotalIndex >= 0) {
            com.aspose.cells.Cell celdaSubtotal = cells.get(context.filaSubtotalIndex, context.colTotal);
            if (celdaSubtotal != null) {
                String formulaAnterior = celdaSubtotal.isFormula() ? celdaSubtotal.getFormula() : "(sin fórmula)";
                
                // Crear fórmula SUMA para Item Sub Total
                String formulaItemSubTotal = "=SUM(" + letraCol + primeraFilaDatosExcel + ":" + letraCol + ultimaFilaDatosExcel + ")";
                
                // Limpiar celda y establecer nueva fórmula
                celdaSubtotal.putValue("");  // Limpiar
                celdaSubtotal.setFormula(formulaItemSubTotal);
                
                // Establecer formato numérico
                com.aspose.cells.Style style = celdaSubtotal.getStyle();
                style.setNumber(2);  // 2 decimales
                celdaSubtotal.setStyle(style);
                
                logger.info("✅ CMA CGM: 'Item Sub Total' actualizado en fila {}", context.filaSubtotalIndex + 1);
                logger.info("   Fórmula anterior: {}", formulaAnterior);
                logger.info("   Fórmula nueva: {}", formulaItemSubTotal);
            }
        }
        
        // ============================
        // 2. ACTUALIZAR "Total Price"
        // ============================
        if (context.filaTotalPriceIndex >= 0) {
            com.aspose.cells.Cell celdaTotalPrice = cells.get(context.filaTotalPriceIndex, context.colTotal);
            if (celdaTotalPrice != null) {
                String formulaAnterior = celdaTotalPrice.isFormula() ? celdaTotalPrice.getFormula() : "(sin fórmula)";
                
                // Total Price generalmente referencia al Item Sub Total (o suma lo mismo)
                // Opción A: Referenciar directamente al Item Sub Total
                String formulaTotalPrice;
                if (context.filaSubtotalIndex >= 0) {
                    // Referenciar al Item Sub Total
                    String celdaSubtotalRef = letraCol + (context.filaSubtotalIndex + 1);
                    formulaTotalPrice = "=" + celdaSubtotalRef;
                    logger.info("🔗 Total Price referenciará al Item Sub Total: {}", celdaSubtotalRef);
                } else {
                    // Si no hay Item Sub Total, sumar directamente
                    formulaTotalPrice = "=SUM(" + letraCol + primeraFilaDatosExcel + ":" + letraCol + ultimaFilaDatosExcel + ")";
                    logger.info("📊 Total Price sumará directamente los productos");
                }
                
                // Limpiar celda y establecer nueva fórmula
                celdaTotalPrice.putValue("");  // Limpiar
                celdaTotalPrice.setFormula(formulaTotalPrice);
                
                // Establecer formato numérico
                com.aspose.cells.Style style = celdaTotalPrice.getStyle();
                style.setNumber(2);  // 2 decimales
                celdaTotalPrice.setStyle(style);
                
                logger.info("✅ CMA CGM: 'Total Price' actualizado en fila {}", context.filaTotalPriceIndex + 1);
                logger.info("   Fórmula anterior: {}", formulaAnterior);
                logger.info("   Fórmula nueva: {}", formulaTotalPrice);
            }
        }
        
        logger.info("✅ CMA CGM: Totales actualizados exitosamente");
    }
    
    /**
     * Lógica específica para MCTC MARINE LTD.
     */
    private void aplicarLogicaMCTC(com.aspose.cells.Cells cells, BrokerExportContext context) {
        logger.info("🏛️ Aplicando lógica específica de MCTC MARINE LTD");
        aplicarLogicaGenerica(cells, context);
    }
    
    /**
     * Lógica específica para OCEANIC CATERING LTD.
     */
    private void aplicarLogicaOceanic(com.aspose.cells.Cells cells, BrokerExportContext context) {
        logger.info("🏛️ Aplicando lógica específica de OCEANIC CATERING LTD");
        aplicarLogicaGenerica(cells, context);
    }
    
    /**
     * Lógica específica para GARRETS INTERNATIONAL LTD.
     */
    private void aplicarLogicaGarrets(com.aspose.cells.Cells cells, BrokerExportContext context) {
        logger.info("🏛️ Aplicando lógica específica de GARRETS INTERNATIONAL LTD");
        aplicarLogicaGenerica(cells, context);
    }
    
    /**
     * Lógica específica para PROCURESHIP.
     */
    private void aplicarLogicaProcureship(com.aspose.cells.Cells cells, BrokerExportContext context) {
        logger.info("🏛️ Aplicando lógica específica de PROCURESHIP");
        aplicarLogicaGenerica(cells, context);
    }
    
    /**
     * Detecta fila especial buscando textos específicos en la primera fila de datos.
     */
    private void detectarFilaEspecialPorTexto(com.aspose.cells.Cells cells, BrokerExportContext context, String[] textosABuscar) {
        for (int colCheck = 0; colCheck <= 20; colCheck++) {
            com.aspose.cells.Cell celdaCheck = cells.get(context.dataStartRow, colCheck);
            if (celdaCheck != null && celdaCheck.getValue() != null) {
                String valorCelda = celdaCheck.getStringValue().trim().toUpperCase();
                for (String texto : textosABuscar) {
                    if (valorCelda.equals(texto)) {
                        context.filaEspecialIndex = context.dataStartRow;
                        context.dataStartRow = context.dataStartRow + 1;
                        logger.info("🟡 Fila especial '{}' detectada en col {} de fila {}", 
                            valorCelda, colCheck, context.filaEspecialIndex + 1);
                        logger.info("📦 Datos empezarán desde fila {}", context.dataStartRow + 1);
                        return;
                    }
                }
            }
        }
    }
    
    /**
     * Detecta automáticamente la fila de subtotal buscando fórmulas SUM/SUMA
     * que sumen desde cerca de dataStartRow.
     */
    private void detectarFilaSubtotalAutomatica(com.aspose.cells.Cells cells, BrokerExportContext context) {
        if (context.colTotal < 0) {
            return; // No hay columna Total, no se puede detectar subtotal
        }
        
        int lastDataRow = cells.getMaxDataRow();
        logger.info("🔍 Buscando subtotal entre filas {} y {} (lastDataRow: {})...", 
            context.dataStartRow + 1, lastDataRow + 1, lastDataRow);
        
        // Buscar desde el final hacia arriba
        for (int searchRow = lastDataRow; searchRow >= context.dataStartRow; searchRow--) {
            com.aspose.cells.Cell celdaTotalCheck = cells.get(searchRow, context.colTotal);
            if (celdaTotalCheck != null && celdaTotalCheck.isFormula()) {
                String formulaCheck = celdaTotalCheck.getFormula().toUpperCase();
                logger.info("🔍 Fila {} tiene fórmula: {}", searchRow + 1, formulaCheck);
                
                // Verificar si es una fórmula SUM/SUMA que suma desde cerca de dataStartRow
                if (formulaCheck.contains("SUM") || formulaCheck.contains("SUMA")) {
                    // Extraer el rango de la fórmula: =SUM(P21:P234) -> P21:P234
                    java.util.regex.Pattern patronRango = java.util.regex.Pattern.compile("([A-Z]+)(\\d+):([A-Z]+)(\\d+)");
                    java.util.regex.Matcher matcherRango = patronRango.matcher(formulaCheck);
                    
                    if (matcherRango.find()) {
                        int filaInicio = Integer.parseInt(matcherRango.group(2));
                        int filaFin = Integer.parseInt(matcherRango.group(4));
                        
                        int dataStartExcel = context.dataStartRow + 1;  // Convertir a 1-based
                        int rangoReal = filaFin - filaInicio + 1;
                        
                        logger.info("  → Rango: {}-{} ({} filas), esperado desde fila {}",
                            filaInicio, filaFin, rangoReal, dataStartExcel);
                        
                        // Si empieza cerca de dataStartRow (dentro de 5 filas) y suma un rango grande
                        if (Math.abs(filaInicio - dataStartExcel) <= 5 && rangoReal >= 10) {
                            context.filaSubtotalIndex = searchRow;
                            logger.info("📊 ✅ FILA DE SUBTOTAL DETECTADA: fila {} (0-idx: {}) con fórmula: {}",
                                searchRow + 1, searchRow, celdaTotalCheck.getFormula());
                            return;
                        }
                    }
                }
            }
        }
        
        logger.info("⚠️ No se detectó fila de subtotal automáticamente");
    }
    
    // ============================================================
    // 🛠️ MÉTODOS AUXILIARES
    // ============================================================
    
    /**
     * Ajusta las referencias de fila en una fórmula de Excel.
     * Por ejemplo, si la fórmula es "=K25*M25" y el desplazamiento es 1,
     * el resultado será "=K26*M26".
     * 
     * @param formula Fórmula original
     * @param desplazamiento Número de filas a desplazar (positivo = abajo, negativo = arriba)
     * @return Fórmula con las referencias de fila ajustadas
     */
    private String ajustarReferenciasFormula(String formula, int desplazamiento) {
        if (formula == null || formula.isEmpty() || desplazamiento == 0) {
            return formula;
        }
        
        // Usar regex para encontrar referencias de celdas (ej: A1, AB123, $A$1, A$1, $A1)
        // Patrón: letra(s) seguida(s) de número(s), opcionalmente con $ antes de cada parte
        StringBuilder resultado = new StringBuilder();
        java.util.regex.Pattern patron = java.util.regex.Pattern.compile(
            "(\\$?[A-Z]+)(\\$?)(\\d+)"
        );
        java.util.regex.Matcher matcher = patron.matcher(formula);
        
        int ultimaPosicion = 0;
        while (matcher.find()) {
            // Agregar texto antes de la coincidencia
            resultado.append(formula, ultimaPosicion, matcher.start());
            
            String columna = matcher.group(1);  // Ej: "K" o "$K"
            String signoFila = matcher.group(2); // "$" si es referencia absoluta de fila, "" si es relativa
            String filaStr = matcher.group(3);   // Ej: "25"
            
            // Si la fila tiene $, es absoluta y no se debe ajustar
            if ("$".equals(signoFila)) {
                resultado.append(columna).append(signoFila).append(filaStr);
            } else {
                // Ajustar la fila
                int filaOriginal = Integer.parseInt(filaStr);
                int filaNueva = filaOriginal + desplazamiento;
                resultado.append(columna).append(filaNueva);
            }
            
            ultimaPosicion = matcher.end();
        }
        
        // Agregar el resto de la fórmula
        resultado.append(formula.substring(ultimaPosicion));
        
        return resultado.toString();
    }
    
    /**
     * Convierte una letra de columna de Excel (A, B, ..., Z, AA, AB, ...) a un índice (0, 1, ..., 25, 26, 27, ...)
     * @param letra Letra de columna (ej: "A", "B", "AA", "AB")
     * @return Índice de columna (0-indexed)
     */
    private int letraAIndice(String letra) {
        if (letra == null || letra.isEmpty()) {
            return -1;
        }
        letra = letra.toUpperCase();
        int indice = 0;
        for (int i = 0; i < letra.length(); i++) {
            indice = indice * 26 + (letra.charAt(i) - 'A' + 1);
        }
        return indice - 1; // Convertir a 0-indexed
    }

    // ============================
    // CMA CGM: Detección de TODAS las filas especiales durante la carga
    // ============================
    private void detectarTotalesCMACGMEnCarga(com.aspose.cells.Cells cells) {
        try {
            logger.info("🔎 CMA CGM (carga): iniciando detección de totales");

            if (formatoActual == null || formatoActual.getBrokerName() == null) {
                logger.debug("CMA CGM (carga): formatoActual o brokerName es null, se omite detección");
                return;
            }

            String brokerUpper = formatoActual.getBrokerName().toUpperCase();
            if (!(brokerUpper.contains("CMA") && brokerUpper.contains("CGM"))) {
                logger.debug("CMA CGM (carga): broker '{}' no corresponde a CMA CGM, se omite", formatoActual.getBrokerName());
                return;
            }

            logger.info("CMA CGM (carga): broker detectado='{}'", formatoActual.getBrokerName());

            // Reset estado previo
            cmaCgmSpecialRows.clear();

            int headerRowIndex = formatoActual.getHeaderRow();
            int dataStartRow = headerRowIndex + 1;

            // Detectar columna TOTAL según formato
            int colTotal = -1;
            String letraTotal = "";
            for (FormatoColumna columna : formatoActual.getColumnas()) {
                String campo = columna.getCampoEstandar() != null ? columna.getCampoEstandar().toUpperCase() : "";
                String nombreCol = columna.getNombreColumnaOriginal() != null ? columna.getNombreColumnaOriginal().toUpperCase() : "";
                if (campo.contains("TOTAL") || campo.equals("AMOUNT") || nombreCol.contains("TOTAL PRICE") || nombreCol.contains("TOTAL")) {
                    colTotal = columna.getIndiceColumna();
                    letraTotal = columna.getLetraColumna();
                    break;
                }
            }

            if (colTotal >= 0) {
                logger.info("CMA CGM (carga): columna TOTAL detectada en {} (índice {})", letraTotal, colTotal);
            } else {
                logger.warn("CMA CGM (carga): no se detectó columna TOTAL en el formato");
            }

            int lastDataRow = cells.getMaxDataRow();
            logger.info("CMA CGM (carga): escaneando filas {}..{} (1-based)", dataStartRow + 1, lastDataRow + 1);

            // Escanear de arriba a abajo para capturar TODAS las filas especiales
            for (int searchRow = dataStartRow; searchRow <= lastDataRow; searchRow++) {
                for (int colCheck = 0; colCheck <= 20; colCheck++) {
                    com.aspose.cells.Cell celdaCheck = cells.get(searchRow, colCheck);
                    if (celdaCheck == null || celdaCheck.getValue() == null) continue;
                    String valor = "";
                    try { valor = celdaCheck.getStringValue().trim().toUpperCase(); } catch (Exception e) { continue; }

                    boolean esItemSubTotal = valor.equals("ITEM SUB TOTAL") ||
                                             (valor.contains("ITEM") && valor.contains("SUB") && valor.contains("TOTAL")) ||
                                             valor.equals("SUB TOTAL") || valor.equals("SUBTOTAL");
                    boolean esGrandTotal = valor.contains("GRAND") && valor.contains("TOTAL");
                    boolean esTotalPrice = !esGrandTotal && valor.contains("TOTAL") && valor.contains("PRICE");

                    if (esItemSubTotal || esGrandTotal || esTotalPrice) {
                        // Capturar fórmula original de la columna TOTAL
                        String formula = null;
                        if (colTotal >= 0) {
                            com.aspose.cells.Cell c = cells.get(searchRow, colTotal);
                            if (c != null && c.isFormula()) {
                                formula = c.getFormula();
                            }
                        }
                        cmaCgmSpecialRows.put(searchRow, formula != null ? formula : "");

                        String tipo = esItemSubTotal ? "Item Sub Total" : (esGrandTotal ? "Grand Total" : "Total Price");
                        logger.info("CMA CGM (carga): detectado '{}' en fila {} (col {}), fórmula='{}'",
                            tipo, searchRow + 1, colCheck + 1, formula);

                        // Para Item Sub Total: también registrar la fila siguiente como Total Price
                        // si aún no ha sido detectada explícitamente
                        if (esItemSubTotal) {
                            int totalPriceRow = searchRow + 1;
                            if (totalPriceRow <= lastDataRow && !cmaCgmSpecialRows.containsKey(totalPriceRow)) {
                                String formulaTP = null;
                                if (colTotal >= 0) {
                                    com.aspose.cells.Cell cTP = cells.get(totalPriceRow, colTotal);
                                    if (cTP != null && cTP.isFormula()) {
                                        formulaTP = cTP.getFormula();
                                    }
                                }
                                // Si no tiene fórmula, construir una referenciando el subtotal
                                if (formulaTP == null || formulaTP.isEmpty()) {
                                    formulaTP = "=SUM(" + letraTotal + (searchRow + 1) + ")";
                                }
                                cmaCgmSpecialRows.put(totalPriceRow, formulaTP);
                                logger.info("CMA CGM (carga): Total Price implícito en fila {}, fórmula='{}'",
                                    totalPriceRow + 1, formulaTP);
                            }
                        }
                        break; // Ya encontramos etiqueta en esta fila
                    }
                }
            }

            logger.info("CMA CGM (carga): {} filas especiales detectadas: {}",
                cmaCgmSpecialRows.size(), cmaCgmSpecialRows.keySet());
            for (java.util.Map.Entry<Integer, String> entry : cmaCgmSpecialRows.entrySet()) {
                logger.info("  fila {} (1-based): fórmula='{}'", entry.getKey() + 1, entry.getValue());
            }
        } catch (Exception e) {
            logger.warn("CMA CGM: error al detectar totales en carga", e);
        }
    }

    /**
     * Reaplica fórmulas de CMA CGM usando lo detectado en carga.
     * Itera sobre TODAS las filas especiales del mapa cmaCgmSpecialRows.
     */
    private void aplicarFormulasCMACGMDespuesDeEscritura(com.aspose.cells.Cells cells, int dataStartRow, int currentRow, int colTotal, String letraTotal) {
        try {
            logger.info("✨ CMA CGM aplicarFormulas ENTRADA: broker={}, specialRows={}, colTotal={}, letraTotal='{}', dataStartRow={}",
                formatoActual != null ? formatoActual.getBrokerName() : "null",
                cmaCgmSpecialRows.size(), colTotal, letraTotal, dataStartRow);

            if (cmaCgmSpecialRows.isEmpty()) {
                logger.warn("✨ CMA CGM aplicarFormulas: SALIDA TEMPRANA - no hay filas especiales detectadas");
                return;
            }
            if (colTotal < 0) {
                logger.warn("✨ CMA CGM aplicarFormulas: SALIDA TEMPRANA - colTotal={}", colTotal);
                return;
            }

            // Rellenar con 0.0 celdas vacías en colTotal dentro de rangos SUM
            // para evitar #VALUE! cuando la fórmula referencia celdas con string vacío
            for (java.util.Map.Entry<Integer, String> entry : cmaCgmSpecialRows.entrySet()) {
                String f = entry.getValue();
                if (f == null || f.isEmpty()) continue;
                String fUpper = f.toUpperCase();
                if (fUpper.contains("SUM")) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("([A-Z]+)(\\d+):([A-Z]+)(\\d+)").matcher(fUpper);
                    if (m.find()) {
                        int rangeStart = Integer.parseInt(m.group(2)) - 1; // a 0-based
                        int rangeEnd = Integer.parseInt(m.group(4)) - 1;
                        for (int r = rangeStart; r <= rangeEnd; r++) {
                            if (cmaCgmSpecialRows.containsKey(r)) continue; // no tocar filas especiales
                            com.aspose.cells.Cell c = cells.get(r, colTotal);
                            if (c != null) {
                                boolean vacia = false;
                                try {
                                    Object val = c.getValue();
                                    if (val == null) vacia = true;
                                    else if (val instanceof String && ((String) val).trim().isEmpty()) vacia = true;
                                } catch (Exception ex) { vacia = true; }
                                if (vacia) {
                                    c.putValue(0.0);
                                    logger.debug("✨ CMA CGM: celda vacía {}{} rellenada con 0.0", letraTotal, r + 1);
                                }
                            }
                        }
                    }
                }
            }

            // Iterar sobre TODAS las filas especiales y aplicar sus fórmulas originales
            for (java.util.Map.Entry<Integer, String> entry : cmaCgmSpecialRows.entrySet()) {
                int specialRow = entry.getKey();
                String formula = entry.getValue();

                com.aspose.cells.Cell celda = cells.get(specialRow, colTotal);
                if (celda == null) {
                    logger.warn("✨ CMA CGM export: celda NULL en fila {}, col {}", specialRow + 1, colTotal);
                    continue;
                }

                if (formula != null && !formula.isEmpty()) {
                    celda.setFormula(formula);
                    com.aspose.cells.Style st = celda.getStyle();
                    st.setNumber(2);
                    celda.setStyle(st);
                    logger.info("✨ CMA CGM export: fila {} fórmula='{}' [isFormula={}]",
                        specialRow + 1, formula, celda.isFormula());
                } else {
                    logger.warn("✨ CMA CGM export: fila {} sin fórmula capturada, se omite", specialRow + 1);
                }
            }
        } catch (Exception e) {
            logger.error("✨ CMA CGM: EXCEPCIÓN al aplicar fórmulas después de escritura", e);
        }
    }

    /**
     * Crea un estilo de celda para una columna específica del formato
     */
    private CellStyle crearEstiloColumna(XSSFWorkbook workbook, FormatoColumna columna) {
        XSSFCellStyle estilo = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        
        // Aplicar formato de texto
        if (columna.getEsNegrita() != null && columna.getEsNegrita()) {
            font.setBold(true);
        }
        if (columna.getEsCursiva() != null && columna.getEsCursiva()) {
            font.setItalic(true);
        }
        
        // Aplicar color de texto (soporta hex, nombres, índices)
        if (columna.getColorTexto() != null && !columna.getColorTexto().isEmpty()) {
            aplicarColorTexto(font, columna.getColorTexto());
        }
        
        estilo.setFont(font);
        
        // Aplicar color de fondo (soporta hex, nombres, índices)
        if (columna.getColorFondo() != null && !columna.getColorFondo().isEmpty()) {
            aplicarColorFondo(estilo, columna.getColorFondo());
        }
        
        // Aplicar bordes
        if (columna.getTieneBorde() != null && columna.getTieneBorde()) {
            estilo.setBorderBottom(BorderStyle.THIN);
            estilo.setBorderTop(BorderStyle.THIN);
            estilo.setBorderLeft(BorderStyle.THIN);
            estilo.setBorderRight(BorderStyle.THIN);
        }
        
        return estilo;
    }
    
    /**
     * Aplica color de texto a una fuente (soporta hex, nombres, índices)
     */
    private void aplicarColorTexto(XSSFFont font, String color) {
        try {
            if (color.startsWith("#")) {
                // Color RGB hexadecimal
                XSSFColor xssfColor = convertirHexAXSSFColor(color);
                font.setColor(xssfColor);
            } else {
                // Color por nombre o índice
                short colorIndex = convertirColorAIndex(color);
                font.setColor(colorIndex);
            }
        } catch (Exception e) {
            logger.warn("⚠️ No se pudo aplicar color de texto: {}", color);
        }
    }
    
    /**
     * Aplica color de fondo a un estilo (soporta hex, nombres, índices)
     */
    private void aplicarColorFondo(XSSFCellStyle estilo, String color) {
        try {
            if (color.startsWith("#")) {
                // Color RGB hexadecimal
                XSSFColor xssfColor = convertirHexAXSSFColor(color);
                estilo.setFillForegroundColor(xssfColor);
                estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            } else {
                // Color por nombre o índice
                short colorIndex = convertirColorAIndex(color);
                estilo.setFillForegroundColor(colorIndex);
                estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
        } catch (Exception e) {
            logger.warn("⚠️ No se pudo aplicar color de fondo: {}", color);
        }
    }
    
    /**
     * Convierte un código hexadecimal a XSSFColor (colores RGB personalizados)
     */
    private XSSFColor convertirHexAXSSFColor(String hex) {
        // Remover el # si está presente
        String hexClean = hex.startsWith("#") ? hex.substring(1) : hex;
        
        // Convertir a RGB
        int rgb = Integer.parseInt(hexClean, 16);
        byte r = (byte) ((rgb >> 16) & 0xFF);
        byte g = (byte) ((rgb >> 8) & 0xFF);
        byte b = (byte) (rgb & 0xFF);
        
        return new XSSFColor(new byte[]{r, g, b}, null);
    }
    
    /**
     * Convierte un color en formato texto a IndexedColors
     * Soporta: nombres en español/inglés, códigos hex (#FFFFFF), IndexedColors
     */
    private short convertirColorAIndex(String colorNombre) {
        if (colorNombre == null || colorNombre.isEmpty()) {
            return IndexedColors.AUTOMATIC.getIndex();
        }
        
        // Normalizar el nombre
        String color = colorNombre.toUpperCase().trim();
        
        // Si es un código hex, convertirlo al color más cercano
        if (color.startsWith("#")) {
            return convertirHexAColorIndex(color);
        }
        
        // Si es un número, retornarlo directamente
        if (color.matches("\\d+")) {
            try {
                return Short.parseShort(color);
            } catch (NumberFormatException e) {
                logger.warn("⚠️ Número de color inválido: {}", colorNombre);
            }
        }
        
        try {
            // Intentar encontrar el color por nombre de IndexedColors
            IndexedColors indexedColor = IndexedColors.valueOf(color.replace(" ", "_"));
            return indexedColor.getIndex();
        } catch (IllegalArgumentException e) {
            // Mapeo manual extendido para colores comunes
            switch (color) {
                // Azules
                case "AZUL":
                case "BLUE":
                    return IndexedColors.BLUE.getIndex();
                case "AZUL_CLARO":
                case "LIGHT_BLUE":
                case "CELESTE":
                    return IndexedColors.LIGHT_BLUE.getIndex();
                case "AZUL_OSCURO":
                case "DARK_BLUE":
                    return IndexedColors.DARK_BLUE.getIndex();
                case "AZUL_CIELO":
                case "SKY_BLUE":
                    return IndexedColors.SKY_BLUE.getIndex();
                
                // Rojos
                case "ROJO":
                case "RED":
                    return IndexedColors.RED.getIndex();
                case "ROJO_OSCURO":
                case "DARK_RED":
                    return IndexedColors.DARK_RED.getIndex();
                case "ROSA":
                case "PINK":
                case "ROSE":
                    return IndexedColors.ROSE.getIndex();
                
                // Verdes
                case "VERDE":
                case "GREEN":
                    return IndexedColors.GREEN.getIndex();
                case "VERDE_CLARO":
                case "LIGHT_GREEN":
                    return IndexedColors.LIGHT_GREEN.getIndex();
                case "VERDE_OSCURO":
                case "DARK_GREEN":
                    return IndexedColors.DARK_GREEN.getIndex();
                
                // Amarillos/Naranjas
                case "AMARILLO":
                case "YELLOW":
                    return IndexedColors.YELLOW.getIndex();
                case "NARANJA":
                case "ORANGE":
                    return IndexedColors.LIGHT_ORANGE.getIndex();
                case "ORO":
                case "GOLD":
                    return IndexedColors.GOLD.getIndex();
                
                // Grises
                case "GRIS":
                case "GRAY":
                case "GREY":
                    return IndexedColors.GREY_25_PERCENT.getIndex();
                case "GRIS_25":
                case "GREY_25_PERCENT":
                    return IndexedColors.GREY_25_PERCENT.getIndex();
                case "GRIS_40":
                case "GREY_40_PERCENT":
                    return IndexedColors.GREY_40_PERCENT.getIndex();
                case "GRIS_50":
                case "GREY_50_PERCENT":
                    return IndexedColors.GREY_50_PERCENT.getIndex();
                case "GRIS_80":
                case "GREY_80_PERCENT":
                    return IndexedColors.GREY_80_PERCENT.getIndex();
                
                // Básicos
                case "BLANCO":
                case "WHITE":
                    return IndexedColors.WHITE.getIndex();
                case "NEGRO":
                case "BLACK":
                    return IndexedColors.BLACK.getIndex();
                
                // Otros
                case "VIOLETA":
                case "VIOLET":
                case "PURPLE":
                case "MORADO":
                    return IndexedColors.VIOLET.getIndex();
                case "TURQUESA":
                case "TURQUOISE":
                case "AQUA":
                    return IndexedColors.TURQUOISE.getIndex();
                case "LAVANDA":
                case "LAVENDER":
                    return IndexedColors.LAVENDER.getIndex();
                case "CORAL":
                    return IndexedColors.CORAL.getIndex();
                case "TAN":
                case "BEIGE":
                    return IndexedColors.TAN.getIndex();
                case "MARRON":
                case "BROWN":
                    return IndexedColors.BROWN.getIndex();
                    
                default:
                    logger.warn("⚠️ Color no reconocido: '{}', usando AUTOMATIC", colorNombre);
                    return IndexedColors.AUTOMATIC.getIndex();
            }
        }
    }
    
    /**
     * Convierte un color hexadecimal (#RRGGBB) al IndexedColor más cercano
     */
    private short convertirHexAColorIndex(String hex) {
        try {
            // Remover el # si está presente
            String hexClean = hex.startsWith("#") ? hex.substring(1) : hex;
            
            // Convertir a RGB
            int rgb = Integer.parseInt(hexClean, 16);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            
            logger.debug("Convirtiendo color hex {} -> RGB({}, {}, {})", hex, r, g, b);
            
            // Mapeo aproximado de colores comunes
            // Blancos/Grises/Negros
            if (r > 240 && g > 240 && b > 240) return IndexedColors.WHITE.getIndex();
            if (r < 20 && g < 20 && b < 20) return IndexedColors.BLACK.getIndex();
            if (Math.abs(r - g) < 20 && Math.abs(g - b) < 20) {
                int avg = (r + g + b) / 3;
                if (avg > 200) return IndexedColors.GREY_25_PERCENT.getIndex();
                if (avg > 150) return IndexedColors.GREY_40_PERCENT.getIndex();
                if (avg > 100) return IndexedColors.GREY_50_PERCENT.getIndex();
                return IndexedColors.GREY_80_PERCENT.getIndex();
            }
            
            // Colores dominantes
            if (r > g && r > b) {
                if (r > 200) return IndexedColors.RED.getIndex();
                return IndexedColors.DARK_RED.getIndex();
            } else if (g > r && g > b) {
                if (g > 200) return IndexedColors.GREEN.getIndex();
                return IndexedColors.DARK_GREEN.getIndex();
            } else if (b > r && b > g) {
                if (b > 200) return IndexedColors.BLUE.getIndex();
                return IndexedColors.DARK_BLUE.getIndex();
            } else if (r > 200 && g > 200 && b < 100) {
                return IndexedColors.YELLOW.getIndex();
            }
            
            // Si no coincide con nada, usar automático
            return IndexedColors.AUTOMATIC.getIndex();
            
        } catch (Exception e) {
            logger.warn("⚠️ Error al convertir color hex: {}", hex, e);
            return IndexedColors.AUTOMATIC.getIndex();
        }
    }
    
    /**
     * Cuenta el total de campos de metadata
     */
    private int contarMetadataTotal() {
        if (metadataActual == null || metadataActual.isEmpty()) {
            return 0;
        }
        return metadataActual.values().stream()
            .mapToInt(List::size)
            .sum();
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
// 🔵 FILTRADO AVANZADO DE PRODUCTOS + ORDEN ALFABÉTICO (EN INGLÉS)
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

    // 👉 ORDENAR DESPUÉS DE FILTRAR (POR DESCRIPCIÓN EN INGLÉS)
    tablaProductos.getSortOrder().clear();
    colDescEn.setSortType(TableColumn.SortType.ASCENDING);
    tablaProductos.getSortOrder().add(colDescEn);
    tablaProductos.sort();
}

private void exportarExcelTodasLasFamilias() {
    try {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Maestra de Precios");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx")
        );
        File archivo = fileChooser.showSaveDialog(null);
        if (archivo == null) return;

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Maestra Precios");

        // ============================================================
        // 🎨 1. ESTILOS CORPORATIVOS
        // ============================================================

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        XSSFFont headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);

        CellStyle bordered = workbook.createCellStyle();
        bordered.setBorderBottom(BorderStyle.THIN);
        bordered.setBorderTop(BorderStyle.THIN);
        bordered.setBorderLeft(BorderStyle.THIN);
        bordered.setBorderRight(BorderStyle.THIN);

        DataFormat format = workbook.createDataFormat();

        CellStyle monedaUSD = workbook.createCellStyle();
        monedaUSD.cloneStyleFrom(bordered);
        monedaUSD.setDataFormat(format.getFormat("#,##0.00"));

        CellStyle monedaCLP = workbook.createCellStyle();
        monedaCLP.cloneStyleFrom(bordered);
        monedaCLP.setDataFormat(format.getFormat("#,###"));

        // 👉 Estilo verde claro para “Precio + Utilidad (USD)”
        CellStyle utilidadStyle = workbook.createCellStyle();
        utilidadStyle.cloneStyleFrom(monedaUSD);
        utilidadStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        utilidadStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // ============================================================
        // 🎨 2. COLORES POR FAMILIA
        // ============================================================

        Map<Integer, Short> coloresFamilia = Map.of(
                1, IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex(),
                2, IndexedColors.LIGHT_GREEN.getIndex(),
                3, IndexedColors.LIGHT_YELLOW.getIndex(),
                4, IndexedColors.LIGHT_ORANGE.getIndex(),
                5, IndexedColors.LIGHT_TURQUOISE.getIndex(),
                6, IndexedColors.LIGHT_BLUE.getIndex(),
                7, IndexedColors.LIGHT_GREEN.getIndex(),
                8, IndexedColors.GREY_25_PERCENT.getIndex(),
                9, IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex()
        );

        // ============================================================
        // 🎨 3. NOMBRES DE FAMILIA
        // ============================================================

        Map<Integer, String> nombresFamilia = new HashMap<>();
        nombresFamilia.put(1, "Tostaduría");
        nombresFamilia.put(2, "Lácteos");
        nombresFamilia.put(3, "Carnes");
        nombresFamilia.put(4, "Bebestibles");
        nombresFamilia.put(5, "Congelados");
        nombresFamilia.put(6, "Pescados y mariscos");
        nombresFamilia.put(7, "Frutas y verduras");
        nombresFamilia.put(8, "Indu");
        nombresFamilia.put(9, "Abarrotes");

        // ============================================================
        // 🎨 4. TÍTULO CORPORATIVO
        // ============================================================

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Reporte Corporativo — Maestra de Precios");

        CellStyle titleStyle = workbook.createCellStyle();
        XSSFFont titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);

        titleCell.setCellStyle(titleStyle);

        // 👉 NUEVO ORDEN DE COLUMNAS
        String[] columnas = {
                "Familia",
                "Descripción EN",
                "Descripción ES",
                "Unidad",
                "Valor Pesos",
                "Precio USD",
                "Precio + Utilidad (USD)"
        };

        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columnas.length - 1));

        // ============================================================
        // 🎨 5. ENCABEZADOS
        // ============================================================

        Row header = sheet.createRow(1);

        for (int i = 0; i < columnas.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columnas[i]);
            cell.setCellStyle(headerStyle);
        }

        sheet.createFreezePane(0, 2);

        // ============================================================
        // 🎨 6. OBTENER PRODUCTOS + ORDENAR SIN ROMPER NADA
        // ============================================================

        List<Producto> lista = productoService.listarProductos();

        // 👉 Ordenar sin Comparator.comparing (que te dio problemas)
        lista.sort((a, b) -> {
            int cmp = Integer.compare(a.getFamiliaId(), b.getFamiliaId());
            if (cmp != 0) return cmp;
            return a.getDescripcionEn().compareToIgnoreCase(b.getDescripcionEn());
        });

        int fila = 2;

        // ============================================================
        // 🎨 7. GENERAR FILAS
        // ============================================================

        for (Producto p : lista) {

            double usd = (tipoCambioActual == 0) ? 0 : p.getValorPesos() / tipoCambioActual;
            double conUtil = usd * utilidadActual;

            Row row = sheet.createRow(fila++);

            Short color = coloresFamilia.getOrDefault(
                    p.getFamiliaId(),
                    IndexedColors.WHITE.getIndex()
            );

            CellStyle rowStyle = workbook.createCellStyle();
            rowStyle.cloneStyleFrom(bordered);
            rowStyle.setFillForegroundColor(color);
            rowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String nombreFamilia = nombresFamilia.getOrDefault(p.getFamiliaId(), "Sin nombre");

            // 👉 Familia
            Cell c0 = row.createCell(0);
            c0.setCellValue(nombreFamilia);
            c0.setCellStyle(rowStyle);

            // 👉 Descripción EN
            Cell c1 = row.createCell(1);
            c1.setCellValue(p.getDescripcionEn());
            c1.setCellStyle(rowStyle);

            // 👉 Descripción ES
            Cell c2 = row.createCell(2);
            c2.setCellValue(p.getDescripcionEs());
            c2.setCellStyle(rowStyle);

            // 👉 Unidad
            Cell c3 = row.createCell(3);
            c3.setCellValue(p.getUnidadMedida());
            c3.setCellStyle(rowStyle);

            // 👉 Valor Pesos
            Cell c4 = row.createCell(4);
            c4.setCellValue(p.getValorPesos());
            c4.setCellStyle(monedaCLP);

            // 👉 Precio USD
            Cell c5 = row.createCell(5);
            c5.setCellValue(usd);
            c5.setCellStyle(monedaUSD);

            // 👉 Precio + Utilidad (USD) — VERDE CLARO
            Cell c6 = row.createCell(6);
            c6.setCellValue(conUtil);
            c6.setCellStyle(utilidadStyle);
        }

        // ============================================================
        // 🎨 8. AUTOAJUSTE (mantengo porque tu versión funciona)
        // ============================================================

        for (int i = 0; i < columnas.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // ============================================================
        // 🎨 9. GUARDAR ARCHIVO
        // ============================================================

        FileOutputStream fos = new FileOutputStream(archivo);
        workbook.write(fos);
        fos.close();
        workbook.close();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Exportación exitosa");
        alert.setContentText("La maestra de precios fue exportada correctamente.");
        alert.showAndWait();

    } catch (Exception e) {
        e.printStackTrace();
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Error al exportar");
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}

















// ============================================================
// 🔵 CARGAR BROKERS DESDE LA BD
// ============================================================
private void cargarBrokers() {
    try (Connection conn = DBConnection.getConnection()) {
        BrokerDAO brokerDAO = new BrokerDAO(conn);
        List<Broker> brokers = brokerDAO.listarBrokersActivos();
        
        listaBrokers = FXCollections.observableArrayList(brokers);
        comboBrokers.setItems(listaBrokers);
        
        logger.info("Se cargaron {} brokers en el ComboBox", brokers.size());
        
    } catch (SQLException e) {
        logger.error("Error al cargar brokers", e);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error al cargar brokers");
        alert.setContentText("No se pudieron cargar los brokers desde la base de datos: " + e.getMessage());
        alert.showAndWait();
    }
}

// ============================================================
// 🔵 CARGAR FORMATO DEL BROKER SELECCIONADO
// ============================================================
private void cargarFormatoBroker(Broker broker) {
    try (Connection conn = DBConnection.getConnection()) {
        FormatoDAO formatoDAO = new FormatoDAO(conn);
        formatoActual = formatoDAO.obtenerFormatoPorBrokerId(broker.getBrokerId());
        
        if (formatoActual != null) {
            logger.info("Formato cargado para broker {}: {} columnas", 
                       broker.getBrokerName(), formatoActual.getColumnas().size());
            
            // Limpiar tabla dinámica y configurar nuevas columnas
            configurarTablaDinamica();
            
            // 📋 Cargar metadata del broker
            cargarMetadataBroker(formatoActual.getFormatoId());
            
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Formato detectado");
            info.setHeaderText("Broker: " + broker.getBrokerName());
            info.setContentText("Formato detectado con " + formatoActual.getColumnas().size() + 
                              " columnas.\nFila de encabezado: " + formatoActual.getHeaderRow());
            info.showAndWait();
        } else {
            logger.warn("No se encontró formato para broker {}", broker.getBrokerName());
            
            // Limpiar panel de metadata
            //actualizarPanelMetadata(null);
            
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Formato no encontrado");
            alert.setHeaderText("Broker: " + broker.getBrokerName());
            alert.setContentText("No se encontró un formato activo para este broker en la base de datos.");
            alert.showAndWait();
        }
        
    } catch (SQLException e) {
        logger.error("Error al cargar formato del broker", e);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error al cargar formato");
        alert.setContentText("Error al obtener formato del broker: " + e.getMessage());
        alert.showAndWait();
    }
}

// ============================================================
// 🔵 CONFIGURAR TABLA DINÁMICA SEGÚN FORMATO DEL BROKER
// ============================================================
private void configurarTablaDinamica() {
    // Limpiar columnas existentes
    tablaDinamica.getColumns().clear();
    tablaDinamica.getItems().clear();
    
    if (formatoActual == null || formatoActual.getColumnas().isEmpty()) {
        logger.warn("No hay formato activo o no tiene columnas");
        return;
    }
    
    // Crear columnas dinámicamente según el formato
    for (FormatoColumna col : formatoActual.getColumnas()) {
        String campoEstandar = col.getCampoEstandar();
        
        // 🚫 Ocultar la columna PRECIO_VSS de la BDD (usamos la calculada)
        if ("PRECIO_VSS".equals(campoEstandar)) {
            continue; // Saltar esta columna
        }
        
        // 🚫 Ocultar la columna TOTAL en la tabla dinámica
        if ("TOTAL".equals(campoEstandar) || "TOTAL_LINE".equals(campoEstandar)) {
            continue;
        }
        
        TableColumn<RowData, String> column = new TableColumn<>(col.getNombreColumnaOriginal());
        
        // 📏 Establecer ancho de columna según el tipo de campo
        if (campoEstandar.contains("DESCRIPTION") || campoEstandar.contains("COMMENTS")) {
            column.setPrefWidth(250); // Columnas de texto largo
        } else if (campoEstandar.contains("PRICE") || campoEstandar.contains("TOTAL")) {
            column.setPrefWidth(100); // Columnas de precio
        } else if (campoEstandar.contains("QUANTITY") || campoEstandar.contains("UNIT")) {
            column.setPrefWidth(80); // Columnas numéricas cortas
        } else {
            column.setPrefWidth(120); // Ancho predeterminado
        }
        
        // Configurar cell value factory
        column.setCellValueFactory(cellData -> cellData.getValue().getProperty(campoEstandar));
        
        // 📐 Configurar cellFactory para centrar texto y aplicar estilos
        column.setCellFactory(tc -> new TableCell<RowData, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        
                        // Aplicar estilos del formato + centrado
                        StringBuilder style = new StringBuilder();
                        
                        // 🎯 Centrar texto en todas las celdas
                        style.append("-fx-alignment: CENTER; ");
                        
                        // Aplicar estilos personalizados
                        if (col.getColorFondo() != null && !col.getColorFondo().isEmpty()) {
                            style.append("-fx-background-color: ").append(col.getColorFondo()).append(";");
                        }
                        // Aplicar color de texto
                        if (col.getColorTexto() != null && !col.getColorTexto().isEmpty()) {
                            style.append("-fx-text-fill: ").append(col.getColorTexto()).append(";");
                        }
                        // Aplicar negrita y cursiva
                        if (col.getEsNegrita() != null && col.getEsNegrita()) {
                            style.append("-fx-font-weight: bold;");
                        }
                        // Aplicar cursiva
                        if (col.getEsCursiva() != null && col.getEsCursiva()) {
                            style.append("-fx-font-style: italic;");
                        }
                        // Aplicar subrayado
                        setStyle(style.toString());
                    }
                }
            });
        
        tablaDinamica.getColumns().add(column);
    }
    
    // �👆 Agregar listener de doble clic para editar producto
    tablaDinamica.setRowFactory(tv -> {
        TableRow<RowData> row = new TableRow<>();
        row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !row.isEmpty()) {
                abrirPopupEdicionProducto(row.getItem());
            }
        });
        return row;
    });
    
    logger.info("Tabla dinámica configurada con {} columnas", tablaDinamica.getColumns().size());
}

// ============================================================
// 🔵 LEER EXCEL CON FORMATO DEL BROKER
// ============================================================
private void leerExcelConFormato(File archivo) {
    if (formatoActual == null) {
        ejecutarEnHiloFX(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Formato no seleccionado");
            alert.setHeaderText("Debe seleccionar un broker primero");
            alert.setContentText("Por favor, seleccione un broker del combo box antes de cargar el archivo.");
            alert.showAndWait();
        });
        return;
    }
    
    // 🔷 SIEMPRE usar Aspose para cargar archivos Excel
    // Aspose maneja mejor los formatos, fórmulas y preserva macros si existen
    boolean usarAspose = true; // Siempre usar Aspose
    
    if (usarAspose) {
        leerExcelConAspose(archivo);
        return;
    }
    
    // NOTA: El código de Apache POI a continuación ya no se usa,
    // pero se mantiene como fallback en caso de problemas con Aspose
    try {
        // Usar Apache POI para leer el Excel (archivos sin macros)
        org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(archivo);
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
        
        ObservableList<RowData> datos = FXCollections.observableArrayList();
        
        // Leer desde la fila siguiente al header
        int startRow = formatoActual.getHeaderRow() + 1;
        int lastRow = sheet.getLastRowNum();
        
        for (int i = startRow; i <= lastRow; i++) {
            org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
            if (row == null) continue;
            
            RowData rowData = new RowData();
            boolean filaVacia = true;
            int camposConDatos = 0;
            
            // Leer cada columna según el formato
            for (FormatoColumna col : formatoActual.getColumnas()) {
                int colIndex = col.getIndiceColumna();
                org.apache.poi.ss.usermodel.Cell cell = row.getCell(colIndex);
                
                String valor = "";
                if (cell != null) {
                    switch (cell.getCellType()) {
                        case STRING:
                            valor = cell.getStringCellValue();
                            break;
                        case NUMERIC:
                            if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                                valor = cell.getDateCellValue().toString();
                            } else {
                                valor = String.valueOf(cell.getNumericCellValue());
                            }
                            break;
                        case BOOLEAN:
                            valor = String.valueOf(cell.getBooleanCellValue());
                            break;
                        case FORMULA:
                            try {
                                valor = String.valueOf(cell.getNumericCellValue());
                            } catch (Exception e) {
                                valor = cell.getStringCellValue();
                            }
                            break;
                        default:
                            valor = "";
                    }
                }
                
                // Verificar si el valor tiene contenido real
                if (valor != null && !valor.trim().isEmpty() && !valor.equals("0.0")) {
                    filaVacia = false;
                    camposConDatos++;
                }
                
                rowData.set(col.getCampoEstandar(), valor);
            }
            
            // 💰 Inicializar columna PRECIO_VSS con 0.0 si existe en el formato
            for (FormatoColumna col : formatoActual.getColumnas()) {
                if ("PRECIO_VSS".equals(col.getCampoEstandar())) {
                    // Si la columna no tiene valor o está vacía, inicializar con 0.0
                    String valorActual = rowData.get("PRECIO_VSS");
                    if (valorActual == null || valorActual.trim().isEmpty()) {
                        rowData.set("PRECIO_VSS", "0.0");
                    }
                    break;
                }
            }
            
            // 🔍 Buscar automáticamente el producto en la BD y calcular precio VSS
            // Prioridad: ITEM_NAME (campo estándar MCTC) > ITEM > ITEM_DESCRIPTION > DESCRIPTION
            String descripcion = obtenerValorDeCampo(rowData, "ITEM_NAME", "ITEM", 
                "ITEM_DESCRIPTION", "DESCRIPTION", "PRODUCT_NAME", "DESCRIPCION", "NOMBRE", "PRODUCTO");
            
            if (descripcion != null && !descripcion.trim().isEmpty()) {
                // Buscar producto en la base de datos
                List<cl.vss.cotizador.model.ProductoSimilar> productos = 
                    cotizacionService.buscarProductosSimilares(descripcion);
                
                if (!productos.isEmpty()) {
                    // Tomar el primer producto encontrado
                    cl.vss.cotizador.model.ProductoSimilar producto = productos.get(0);
                    
                    // Obtener cantidad
                    String cantidadStr = obtenerValorDeCampo(rowData, "QUANTITY", "QTY", "CANTIDAD");
                    double cantidad = 1.0;
                    try {
                        if (cantidadStr != null && !cantidadStr.trim().isEmpty()) {
                            cantidad = Double.parseDouble(cantidadStr.trim());
                            if (cantidad <= 0) cantidad = 1.0;
                        }
                    } catch (NumberFormatException ex) {
                        cantidad = 1.0;
                    }
                    
                    // Calcular precio VSS (precio_venta_neto × cantidad)
                    double precioVentaNeto = producto.getPrecioVentaNeto();
                    double precioVentaNetoDolares = producto.getPrecioVentaNetoDolares();
                    double precioVSS = precioVentaNeto * cantidad;
                    
                    // Guardar precio total en rowData
                    rowData.set("precio_vss_calculado", String.valueOf(precioVSS));
                    
                    // 💰 Actualizar UNIT_PRICE con precio_venta_neto_dolares desde la BD
                    // Siempre intentamos establecer el valor, sin verificar hasKey
                    rowData.set("UNIT_PRICE", String.format("%.2f", precioVentaNetoDolares));
                    logger.info("💰 UNIT_PRICE actualizado con precio_venta_neto: ${} para producto: {}", 
                        String.format("%.2f", precioVentaNetoDolares), descripcion.substring(0, Math.min(30, descripcion.length())));
                    
                    logger.debug("Producto encontrado automáticamente: {} - Precio VSS: ${}", 
                        descripcion, String.format("%,.2f", precioVSS));
                } else {
                    // No se encontró producto, dejar en 0
                    rowData.set("precio_vss_calculado", "0.0");
                }
            } else {
                // Sin descripción, dejar en 0
                rowData.set("precio_vss_calculado", "0.0");
            }
            
            // 🚫 Filtrar filas que son títulos/encabezados adicionales
            boolean esFilaTitulo = false;
            
            // Verificar en múltiples campos posibles
            String descripcionFila = obtenerValorDeCampo(rowData, "ITEM_NAME", "ITEM", 
                "ITEM_DESCRIPTION", "DESCRIPTION", "PRODUCT_NAME", "DESCRIPCION", "NOMBRE", "PRODUCTO");
            String productCode = obtenerValorDeCampo(rowData, "PRODUCT_CODE", "ITEM_CODE", "CODE", "CODIGO");
            
            // Revisar todos los campos relevantes
            String[] camposARevisar = {descripcionFila, productCode};
            
            for (String campo : camposARevisar) {
                if (campo != null && !campo.trim().isEmpty()) {
                    String valorUpper = campo.trim().toUpperCase();
                    // Filtrar títulos comunes que aparecen como filas
                    if (valorUpper.equals("PROVISIONS") || valorUpper.equals("PROVISION") ||
                        valorUpper.equals("ITEMS") || valorUpper.equals("PRODUCTS") ||
                        valorUpper.equals("DESCRIPCION") || valorUpper.equals("DESCRIPTION") ||
                        valorUpper.equals("ITEM DESCRIPTION") || valorUpper.equals("PRODUCT LIST") ||
                        valorUpper.equals("PRODUCT CODE") || valorUpper.equals("ITEM CODE") ||
                        valorUpper.startsWith("----") || valorUpper.startsWith("====")) {
                        esFilaTitulo = true;
                        logger.debug("Fila filtrada (título/encabezado): {} = '{}'", 
                            campo.equals(descripcionFila) ? "DESCRIPTION" : "PRODUCT_CODE", campo);
                        break;
                    }
                }
            }
            
            // Agregar la fila solo si no está vacía y no es un título
            if (!filaVacia && !esFilaTitulo) {
                datos.add(rowData);
            }
        }
        
        workbook.close();
        
        int totalFilasLeidas = lastRow - startRow + 1;
        int filasVacias = totalFilasLeidas - datos.size();
        
        // Contar productos con precio calculado
        int productosConPrecio = 0;
        for (RowData row : datos) {
            String precio = row.get("precio_vss_calculado");
            if (precio != null) {
                try {
                    double precioVal = Double.parseDouble(precio);
                    if (precioVal > 0) {
                        productosConPrecio++;
                    }
                } catch (NumberFormatException e) {
                    // Ignorar
                }
            }
        }
        
        logger.info("Se cargaron {} filas con datos desde el Excel", datos.size());
        logger.info("Se filtraron {} filas vacías", filasVacias);
        logger.info("Se calcularon precios automáticamente para {} productos", productosConPrecio);
        final int totalFilasCargadas = datos.size();
        final int filasVaciasFinal = filasVacias;
        final int productosConPrecioFinal = productosConPrecio;
        final String brokerNombre = formatoActual != null ? formatoActual.getBrokerName() : "Broker";

        ejecutarEnHiloFX(() -> {
            tablaDinamica.setItems(datos);

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Archivo cargado");
            info.setHeaderText("Excel procesado exitosamente");
            String mensajeFilasVacias = filasVaciasFinal > 0 ? "\n🗑️ Filas vacías filtradas: " + filasVaciasFinal : "";
            info.setContentText("Se cargaron " + totalFilasCargadas + " filas con formato de " + 
                              brokerNombre + 
                              mensajeFilasVacias +
                              "\n\n✅ Precios VSS calculados automáticamente: " + productosConPrecioFinal + 
                              " de " + totalFilasCargadas + " productos");
            info.showAndWait();
        });
        
    } catch (Exception e) {
        logger.error("Error al leer Excel con formato", e);
        final String mensajeError = e.getMessage();
        ejecutarEnHiloFX(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error al procesar Excel");
            alert.setContentText("Error al leer el archivo Excel: " + mensajeError);
            alert.showAndWait();
        });
    }
}


// ============================================================
// 🔷 LEER EXCEL CON ASPOSE (PRESERVA MACROS VBA)
// ============================================================
/**
 * Lee archivos Excel con macros (.xlsm) usando Aspose.Cells.
 * Preserva el workbook en memoria para exportación posterior con macros intactas.
 */
private void leerExcelConAspose(File archivo) {
    try {
        logger.info("🔷 Cargando archivo con Aspose (macros preservadas): {}", archivo.getName());
        
        // Cargar workbook con Aspose (preserva macros)
        asposeService.cargarWorkbook(archivo.getAbsolutePath());
        rutaArchivoConMacros = archivo.getAbsolutePath();
        
        com.aspose.cells.Workbook workbook = asposeService.getWorkbookActual();
        com.aspose.cells.Worksheet sheet = workbook.getWorksheets().get(0);
        com.aspose.cells.Cells cells = sheet.getCells();
        
        ObservableList<RowData> datos = FXCollections.observableArrayList();
        
        // Leer desde la fila siguiente al header
        int startRow = formatoActual.getHeaderRow() + 1; // Primera fila de datos (después del header)
        int lastRow = cells.getMaxDataRow();
        
        logger.info("📖 Leyendo datos desde fila {} hasta {}", startRow + 1, lastRow + 1);

        // CMA CGM: Detectar 'Item Sub Total' y 'Grand Total' durante la carga
        detectarTotalesCMACGMEnCarga(cells);
        
        // 📝 PROCURESHIP: Detectar columna "Supplier Notes" que no está en el formato BD
        int supplierNotesColIndex = -1;
        boolean esProcureshipLoad = formatoActual.getBrokerName() != null && 
            formatoActual.getBrokerName().toUpperCase().contains("PROCURE");
        if (esProcureshipLoad) {
            int maxCol = cells.getMaxDataColumn();
            for (int col = 0; col <= maxCol; col++) {
                com.aspose.cells.Cell headerCell = cells.get(formatoActual.getHeaderRow(), col);
                if (headerCell != null && headerCell.getStringValue() != null) {
                    String headerValue = headerCell.getStringValue().trim();
                    if (headerValue.toUpperCase().contains("SUPPLIER") && 
                        headerValue.toUpperCase().contains("NOTE")) {
                        supplierNotesColIndex = col;
                        logger.info("📝 PROCURESHIP: Columna 'Supplier Notes' detectada en columna {} ({})", 
                            col, headerValue);
                        break;
                    }
                }
            }
            if (supplierNotesColIndex < 0) {
                logger.warn("⚠️ PROCURESHIP: No se encontró columna 'Supplier Notes' en el header");
            }
        }
        
        for (int i = startRow; i <= lastRow; i++) {
            // Omitir filas especiales CMA CGM para no tratarlas como productos
            if (!cmaCgmSpecialRows.isEmpty() && cmaCgmSpecialRows.containsKey(i)) {
                continue;
            }
            RowData rowData = new RowData();
            boolean filaVacia = true;
            int camposConDatos = 0;
            
            // Leer cada columna según el formato
            for (FormatoColumna col : formatoActual.getColumnas()) {
                int colIndex = col.getIndiceColumna();
                com.aspose.cells.Cell cell = cells.get(i, colIndex);
                
                String valor = "";
                if (cell != null) {
                    int cellType = cell.getType();
                    if (cellType == com.aspose.cells.CellValueType.IS_STRING) {
                        valor = cell.getStringValue();
                    } else if (cellType == com.aspose.cells.CellValueType.IS_NUMERIC) {
                        // Simplemente obtener el valor numérico como string
                        valor = String.valueOf(cell.getDoubleValue());
                    } else if (cellType == com.aspose.cells.CellValueType.IS_BOOL) {
                        valor = String.valueOf(cell.getBoolValue());
                    } else {
                        try {
                            valor = cell.getStringValue();
                        } catch (Exception e) {
                            valor = "";
                        }
                    }
                }
                
                // Verificar si el valor tiene contenido real
                if (valor != null && !valor.trim().isEmpty() && !valor.equals("0.0")) {
                    filaVacia = false;
                    camposConDatos++;
                }
                
                rowData.set(col.getCampoEstandar(), valor);
            }
            
            // 📝 PROCURESHIP: Leer columna "Supplier Notes" adicional (no está en formato BD)
            if (supplierNotesColIndex >= 0) {
                com.aspose.cells.Cell supplierNotesCell = cells.get(i, supplierNotesColIndex);
                String supplierNotesValue = "";
                if (supplierNotesCell != null) {
                    try {
                        supplierNotesValue = supplierNotesCell.getStringValue();
                    } catch (Exception e) {
                        supplierNotesValue = "";
                    }
                }
                if (supplierNotesValue != null && !supplierNotesValue.trim().isEmpty()) {
                    rowData.set("SUPPLIER_NOTES", supplierNotesValue);
                }
            }
            
            // 💰 Inicializar columna PRECIO_VSS con 0.0 si existe en el formato
            for (FormatoColumna col : formatoActual.getColumnas()) {
                if ("PRECIO_VSS".equals(col.getCampoEstandar())) {
                    String valorActual = rowData.get("PRECIO_VSS");
                    if (valorActual == null || valorActual.trim().isEmpty()) {
                        rowData.set("PRECIO_VSS", "0.0");
                    }
                    break;
                }
            }
            
            // 🔍 Buscar automáticamente el producto en la BD y calcular precio VSS
            // Prioridad: ITEM_NAME (campo estándar MCTC) > ITEM > ITEM_DESCRIPTION > DESCRIPTION
            String descripcion = obtenerValorDeCampo(rowData, "ITEM_NAME", "ITEM", 
                "ITEM_DESCRIPTION", "DESCRIPTION", "PRODUCT_NAME", "DESCRIPCION", "NOMBRE", "PRODUCTO");
            
            if (descripcion != null && !descripcion.trim().isEmpty()) {
                List<cl.vss.cotizador.model.ProductoSimilar> productos =     
                    cotizacionService.buscarMatchLoMasExactoPosible(descripcion);
                
                if (!productos.isEmpty()) {
                    cl.vss.cotizador.model.ProductoSimilar producto = productos.get(0);
                    
                    String cantidadStr = obtenerValorDeCampo(rowData, "QUANTITY", "QTY", "CANTIDAD");
                    double cantidad = 1.0;
                    try {
                        if (cantidadStr != null && !cantidadStr.trim().isEmpty()) {
                            cantidad = Double.parseDouble(cantidadStr.trim());
                            if (cantidad <= 0) cantidad = 1.0;
                        }
                    } catch (NumberFormatException ex) {
                        cantidad = 1.0;
                    }
                    
                    
                    double precioVentaNeto = producto.getPrecioVentaNeto();
                    double precioVentaNetoDolares = producto.getPrecioVentaNetoDolares();                    
                    double precioVSS = precioVentaNeto * cantidad;
                    
                    rowData.set("precio_vss_calculado", String.valueOf(precioVSS));
                    rowData.set("UNIT_PRICE", String.format("%.2f", precioVentaNetoDolares));
                    
                    logger.debug("Producto encontrado: {} - Precio VSS: ${}", 
                        descripcion.substring(0, Math.min(30, descripcion.length())), 
                        String.format("%,.2f", precioVSS));

                } else {
                    //rowData.set("precio_vss_calculado", "0.0");
                    rowData.set("UNIT_PRICE", String.format("%.2f", 0.00));
                }
            } else {
                //rowData.set("precio_vss_calculado", "0.0");
                rowData.set("UNIT_PRICE", String.format("%.2f", 0.00));
            }
            
            // 🚠 Filtrar filas que son títulos/encabezados adicionales
            boolean esFilaTitulo = false;
            String descripcionFila = obtenerValorDeCampo(rowData, "ITEM_NAME", "ITEM", 
                "ITEM_DESCRIPTION", "DESCRIPTION", "PRODUCT_NAME", "DESCRIPCION", "NOMBRE", "PRODUCTO");
            String productCode = obtenerValorDeCampo(rowData, "PRODUCT_CODE", "ITEM_CODE", "CODE", "CODIGO");
            
            String[] camposARevisar = {descripcionFila, productCode};
            
            for (String campo : camposARevisar) {
                if (campo != null && !campo.trim().isEmpty()) {
                    String valorUpper = campo.trim().toUpperCase();
                    if (valorUpper.equals("PROVISIONS") || valorUpper.equals("PROVISION") ||
                        valorUpper.equals("ITEMS") || valorUpper.equals("PRODUCTS") ||
                        valorUpper.equals("DESCRIPCION") || valorUpper.equals("DESCRIPTION") ||
                        valorUpper.equals("ITEM DESCRIPTION") || valorUpper.equals("PRODUCT LIST") ||
                        valorUpper.equals("PRODUCT CODE") || valorUpper.equals("ITEM CODE") ||
                        valorUpper.startsWith("----") || valorUpper.startsWith("====")) {
                        esFilaTitulo = true;
                        break;
                    }
                }
            }
            
            if (!filaVacia && !esFilaTitulo) {
                datos.add(rowData);
            }
        }
        
        // NO cerrar el workbook - lo mantenemos en memoria para exportar
        
        int totalFilasLeidas = lastRow - startRow + 1;
        int filasVacias = totalFilasLeidas - datos.size();
        
        int productosConPrecio = 0;
        for (RowData row : datos) {
            String precio = row.get("precio_vss_calculado");
            if (precio != null) {
                try {
                    double precioVal = Double.parseDouble(precio);
                    if (precioVal > 0) {
                        productosConPrecio++;
                    }
                } catch (NumberFormatException e) {
                    // Ignorar
                }
            }
        }
        
        logger.info("✅ Se cargaron {} filas con Aspose (macros preservadas)", datos.size());
        logger.info("📎 Macros VBA: {}", workbook.getVbaProject() != null ? "✅ Detectadas" : "❌ No hay");
        final int totalFilasCargadas = datos.size();
        final int filasVaciasFinal = filasVacias;
        final int productosConPrecioFinal = productosConPrecio;
        final String brokerNombre = formatoActual != null ? formatoActual.getBrokerName() : "Broker";
        final String infoMacros = workbook.getVbaProject() != null
            ? "\n\n📌 Macros VBA: " + workbook.getVbaProject().getModules().getCount() + " módulos preservados"
            : "";

        ejecutarEnHiloFX(() -> {
            tablaDinamica.setItems(datos);

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Archivo cargado");
            info.setHeaderText("✅ Excel con macros procesado exitosamente");

            String mensajeFilasVacias = filasVaciasFinal > 0 ? "\n🗑️ Filas vacías filtradas: " + filasVaciasFinal : "";
            info.setContentText("Se cargaron " + totalFilasCargadas + " filas con formato de " + 
                              brokerNombre + 
                              mensajeFilasVacias +
                              "\n\n✅ Precios VSS calculados: " + productosConPrecioFinal + 
                              " de " + totalFilasCargadas + " productos" +
                              infoMacros +
                              "\n\n💡 Al exportar, las macros se preservarán.");
            info.showAndWait();
        });
        
    } catch (Exception e) {
        logger.error("❌ Error al leer Excel con Aspose", e);
        rutaArchivoConMacros = null;
        final String mensajeError = e.getMessage();

        ejecutarEnHiloFX(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error al procesar Excel con Aspose");
            alert.setContentText("Error: " + mensajeError + "\n\nIntentando con Apache POI...");
            alert.showAndWait();
        });
        
        // Fallback a POI si Aspose falla
        // (no debería ocurrir, pero por seguridad)
    }
}

// ============================================================
// 🔵 MÉTODO AUXILIAR: OBTENER VALOR DE CAMPO FLEXIBLE
// ============================================================
/**
 * Busca el valor de un campo intentando múltiples nombres posibles
 * @param rowData Datos de la fila
 * @param camposPosibles Nombres de campos a buscar en orden de prioridad
 * @return El valor del primer campo encontrado, o cadena vacía si ninguno existe
 */
private String obtenerValorDeCampo(RowData rowData, String... camposPosibles) {
    for (String campo : camposPosibles) {
        if (rowData.hasKey(campo)) {
            String valor = rowData.get(campo);
            if (valor != null && !valor.trim().isEmpty()) {
                return valor;
            }
        }
    }
    return "";
}

// ============================================================
// 🔵 ABRIR POPUP DE EDICIÓN DE PRODUCTO
// ============================================================
private void abrirPopupEdicionProducto(RowData rowData) {
    // si no se selecciona ninguna fila, no muestra popup.
    if (rowData == null) return;
    
    // Crear diálogo
    Dialog<Void> dialog = new Dialog<>();
    dialog.setTitle("Editar Producto");
    
    // 🔍 Extraer datos de la fila - intentar múltiples campos posibles
    // Prioridad: ITEM_NAME (campo estándar MCTC) > ITEM > ITEM_DESCRIPTION > DESCRIPTION
    String descripcion = obtenerValorDeCampo(rowData, "ITEM_NAME", "ITEM", 
        "ITEM_DESCRIPTION", "DESCRIPTION", "PRODUCT_NAME", "DESCRIPCION", "NOMBRE", "PRODUCTO");
    
    String cantidad = obtenerValorDeCampo(rowData, 
        "QUANTITY", "QTY", "CANTIDAD", "CANT", "QUANTITY_ORDER");
    
    String precioActual = rowData.get("precio_vss_calculado");
    
    String unidad = obtenerValorDeCampo(rowData, 
        "UOM", "UNIT", "UNIDAD", "UNIT_OF_MEASURE");
    
    // Extraer Vendor Remarks si existe
    // IMPORTANTE: Los campos SUPPLIER tienen prioridad sobre COMMENTS genérico
    String vendorRemarks = obtenerValorDeCampo(rowData,
        "VENDOR_REMARKS", "VENDOR_REMARK", "VENDOR_NOTES", "VENDOR_NOTE", 
        "VENDOR_COMMENTS", "VENDOR_COMMENT",
        "SUPPLIER_COMMENTS", "SUPPLIER_COMMENT", "SUPPLIER COMMENTS",
        "Supplier Commnets", "Supplier Comments", "SUPPLIER COMMNETS",
        "SUPPLIER_NOTES", "SUPPLIER NOTES", "Supplier Notes",
        "REMARKS", "COMMENTS");
    
    // Extraer Notes para Garret
    String notesGarret = obtenerValorDeCampo(rowData, "NOTES", "Notes");
    
    // 🚨 DEBUG: Mostrar todos los campos disponibles en la fila
    logger.debug("🚨 DEBUG - Campos disponibles en RowData: {}", rowData.getKeys());
    logger.debug("🚨 DEBUG - Descripción extraída: '{}'", descripcion);
    logger.debug("🚨 DEBUG - Cantidad extraída: '{}'", cantidad);
    logger.debug("🚨 DEBUG - Unidad extraída: '{}'", unidad);
    logger.debug("🚨 DEBUG - Vendor Remarks extraído: '{}'", vendorRemarks);
    logger.debug("🚨 DEBUG - Notes (Garret) extraído: '{}'", notesGarret);
    
    dialog.setHeaderText("Producto: " + (descripcion != null && !descripcion.isEmpty() ? descripcion : "[Sin descripción]"));
    
    // ============================
    // PANEL DE INFORMACIÓN ACTUAL
    // ============================
    VBox infoPanel = new VBox(10);
    infoPanel.setStyle("-fx-padding: 10; -fx-background-color: #f0f0f0; -fx-border-color: #0A3D91; -fx-border-width: 2;");
    
    Label lblTitulo = new Label("📦 Información Actual");
    lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0A3D91;");
    
    Label lblDesc = new Label("Descripción: " + (descripcion != null && !descripcion.isEmpty() ? descripcion : "[No disponible]"));
    Label lblCant = new Label("Cantidad: " + (cantidad != null && !cantidad.isEmpty() ? cantidad : "0") + " " + (unidad != null && !unidad.isEmpty() ? unidad : ""));
    Label lblPrecio = new Label("Precio VSS Actual: $" + (precioActual != null && !precioActual.isEmpty() ? precioActual : "0.00"));
    lblPrecio.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
    
    infoPanel.getChildren().addAll(lblTitulo, lblDesc, lblCant, lblPrecio);
    
    // ============================
    // PANEL VENDOR REMARKS (Para BSM, CMA CGM, MCTC, OCEANIC, PROCURESHIP)
    // ============================
    VBox panelVendorRemarks = null;
    TextField txtVendorRemarks = null;
    
    // Verificar si es BSM CATERING, CMA CGM, MCTC MARINE, OCEANIC CATERING o PROCURESHIP
    boolean permitirVendorRemarks = formatoActual != null && formatoActual.getBrokerName() != null && 
                            (formatoActual.getBrokerName().toUpperCase().contains("BSM") ||
                             formatoActual.getBrokerName().toUpperCase().contains("CMA") ||
                             formatoActual.getBrokerName().toUpperCase().contains("MCTC") ||
                             formatoActual.getBrokerName().toUpperCase().contains("OCEANIC") ||
                             formatoActual.getBrokerName().toUpperCase().contains("PROCURE"));
    
    if (permitirVendorRemarks) {
        panelVendorRemarks = new VBox(8);
        panelVendorRemarks.setStyle("-fx-padding: 10; -fx-background-color: #FFF9E6; -fx-border-color: #FFA500; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        Label lblRemarksTitle = new Label("📝 Vendor Remarks");
        lblRemarksTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #CC6600;");
        
        txtVendorRemarks = new TextField();
        txtVendorRemarks.setPromptText("Ingrese notas o comentarios para el vendor...");
        txtVendorRemarks.setPrefWidth(800);
        if (vendorRemarks != null && !vendorRemarks.isEmpty()) {
            txtVendorRemarks.setText(vendorRemarks);
        }
        
        Label lblRemarksHelp = new Label("💡 Estas notas se guardarán en la columna Vendor Remarks");
        lblRemarksHelp.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666; -fx-font-style: italic;");
        
        panelVendorRemarks.getChildren().addAll(lblRemarksTitle, txtVendorRemarks, lblRemarksHelp);
    }
    
    // ============================
    // PANEL NOTES (Para GARRETS)
    // ============================
    VBox panelNotes = null;
    TextField txtNotes = null;
    
    // Verificar si es GARRETS
    boolean esGarrets = formatoActual != null && formatoActual.getBrokerName() != null && 
                        formatoActual.getBrokerName().toUpperCase().contains("GARRET");
    
    if (esGarrets) {
        panelNotes = new VBox(8);
        panelNotes.setStyle("-fx-padding: 10; -fx-background-color: #E8F5E9; -fx-border-color: #4CAF50; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        Label lblNotesTitle = new Label("📝 Notes");
        lblNotesTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2E7D32;");
        
        txtNotes = new TextField();
        txtNotes.setPromptText("Ingrese notas para este producto...");
        txtNotes.setPrefWidth(800);
        if (notesGarret != null && !notesGarret.isEmpty()) {
            txtNotes.setText(notesGarret);
        }
        
        Label lblNotesHelp = new Label("💡 Estas notas se guardarán en la columna Notes");
        lblNotesHelp.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666; -fx-font-style: italic;");
        
        panelNotes.getChildren().addAll(lblNotesTitle, txtNotes, lblNotesHelp);
    }
    
    // ============================
    // TABLA DE PRODUCTOS SIMILARES
    // ============================
    TableView<cl.vss.cotizador.model.ProductoSimilar> tablaProductos = new TableView<>();
    
    // Columnas
    TableColumn<cl.vss.cotizador.model.ProductoSimilar, String> colDescEs = new TableColumn<>("Descripción ES");
    colDescEs.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescripcionEs()));
    colDescEs.setPrefWidth(200);
    
    TableColumn<cl.vss.cotizador.model.ProductoSimilar, String> colDescEn = new TableColumn<>("Descripción EN");
    colDescEn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescripcionEn()));
    colDescEn.setPrefWidth(200);
    
    TableColumn<cl.vss.cotizador.model.ProductoSimilar, String> colUnidad = new TableColumn<>("Unidad");
    colUnidad.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUnidadMedida()));
    colUnidad.setPrefWidth(80);
    
    TableColumn<cl.vss.cotizador.model.ProductoSimilar, Double> colPrecioVentaNeto = new TableColumn<>("Precio Venta Neto");
    colPrecioVentaNeto.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getPrecioVentaNeto()).asObject());
    colPrecioVentaNeto.setPrefWidth(120);
    colPrecioVentaNeto.setCellFactory(column -> new TableCell<cl.vss.cotizador.model.ProductoSimilar, Double>() {
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
    
    TableColumn<cl.vss.cotizador.model.ProductoSimilar, Double> colPrecioUSD = new TableColumn<>("Precio Neto");
    colPrecioUSD.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getPrecioVentaNetoDolares()).asObject());
    colPrecioUSD.setPrefWidth(140);
    colPrecioUSD.setCellFactory(column -> new TableCell<cl.vss.cotizador.model.ProductoSimilar, Double>() {
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

    TableColumn<cl.vss.cotizador.model.ProductoSimilar, Double> colValorPesos = new TableColumn<>("Valor Pesos");
    colValorPesos.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getValorPesos()).asObject());
    colValorPesos.setPrefWidth(120);
    colValorPesos.setCellFactory(column -> new TableCell<cl.vss.cotizador.model.ProductoSimilar, Double>() {
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
    
    tablaProductos.getColumns().addAll(colDescEs, colDescEn, colUnidad, colPrecioUSD, colPrecioVentaNeto, colValorPesos);
    tablaProductos.setPrefSize(800, 300);
    
    // ============================
    // BÚSQUEDA DE PRODUCTOS
    // ============================
    HBox panelBusqueda = new HBox(10);
    panelBusqueda.setStyle("-fx-padding: 10; -fx-alignment: center-left;");
    
    Label lblBuscar = new Label("🔍 Buscar producto:");
    lblBuscar.setStyle("-fx-font-weight: bold;");
    
    TextField txtBusqueda = new TextField();
    txtBusqueda.setPromptText("Ingrese términos de búsqueda...");
    txtBusqueda.setPrefWidth(350);
    // Pre-llenar con descripción actual si existe
    if (descripcion != null && !descripcion.isEmpty()) {
        logger.debug("Pre-llenando campo de búsqueda con descripción: '{}'", descripcion);
        txtBusqueda.setText(descripcion);
    }
    
    Button btnBuscar = new Button("Buscar");
    btnBuscar.setStyle("-fx-background-color: #0A3D91; -fx-text-fill: white; -fx-font-weight: bold;");
    btnBuscar.setPrefWidth(100);
    
    panelBusqueda.getChildren().addAll(lblBuscar, txtBusqueda, btnBuscar);
    
    // ============================
    // PANEL DE PRECIO CALCULADO
    // ============================
    VBox panelPrecioCalculado = new VBox(10);
    panelPrecioCalculado.setStyle("-fx-padding: 10; -fx-background-color: #D6E4FF; -fx-border-color: #0A3D91; -fx-border-width: 2;");
    
    Label lblTituloPrecio = new Label("💰 Precio Total Calculado");
    lblTituloPrecio.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0A3D91;");
    
    Label lblPrecioTotal = new Label("Precio unitario × Cantidad = Total");
    lblPrecioTotal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0A3D91;");
    
    panelPrecioCalculado.getChildren().addAll(lblTituloPrecio, lblPrecioTotal);
    
    // ============================
    // BOTÓN APLICAR PRECIO
    // ============================
    Button btnAplicarPrecio = new Button("✅ Aplicar Precio Seleccionado o Guardar Notas");
    btnAplicarPrecio.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
    btnAplicarPrecio.setPrefWidth(310);
    btnAplicarPrecio.setPrefHeight(40);
    
    // ============================
    // LÓGICA DE BÚSQUEDA
    // ============================
    btnBuscar.setOnAction(e -> {
        String termino = txtBusqueda.getText().trim();
        if (!termino.isEmpty()) {
            List<cl.vss.cotizador.model.ProductoSimilar> resultados = 
                cotizacionService.buscarProductosSimilares(termino);
            
            ObservableList<cl.vss.cotizador.model.ProductoSimilar> datos = 
                FXCollections.observableArrayList(resultados);
            tablaProductos.setItems(datos);
            
            if (resultados.isEmpty()) {
                Label sinResultados = new Label("❌ No se encontraron productos para: " + termino);
                sinResultados.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");
                tablaProductos.setPlaceholder(sinResultados);
            }
        }
    });
    
    // Búsqueda con Enter
    txtBusqueda.setOnAction(e -> btnBuscar.fire());
    
    // Búsqueda inicial
    btnBuscar.fire();
    
    // ============================
    // LÓGICA DE SELECCIÓN
    // ============================
    tablaProductos.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
        if (newSelection != null) {
            // Calcular precio total
            double precioUnitario = newSelection.getPrecioVentaNeto();
            int cant = 1; // valor por defecto
            if (cantidad != null && !cantidad.isEmpty()) {
                try {
                    cant = (int) Double.parseDouble(cantidad.trim());
                    if (cant <= 0) cant = 1;
                } catch (NumberFormatException ex) {
                    logger.warn("⚠️ No se pudo parsear cantidad: '{}', usando 1", cantidad);
                    cant = 1;
                }
            }
            
            double precioTotal = precioUnitario * cant;
            
            lblPrecioTotal.setText(String.format("$%,.2f × %d = $%,.2f", 
                precioUnitario, cant, precioTotal));
        }
    });
    
    // ============================
    // LÓGICA DE APLICAR PRECIO
    // ============================
    final TextField txtVendorRemarksBtnRef = txtVendorRemarks;
    final TextField txtNotesBtnRef = txtNotes;
    
    btnAplicarPrecio.setOnAction(e -> {
        cl.vss.cotizador.model.ProductoSimilar seleccionado = 
            tablaProductos.getSelectionModel().getSelectedItem();
        
        // ============================
        // GUARDAR NOTAS/REMARKS (siempre, aunque no haya producto seleccionado)
        // ============================
        boolean notasGuardadas = false;
        StringBuilder mensajeNotas = new StringBuilder();
        
        // Guardar Vendor Remarks (BSM, CMA CGM, MCTC, OCEANIC, PROCURESHIP)
        if (txtVendorRemarksBtnRef != null) {
            String nuevoValorRemarks = txtVendorRemarksBtnRef.getText();
            String campoVendorRemarks = null;
            for (String key : rowData.getKeys()) {
                String keyUpper = key.toUpperCase();
                // Buscar VENDOR_REMARKS, VENDOR_COMMENTS, etc.
                if ((keyUpper.contains("VENDOR") && 
                    (keyUpper.contains("REMARK") || keyUpper.contains("NOTE") || keyUpper.contains("COMMENT")))) {
                    campoVendorRemarks = key;
                    break;
                }
                // Buscar SUPPLIER_COMMENTS, SUPPLIER COMMNETS, SUPPLIER NOTES (MCTC, Oceanic, ProcureShip)
                if (keyUpper.contains("SUPPLIER") && 
                    (keyUpper.contains("COMMENT") || keyUpper.contains("COMMNET") || keyUpper.contains("NOTE"))) {
                    campoVendorRemarks = key;
                    break;
                }
            }
            if (campoVendorRemarks != null) {
                rowData.set(campoVendorRemarks, nuevoValorRemarks);
                if (nuevoValorRemarks != null && !nuevoValorRemarks.trim().isEmpty()) {
                    notasGuardadas = true;
                    mensajeNotas.append("✅ Vendor Remarks guardado\n");
                }
                logger.info("✅ Vendor Remarks actualizado: {} = '{}'", campoVendorRemarks, nuevoValorRemarks);
            }
        }
        
        // Guardar Notes (GARRETS)
        if (txtNotesBtnRef != null) {
            String nuevoValorNotes = txtNotesBtnRef.getText();
            String campoNotes = null;
            for (String key : rowData.getKeys()) {
                String keyUpper = key.toUpperCase();
                if (keyUpper.equals("NOTES")) {
                    campoNotes = key;
                    break;
                }
            }
            if (campoNotes != null) {
                rowData.set(campoNotes, nuevoValorNotes);
            } else {
                rowData.set("NOTES", nuevoValorNotes);
            }
            if (nuevoValorNotes != null && !nuevoValorNotes.trim().isEmpty()) {
                notasGuardadas = true;
                mensajeNotas.append("✅ Notes guardado\n");
            }
            logger.info("✅ Notes actualizado: NOTES = '{}'", nuevoValorNotes);
        }
        
        // ============================
        // APLICAR PRECIO (solo si hay producto seleccionado)
        // ============================
        if (seleccionado != null) {
            // Calcular precio total
            double precioUnitario = seleccionado.getPrecioVentaNeto();
            int cant = 1; // valor por defecto
            if (cantidad != null && !cantidad.isEmpty()) {
                try {
                    cant = (int) Double.parseDouble(cantidad.trim());
                    if (cant <= 0) cant = 1;
                } catch (NumberFormatException ex) {
                    logger.warn("⚠️ No se pudo parsear cantidad: '{}', usando 1", cantidad);
                    cant = 1;
                }
            }
            
            double precioTotal = precioUnitario * cant;
            
            // Actualizar la fila con el nuevo precio total
            rowData.set("precio_vss_calculado", String.valueOf(precioTotal));
            
            // 💰 Actualizar UNIT_PRICE con precio_venta_neto (precio unitario en USD)
            rowData.set("UNIT_PRICE", String.format("%.2f", precioUnitario));
            logger.info("💰 UNIT_PRICE actualizado con precio_venta_neto: ${} para producto: {}", 
                String.format("%.2f", precioUnitario), seleccionado.getDescripcionEs().substring(0, Math.min(30, seleccionado.getDescripcionEs().length())));
            
            // Refrescar la tabla
            tablaDinamica.refresh();
            
            // Mostrar confirmación
            Alert confirmacion = new Alert(Alert.AlertType.INFORMATION);
            confirmacion.setTitle("Precio Actualizado");
            confirmacion.setHeaderText("✅ Precio aplicado exitosamente");
            confirmacion.setContentText(String.format(
                "Producto: %s\n" +
                "Precio unitario: $%,.2f\n" +
                "Cantidad: %d\n" +
                "Precio total: $%,.2f" +
                (notasGuardadas ? "\n\n" + mensajeNotas.toString() : ""),
                seleccionado.getDescripcionEs(),
                precioUnitario,
                cant,
                precioTotal
            ));
            confirmacion.showAndWait();
            
            logger.info("✅ Precio actualizado: {} → ${}", descripcion, precioTotal);
            
            // Cerrar el diálogo
            dialog.close();
        } else {
            // No hay producto seleccionado, pero igual se guardan las notas
            tablaDinamica.refresh();
            
            if (notasGuardadas) {
                // Mostrar confirmación de que las notas fueron guardadas
                Alert confirmacion = new Alert(Alert.AlertType.INFORMATION);
                confirmacion.setTitle("Notas Guardadas");
                confirmacion.setHeaderText("✅ Notas guardadas exitosamente");
                confirmacion.setContentText(mensajeNotas.toString() + "\n(No se seleccionó producto para actualizar precio)");
                confirmacion.showAndWait();
                
                // Cerrar el diálogo
                dialog.close();
            } else {
                // No hay notas ni producto seleccionado
                Alert advertencia = new Alert(Alert.AlertType.WARNING);
                advertencia.setTitle("Sin Cambios");
                advertencia.setHeaderText("No hay cambios para guardar");
                advertencia.setContentText("Seleccione un producto para actualizar el precio,\no escriba una nota/remarks para guardar.");
                advertencia.showAndWait();
            }
        }
    });
    
    // ============================
    // LAYOUT PRINCIPAL
    // ============================
    VBox contenidoPrincipal = new VBox(15);
    contenidoPrincipal.getChildren().add(infoPanel);
    
    // Agregar panel de Vendor Remarks si es BSM CATERING, CMA CGM, MCTC MARINE u OCEANIC CATERING
    if (panelVendorRemarks != null) {
        contenidoPrincipal.getChildren().add(panelVendorRemarks);
    }
    
    // Agregar panel de Notes si es GARRETS
    if (panelNotes != null) {
        contenidoPrincipal.getChildren().add(panelNotes);
    }
    
    contenidoPrincipal.getChildren().addAll(
        panelBusqueda,
        new Label("🔍 Productos en Base de Datos:"),
        tablaProductos,
        panelPrecioCalculado,
        btnAplicarPrecio
    );
    contenidoPrincipal.setStyle("-fx-padding: 10;");
    
    // Configurar diálogo
    dialog.getDialogPane().setContent(contenidoPrincipal);
    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    dialog.getDialogPane().setPrefSize(900, 750);
    
    // Guardar Vendor Remarks y Notes al cerrar el diálogo
    final TextField txtVendorRemarksFinal = txtVendorRemarks;
    final TextField txtNotesFinal = txtNotes;
    dialog.setOnCloseRequest(event -> {
        // Guardar Vendor Remarks (BSM, CMA CGM, MCTC, OCEANIC, PROCURESHIP)
        if (txtVendorRemarksFinal != null) {
            String nuevoValorRemarks = txtVendorRemarksFinal.getText();
            
            // Buscar el campo correcto para guardar
            String campoVendorRemarks = null;
            for (String key : rowData.getKeys()) {
                String keyUpper = key.toUpperCase();
                // Buscar VENDOR_REMARKS, VENDOR_COMMENTS, etc.
                if ((keyUpper.contains("VENDOR") && 
                    (keyUpper.contains("REMARK") || keyUpper.contains("NOTE") || keyUpper.contains("COMMENT")))) {
                    campoVendorRemarks = key;
                    break;
                }
                // Buscar SUPPLIER_COMMENTS, SUPPLIER COMMNETS, SUPPLIER NOTES (MCTC, Oceanic, ProcureShip)
                if (keyUpper.contains("SUPPLIER") && 
                    (keyUpper.contains("COMMENT") || keyUpper.contains("COMMNET") || keyUpper.contains("NOTE"))) {
                    campoVendorRemarks = key;
                    break;
                }
            }
            
            if (campoVendorRemarks != null) {
                rowData.set(campoVendorRemarks, nuevoValorRemarks);
                tablaDinamica.refresh();
                logger.info("✅ Vendor Remarks actualizado: {} = '{}'", campoVendorRemarks, nuevoValorRemarks);
            } else {
                logger.warn("⚠️ No se encontró el campo Vendor Remarks en la fila");
            }
        }
        
        // Guardar Notes (GARRETS)
        if (txtNotesFinal != null) {
            String nuevoValorNotes = txtNotesFinal.getText();
            
            // Buscar el campo Notes en la fila
            String campoNotes = null;
            for (String key : rowData.getKeys()) {
                String keyUpper = key.toUpperCase();
                if (keyUpper.equals("NOTES")) {
                    campoNotes = key;
                    break;
                }
            }
            
            if (campoNotes != null) {
                rowData.set(campoNotes, nuevoValorNotes);
                tablaDinamica.refresh();
                logger.info("✅ Notes actualizado: {} = '{}'", campoNotes, nuevoValorNotes);
            } else {
                // Si no existe el campo, crearlo
                rowData.set("NOTES", nuevoValorNotes);
                tablaDinamica.refresh();
                logger.info("✅ Notes creado: NOTES = '{}'", nuevoValorNotes);
            }
        }
    });
    
    // Mostrar diálogo
    dialog.showAndWait();
}

// ============================================================
// 🔵 CREAR PANEL DE METADATA VACÍO
// ============================================================
private VBox crearPanelMetadata() {
    VBox panel = new VBox(10);
    panel.setStyle("-fx-padding: 15; -fx-background-color: #f5f7fa; -fx-border-color: #0A3D91; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
    
    Label lblTitulo = new Label("📋 Metadata del Broker");
    lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0A3D91;");
    
    Label lblSinDatos = new Label("Seleccione un broker para ver su metadata");
    lblSinDatos.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666; -fx-font-style: italic;");
    
    panel.getChildren().addAll(lblTitulo, lblSinDatos);
    panel.setManaged(false); // Ocultar inicialmente
    panel.setVisible(false);
    
    return panel;
}

// ============================================================
// 🔵 CARGAR METADATA DEL BROKER
// ============================================================
private void cargarMetadataBroker(int formatoId) {
    try (Connection conn = DBConnection.getConnection()) {
        BrokerMetadataDAO metadataDAO = new BrokerMetadataDAO(conn);
        Map<String, List<BrokerMetadata>> metadataPorSeccion = metadataDAO.obtenerMetadataPorSeccion(formatoId);
        
        // Guardar metadata en variable de instancia
        this.metadataActual = metadataPorSeccion;
        
        // TODO: Descomentar cuando se necesite usar el panel de metadata
        // actualizarPanelMetadata(metadataPorSeccion);
        
        logger.info("Metadata cargada para formato ID {}: {} secciones", formatoId, metadataPorSeccion.size());
        
    } catch (SQLException e) {
        logger.error("Error al cargar metadata del formato ID {}", formatoId, e);
        this.metadataActual = null;
        // TODO: Descomentar cuando se necesite usar el panel de metadata
        // actualizarPanelMetadata(null);
    }
}

// ============================================================
// 🔵 ACTUALIZAR PANEL DE METADATA CON DATOS
// ============================================================
// TODO: Descomentar cuando se necesite usar el panel de metadata
/*
private void actualizarPanelMetadata(Map<String, List<BrokerMetadata>> metadataPorSeccion) {
    panelMetadata.getChildren().clear();
    
    // Título principal
    Label lblTitulo = new Label("📋 Metadata del Broker");
    lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0A3D91;");
    panelMetadata.getChildren().add(lblTitulo);
    
    if (metadataPorSeccion == null || metadataPorSeccion.isEmpty()) {
        Label lblSinDatos = new Label("No hay metadata disponible para este broker");
        lblSinDatos.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666; -fx-font-style: italic;");
        panelMetadata.getChildren().add(lblSinDatos);
        panelMetadata.setManaged(false);
        panelMetadata.setVisible(false);
        return;
    }
    
    // Mostrar el panel
    panelMetadata.setManaged(true);
    panelMetadata.setVisible(true);
    
    // Crear un HBox para organizar las secciones en columnas
    HBox contenedorSecciones = new HBox(20);
    contenedorSecciones.setStyle("-fx-padding: 10 0 0 0;");
    
    // Crear columnas para diferentes secciones
    VBox columna1 = new VBox(10);
    VBox columna2 = new VBox(10);
    VBox columna3 = new VBox(10);
    
    int contadorSeccion = 0;
    
    // Ordenar secciones alfabéticamente
    List<String> seccionesOrdenadas = new java.util.ArrayList<>(metadataPorSeccion.keySet());
    java.util.Collections.sort(seccionesOrdenadas);
    
    for (String seccion : seccionesOrdenadas) {
        List<BrokerMetadata> items = metadataPorSeccion.get(seccion);
        
        // Crear panel para cada sección
        VBox panelSeccion = new VBox(5);
        panelSeccion.setStyle("-fx-padding: 10; -fx-background-color: white; -fx-border-color: #d0d0d0; -fx-border-width: 1; -fx-border-radius: 3; -fx-background-radius: 3;");
        
        // Título de la sección
        Label lblSeccion = new Label("🔹 " + seccion);
        lblSeccion.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #0A3D91;");
        panelSeccion.getChildren().add(lblSeccion);
        
        // Agregar cada campo de la sección
        for (BrokerMetadata metadata : items) {
            HBox fila = new HBox(5);
            fila.setStyle("-fx-padding: 2 0 2 0;");
            
            Label lblCampo = new Label(metadata.getCampoNombre() + ":");
            lblCampo.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #333333; -fx-min-width: 150;");
            
            // 📝 Si es RFQ Information o Request Information, hacer editable
            if ("RFQ Information".equals(seccion) || "Request Information".equals(seccion)) {
                TextField txtValor = new TextField(metadata.getCampoValor() != null ? metadata.getCampoValor() : "");
                txtValor.setStyle("-fx-font-size: 11px;");
                txtValor.setPrefWidth(300);
                
                // Guardar referencia al metadata para actualizar
                txtValor.setUserData(metadata);
                
                // Listener para actualizar el valor en el objeto metadata
                txtValor.textProperty().addListener((obs, oldVal, newVal) -> {
                    metadata.setCampoValor(newVal);
                });
                
                fila.getChildren().addAll(lblCampo, txtValor);
            } else {
                // Campos no editables (solo lectura)
                Label lblValor = new Label(metadata.getCampoValor() != null ? metadata.getCampoValor() : "");
                lblValor.setStyle("-fx-font-size: 11px; -fx-text-fill: #555555;");
                lblValor.setWrapText(true);
                lblValor.setMaxWidth(300);
                
                fila.getChildren().addAll(lblCampo, lblValor);
            }
            
            panelSeccion.getChildren().add(fila);
        }
        
        // 💾 Si es RFQ Information o Request Information, agregar botón de guardar
        if ("RFQ Information".equals(seccion) || "Request Information".equals(seccion)) {
            Button btnGuardar = new Button("💾 Guardar Cambios");
            btnGuardar.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-margin-top: 10;");
            btnGuardar.setPrefWidth(150);
            
            btnGuardar.setOnAction(e -> guardarMetadataRFQ(items));
            
            panelSeccion.getChildren().add(btnGuardar);
        }
        
        // Distribuir secciones en 3 columnas
        if (contadorSeccion % 3 == 0) {
            columna1.getChildren().add(panelSeccion);
        } else if (contadorSeccion % 3 == 1) {
            columna2.getChildren().add(panelSeccion);
        } else {
            columna3.getChildren().add(panelSeccion);
        }
        
        contadorSeccion++;
    }
    
    // Agregar columnas al contenedor solo si tienen contenido
    if (!columna1.getChildren().isEmpty()) contenedorSecciones.getChildren().add(columna1);
    if (!columna2.getChildren().isEmpty()) contenedorSecciones.getChildren().add(columna2);
    if (!columna3.getChildren().isEmpty()) contenedorSecciones.getChildren().add(columna3);
    
    panelMetadata.getChildren().add(contenedorSecciones);
}
*/

// ============================================================
// 🔵 GUARDAR METADATA DE RFQ
// ============================================================
private void guardarMetadataRFQ(List<BrokerMetadata> items) {
    try (Connection conn = DBConnection.getConnection()) {
        BrokerMetadataDAO metadataDAO = new BrokerMetadataDAO(conn);
        
        int actualizados = 0;
        
        for (BrokerMetadata metadata : items) {
            // Actualizar cada campo en la base de datos
            String sql = "UPDATE broker_metadata SET campo_valor = ? WHERE metadata_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, metadata.getCampoValor());
                stmt.setInt(2, metadata.getMetadataId());
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    actualizados++;
                }
            }
        }
        
        logger.info("Se actualizaron {} campos de RFQ Information", actualizados);
        
        // Mostrar confirmación
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Cambios Guardados");
        alert.setHeaderText("Metadata RFQ actualizada");
        alert.setContentText("Se actualizaron " + actualizados + " campos correctamente.");
        alert.showAndWait();
        
    } catch (SQLException e) {
        logger.error("Error al guardar metadata RFQ", e);
        
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error al guardar cambios");
        alert.setContentText("No se pudieron guardar los cambios: " + e.getMessage());
        alert.showAndWait();
    }
}

    // ⚠️ El main siempre al final
    public static void main(String[] args) {
        launch(args);
    }

}
