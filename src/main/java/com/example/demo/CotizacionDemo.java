package com.example.demo;

import com.example.model.*;
import com.example.service.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Demostración del modelo genérico de cotizaciones
 * Muestra cómo usar las clases y servicios creados
 */
public class CotizacionDemo {
    
    public static void main(String[] args) {
        System.out.println("🚀 Demostración del Modelo Genérico de Cotizaciones");
        System.out.println("=====================================================");
        
        // Obtener servicios
        CotizacionService cotizacionService = CotizacionService.getInstance();
        ExportacionService exportacionService = ExportacionService.getInstance();
        
        // Mostrar cotizaciones existentes
        System.out.println("\n📋 Cotizaciones existentes:");
        List<Cotizacion> cotizaciones = cotizacionService.obtenerTodasLasCotizaciones();
        for (Cotizacion cotizacion : cotizaciones) {
            System.out.println("  - " + cotizacion);
        }
        
        // Crear nueva cotización
        System.out.println("\n➕ Creando nueva cotización...");
        Cliente nuevoCliente = new Cliente("Empresa XYZ Ltda.", "ventas@empresaxyz.com", 
                                          "+56 9 5555 1234", "Av. Industrial 789");
        nuevoCliente.setCiudad("Concepción");
        nuevoCliente.setPais("Chile");
        nuevoCliente.setTipoDocumento("RUT");
        nuevoCliente.setNumeroDocumento("11.222.333-4");
        
        Cotizacion nuevaCotizacion = new Cotizacion(nuevoCliente);
        nuevaCotizacion.setFechaVencimiento(LocalDateTime.now().plusDays(15));
        nuevaCotizacion.setEstado("PENDIENTE");
        nuevaCotizacion.setNotas("Cotización para equipos de cómputo");
        nuevaCotizacion.setCondiciones("Pago contado con 5% descuento");
        
        // Agregar items
        ItemCotizacion item1 = new ItemCotizacion("PC001", "Computador Desktop Intel i7", 650000, 1, "unidad", "Electrónicos");
        ItemCotizacion item2 = new ItemCotizacion("IMP001", "Impresora Multifuncional HP", 120000, 2, "unidad", "Oficina");
        ItemCotizacion item3 = new ItemCotizacion("RED001", "Router WiFi 6", 85000, 1, "unidad", "Redes");
        
        nuevaCotizacion.agregarItem(item1);
        nuevaCotizacion.agregarItem(item2);
        nuevaCotizacion.agregarItem(item3);
        
        // Aplicar descuento
        nuevaCotizacion.setDescuento(50000);
        nuevaCotizacion.setImpuestos(nuevaCotizacion.getSubtotal() * 0.19); // IVA 19%
        
        // Guardar cotización
        cotizacionService.crearCotizacion(nuevaCotizacion);
        System.out.println("✅ Nueva cotización creada: " + nuevaCotizacion.getNumeroCotizacion());
        
        // Mostrar detalles de la nueva cotización
        System.out.println("\n📊 Detalles de la nueva cotización:");
        System.out.println("  Cliente: " + nuevaCotizacion.getCliente().getNombre());
        System.out.println("  Items: " + nuevaCotizacion.getItems().size());
        System.out.println("  Subtotal: $" + String.format("%,.0f", nuevaCotizacion.getSubtotal()));
        System.out.println("  Descuento: $" + String.format("%,.0f", nuevaCotizacion.getDescuento()));
        System.out.println("  Impuestos: $" + String.format("%,.0f", nuevaCotizacion.getImpuestos()));
        System.out.println("  Total: $" + String.format("%,.0f", nuevaCotizacion.getTotal()));
        
        // Mostrar estadísticas
        System.out.println("\n📈 Estadísticas del sistema:");
        Map<String, Object> stats = cotizacionService.obtenerEstadisticas();
        stats.forEach((key, value) -> {
            System.out.println("  " + key + ": " + value);
        });
        
        // Exportar cotización a Excel
        System.out.println("\n📄 Exportando cotización a Excel...");
        try {
            String archivo = "cotizacion_" + nuevaCotizacion.getNumeroCotizacion() + ".xlsx";
            exportacionService.exportarCotizacionAExcel(nuevaCotizacion, archivo);
            System.out.println("✅ Cotización exportada a: " + archivo);
        } catch (IOException e) {
            System.out.println("❌ Error al exportar: " + e.getMessage());
        }
        
        // Exportar todas las cotizaciones
        System.out.println("\n📄 Exportando todas las cotizaciones...");
        try {
            String archivo = "todas_las_cotizaciones.xlsx";
            exportacionService.exportarCotizacionesAExcel(cotizacionService.obtenerTodasLasCotizaciones(), archivo);
            System.out.println("✅ Todas las cotizaciones exportadas a: " + archivo);
        } catch (IOException e) {
            System.out.println("❌ Error al exportar: " + e.getMessage());
        }
        
        System.out.println("\n🎉 Demostración completada!");
    }
}
