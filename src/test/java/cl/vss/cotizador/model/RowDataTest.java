package cl.vss.cotizador.model;

import javafx.beans.property.StringProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para la clase RowData
 */
class RowDataTest {
    
    private RowData rowData;
    
    @BeforeEach
    void setUp() {
        rowData = new RowData();
    }
    
    @Test
    @DisplayName("Debe establecer y obtener un valor correctamente")
    void debeEstablecerYObtenerValor() {
        // Given
        String clave = "ITEM_NAME";
        String valor = "Producto de prueba";
        
        // When
        rowData.set(clave, valor);
        String resultado = rowData.get(clave);
        
        // Then
        assertEquals(valor, resultado);
    }
    
    @Test
    @DisplayName("Debe actualizar un valor existente")
    void debeActualizarValorExistente() {
        // Given
        String clave = "QUANTITY";
        String valorInicial = "10";
        String valorNuevo = "20";
        
        // When
        rowData.set(clave, valorInicial);
        rowData.set(clave, valorNuevo);
        String resultado = rowData.get(clave);
        
        // Then
        assertEquals(valorNuevo, resultado);
    }
    
    @Test
    @DisplayName("Debe devolver cadena vacía para clave inexistente")
    void debeDevolverCadenaVaciaParaClaveInexistente() {
        // When
        String resultado = rowData.get("CLAVE_INEXISTENTE");
        
        // Then
        assertEquals("", resultado);
    }
    
    @Test
    @DisplayName("Debe obtener StringProperty para una clave existente")
    void debeObtenerPropertyParaClaveExistente() {
        // Given
        String clave = "DESCRIPTION";
        String valor = "Descripción de prueba";
        rowData.set(clave, valor);
        
        // When
        StringProperty property = rowData.getProperty(clave);
        
        // Then
        assertNotNull(property);
        assertEquals(valor, property.get());
    }
    
    @Test
    @DisplayName("Debe crear StringProperty vacía para clave nueva al llamar getProperty")
    void debeCrearPropertyVaciaParaClaveLlaveNueva() {
        // Given
        String clave = "NUEVA_CLAVE";
        
        // When
        StringProperty property = rowData.getProperty(clave);
        
        // Then
        assertNotNull(property);
        assertEquals("", property.get());
        assertTrue(rowData.hasKey(clave));
    }
    
    @Test
    @DisplayName("Debe mantener sincronización entre Property y get/set")
    void debeMantenerSincronizacionEntrePropertyYGetSet() {
        // Given
        String clave = "UNIT_PRICE";
        String valorInicial = "100.50";
        rowData.set(clave, valorInicial);
        
        // When
        StringProperty property = rowData.getProperty(clave);
        property.set("200.75");
        String resultado = rowData.get(clave);
        
        // Then
        assertEquals("200.75", resultado);
    }
    
    @Test
    @DisplayName("Debe verificar correctamente si existe una clave")
    void debeVerificarExistenciaDeClave() {
        // Given
        String claveExistente = "BRAND";
        String claveInexistente = "CLAVE_NO_EXISTE";
        rowData.set(claveExistente, "Marca de prueba");
        
        // When & Then
        assertTrue(rowData.hasKey(claveExistente));
        assertFalse(rowData.hasKey(claveInexistente));
    }
    
    @Test
    @DisplayName("Debe devolver todas las claves almacenadas")
    void debeDevolverTodasLasClaves() {
        // Given
        rowData.set("ITEM_NAME", "Item");
        rowData.set("QUANTITY", "5");
        rowData.set("UNIT_PRICE", "100");
        
        // When
        var claves = rowData.getKeys();
        
        // Then
        assertEquals(3, claves.size());
        assertTrue(claves.contains("ITEM_NAME"));
        assertTrue(claves.contains("QUANTITY"));
        assertTrue(claves.contains("UNIT_PRICE"));
    }
    
    @Test
    @DisplayName("Debe manejar múltiples campos como en un formato real de broker")
    void debeManejarMultiplesCamposComoFormatoReal() {
        // Given - Simular campos típicos de un formato de broker
        rowData.set("ITEM_NAME", "Aceite Hidráulico");
        rowData.set("ITEM_CODE", "AH-001");
        rowData.set("CATEGORY", "Lubricantes");
        rowData.set("DESCRIPTION", "Aceite hidráulico ISO 68");
        rowData.set("QUANTITY", "10");
        rowData.set("UOM", "Litros");
        rowData.set("UNIT_PRICE", "15.50");
        rowData.set("TOTAL", "155.00");
        
        // When & Then
        assertEquals("Aceite Hidráulico", rowData.get("ITEM_NAME"));
        assertEquals("AH-001", rowData.get("ITEM_CODE"));
        assertEquals("Lubricantes", rowData.get("CATEGORY"));
        assertEquals("Aceite hidráulico ISO 68", rowData.get("DESCRIPTION"));
        assertEquals("10", rowData.get("QUANTITY"));
        assertEquals("Litros", rowData.get("UOM"));
        assertEquals("15.50", rowData.get("UNIT_PRICE"));
        assertEquals("155.00", rowData.get("TOTAL"));
        assertEquals(8, rowData.getKeys().size());
    }
    
    @Test
    @DisplayName("Debe manejar valores nulos y vacíos correctamente")
    void debeManejarValoresNulosYVacios() {
        // Given
        String claveVacia = "CAMPO_VACIO";
        String claveNull = "CAMPO_NULL";
        
        // When
        rowData.set(claveVacia, "");
        rowData.set(claveNull, null);
        
        // Then
        assertEquals("", rowData.get(claveVacia));
        // El comportamiento con null depende de SimpleStringProperty
        // que puede aceptar null
        assertTrue(rowData.hasKey(claveVacia));
        assertTrue(rowData.hasKey(claveNull));
    }
    
    @Test
    @DisplayName("Debe generar toString con información del mapa de datos")
    void debeGenerarToStringConInformacion() {
        // Given
        rowData.set("ITEM_NAME", "Test Item");
        rowData.set("QUANTITY", "5");
        
        // When
        String resultado = rowData.toString();
        
        // Then
        assertNotNull(resultado);
        assertTrue(resultado.contains("RowData"));
    }
}
