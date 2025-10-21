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

public class Main extends Application {

    // Clase para los datos de la tabla
    public static class Producto {
        private String nombre;
        private double precio;
        private int cantidad;
        private String categoria;

        public Producto(String nombre, double precio, int cantidad, String categoria) {
            this.nombre = nombre;
            this.precio = precio;
            this.cantidad = cantidad;
            this.categoria = categoria;
        }

        // Getters y setters
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        
        public double getPrecio() { return precio; }
        public void setPrecio(double precio) { this.precio = precio; }
        
        public int getCantidad() { return cantidad; }
        public void setCantidad(int cantidad) { this.cantidad = cantidad; }
        
        public String getCategoria() { return categoria; }
        public void setCategoria(String categoria) { this.categoria = categoria; }
    }

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("Cotizador JavaFX - Sistema Completo");

        // Crear TabPane principal
        TabPane tabPane = new TabPane();

        // Pestana 1: Dashboard
        Tab dashboardTab = new Tab("Dashboard");
        dashboardTab.setClosable(false);
        
        VBox dashboardContent = new VBox(15);
        dashboardContent.setPadding(new Insets(20));
        
        Label welcomeLabel = new Label("Bienvenido al Sistema de Cotizacion");
        welcomeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label infoLabel = new Label("Seleccione una pestana para comenzar");
        infoLabel.setStyle("-fx-font-size: 14px;");
        
