package com.example;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

// Importar el modelo genérico
import com.example.model.*;
import com.example.service.*;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Main extends Application {

    // Servicios
    private CotizacionService cotizacionService;
    private ExportacionService exportacionService;
    private LecturaCotizacionService lecturaService;
    
    // Listas observables para las tablas
    private ObservableList<Cotizacion> cotizacionesObservable;
    private ObservableList<Cliente> clientesObservable;
    private ObservableList<ItemCotizacion> itemsObservable;

    @Override
    public void start(Stage primaryStage) {
        
        // Inicializar servicios
        cotizacionService = CotizacionService.getInstance();
        exportacionService = ExportacionService.getInstance();
        lecturaService = LecturaCotizacionService.getInstance();
        
        // Inicializar listas observables
        cotizacionesObservable = FXCollections.observableArrayList();
        clientesObservable = FXCollections.observableArrayList();
        itemsObservable = FXCollections.observableArrayList();
        
        // Cargar datos iniciales
        cargarDatosIniciales();

        primaryStage.setTitle("Cotizador JavaFX - Sistema de Cotizaciones");

        // Crear TabPane principal
        TabPane tabPane = new TabPane();

        // Pestaña 1: Dashboard
        Tab dashboardTab = new Tab("Dashboard");
        dashboardTab.setClosable(false);
        dashboardTab.setContent(crearDashboard());

        // Pestaña 2: Cotizaciones
        Tab cotizacionesTab = new Tab("Cotizaciones");
        cotizacionesTab.setClosable(false);
        cotizacionesTab.setContent(crearPanelCotizaciones());

        // Pestaña 3: Clientes
        Tab clientesTab = new Tab("Clientes");
        clientesTab.setClosable(false);
        clientesTab.setContent(crearPanelClientes());

        // Pestaña 4: Productos
        Tab productosTab = new Tab("Productos");
        productosTab.setClosable(false);
        productosTab.setContent(crearPanelProductos());

        // Pestaña 5: Reportes
        Tab reportesTab = new Tab("Reportes");
        reportesTab.setClosable(false);
        reportesTab.setContent(crearPanelReportes());

        // Pestaña 6: Importar Cotizaciones
        Tab importarTab = new Tab("Importar");
        importarTab.setClosable(false);
        importarTab.setContent(crearPanelImportar());

        // Agregar todas las pestañas
        tabPane.getTabs().addAll(dashboardTab, cotizacionesTab, clientesTab, productosTab, reportesTab, importarTab);

        // Crear escena
        Scene scene = new Scene(tabPane, 1200, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void cargarDatosIniciales() {
        // Cargar cotizaciones
        cotizacionesObservable.clear();
        cotizacionesObservable.addAll(cotizacionService.obtenerTodasLasCotizaciones());
        
        // Cargar clientes
        clientesObservable.clear();
        clientesObservable.addAll(cotizacionService.obtenerTodosLosClientes());
    }

    private VBox crearDashboard() {
        VBox dashboard = new VBox(15);
        dashboard.setPadding(new Insets(20));

        Label titulo = new Label("Dashboard - Sistema de Cotizaciones");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // Estadísticas
        HBox statsBox = new HBox(20);
        statsBox.setPadding(new Insets(10));

        Map<String, Object> stats = cotizacionService.obtenerEstadisticas();
        
        VBox stat1 = crearStatCard("Total Cotizaciones", stats.get("totalCotizaciones").toString());
        VBox stat2 = crearStatCard("Total Clientes", stats.get("totalClientes").toString());
        VBox stat3 = crearStatCard("Pendientes", stats.get("cotizacionesPendientes").toString());
        VBox stat4 = crearStatCard("Aprobadas", stats.get("cotizacionesAprobadas").toString());

        statsBox.getChildren().addAll(stat1, stat2, stat3, stat4);

        // Botones de acción rápida
        HBox botonesAccion = new HBox(10);
        Button btnNuevaCotizacion = new Button("Nueva Cotización");
        btnNuevaCotizacion.setOnAction(e -> mostrarDialogoNuevaCotizacion());
        
        Button btnNuevoCliente = new Button("Nuevo Cliente");
        btnNuevoCliente.setOnAction(e -> mostrarDialogoNuevoCliente());

        botonesAccion.getChildren().addAll(btnNuevaCotizacion, btnNuevoCliente);

        dashboard.getChildren().addAll(titulo, statsBox, botonesAccion);
        return dashboard;
    }

    private VBox crearStatCard(String titulo, String valor) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 5;");
        
        Label tituloLabel = new Label(titulo);
        tituloLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        
        Label valorLabel = new Label(valor);
        valorLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        card.getChildren().addAll(tituloLabel, valorLabel);
        return card;
    }

    private VBox crearPanelCotizaciones() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(20));

        Label titulo = new Label("Gestión de Cotizaciones");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Botones de acción
        HBox botones = new HBox(10);
        Button btnNueva = new Button("Nueva Cotización");
        Button btnEditar = new Button("Editar");
        Button btnEliminar = new Button("Eliminar");
        Button btnExportar = new Button("Exportar a Excel");
        Button btnActualizar = new Button("Actualizar");

        btnNueva.setOnAction(e -> mostrarDialogoNuevaCotizacion());
        btnEditar.setOnAction(e -> editarCotizacionSeleccionada());
        btnEliminar.setOnAction(e -> eliminarCotizacionSeleccionada());
        btnExportar.setOnAction(e -> exportarCotizacionSeleccionada());
        btnActualizar.setOnAction(e -> cargarDatosIniciales());

        botones.getChildren().addAll(btnNueva, btnEditar, btnEliminar, btnExportar, btnActualizar);

        // Tabla de cotizaciones
        TableView<Cotizacion> tablaCotizaciones = new TableView<>();
        
        TableColumn<Cotizacion, String> colNumero = new TableColumn<>("Número");
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numeroCotizacion"));
        
        TableColumn<Cotizacion, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(cellData -> {
            Cotizacion cotizacion = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                cotizacion.getCliente() != null ? cotizacion.getCliente().getNombre() : "Sin cliente"
            );
        });
        
        TableColumn<Cotizacion, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(cellData -> {
            Cotizacion cotizacion = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                cotizacion.getFechaCreacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            );
        });
        
        TableColumn<Cotizacion, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        
        TableColumn<Cotizacion, Double> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(column -> new TableCell<Cotizacion, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("$%,.0f", item));
                }
            }
        });

        tablaCotizaciones.getColumns().addAll(colNumero, colCliente, colFecha, colEstado, colTotal);
        tablaCotizaciones.setItems(cotizacionesObservable);

        panel.getChildren().addAll(titulo, botones, tablaCotizaciones);
        return panel;
    }

    private VBox crearPanelClientes() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(20));

        Label titulo = new Label("Gestión de Clientes");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Botones
        HBox botones = new HBox(10);
        Button btnNuevo = new Button("Nuevo Cliente");
        Button btnEditar = new Button("Editar");
        Button btnEliminar = new Button("Eliminar");

        btnNuevo.setOnAction(e -> mostrarDialogoNuevoCliente());
        btnEditar.setOnAction(e -> editarClienteSeleccionado());
        btnEliminar.setOnAction(e -> eliminarClienteSeleccionado());

        botones.getChildren().addAll(btnNuevo, btnEditar, btnEliminar);

        // Tabla de clientes
        TableView<Cliente> tablaClientes = new TableView<>();
        
        TableColumn<Cliente, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        
        TableColumn<Cliente, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        
        TableColumn<Cliente, String> colTelefono = new TableColumn<>("Teléfono");
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        
        TableColumn<Cliente, String> colDireccion = new TableColumn<>("Dirección");
        colDireccion.setCellValueFactory(cellData -> {
            Cliente cliente = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(cliente.getDireccionCompleta());
        });

        tablaClientes.getColumns().addAll(colNombre, colEmail, colTelefono, colDireccion);
        tablaClientes.setItems(clientesObservable);

        panel.getChildren().addAll(titulo, botones, tablaClientes);
        return panel;
    }

    private VBox crearPanelProductos() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(20));

        Label titulo = new Label("Gestión de Productos");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Formulario para agregar productos
        GridPane formulario = new GridPane();
        formulario.setHgap(10);
        formulario.setVgap(10);

        formulario.add(new Label("Código:"), 0, 0);
        TextField txtCodigo = new TextField();
        formulario.add(txtCodigo, 1, 0);

        formulario.add(new Label("Descripción:"), 0, 1);
        TextField txtDescripcion = new TextField();
        formulario.add(txtDescripcion, 1, 1);

        formulario.add(new Label("Precio:"), 0, 2);
        TextField txtPrecio = new TextField();
        formulario.add(txtPrecio, 1, 2);

        formulario.add(new Label("Categoría:"), 0, 3);
        ComboBox<String> cmbCategoria = new ComboBox<>();
        cmbCategoria.getItems().addAll(cotizacionService.obtenerCategoriasDisponibles());
        formulario.add(cmbCategoria, 1, 3);

        Button btnAgregar = new Button("Agregar Producto");
        btnAgregar.setOnAction(e -> {
            // Aquí se podría agregar lógica para gestionar productos
            mostrarMensaje("Producto agregado", "El producto se ha agregado correctamente.");
        });

        formulario.add(btnAgregar, 1, 4);

        panel.getChildren().addAll(titulo, formulario);
        return panel;
    }

    private VBox crearPanelReportes() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));

        Label titulo = new Label("Reportes y Estadísticas");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Botones de reportes
        HBox botonesReportes = new HBox(10);
        Button btnExportarTodas = new Button("Exportar Todas las Cotizaciones");
        Button btnReporteClientes = new Button("Reporte de Clientes");
        Button btnEstadisticas = new Button("Ver Estadísticas");

        btnExportarTodas.setOnAction(e -> exportarTodasLasCotizaciones());
        btnReporteClientes.setOnAction(e -> mostrarReporteClientes());
        btnEstadisticas.setOnAction(e -> mostrarEstadisticas());

        botonesReportes.getChildren().addAll(btnExportarTodas, btnReporteClientes, btnEstadisticas);

        // Área de texto para mostrar reportes
        TextArea areaReportes = new TextArea();
        areaReportes.setPrefRowCount(15);
        areaReportes.setEditable(false);

        panel.getChildren().addAll(titulo, botonesReportes, areaReportes);
        return panel;
    }

    // Métodos para diálogos y acciones
    private void mostrarDialogoNuevaCotizacion() {
        Dialog<Cotizacion> dialog = new Dialog<>();
        dialog.setTitle("Nueva Cotización");
        dialog.setHeaderText("Crear una nueva cotización");

        // Crear formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ComboBox<Cliente> cmbCliente = new ComboBox<>();
        cmbCliente.getItems().addAll(clientesObservable);
        cmbCliente.setPromptText("Seleccionar cliente");

        DatePicker datePicker = new DatePicker();
        datePicker.setValue(java.time.LocalDate.now().plusDays(30));

        TextArea txtNotas = new TextArea();
        txtNotas.setPrefRowCount(3);

        TextArea txtCondiciones = new TextArea();
        txtCondiciones.setPrefRowCount(3);

        grid.add(new Label("Cliente:"), 0, 0);
        grid.add(cmbCliente, 1, 0);
        grid.add(new Label("Válida hasta:"), 0, 1);
        grid.add(datePicker, 1, 1);
        grid.add(new Label("Notas:"), 0, 2);
        grid.add(txtNotas, 1, 2);
        grid.add(new Label("Condiciones:"), 0, 3);
        grid.add(txtCondiciones, 1, 3);

        dialog.getDialogPane().setContent(grid);

        ButtonType crearButtonType = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(crearButtonType, ButtonType.CANCEL);

        Optional<Cotizacion> result = dialog.showAndWait();
        result.ifPresent(cotizacion -> {
            if (cmbCliente.getValue() != null) {
                Cotizacion nuevaCotizacion = new Cotizacion(cmbCliente.getValue());
                nuevaCotizacion.setFechaVencimiento(datePicker.getValue().atStartOfDay());
                nuevaCotizacion.setNotas(txtNotas.getText());
                nuevaCotizacion.setCondiciones(txtCondiciones.getText());
                
                cotizacionService.crearCotizacion(nuevaCotizacion);
                cargarDatosIniciales();
                mostrarMensaje("Cotización creada", "La cotización se ha creado exitosamente.");
            }
        });
    }

    private void mostrarDialogoNuevoCliente() {
        Dialog<Cliente> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Cliente");
        dialog.setHeaderText("Agregar un nuevo cliente");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField txtNombre = new TextField();
        TextField txtEmail = new TextField();
        TextField txtTelefono = new TextField();
        TextField txtDireccion = new TextField();
        TextField txtCiudad = new TextField();
        TextField txtRut = new TextField();

        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(txtNombre, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(txtEmail, 1, 1);
        grid.add(new Label("Teléfono:"), 0, 2);
        grid.add(txtTelefono, 1, 2);
        grid.add(new Label("Dirección:"), 0, 3);
        grid.add(txtDireccion, 1, 3);
        grid.add(new Label("Ciudad:"), 0, 4);
        grid.add(txtCiudad, 1, 4);
        grid.add(new Label("RUT:"), 0, 5);
        grid.add(txtRut, 1, 5);

        dialog.getDialogPane().setContent(grid);

        ButtonType crearButtonType = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(crearButtonType, ButtonType.CANCEL);

        Optional<Cliente> result = dialog.showAndWait();
        result.ifPresent(cliente -> {
            Cliente nuevoCliente = new Cliente(txtNombre.getText(), txtEmail.getText());
            nuevoCliente.setTelefono(txtTelefono.getText());
            nuevoCliente.setDireccion(txtDireccion.getText());
            nuevoCliente.setCiudad(txtCiudad.getText());
            nuevoCliente.setNumeroDocumento(txtRut.getText());
            
            cotizacionService.crearCliente(nuevoCliente);
            cargarDatosIniciales();
            mostrarMensaje("Cliente creado", "El cliente se ha agregado exitosamente.");
        });
    }

    private void editarCotizacionSeleccionada() {
        // Implementar edición de cotización
        mostrarMensaje("En desarrollo", "La funcionalidad de edición estará disponible próximamente.");
    }

    private void eliminarCotizacionSeleccionada() {
        // Implementar eliminación de cotización
        mostrarMensaje("En desarrollo", "La funcionalidad de eliminación estará disponible próximamente.");
    }

    private void exportarCotizacionSeleccionada() {
        // Implementar exportación de cotización específica
        mostrarMensaje("En desarrollo", "La funcionalidad de exportación específica estará disponible próximamente.");
    }

    private void editarClienteSeleccionado() {
        mostrarMensaje("En desarrollo", "La funcionalidad de edición de clientes estará disponible próximamente.");
    }

    private void eliminarClienteSeleccionado() {
        mostrarMensaje("En desarrollo", "La funcionalidad de eliminación de clientes estará disponible próximamente.");
    }

    private void exportarTodasLasCotizaciones() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar Reporte de Cotizaciones");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos Excel", "*.xlsx")
            );
            fileChooser.setInitialFileName("todas_las_cotizaciones.xlsx");
            
            File archivo = fileChooser.showSaveDialog(null);
            if (archivo != null) {
                exportacionService.exportarCotizacionesAExcel(
                    cotizacionService.obtenerTodasLasCotizaciones(), 
                    archivo.getAbsolutePath()
                );
                mostrarMensaje("Exportación exitosa", "El archivo se ha guardado en: " + archivo.getAbsolutePath());
            }
        } catch (Exception e) {
            mostrarMensaje("Error", "Error al exportar: " + e.getMessage());
        }
    }

    private void mostrarReporteClientes() {
        mostrarMensaje("Reporte de Clientes", "Total de clientes: " + clientesObservable.size());
    }

    private void mostrarEstadisticas() {
        Map<String, Object> stats = cotizacionService.obtenerEstadisticas();
        StringBuilder mensaje = new StringBuilder("Estadísticas del Sistema:\n\n");
        stats.forEach((key, value) -> {
            mensaje.append(key).append(": ").append(value).append("\n");
        });
        mostrarMensaje("Estadísticas", mensaje.toString());
    }

    private VBox crearPanelImportar() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));

        Label titulo = new Label("Importar Cotizaciones desde Excel");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Área de instrucciones
        VBox instrucciones = new VBox(10);
        instrucciones.setStyle("-fx-background-color: #f0f8ff; -fx-padding: 15; -fx-background-radius: 5;");
        
        Label instruccionesTitulo = new Label("📋 Instrucciones:");
        instruccionesTitulo.setStyle("-fx-font-weight: bold;");
        
        Label instruccionesTexto = new Label(
            "1. Selecciona un archivo Excel (.xlsx o .xls)\n" +
            "2. El archivo debe contener columnas con nombres como:\n" +
            "   • Número/Número de Cotización\n" +
            "   • Cliente/Empresa\n" +
            "   • Fecha\n" +
            "   • Estado\n" +
            "   • Subtotal, Descuento, Impuestos, Total\n" +
            "3. Haz clic en 'Cargar Archivo' para procesar\n" +
            "4. Revisa los datos cargados antes de importar"
        );
        instruccionesTexto.setStyle("-fx-font-size: 12px;");
        
        instrucciones.getChildren().addAll(instruccionesTitulo, instruccionesTexto);

        // Botones de acción
        HBox botonesAccion = new HBox(10);
        Button btnSeleccionarArchivo = new Button("📁 Seleccionar Archivo Excel");
        Button btnCargarArchivo = new Button("📤 Cargar Archivo");
        Button btnImportarSeleccionadas = new Button("✅ Importar Seleccionadas");
        Button btnLimpiarVista = new Button("🗑️ Limpiar Vista");

        btnSeleccionarArchivo.setOnAction(e -> seleccionarArchivoExcel());
        btnCargarArchivo.setOnAction(e -> cargarArchivoExcel());
        btnImportarSeleccionadas.setOnAction(e -> importarCotizacionesSeleccionadas());
        btnLimpiarVista.setOnAction(e -> limpiarVistaImportacion());

        botonesAccion.getChildren().addAll(btnSeleccionarArchivo, btnCargarArchivo, btnImportarSeleccionadas, btnLimpiarVista);

        // Información del archivo seleccionado
        Label lblArchivoSeleccionado = new Label("Ningún archivo seleccionado");
        lblArchivoSeleccionado.setStyle("-fx-font-style: italic; -fx-text-fill: #666;");

        // Tabla para mostrar cotizaciones cargadas
        TableView<Cotizacion> tablaImportadas = new TableView<>();
        tablaImportadas.setPrefHeight(300);
        
        TableColumn<Cotizacion, String> colNumero = new TableColumn<>("Número");
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numeroCotizacion"));
        
        TableColumn<Cotizacion, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(cellData -> {
            Cotizacion cotizacion = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                cotizacion.getCliente() != null ? cotizacion.getCliente().getNombre() : "Sin cliente"
            );
        });
        
        TableColumn<Cotizacion, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(cellData -> {
            Cotizacion cotizacion = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                cotizacion.getFechaCreacion() != null ? 
                cotizacion.getFechaCreacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Sin fecha"
            );
        });
        
        TableColumn<Cotizacion, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        
        TableColumn<Cotizacion, Double> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(column -> new TableCell<Cotizacion, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("$%,.0f", item));
                }
            }
        });

        // Columna de selección
        TableColumn<Cotizacion, Boolean> colSeleccionar = new TableColumn<>("Importar");
        colSeleccionar.setCellValueFactory(cellData -> {
            Cotizacion cotizacion = cellData.getValue();
            return new javafx.beans.property.SimpleBooleanProperty(cotizacion.getEstado() != null);
        });
        colSeleccionar.setCellFactory(column -> new TableCell<Cotizacion, Boolean>() {
            private CheckBox checkBox = new CheckBox();
            
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(item);
                    setGraphic(checkBox);
                }
            }
        });

        tablaImportadas.getColumns().addAll(colSeleccionar, colNumero, colCliente, colFecha, colEstado, colTotal);
        
        // Lista observable para cotizaciones importadas
        ObservableList<Cotizacion> cotizacionesImportadas = FXCollections.observableArrayList();
        tablaImportadas.setItems(cotizacionesImportadas);

        // Área de estado
        Label lblEstado = new Label("Listo para cargar archivo");
        lblEstado.setStyle("-fx-font-weight: bold;");

        panel.getChildren().addAll(titulo, instrucciones, botonesAccion, lblArchivoSeleccionado, tablaImportadas, lblEstado);
        return panel;
    }

    private File archivoSeleccionado = null;

    private void seleccionarArchivoExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo Excel con cotizaciones");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Archivos Excel", "*.xlsx", "*.xls"),
            new FileChooser.ExtensionFilter("Excel 2007+", "*.xlsx"),
            new FileChooser.ExtensionFilter("Excel 97-2003", "*.xls")
        );
        
        archivoSeleccionado = fileChooser.showOpenDialog(null);
        if (archivoSeleccionado != null) {
            // Actualizar la etiqueta del archivo seleccionado
            // Esto se haría en el método cargarArchivoExcel()
        }
    }

    private void cargarArchivoExcel() {
        if (archivoSeleccionado == null) {
            mostrarMensaje("Error", "Por favor selecciona un archivo Excel primero.");
            return;
        }

        try {
            List<Cotizacion> cotizacionesLeidas = lecturaService.leerCotizacionesDesdeExcel(archivoSeleccionado.getAbsolutePath());
            
            if (cotizacionesLeidas.isEmpty()) {
                mostrarMensaje("Sin datos", "No se encontraron cotizaciones válidas en el archivo.");
                return;
            }

            // Mostrar las cotizaciones cargadas en un diálogo
            mostrarCotizacionesCargadas(cotizacionesLeidas);

        } catch (Exception e) {
            mostrarMensaje("Error", "Error al leer el archivo: " + e.getMessage());
        }
    }

    private void mostrarCotizacionesCargadas(List<Cotizacion> cotizaciones) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Cotizaciones Cargadas");
        dialog.setHeaderText(String.format("Se cargaron %d cotizaciones desde el archivo", cotizaciones.size()));

        // Crear tabla para mostrar las cotizaciones
        TableView<Cotizacion> tabla = new TableView<>();
        
        TableColumn<Cotizacion, String> colNumero = new TableColumn<>("Número");
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numeroCotizacion"));
        
        TableColumn<Cotizacion, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(cellData -> {
            Cotizacion cotizacion = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                cotizacion.getCliente() != null ? cotizacion.getCliente().getNombre() : "Sin cliente"
            );
        });
        
        TableColumn<Cotizacion, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        
        TableColumn<Cotizacion, Double> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(column -> new TableCell<Cotizacion, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("$%,.0f", item));
                }
            }
        });

        tabla.getColumns().addAll(colNumero, colCliente, colEstado, colTotal);
        tabla.setItems(FXCollections.observableArrayList(cotizaciones));
        tabla.setPrefHeight(300);

        dialog.getDialogPane().setContent(tabla);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        
        // Botón para importar todas
        ButtonType importarButtonType = new ButtonType("Importar Todas", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(importarButtonType);
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == importarButtonType) {
            // Importar todas las cotizaciones
            for (Cotizacion cotizacion : cotizaciones) {
                cotizacionService.crearCotizacion(cotizacion);
            }
            cargarDatosIniciales();
            mostrarMensaje("Importación exitosa", 
                String.format("Se importaron %d cotizaciones al sistema.", cotizaciones.size()));
        }
    }

    private void importarCotizacionesSeleccionadas() {
        mostrarMensaje("En desarrollo", "La funcionalidad de importación selectiva estará disponible próximamente.");
    }

    private void limpiarVistaImportacion() {
        archivoSeleccionado = null;
        mostrarMensaje("Vista limpiada", "Se ha limpiado la vista de importación.");
    }

    private void mostrarMensaje(String titulo, String mensaje) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        launch(args);
    }
}