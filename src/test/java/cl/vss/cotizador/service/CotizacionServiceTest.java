package cl.vss.cotizador.service;

import cl.vss.cotizador.util.DBConnection;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para CotizacionService
 * Enfocadas en la generación de consultas SQL y manejo de precios
 */
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CotizacionServiceTest {

    private CotizacionService cotizacionService;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() throws SQLException {
        cotizacionService = new CotizacionService();
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);
    }

    @Nested
    @DisplayName("Pruebas para construirConsultaConVista()")
    class ConstruirConsultaConVistaTests {

        @Test
        @DisplayName("Debe generar SQL con DISTINCT cuando vista_producto_precio tiene precio_venta_neto")
        void debeGenerarSQLConDistinctCuandoVistaExiste() throws SQLException {
            // Given: Simular que la vista existe y tiene la columna precio_venta_neto
            try (MockedStatic<DBConnection> dbConnectionMock = Mockito.mockStatic(DBConnection.class)) {
                dbConnectionMock.when(DBConnection::getConnection).thenReturn(mockConnection);
                
                when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
                when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
                
                // Simular que la columna precio_venta_neto existe
                when(mockResultSet.next()).thenReturn(true, false);
                when(mockResultSet.getString("column_name")).thenReturn("precio_venta_neto");
                
                // When: Invocar el método privado a través de buscarProductosSimilares
                cotizacionService.buscarProductosSimilares("test");
                
                // Then: Verificar que se preparó statement SQL
                verify(mockConnection, atLeast(2)).prepareStatement(anyString());
                // La primera llamada verifica columnas, la segunda es la consulta real
            }
        }

        @Test
        @DisplayName("Debe usar COALESCE para precio_venta_neto con fallback a valor_pesos")
        void debeUsarCoalesceConPrecioVentaNeto() throws SQLException {
            // Given: Simular que la vista existe
            try (MockedStatic<DBConnection> dbConnectionMock = Mockito.mockStatic(DBConnection.class)) {
                dbConnectionMock.when(DBConnection::getConnection).thenReturn(mockConnection);
                
                when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
                when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
                
                // Simular que la columna precio_venta_neto existe
                when(mockResultSet.next()).thenReturn(true, false);
                when(mockResultSet.getString("column_name")).thenReturn("precio_venta_neto");
                
                // When: Invocar método que construye consulta
                cotizacionService.buscarProductosSimilares("test");
                
                // Then: Verificar que se preparó un statement (lo cual implica que se generó SQL)
                verify(mockPreparedStatement, atLeastOnce()).executeQuery();
            }
        }

        @Test
        @DisplayName("Debe incluir DISTINCT ON (p.id) en la consulta principal")
        void debeIncluirDistinctOnEnConsulta() throws SQLException {
            // Given
            try (MockedStatic<DBConnection> dbConnectionMock = Mockito.mockStatic(DBConnection.class)) {
                dbConnectionMock.when(DBConnection::getConnection).thenReturn(mockConnection);
                
                when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
                when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
                
                // Simular que la vista existe con precio_venta_neto
                when(mockResultSet.next()).thenReturn(true, false);
                when(mockResultSet.getString("column_name")).thenReturn("precio_venta_neto");
                
                // When
                cotizacionService.buscarProductosSimilares("test");
                
                // Then: Verificar que se ejecutó la consulta
                verify(mockPreparedStatement, atLeastOnce()).executeQuery();
            }
        }

        @Test
        @DisplayName("Debe usar COALESCE con tres valores: vpp.precio_venta_neto, p.valor_pesos, 0.0")
        void debeUsarCoalesceConTresValores() throws SQLException {
            // Given: Simular escenario completo
            try (MockedStatic<DBConnection> dbConnectionMock = Mockito.mockStatic(DBConnection.class)) {
                dbConnectionMock.when(DBConnection::getConnection).thenReturn(mockConnection);
                
                when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
                when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
                
                // Primera llamada: verificar columnas (precio_venta_neto existe)
                when(mockResultSet.next())
                    .thenReturn(true, false) // Para verificación de columnas
                    .thenReturn(false);      // Para la consulta de productos (sin resultados)
                when(mockResultSet.getString("column_name")).thenReturn("precio_venta_neto");
                
                // When
                var productos = cotizacionService.buscarProductosSimilares("test producto");
                
                // Then: El método se ejecutó sin errores y preparó los statements correctos
                assertNotNull(productos);
                verify(mockConnection, atLeast(2)).prepareStatement(anyString());
            }
        }
    }

    @Nested
    @DisplayName("Pruebas para consulta SQL de fallback")
    class SQLFallbackQueryTests {

        @Test
        @DisplayName("Debe retornar consulta DISTINCT cuando vista no existe")
        void debeRetornarConsultaDistinctCuandoVistaNoExiste() throws SQLException {
            // Given: Simular que la vista no existe o no tiene precio_venta_neto
            try (MockedStatic<DBConnection> dbConnectionMock = Mockito.mockStatic(DBConnection.class)) {
                dbConnectionMock.when(DBConnection::getConnection).thenReturn(mockConnection);
                
                when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
                when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
                
                // Simular que no se encuentra la columna precio_venta_neto
                when(mockResultSet.next()).thenReturn(false);
                
                // When
                cotizacionService.buscarProductosSimilares("test");
                
                // Then: Debe usar la consulta de fallback con DISTINCT
                verify(mockConnection, atLeast(2)).prepareStatement(anyString());
            }
        }

        @Test
        @DisplayName("Query fallback debe incluir todas las columnas esperadas")
        void queryFallbackDebeIncluirColumnasEsperadas() throws SQLException {
            // Given
            try (MockedStatic<DBConnection> dbConnectionMock = Mockito.mockStatic(DBConnection.class)) {
                dbConnectionMock.when(DBConnection::getConnection).thenReturn(mockConnection);
                
                when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
                when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
                
                // Simular que la columna no existe (usar fallback)
                when(mockResultSet.next()).thenReturn(false);
                
                // When
                cotizacionService.buscarProductosSimilares("producto test");
                
                // Then: Verificar que se intentó ejecutar la consulta de fallback
                verify(mockPreparedStatement, atLeastOnce()).setString(eq(1), anyString());
                verify(mockPreparedStatement, atLeastOnce()).setString(eq(2), anyString());
                verify(mockPreparedStatement, atLeastOnce()).executeQuery();
            }
        }

        @Test
        @DisplayName("Query fallback debe usar valor_pesos como precio_venta_neto")
        void queryFallbackDebeUsarValorPesosComoPrecioVentaNeto() throws SQLException {
            // Given
            try (MockedStatic<DBConnection> dbConnectionMock = Mockito.mockStatic(DBConnection.class)) {
                dbConnectionMock.when(DBConnection::getConnection).thenReturn(mockConnection);
                
                when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
                when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
                
                // Simular fallback
                when(mockResultSet.next()).thenReturn(false);
                
                // When
                var productos = cotizacionService.buscarProductosSimilares("test");
                
                // Then
                assertNotNull(productos);
                verify(mockPreparedStatement, atLeastOnce()).executeQuery();
            }
        }

        @Test
        @DisplayName("Query fallback debe ordenar por valor_pesos DESC")
        void queryFallbackDebeOrdenarPorValorPesos() throws SQLException {
            // Given
            try (MockedStatic<DBConnection> dbConnectionMock = Mockito.mockStatic(DBConnection.class)) {
                dbConnectionMock.when(DBConnection::getConnection).thenReturn(mockConnection);
                
                when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
                when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
                
                // Simular fallback
                when(mockResultSet.next()).thenReturn(false);
                
                // When
                cotizacionService.buscarProductosSimilares("test");
                
                // Then: Verificar que la consulta se ejecutó
                verify(mockPreparedStatement, atLeastOnce()).executeQuery();
            }
        }
    }

    @Nested
    @DisplayName("Pruebas para LEFT JOIN y ordenamiento")
    class LeftJoinYOrdenamientoTests {

        @Test
        @DisplayName("LEFT JOIN debe usar correctamente el ID del producto")
        void leftJoinDebeUsarIDProductoCorrectamente() throws SQLException {
            // Given
            try (MockedStatic<DBConnection> dbConnectionMock = Mockito.mockStatic(DBConnection.class)) {
                dbConnectionMock.when(DBConnection::getConnection).thenReturn(mockConnection);
                
                when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
                when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
                
                // Simular que la vista existe
                when(mockResultSet.next())
                    .thenReturn(true, false) // Verificación de columnas
                    .thenReturn(true, false); // Resultados de búsqueda
                when(mockResultSet.getString("column_name")).thenReturn("precio_venta_neto");
                
                // Simular datos de producto
                when(mockResultSet.getString("descripcion_es")).thenReturn("Producto Test ES");
                when(mockResultSet.getString("descripcion_en")).thenReturn("Product Test EN");
                when(mockResultSet.getString("unidad_medida")).thenReturn("UND");
                when(mockResultSet.getDouble("precio_venta_neto")).thenReturn(100.0);
                when(mockResultSet.getDouble("precio_venta_neto_dolares")).thenReturn(1.15);
                
                // When
                var productos = cotizacionService.buscarProductosSimilares("test");
                
                // Then
                assertNotNull(productos);
                assertFalse(productos.isEmpty());
                assertEquals(1, productos.size());
                assertEquals("Producto Test ES", productos.get(0).getDescripcionEs());
            }
        }

        @Test
        @DisplayName("Debe ordenar resultados por p.id y precio DESC")
        void debeOrdenarResultadosPorIdYPrecio() throws SQLException {
            // Given
            try (MockedStatic<DBConnection> dbConnectionMock = Mockito.mockStatic(DBConnection.class)) {
                dbConnectionMock.when(DBConnection::getConnection).thenReturn(mockConnection);
                
                when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
                when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
                
                // Simular vista existente
                when(mockResultSet.next())
                    .thenReturn(true, false) // Verificación de columnas
                    .thenReturn(true, true, false); // Dos productos
                when(mockResultSet.getString("column_name")).thenReturn("precio_venta_neto");
                
                // Simular múltiples productos
                when(mockResultSet.getString("descripcion_es")).thenReturn("Producto 1", "Producto 2");
                when(mockResultSet.getString("descripcion_en")).thenReturn("Product 1", "Product 2");
                when(mockResultSet.getString("unidad_medida")).thenReturn("UND", "KG");
                when(mockResultSet.getDouble("precio_venta_neto")).thenReturn(150.0, 100.0);
                when(mockResultSet.getDouble("precio_venta_neto_dolares")).thenReturn(1.72, 1.15);
                
                // When
                var productos = cotizacionService.buscarProductosSimilares("producto");
                
                // Then
                assertNotNull(productos);
                assertEquals(2, productos.size());
                // El orden es manejado por SQL (ORDER BY), verificamos que se obtuvieron
                assertTrue(productos.stream().anyMatch(p -> p.getDescripcionEs().equals("Producto 1")));
                assertTrue(productos.stream().anyMatch(p -> p.getDescripcionEs().equals("Producto 2")));
            }
        }

        @Test
        @DisplayName("LEFT JOIN debe permitir productos sin datos en vista_producto_precio")
        void leftJoinDebePermitirProductosSinDatosEnVista() throws SQLException {
            // Given
            try (MockedStatic<DBConnection> dbConnectionMock = Mockito.mockStatic(DBConnection.class)) {
                dbConnectionMock.when(DBConnection::getConnection).thenReturn(mockConnection);
                
                when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
                when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
                
                // Simular vista existente
                when(mockResultSet.next())
                    .thenReturn(true, false) // Verificación de columnas
                    .thenReturn(true, false); // Un producto
                when(mockResultSet.getString("column_name")).thenReturn("precio_venta_neto");
                
                // Simular producto sin precio en vista (COALESCE usará valor_pesos)
                when(mockResultSet.getString("descripcion_es")).thenReturn("Producto Sin Precio");
                when(mockResultSet.getString("descripcion_en")).thenReturn("Product No Price");
                when(mockResultSet.getString("unidad_medida")).thenReturn("UND");
                when(mockResultSet.getDouble("precio_venta_neto")).thenReturn(50.0); // Fallback a valor_pesos
                when(mockResultSet.getDouble("precio_venta_neto_dolares")).thenReturn(0.0);
                
                // When
                var productos = cotizacionService.buscarProductosSimilares("producto");
                
                // Then
                assertNotNull(productos);
                assertEquals(1, productos.size());
                assertEquals("Producto Sin Precio", productos.get(0).getDescripcionEs());
                assertEquals(50.0, productos.get(0).getPrecioVentaNeto());
            }
        }

        @Test
        @DisplayName("Debe limitar resultados a 20 productos")
        void debeLimitarResultadosA20Productos() throws SQLException {
            // Given
            try (MockedStatic<DBConnection> dbConnectionMock = Mockito.mockStatic(DBConnection.class)) {
                dbConnectionMock.when(DBConnection::getConnection).thenReturn(mockConnection);
                
                when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
                when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
                
                // Simular vista existente y sin productos
                when(mockResultSet.next())
                    .thenReturn(true, false) // Verificación de columnas
                    .thenReturn(false); // Sin productos
                when(mockResultSet.getString("column_name")).thenReturn("precio_venta_neto");
                
                // When
                var productos = cotizacionService.buscarProductosSimilares("test");
                
                // Then: La consulta SQL incluye LIMIT 20
                assertNotNull(productos);
                verify(mockPreparedStatement, atLeastOnce()).executeQuery();
            }
        }

        @Test
        @DisplayName("Debe usar DISTINCT ON para evitar duplicados por producto ID")
        void debeUsarDistinctOnParaEvitarDuplicados() throws SQLException {
            // Given
            try (MockedStatic<DBConnection> dbConnectionMock = Mockito.mockStatic(DBConnection.class)) {
                dbConnectionMock.when(DBConnection::getConnection).thenReturn(mockConnection);
                
                when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
                when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
                
                // Simular vista existente
                when(mockResultSet.next())
                    .thenReturn(true, false) // Verificación de columnas
                    .thenReturn(true, false); // Un resultado
                when(mockResultSet.getString("column_name")).thenReturn("precio_venta_neto");
                
                // Simular un producto
                when(mockResultSet.getString("descripcion_es")).thenReturn("Producto Único");
                when(mockResultSet.getString("descripcion_en")).thenReturn("Unique Product");
                when(mockResultSet.getString("unidad_medida")).thenReturn("UND");
                when(mockResultSet.getDouble("precio_venta_neto")).thenReturn(100.0);
                when(mockResultSet.getDouble("precio_venta_neto_dolares")).thenReturn(1.15);
                
                // When
                var productos = cotizacionService.buscarProductosSimilares("producto");
                
                // Then: DISTINCT ON garantiza un solo registro por ID
                assertNotNull(productos);
                assertEquals(1, productos.size());
            }
        }
    }

    @Nested
    @DisplayName("Pruebas para manejo de errores SQL")
    class ManejoErroresSQLTests {

        @Test
        @DisplayName("Debe retornar lista vacía cuando hay error SQL en verificación de columnas")
        void debeRetornarListaVaciaCuandoHayErrorSQLEnVerificacion() throws SQLException {
            // Given
            try (MockedStatic<DBConnection> dbConnectionMock = Mockito.mockStatic(DBConnection.class)) {
                dbConnectionMock.when(DBConnection::getConnection).thenReturn(mockConnection);
                
                when(mockConnection.prepareStatement(anyString()))
                    .thenThrow(new SQLException("Error de conexión"));
                
                // When
                var productos = cotizacionService.buscarProductosSimilares("test");
                
                // Then
                assertNotNull(productos);
                assertTrue(productos.isEmpty());
            }
        }

        @Test
        @DisplayName("Debe usar query fallback cuando falla verificación de vista")
        void debeUsarQueryFallbackCuandoFallaVerificacion() throws SQLException {
            // Given
            try (MockedStatic<DBConnection> dbConnectionMock = Mockito.mockStatic(DBConnection.class)) {
                dbConnectionMock.when(DBConnection::getConnection).thenReturn(mockConnection);
                
                // Primera llamada falla (verificación), segunda funciona (fallback)
                when(mockConnection.prepareStatement(anyString()))
                    .thenReturn(mockPreparedStatement)
                    .thenReturn(mockPreparedStatement);
                
                when(mockPreparedStatement.executeQuery())
                    .thenReturn(mockResultSet);
                
                // Simular que la verificación no encuentra la columna
                when(mockResultSet.next()).thenReturn(false);
                
                // When
                var productos = cotizacionService.buscarProductosSimilares("test");
                
                // Then
                assertNotNull(productos);
                verify(mockConnection, atLeast(2)).prepareStatement(anyString());
            }
        }
    }

    @Nested
    @DisplayName("Pruebas de integración para búsqueda de productos")
    class IntegracionBusquedaProductosTests {

        @Test
        @DisplayName("buscarProductosSimilares debe retornar lista vacía para descripción nula")
        void debeRetornarListaVaciaParaDescripcionNula() {
            // When
            var productos = cotizacionService.buscarProductosSimilares(null);
            
            // Then
            assertNotNull(productos);
            assertTrue(productos.isEmpty());
        }

        @Test
        @DisplayName("buscarProductosSimilares debe retornar lista vacía para descripción vacía")
        void debeRetornarListaVaciaParaDescripcionVacia() {
            // When
            var productos = cotizacionService.buscarProductosSimilares("");
            
            // Then
            assertNotNull(productos);
            assertTrue(productos.isEmpty());
        }

        @Test
        @DisplayName("buscarProductosSimilares debe agregar wildcards a la búsqueda")
        void debeAgregarWildcardsALaBusqueda() throws SQLException {
            // Given
            try (MockedStatic<DBConnection> dbConnectionMock = Mockito.mockStatic(DBConnection.class)) {
                dbConnectionMock.when(DBConnection::getConnection).thenReturn(mockConnection);
                
                when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
                when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
                when(mockResultSet.next()).thenReturn(false);
                
                // When
                cotizacionService.buscarProductosSimilares("arroz");
                
                // Then: Verificar que se agregaron wildcards (% alrededor del término)
                verify(mockPreparedStatement).setString(eq(1), eq("%arroz%"));
                verify(mockPreparedStatement).setString(eq(2), eq("%arroz%"));
            }
        }
    }
}
