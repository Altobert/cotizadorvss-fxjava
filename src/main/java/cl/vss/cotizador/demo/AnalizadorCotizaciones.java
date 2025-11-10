package cl.vss.cotizador.demo;

import cl.vss.cotizador.service.LecturaCotizacionService;
import cl.vss.cotizador.model.Cotizacion;
import java.util.List;

/**
 * Analizador de archivos Excel en la carpeta cotizaciones
 */
public class AnalizadorCotizaciones {
    
    public static void main(String[] args) {
        System.out.println("🔍 Analizando archivos Excel en carpeta 'cotizaciones'...");
        
        LecturaCotizacionService servicio = LecturaCotizacionService.getInstance();
        
        String[] archivos = {
            "cotizaciones/Libro2.xlsx",
            "cotizaciones/Libro3.xlsx", 
            "cotizaciones/Libro4.xlsx",
            "cotizaciones/Libro5.xlsx",
            "cotizaciones/RFQ_excel (1).xlsx"
        };
        
        for (String archivo : archivos) {
            System.out.println("\n📄 Analizando: " + archivo);
            
            try {
                List<Cotizacion> cotizaciones = servicio.leerCotizacionesDesdeExcel(archivo);
                
                if (cotizaciones.isEmpty()) {
                    System.out.println("❌ No se encontraron cotizaciones válidas");
                } else {
                    System.out.println("✅ Se encontraron " + cotizaciones.size() + " cotizaciones:");
                    
                    for (int i = 0; i < Math.min(cotizaciones.size(), 3); i++) {
                        Cotizacion cot = cotizaciones.get(i);
                        System.out.println("   📋 " + (i + 1) + ". " + 
                            (cot.getNumeroCotizacion() != null ? cot.getNumeroCotizacion() : "Sin número") + 
                            " - " + (cot.getCliente() != null ? cot.getCliente().getNombre() : "Sin cliente"));
                    }
                    
                    if (cotizaciones.size() > 3) {
                        System.out.println("   ... y " + (cotizaciones.size() - 3) + " más");
                    }
                }
                
            } catch (Exception e) {
                System.out.println("❌ Error al leer: " + e.getMessage());
            }
        }
        
        System.out.println("\n🎉 Análisis completado");
    }
}
