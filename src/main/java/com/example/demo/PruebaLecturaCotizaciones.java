package com.example.demo;

import com.example.service.LecturaCotizacionService;
import com.example.model.Cotizacion;
import java.util.List;

/**
 * Prueba del servicio de lectura de cotizaciones
 */
public class PruebaLecturaCotizaciones {
    
    public static void main(String[] args) {
        System.out.println("🧪 Probando lectura de cotizaciones desde Excel...");
        
        LecturaCotizacionService servicio = LecturaCotizacionService.getInstance();
        
        // Probar con el archivo simple
        String archivoSimple = "cotizaciones_simple_para_importar.xlsx";
        System.out.println("\n📄 Probando archivo: " + archivoSimple);
        
        try {
            List<Cotizacion> cotizaciones = servicio.leerCotizacionesDesdeExcel(archivoSimple);
            
            if (cotizaciones.isEmpty()) {
                System.out.println("❌ No se encontraron cotizaciones en el archivo");
            } else {
                System.out.println("✅ Se encontraron " + cotizaciones.size() + " cotizaciones:");
                
                for (int i = 0; i < cotizaciones.size(); i++) {
                    Cotizacion cot = cotizaciones.get(i);
                    System.out.println("\n📋 Cotización " + (i + 1) + ":");
                    System.out.println("   Número: " + cot.getNumeroCotizacion());
                    System.out.println("   Cliente: " + (cot.getCliente() != null ? cot.getCliente().getNombre() : "Sin cliente"));
                    System.out.println("   Estado: " + cot.getEstado());
                    System.out.println("   Total: $" + String.format("%,.0f", cot.getTotal()));
                }
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error al leer el archivo: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Probar con el archivo original
        String archivoOriginal = "cotizaciones_ejemplo_para_importar.xlsx";
        System.out.println("\n📄 Probando archivo: " + archivoOriginal);
        
        try {
            List<Cotizacion> cotizaciones = servicio.leerCotizacionesDesdeExcel(archivoOriginal);
            
            if (cotizaciones.isEmpty()) {
                System.out.println("❌ No se encontraron cotizaciones en el archivo original");
            } else {
                System.out.println("✅ Se encontraron " + cotizaciones.size() + " cotizaciones en el archivo original");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error al leer el archivo original: " + e.getMessage());
        }
        
        System.out.println("\n🎉 Prueba completada");
    }
}
