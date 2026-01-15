package cl.vss.cotizador.service;

import cl.vss.cotizador.model.BrokerFormato;
import cl.vss.cotizador.model.FormatoColumna;
import cl.vss.cotizador.util.DBConnection;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para FormatoDAO
 * NOTA: Estos tests requieren una base de datos PostgreSQL activa con datos de prueba
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FormatoDAOTest {
    
    private Connection connection;
    private FormatoDAO formatoDAO;
    private Integer brokerIdPrueba;
    private Integer formatoIdPrueba;
    
    @BeforeAll
    void setUpDatabase() throws SQLException {
        // Obtener conexión a la base de datos
        connection = DBConnection.getConnection();
        formatoDAO = new FormatoDAO(connection);
        
        // Insertar datos de prueba
        insertarDatosDePrueba();
    }
    
    @AfterAll
    void tearDownDatabase() throws SQLException {
        // Limpiar datos de prueba
        limpiarDatosDePrueba();
        
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    
    private void insertarDatosDePrueba() throws SQLException {
        // Insertar broker de prueba
        String sqlBroker = "INSERT INTO brokers (broker_name, contacto, activo) " +
                          "VALUES (?, ?, ?) RETURNING broker_id";
        try (PreparedStatement stmt = connection.prepareStatement(sqlBroker)) {
            stmt.setString(1, "BROKER_TEST_UNIT");
            stmt.setString(2, "test@broker.com");
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
            stmt.setInt(3, 10);
            stmt.setString(4, "Formato de prueba");
            stmt.setBoolean(5, true);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                formatoIdPrueba = rs.getInt(1);
            }
        }
        
        // Insertar columnas de prueba
        String sqlColumna = "INSERT INTO formato_columnas " +
                           "(formato_id, campo_estandar, nombre_columna_original, indice_columna, " +
                           "letra_columna, tipo_dato, requerido, color_fondo, color_texto, es_negrita) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        // Columna 1: ITEM_NAME
        try (PreparedStatement stmt = connection.prepareStatement(sqlColumna)) {
            stmt.setInt(1, formatoIdPrueba);
            stmt.setString(2, "ITEM_NAME");
            stmt.setString(3, "Nombre del Item");
            stmt.setInt(4, 0);
            stmt.setString(5, "A");
            stmt.setString(6, "TEXT");
            stmt.setBoolean(7, true);
            stmt.setString(8, "#FFFFFF");
            stmt.setString(9, "#000000");
            stmt.setBoolean(10, false);
            stmt.executeUpdate();
        }
        
        // Columna 2: QUANTITY
        try (PreparedStatement stmt = connection.prepareStatement(sqlColumna)) {
            stmt.setInt(1, formatoIdPrueba);
            stmt.setString(2, "QUANTITY");
            stmt.setString(3, "Cantidad");
            stmt.setInt(4, 1);
            stmt.setString(5, "B");
            stmt.setString(6, "NUMBER");
            stmt.setBoolean(7, true);
            stmt.setString(8, "#E8F4F8");
            stmt.setString(9, "#000000");
            stmt.setBoolean(10, false);
            stmt.executeUpdate();
        }
        
        // Columna 3: UNIT_PRICE
        try (PreparedStatement stmt = connection.prepareStatement(sqlColumna)) {
            stmt.setInt(1, formatoIdPrueba);
            stmt.setString(2, "UNIT_PRICE");
            stmt.setString(3, "Precio Unitario");
            stmt.setInt(4, 2);
            stmt.setString(5, "C");
            stmt.setString(6, "CURRENCY");
            stmt.setBoolean(7, true);
            stmt.setString(8, "#FFF2CC");
            stmt.setString(9, "#000000");
            stmt.setBoolean(10, true);
            stmt.executeUpdate();
        }
        
        connection.commit();
    }
    
    private void limpiarDatosDePrueba() throws SQLException {
        if (formatoIdPrueba != null) {
            String sqlColumnas = "DELETE FROM formato_columnas WHERE formato_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sqlColumnas)) {
                stmt.setInt(1, formatoIdPrueba);
                stmt.executeUpdate();
            }
            
            String sqlFormato = "DELETE FROM broker_formatos WHERE formato_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sqlFormato)) {
                stmt.setInt(1, formatoIdPrueba);
                stmt.executeUpdate();
            }
        }
        
        if (brokerIdPrueba != null) {
            String sqlBroker = "DELETE FROM brokers WHERE broker_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sqlBroker)) {
                stmt.setInt(1, brokerIdPrueba);
                stmt.executeUpdate();
            }
        }
        
        connection.commit();
    }
    
    @Test
    @DisplayName("obtenerFormatoPorBrokerId debe retornar un BrokerFormato válido")
    void debeRetornarBrokerFormatoValido() {
        // When
        BrokerFormato formato = formatoDAO.obtenerFormatoPorBrokerId(brokerIdPrueba);
        
        // Then
        assertNotNull(formato, "El formato no debe ser null");
        assertEquals(formatoIdPrueba, formato.getFormatoId());
        assertEquals(brokerIdPrueba, formato.getBrokerId());
        assertEquals("BROKER_TEST_UNIT", formato.getBrokerName());
        assertEquals("1.0", formato.getVersion());
        assertEquals(10, formato.getHeaderRow());
        assertEquals("Formato de prueba", formato.getDescripcion());
        assertTrue(formato.getActivo());
    }
    
    @Test
    @DisplayName("obtenerFormatoPorBrokerId debe cargar las columnas correctamente")
    void debeCargarColumnasCorrectamente() {
        // When
        BrokerFormato formato = formatoDAO.obtenerFormatoPorBrokerId(brokerIdPrueba);
        
        // Then
        assertNotNull(formato);
        List<FormatoColumna> columnas = formato.getColumnas();
        assertNotNull(columnas, "La lista de columnas no debe ser null");
        assertEquals(3, columnas.size(), "Debe tener 3 columnas");
    }
    
    @Test
    @DisplayName("Las columnas deben estar ordenadas por índice")
    void lasColumnasDebenEstarOrdenadasPorIndice() {
        // When
        BrokerFormato formato = formatoDAO.obtenerFormatoPorBrokerId(brokerIdPrueba);
        List<FormatoColumna> columnas = formato.getColumnas();
        
        // Then
        assertEquals("ITEM_NAME", columnas.get(0).getCampoEstandar());
        assertEquals(0, columnas.get(0).getIndiceColumna());
        
        assertEquals("QUANTITY", columnas.get(1).getCampoEstandar());
        assertEquals(1, columnas.get(1).getIndiceColumna());
        
        assertEquals("UNIT_PRICE", columnas.get(2).getCampoEstandar());
        assertEquals(2, columnas.get(2).getIndiceColumna());
    }
    
    @Test
    @DisplayName("Las columnas deben tener todos los atributos cargados")
    void lasColumnasDebenTenerTodosLosAtributos() {
        // When
        BrokerFormato formato = formatoDAO.obtenerFormatoPorBrokerId(brokerIdPrueba);
        FormatoColumna primeraColumna = formato.getColumnas().get(0);
        
        // Then
        assertNotNull(primeraColumna.getColumnaId());
        assertEquals(formatoIdPrueba, primeraColumna.getFormatoId());
        assertEquals("ITEM_NAME", primeraColumna.getCampoEstandar());
        assertEquals("Nombre del Item", primeraColumna.getNombreColumnaOriginal());
        assertEquals(0, primeraColumna.getIndiceColumna());
        assertEquals("A", primeraColumna.getLetraColumna());
        assertEquals("TEXT", primeraColumna.getTipoDato());
        assertTrue(primeraColumna.getRequerido());
        assertEquals("#FFFFFF", primeraColumna.getColorFondo());
        assertEquals("#000000", primeraColumna.getColorTexto());
        assertFalse(primeraColumna.getEsNegrita());
    }
    
    @Test
    @DisplayName("Las columnas deben cargar estilos correctamente")
    void lasColumnasDebenCargarEstilosCorrectamente() {
        // When
        BrokerFormato formato = formatoDAO.obtenerFormatoPorBrokerId(brokerIdPrueba);
        FormatoColumna columnaConEstilo = formato.getColumnas().get(2); // UNIT_PRICE
        
        // Then
        assertEquals("#FFF2CC", columnaConEstilo.getColorFondo());
        assertEquals("#000000", columnaConEstilo.getColorTexto());
        assertTrue(columnaConEstilo.getEsNegrita());
    }
    
    @Test
    @DisplayName("Debe retornar null para broker inexistente")
    void debeRetornarNullParaBrokerInexistente() {
        // Given
        Integer brokerIdInexistente = 999999;
        
        // When
        BrokerFormato formato = formatoDAO.obtenerFormatoPorBrokerId(brokerIdInexistente);
        
        // Then
        assertNull(formato, "Debe retornar null para un broker que no existe");
    }
    
    @Test
    @DisplayName("listarFormatosActivos debe retornar lista de formatos")
    void debeListarFormatosActivos() {
        // When
        List<BrokerFormato> formatos = formatoDAO.listarFormatosActivos();
        
        // Then
        assertNotNull(formatos);
        assertTrue(formatos.size() > 0, "Debe haber al menos un formato activo");
        
        // Verificar que nuestro formato de prueba está en la lista
        boolean formatoPruebaEncontrado = formatos.stream()
            .anyMatch(f -> f.getFormatoId().equals(formatoIdPrueba));
        assertTrue(formatoPruebaEncontrado, "El formato de prueba debe estar en la lista");
    }
    
    @Test
    @DisplayName("obtenerColumnasPorFormatoId debe retornar columnas ordenadas")
    void debeObtenerColumnasPorFormatoId() {
        // When
        List<FormatoColumna> columnas = formatoDAO.obtenerColumnasPorFormatoId(formatoIdPrueba);
        
        // Then
        assertNotNull(columnas);
        assertEquals(3, columnas.size());
        
        // Verificar orden
        for (int i = 0; i < columnas.size(); i++) {
            assertEquals(i, columnas.get(i).getIndiceColumna());
        }
    }
}
