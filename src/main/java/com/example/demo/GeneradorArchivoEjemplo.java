package com.example.demo;

import com.example.service.ExportacionService;
import com.example.model.*;
import java.time.LocalDateTime;

/**
 * Generador de archivo Excel de ejemplo para probar la funcionalidad de importación
 */
public class GeneradorArchivoEjemplo {
    
    public static void main(String[] args) {
        System.out.println("📄 Generando archivo Excel de ejemplo...");
        
        try {
            // Crear cotizaciones de ejemplo
            Cliente cliente1 = new Cliente("Empresa Demo S.A.", "demo@empresa.com", "+56 9 1234 5678", "Av. Ejemplo 123");
            cliente1.setCiudad("Santiago");
            cliente1.setPais("Chile");
            
            Cliente cliente2 = new Cliente("Cliente Test Ltda.", "test@cliente.com", "+56 9 8765 4321", "Calle Test 456");
            cliente2.setCiudad("Valparaíso");
            cliente2.setPais("Chile");
            
            // Cotización 1
            Cotizacion cotizacion1 = new Cotizacion(cliente1);
            cotizacion1.setNumeroCotizacion("COT-DEMO-001");
            cotizacion1.setFechaCreacion(LocalDateTime.now().minusDays(5));
            cotizacion1.setFechaVencimiento(LocalDateTime.now().plusDays(25));
            cotizacion1.setEstado("PENDIENTE");
            cotizacion1.setNotas("Cotización de ejemplo para pruebas");
            cotizacion1.setCondiciones("Pago a 30 días");
            
            ItemCotizacion item1 = new ItemCotizacion("DEMO001", "Producto Demo 1", 100000, 2, "unidad", "Electrónicos");
            ItemCotizacion item2 = new ItemCotizacion("DEMO002", "Producto Demo 2", 50000, 3, "unidad", "Accesorios");
            
            cotizacion1.agregarItem(item1);
            cotizacion1.agregarItem(item2);
            cotizacion1.setDescuento(10000);
            cotizacion1.setImpuestos(cotizacion1.getSubtotal() * 0.19);
            
            // Cotización 2
            Cotizacion cotizacion2 = new Cotizacion(cliente2);
            cotizacion2.setNumeroCotizacion("COT-DEMO-002");
            cotizacion2.setFechaCreacion(LocalDateTime.now().minusDays(2));
            cotizacion2.setFechaVencimiento(LocalDateTime.now().plusDays(28));
            cotizacion2.setEstado("APROBADA");
            cotizacion2.setNotas("Cotización aprobada para importación");
            cotizacion2.setCondiciones("Pago contado");
            
            ItemCotizacion item3 = new ItemCotizacion("DEMO003", "Servicio Demo", 200000, 1, "servicio", "Servicios");
            
            cotizacion2.agregarItem(item3);
            cotizacion2.setDescuento(20000);
            cotizacion2.setImpuestos(cotizacion2.getSubtotal() * 0.19);
            
            // Exportar a Excel
            ExportacionService exportService = ExportacionService.getInstance();
            
            // Crear lista de cotizaciones
            java.util.List<Cotizacion> cotizaciones = java.util.Arrays.asList(cotizacion1, cotizacion2);
            
            // Exportar archivo de ejemplo
            String archivoEjemplo = "cotizaciones_ejemplo_para_importar.xlsx";
            exportService.exportarCotizacionesAExcel(cotizaciones, archivoEjemplo);
            
            System.out.println("✅ Archivo generado exitosamente: " + archivoEjemplo);
            System.out.println("📋 Contiene " + cotizaciones.size() + " cotizaciones de ejemplo");
            System.out.println("🚀 Ahora puedes usar este archivo para probar la funcionalidad de importación en JavaFX");
            
        } catch (Exception e) {
            System.out.println("❌ Error al generar archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
