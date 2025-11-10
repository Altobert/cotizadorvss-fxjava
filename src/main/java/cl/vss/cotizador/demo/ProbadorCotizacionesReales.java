package cl.vss.cotizador.demo;

import cl.vss.cotizador.service.LecturaCotizacionService;
import cl.vss.cotizador.model.Cotizacion;
import java.util.List;

/**
 * Probador específico para archivos de la carpeta cotizaciones
 */
public class ProbadorCotizacionesReales {
    
    public static void main(String[] args) {
        System.out.println("🧪 Probando lectura de archivos reales de cotizaciones...");
        
        LecturaCotizacionService servicio = LecturaCotizacionService.getInstance();
        
        // Probar con archivos más pequeños primero
        String[] archivos = {
            "cotizaciones/Libro3.xlsx",  // Café - más pequeño
            "cotizaciones/RFQ_excel (1).xlsx",  // RFQ - formato estándar
            "cotizaciones/Libro2.xlsx"   // Productos - mediano
        };
        
        for (String archivo : archivos) {
            System.out.println("\n📄 Probando: " + archivo);
            
            try {
                List<Cotizacion> cotizaciones = servicio.leerCotizacionesDesdeExcel(archivo);
                
                if (cotizaciones.isEmpty()) {
                    System.out.println("❌ No se encontraron cotizaciones válidas");
                } else {
                    System.out.println("✅ Se encontraron " + cotizaciones.size() + " cotizaciones:");
                    
                    // Mostrar primeras 3 cotizaciones como ejemplo
                    for (int i = 0; i < Math.min(cotizaciones.size(), 3); i++) {
                        Cotizacion cot = cotizaciones.get(i);
                        System.out.println("   📋 " + (i + 1) + ". " + 
                            (cot.getNumeroCotizacion() != null ? cot.getNumeroCotizacion() : "Sin número") + 
                            " - " + (cot.getCliente() != null ? cot.getCliente().getNombre() : "Sin cliente") +
                            " - " + (cot.getNotas() != null ? cot.getNotas().substring(0, Math.min(30, cot.getNotas().length())) + "..." : "Sin descripción") +
                            " - $" + String.format("%,.0f", cot.getTotal()));
                    }
                    
                    if (cotizaciones.size() > 3) {
                        System.out.println("   ... y " + (cotizaciones.size() - 3) + " más");
                    }
                }
                
            } catch (Exception e) {
                System.out.println("❌ Error al leer: " + e.getMessage());
                if (e.getMessage().contains("OutOfMemoryError")) {
                    System.out.println("   💡 Archivo muy grande, necesita más memoria");
                }
            }
        }
        
        System.out.println("\n🎉 Prueba completada");
    }
}

