package cl.vss.cotizador.demo;

import cl.vss.cotizador.model.ItemCotizacionExcel;
import cl.vss.cotizador.service.CotizacionService;

import java.io.File;
import java.util.List;

/**
 * 🧪 PRUEBA DEL FORMATO FR250126_CATALOGO
 */
public class PruebaFormato339 {
    
    public static void main(String[] args) {
        System.out.println("🧪 PROBANDO FORMATO PARA 339-FR250126.xlsx");
        System.out.println("===========================================");
        
        CotizacionService service = new CotizacionService();
        
        // Mostrar formatos disponibles
        System.out.println("\n📋 Formatos configurados:");
        service.mostrarInformacionFormatos();
        
        // Probar carga del archivo
        String rutaArchivo = "/Users/albertosanmartin/Documents/VSS-COTIZACIONES/339-FR250126.xlsx";
        File archivo = new File(rutaArchivo);
        
        if (!archivo.exists()) {
            System.out.println("❌ ERROR: Archivo no encontrado: " + rutaArchivo);
            return;
        }
        
        try {
            System.out.println("\n🔄 Intentando cargar archivo...");
            List<ItemCotizacionExcel> items = service.leerItemsDesdeExcel(archivo);
            
            System.out.println("✅ ARCHIVO CARGADO EXITOSAMENTE!");
            System.out.println("📊 Total items encontrados: " + items.size());
            
            if (items.size() > 0) {
                System.out.println("\n🔍 PRIMEROS 10 ITEMS:");
                System.out.println("=====================");
                
                for (int i = 0; i < Math.min(10, items.size()); i++) {
                    ItemCotizacionExcel item = items.get(i);
                    System.out.println("Item " + (i + 1) + ":");
                    System.out.println("  📦 Código: " + item.getCodigo());
                    System.out.println("  📝 Descripción: " + item.getDescripcion());
                    System.out.println("  📏 Unidad: " + item.getUnidad());
                    System.out.println("  🏷️ Categoría: " + item.getCategoria());
                    System.out.println("  💰 Precio ref: " + item.getPrecio());
                    System.out.println("  💬 Comentarios: " + item.getComentarios());
                    System.out.println();
                }
                
                // Mostrar últimos 5 items también
                if (items.size() > 10) {
                    System.out.println("🔍 ÚLTIMOS 5 ITEMS:");
                    System.out.println("===================");
                    
                    for (int i = Math.max(0, items.size() - 5); i < items.size(); i++) {
                        ItemCotizacionExcel item = items.get(i);
                        System.out.println("Item " + (i + 1) + ":");
                        System.out.println("  📦 Código: " + item.getCodigo());
                        System.out.println("  📝 Descripción: " + item.getDescripcion());
                        System.out.println("  📏 Unidad: " + item.getUnidad());
                        System.out.println();
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("❌ ERROR cargando archivo: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n✨ Prueba completada. Ahora puedes usar 'Cargar Excel' en la aplicación JavaFX.");
    }
}