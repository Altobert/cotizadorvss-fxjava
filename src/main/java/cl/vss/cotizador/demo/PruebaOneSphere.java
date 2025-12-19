package cl.vss.cotizador.demo;

import cl.vss.cotizador.model.ItemCotizacionExcel;
import cl.vss.cotizador.service.CotizacionService;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

/**
 * Prueba de reconocimiento del formato M/V One Sphere
 */
public class PruebaOneSphere {
    private static final Logger logger = Logger.getLogger(PruebaOneSphere.class.getName());
    
    public static void main(String[] args) {
        String archivoPath = "/Users/albertosanmartin/Documents/VSS-COTIZACIONES/759191248_ONE Sphere_A25008819-02_Valparaiso Ship Services_2025_05_17_(P) Provision_RFQ.xlsx";
        
        logger.info("🚢 PRUEBA DE RECONOCIMIENTO M/V ONE SPHERE");
        logger.info("═════════════════════════════════════════");
        
        try {
            CotizacionService service = new CotizacionService();
            File archivo = new File(archivoPath);
            
            if (!archivo.exists()) {
                logger.severe("❌ Archivo no encontrado: " + archivoPath);
                return;
            }
            
            System.out.println("📂 Archivo: " + archivo.getName());
            System.out.println("📊 Tamaño: " + archivo.length() + " bytes");
            
            List<ItemCotizacionExcel> items = service.leerItemsDesdeExcel(archivo);
            
            System.out.println("\n📋 RESULTADOS:");
            System.out.println("═════════════");
            System.out.println("✅ Total items leídos: " + items.size());
            
            if (items.isEmpty()) {
                System.out.println("⚠️ No se encontraron items, verifique el formato");
                return;
            }
            
            System.out.println("\n📦 PRIMEROS 10 ITEMS:");
            System.out.println("═══════════════════════");
            
            for (int i = 0; i < Math.min(10, items.size()); i++) {
                ItemCotizacionExcel item = items.get(i);
                System.out.println("\nItem " + (i + 1) + ":");
                System.out.println("  📝 Descripción: " + item.getDescripcion());
                System.out.println("  📦 Código: " + item.getCodigo());
                System.out.println("  🔢 Cantidad: " + item.getCantidad());
                System.out.println("  📏 Unidad: " + item.getUnidad());
                System.out.println("  💰 Precio: " + item.getPrecio());
                System.out.println("  🏷️ Categoría: " + item.getCategoria());
            }
            
        } catch (Exception e) {
            logger.severe("❌ Error durante la prueba: " + e.getMessage());
            e.printStackTrace();
        }
    }
}