        Button actionButton = new Button("Accion Principal");
        actionButton.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Información");
            alert.setHeaderText("Sistema Funcionando");
            alert.setContentText("El sistema de cotizacion esta operativo!");
            alert.showAndWait();
        });
        
        dashboardContent.getChildren().addAll(welcomeLabel, infoLabel, actionButton);
        dashboardTab.setContent(dashboardContent);

        // Pestana 2: Productos
        Tab productosTab = new Tab("Productos");
        productosTab.setClosable(false);
        
        VBox productosContent = new VBox(10);
        productosContent.setPadding(new Insets(20));
        
        // Crear tabla de productos
        TableView<Producto> tablaProductos = new TableView<>();
        
        // Columnas de la tabla
        TableColumn<Producto, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        
        TableColumn<Producto, Double> colPrecio = new TableColumn<>("Precio");
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        
        TableColumn<Producto, Integer> colCantidad = new TableColumn<>("Cantidad");
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        
        TableColumn<Producto, String> colCategoria = new TableColumn<>("Categoria");
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        
        tablaProductos.getColumns().add(colNombre);
        tablaProductos.getColumns().add(colPrecio);
        tablaProductos.getColumns().add(colCantidad);
        tablaProductos.getColumns().add(colCategoria);
        
        // Datos de ejemplo
        ObservableList<Producto> productos = FXCollections.observableArrayList(
            new Producto("Laptop HP", 15000.0, 5, "Electronicos"),
            new Producto("Mouse Inalambrico", 250.0, 20, "Accesorios"),
            new Producto("Teclado Mecanico", 800.0, 15, "Accesorios"),
            new Producto("Monitor 24\"", 3500.0, 8, "Electronicos"),
            new Producto("Impresora Laser", 2200.0, 3, "Oficina")
        );
        
        tablaProductos.setItems(productos);
        
        // Controles para productos
        HBox controlesProductos = new HBox(10);
        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre del producto");
        
        TextField txtPrecio = new TextField();
        txtPrecio.setPromptText("Precio");
        
        TextField txtCantidad = new TextField();
        txtCantidad.setPromptText("Cantidad");
        
        ComboBox<String> cmbCategoria = new ComboBox<>();
        cmbCategoria.getItems().addAll("Electronicos", "Accesorios", "Oficina", "Hogar");
        cmbCategoria.setPromptText("Categoria");
        
        Button btnAgregar = new Button("Agregar");
        btnAgregar.setOnAction(e -> {
            try {
                String nombre = txtNombre.getText();
                double precio = Double.parseDouble(txtPrecio.getText());
                int cantidad = Integer.parseInt(txtCantidad.getText());
                String categoria = cmbCategoria.getValue();
                
                if (!nombre.isEmpty() && categoria != null) {
                    productos.add(new Producto(nombre, precio, cantidad, categoria));
                    txtNombre.clear();
                    txtPrecio.clear();
                    txtCantidad.clear();
                    cmbCategoria.setValue(null);
                }
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("Por favor ingrese valores numericos validos");
                alert.showAndWait();
            }
        });
        
        controlesProductos.getChildren().addAll(txtNombre, txtPrecio, txtCantidad, cmbCategoria, btnAgregar);
        
        productosContent.getChildren().addAll(new Label("Gestion de Productos"), tablaProductos, controlesProductos);
        productosTab.setContent(productosContent);

        // Pestana 3: Cotizaciones
        Tab cotizacionesTab = new Tab("Cotizaciones");
        cotizacionesTab.setClosable(false);
        
        VBox cotizacionesContent = new VBox(15);
        cotizacionesContent.setPadding(new Insets(20));
        
        Label cotizacionesLabel = new Label("Modulo de Cotizaciones");
        cotizacionesLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Formulario de cotización
        GridPane formularioCotizacion = new GridPane();
        formularioCotizacion.setHgap(10);
        formularioCotizacion.setVgap(10);
        
        formularioCotizacion.add(new Label("Cliente:"), 0, 0);
        TextField txtCliente = new TextField();
        formularioCotizacion.add(txtCliente, 1, 0);
        
        formularioCotizacion.add(new Label("Fecha:"), 0, 1);
        DatePicker datePicker = new DatePicker();
        formularioCotizacion.add(datePicker, 1, 1);
        
        formularioCotizacion.add(new Label("Descuento (%):"), 0, 2);
        Slider sliderDescuento = new Slider(0, 50, 0);
        sliderDescuento.setShowTickLabels(true);
        sliderDescuento.setShowTickMarks(true);
        formularioCotizacion.add(sliderDescuento, 1, 2);
        
        Button btnGenerarCotizacion = new Button("Generar Cotizacion");
        btnGenerarCotizacion.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Cotizacion Generada");
            alert.setContentText("Cotizacion generada para: " + txtCliente.getText());
            alert.showAndWait();
        });
        
        cotizacionesContent.getChildren().addAll(cotizacionesLabel, formularioCotizacion, btnGenerarCotizacion);
        cotizacionesTab.setContent(cotizacionesContent);

        // Pestana 4: Reportes
        Tab reportesTab = new Tab("Reportes");
        reportesTab.setClosable(false);
        
        VBox reportesContent = new VBox(15);
        reportesContent.setPadding(new Insets(20));
        
        Label reportesLabel = new Label("Reportes y Estadisticas");
        reportesLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Area de texto para reportes
        TextArea areaReportes = new TextArea();
        areaReportes.setPrefRowCount(10);
        areaReportes.setText("Reporte de Ventas\n" +
                           "================\n" +
                           "Total de productos: " + productos.size() + "\n" +
                           "Categorias disponibles: Electronicos, Accesorios, Oficina, Hogar\n" +
                           "Ultima actualizacion: " + java.time.LocalDateTime.now());
        areaReportes.setEditable(false);
        
        Button btnActualizarReporte = new Button("Actualizar Reporte");
        btnActualizarReporte.setOnAction(e -> {
            areaReportes.setText("Reporte de Ventas\n" +
                               "================\n" +
                               "Total de productos: " + productos.size() + "\n" +
                               "Categorias disponibles: Electronicos, Accesorios, Oficina, Hogar\n" +
                               "Ultima actualizacion: " + java.time.LocalDateTime.now());
        });
        
        reportesContent.getChildren().addAll(reportesLabel, areaReportes, btnActualizarReporte);
        reportesTab.setContent(reportesContent);

        // Agregar todas las pestañas
        tabPane.getTabs().addAll(dashboardTab, productosTab, cotizacionesTab, reportesTab);

        // Crear escena con tamaño más grande
        Scene scene = new Scene(tabPane, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Configurar UTF-8 para caracteres especiales
        System.setProperty("file.encoding", "UTF-8");
        launch(args);
    }
}