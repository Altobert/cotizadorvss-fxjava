package cl.vss.cotizador;

import cl.vss.cotizador.model.Broker;
import cl.vss.cotizador.model.BrokerFormato;
import cl.vss.cotizador.model.FormatoColumna;
import cl.vss.cotizador.model.RowData;
import cl.vss.cotizador.service.BrokerDAO;
import cl.vss.cotizador.service.FormatoDAO;
import cl.vss.cotizador.util.DBConnection;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para los métodos de Main.java relacionados con formatos de broker
 * NOTA: Requiere JavaFX Runtime y base de datos PostgreSQL activa
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MainIntegracionTest {
    
    private Main mainApp;
    private Connection connection;
    private Integer brokerIdPrueba;
    private Integer formatoIdPrueba;
    private File archivoExcelPrueba;
    
    @BeforeAll
    static void initJavaFX() {
        // Inicializar JavaFX toolkit
        new JFXPanel();
    }
    
    @BeforeEach
    void setUp() throws Exception {
        connection = DBConnection.getConnection();
        insertarDatosDePrueba();
        
        // Crear instancia de Main en el hilo de JavaFX
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                mainApp = new Main();
                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        limpiarDatosDePrueba();
        
        if (archivoExcelPrueba != null && archivoExcelPrueba.exists()) {
            archivoExcelPrueba.delete();
        }
        
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    
    private void insertarDatosDePrueba() throws SQLException {
        // Insertar broker de prueba
        String sqlBroker = "INSERT INTO brokers (broker_name, contacto, activo) " +
                          "VALUES (?, ?, ?) RETURNING broker_id";
        try (PreparedStatement stmt = connection.prepareStatement(sqlBroker)) {
            stmt.setString(1, "BROKER_INTEGRACION_TEST");
            stmt.setString(2, "integracion@test.com");
            stmt.setBoolean(3, true);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                brokerIdPrueba = rs.getInt(1);
            }
        }
        
        // Insertar formato de prueba
        String sqlFormato = "INSERT INTO broker_formatos (broker_id, version, header_row, descripcion, activo) " +
                           "VALUES (?, ?, ?, ?, ?) RETURNING formato_id";
        try (PreparedStatement stmt = connection.prepareStatement(sqlFormato)) {
            stmt.setInt(1, brokerIdPrueba);
            stmt.setString(2, "1.0");
            stmt.setInt(3, 0); // Header en fila 0
            stmt.setString(4, "Formato integración");
            stmt.setBoolean(5, true);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                formatoIdPrueba = rs.getInt(1);
            }
        }
        
        // Insertar columnas de prueba
        insertarColumnaPrueba("ITEM_NAME", "Nombre", 0, "A", "#FFFFFF", "#000000", false);
        insertarColumnaPrueba("QUANTITY", "Cantidad", 1, "B", "#E8F4F8", "#000000", false);
        insertarColumnaPrueba("UNIT_PRICE", "Precio", 2, "C", "#FFF2CC", "#000000", true);
        
        connection.commit();
    }
    
    private void insertarColumnaPrueba(String campoEstandar, String nombreOriginal, 
                                       int indice, String letra, String colorFondo,
                                       String colorTexto, boolean negrita) throws SQLException {
        String sql = "INSERT INTO formato_columnas " +
                    "(formato_id, campo_estandar, nombre_columna_original, indice_columna, " +
                    "letra_columna, tipo_dato, requerido, color_fondo, color_texto, es_negrita) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, formatoIdPrueba);
            stmt.setString(2, campoEstandar);
            stmt.setString(3, nombreOriginal);
            stmt.setInt(4, indice);
            stmt.setString(5, letra);
            stmt.setString(6, "TEXT");
            stmt.setBoolean(7, true);
            stmt.setString(8, colorFondo);
            stmt.setString(9, colorTexto);
            stmt.setBoolean(10, negrita);
            stmt.executeUpdate();
        }
    }
    
    private void limpiarDatosDePrueba() throws SQLException {
        if (formatoIdPrueba != null) {
            try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM formato_columnas WHERE formato_id = ?")) {
                stmt.setInt(1, formatoIdPrueba);
                stmt.executeUpdate();
            }
            
            try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM broker_formatos WHERE formato_id = ?")) {
                stmt.setInt(1, formatoIdPrueba);
                stmt.executeUpdate();
            }
        }
        
        if (brokerIdPrueba != null) {
            try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM brokers WHERE broker_id = ?")) {
                stmt.setInt(1, brokerIdPrueba);
                stmt.executeUpdate();
            }
        }
        
        connection.commit();
    }
    
    @Test
    @DisplayName("cargarFormatoBroker debe cargar el formato correctamente y establecer formatoActual")
    void cargarFormatoBrokerDebeCargarFormatoCorrectamente() throws Exception {
        // Given
        Broker broker = new Broker();
        broker.setBrokerId(brokerIdPrueba);
        broker.setBrokerName("BROKER_INTEGRACION_TEST");
        
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                // Acceder al método privado usando reflection
                Method metodo = Main.class.getDeclaredMethod("cargarFormatoBroker", Broker.class);
                metodo.setAccessible(true);
                metodo.invoke(mainApp, broker);
                
                // Verificar que formatoActual fue establecido
                Field campoFormatoActual = Main.class.getDeclaredField("formatoActual");
                campoFormatoActual.setAccessible(true);
                BrokerFormato formatoActual = (BrokerFormato) campoFormatoActual.get(mainApp);
                
                // Then
                assertNotNull(formatoActual, "formatoActual debe estar establecido");
                assertEquals(formatoIdPrueba, formatoActual.getFormatoId());
                assertEquals(3, formatoActual.getColumnas().size());
                assertEquals(0, formatoActual.getHeaderRow());
                
                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
                fail("Error al ejecutar cargarFormatoBroker: " + e.getMessage());
            }
        });
        
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }
    
    @Test
    @DisplayName("configurarTablaDinamica debe crear columnas según formatoActual y aplicar estilos")
    void configurarTablaDinamicaDebeCrearColumnasConEstilos() throws Exception {
        // Given - Primero cargar el formato
        BrokerFormato formato = new BrokerFormato();
        formato.setFormatoId(formatoIdPrueba);
        formato.setHeaderRow(0);
        
        List<FormatoColumna> columnas = new ArrayList<>();
        FormatoColumna col1 = new FormatoColumna();
        col1.setCampoEstandar("ITEM_NAME");
        col1.setNombreColumnaOriginal("Nombre");
        col1.setIndiceColumna(0);
        col1.setColorFondo("#FFFFFF");
        col1.setColorTexto("#000000");
        columnas.add(col1);
        
        FormatoColumna col2 = new FormatoColumna();
        col2.setCampoEstandar("QUANTITY");
        col2.setNombreColumnaOriginal("Cantidad");
        col2.setIndiceColumna(1);
        col2.setColorFondo("#E8F4F8");
        col2.setColorTexto("#000000");
        columnas.add(col2);
        
        formato.setColumnas(columnas);
        
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                // Establecer formatoActual usando reflection
                Field campoFormatoActual = Main.class.getDeclaredField("formatoActual");
                campoFormatoActual.setAccessible(true);
                campoFormatoActual.set(mainApp, formato);
                
                // Obtener tablaDinamica
                Field campoTablaDinamica = Main.class.getDeclaredField("tablaDinamica");
                campoTablaDinamica.setAccessible(true);
                TableView<RowData> tablaDinamica = (TableView<RowData>) campoTablaDinamica.get(mainApp);
                
                // When - Llamar al método configurarTablaDinamica
                Method metodo = Main.class.getDeclaredMethod("configurarTablaDinamica");
                metodo.setAccessible(true);
                metodo.invoke(mainApp);
                
                // Then
                ObservableList<TableColumn<RowData, ?>> columnasTabla = tablaDinamica.getColumns();
                assertEquals(2, columnasTabla.size(), "Debe haber 2 columnas en la tabla");
                assertEquals("Nombre", columnasTabla.get(0).getText());
                assertEquals("Cantidad", columnasTabla.get(1).getText());
                
                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
                fail("Error al ejecutar configurarTablaDinamica: " + e.getMessage());
            }
        });
        
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }
    
    @Test
    @DisplayName("leerExcelConFormato debe leer Excel y poblar tablaDinamica con instancias de RowData")
    void leerExcelConFormatoDebeLeerYPoblarTabla() throws Exception {
        // Given - Crear archivo Excel de prueba
        archivoExcelPrueba = crearArchivoExcelPrueba();
        
        // Preparar formato
        BrokerFormato formato = new BrokerFormato();
        formato.setFormatoId(formatoIdPrueba);
        formato.setHeaderRow(0); // Header en fila 0
        
        List<FormatoColumna> columnas = new ArrayList<>();
        FormatoColumna col1 = new FormatoColumna();
        col1.setCampoEstandar("ITEM_NAME");
        col1.setNombreColumnaOriginal("Nombre");
        col1.setIndiceColumna(0);
        columnas.add(col1);
        
        FormatoColumna col2 = new FormatoColumna();
        col2.setCampoEstandar("QUANTITY");
        col2.setNombreColumnaOriginal("Cantidad");
        col2.setIndiceColumna(1);
        columnas.add(col2);
        
        FormatoColumna col3 = new FormatoColumna();
        col3.setCampoEstandar("UNIT_PRICE");
        col3.setNombreColumnaOriginal("Precio");
        col3.setIndiceColumna(2);
        columnas.add(col3);
        
        formato.setColumnas(columnas);
        
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                // Establecer formatoActual
                Field campoFormatoActual = Main.class.getDeclaredField("formatoActual");
                campoFormatoActual.setAccessible(true);
                campoFormatoActual.set(mainApp, formato);
                
                // Obtener tablaDinamica
                Field campoTablaDinamica = Main.class.getDeclaredField("tablaDinamica");
                campoTablaDinamica.setAccessible(true);
                TableView<RowData> tablaDinamica = (TableView<RowData>) campoTablaDinamica.get(mainApp);
                
                // When - Llamar al método leerExcelConFormato
                Method metodo = Main.class.getDeclaredMethod("leerExcelConFormato", File.class);
                metodo.setAccessible(true);
                metodo.invoke(mainApp, archivoExcelPrueba);
                
                // Then
                ObservableList<RowData> items = tablaDinamica.getItems();
                assertNotNull(items);
                assertEquals(2, items.size(), "Debe haber 2 filas de datos");
                
                // Verificar primera fila
                RowData fila1 = items.get(0);
                assertEquals("Aceite Hidráulico", fila1.get("ITEM_NAME"));
                assertEquals("10.0", fila1.get("QUANTITY"));
                assertEquals("15.5", fila1.get("UNIT_PRICE"));
                
                // Verificar segunda fila
                RowData fila2 = items.get(1);
                assertEquals("Filtro de Aire", fila2.get("ITEM_NAME"));
                assertEquals("5.0", fila2.get("QUANTITY"));
                assertEquals("25.0", fila2.get("UNIT_PRICE"));
                
                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
                fail("Error al ejecutar leerExcelConFormato: " + e.getMessage());
            }
        });
        
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }
    
    private File crearArchivoExcelPrueba() throws IOException {
        File archivo = File.createTempFile("test_cotizacion_", ".xlsx");
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Cotizacion");
            
            // Fila 0: Header
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Nombre");
            headerRow.createCell(1).setCellValue("Cantidad");
            headerRow.createCell(2).setCellValue("Precio");
            
            // Fila 1: Datos
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Aceite Hidráulico");
            row1.createCell(1).setCellValue(10);
            row1.createCell(2).setCellValue(15.50);
            
            // Fila 2: Datos
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Filtro de Aire");
            row2.createCell(1).setCellValue(5);
            row2.createCell(2).setCellValue(25.00);
            
            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                workbook.write(fos);
            }
        }
        
        return archivo;
    }
}
