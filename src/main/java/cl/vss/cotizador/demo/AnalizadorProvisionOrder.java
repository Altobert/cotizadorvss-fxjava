package cl.vss.cotizador.demo;

import cl.vss.cotizador.service.CotizacionService;
import cl.vss.cotizador.model.ItemCotizacionExcel;
import java.io.File;
import java.util.List;

/**
 * Analizador específico para el archivo (HMM BLESSING) 0034W CLVAP PROVISION ORDER 1ST (2).xlsx
 */
public class AnalizadorProvisionOrder {
    
    public static void main(String[] args) {
        CotizacionService service = new CotizacionService();
        
        String rutaArchivo = "/Users/albertosanmartin/Documents/VSS-COTIZACIONES/(HMM BLESSING) 0034W CLVAP PROVISION ORDER 1ST (2).xlsx";
        File archivo = new File(rutaArchivo);
        
        System.out.println("🚢 ANALIZANDO ARCHIVO PROVISION ORDER");
        System.out.println("📁 Archivo: " + archivo.getName());
        System.out.println("📊 Ruta: " + rutaArchivo);
        
        if (!archivo.exists()) {
            System.out.println("❌ ERROR: El archivo no existe en la ruta especificada");
            System.out.println("🔍 Verificar que el archivo esté en la ubicación correcta");
            return;
        }
        
        System.out.println("✅ Archivo encontrado - Tamaño: " + archivo.length() + " bytes");
        System.out.println("=====================================");
        
        // Mostrar formatos soportados
        System.out.println("📋 Formatos soportados:");
        service.getFormatosSoportados().forEach(formato -> 
            System.out.println("  • " + formato)
        );
        System.out.println();
        
        // Ejecutar análisis de estructura
        System.out.println("🔍 === ANÁLISIS DE ESTRUCTURA ===");
        service.analizarEstructuraExcel(archivo);
        System.out.println();
        
        // Intentar cargar el archivo
        System.out.println("📂 === INTENTO DE CARGA ===");
        try {
            List<ItemCotizacionExcel> items = service.leerItemsDesdeExcel(archivo);
            
            if (items.isEmpty()) {
                System.out.println("⚠️ No se cargaron items del archivo");
                System.out.println("💡 Posibles causas:");
                System.out.println("   1. Formato no reconocido");
                System.out.println("   2. Encabezados en ubicación no estándar");
                System.out.println("   3. Columnas con nombres no mapeados");
            } else {
                System.out.println("✅ Archivo cargado exitosamente!");
                System.out.println("📊 Items cargados: " + items.size());
                System.out.println();
                
                // Mostrar algunos ejemplos
                System.out.println("📝 Primeros 3 items:");
                for (int i = 0; i < Math.min(3, items.size()); i++) {
                    ItemCotizacionExcel item = items.get(i);
                    System.out.println("  " + (i+1) + ". " + item.getCodigo() + " - " + 
                                     item.getDescripcion() + " (Cant: " + item.getCantidad() + 
                                     ", Precio: $" + item.getPrecio() + ")");
                }
            }
            
        } catch (Exception e) {
            System.out.println("❌ ERROR durante la carga:");
            System.out.println("   Mensaje: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=====================================");
        System.out.println("✅ ANÁLISIS COMPLETADO");
    }
}