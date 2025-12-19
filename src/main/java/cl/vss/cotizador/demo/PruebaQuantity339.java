package cl.vss.cotizador.demo;

import cl.vss.cotizador.model.ItemCotizacionExcel;
import cl.vss.cotizador.service.CotizacionService;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

/**
 * Prueba específica para verificar la lectura de la columna QUANTITY
 */
public class PruebaQuantity339 {
    private static final Logger logger = Logger.getLogger(PruebaQuantity339.class.getName());
    
    public static void main(String[] args) {
        String archivoPath = "/Users/albertosanmartin/Documents/VSS-COTIZACIONES/339-FR250126.xlsx";
        
        logger.info("🧪 PRUEBA DE COLUMNA QUANTITY");
        logger.info("═══════════════════════════════");
        
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
            System.out.println("═══════════════");
            System.out.println("✅ Total items leídos: " + items.size());
            
            System.out.println("\n🔢 VERIFICACIÓN DE CANTIDADES:");
            System.out.println("═════════════════════════════════");
            
            int itemsConCantidad = 0;
            int itemsSinCantidad = 0;
            
            // Mostrar los primeros 10 items con cantidades
            for (int i = 0; i < Math.min(10, items.size()); i++) {
                ItemCotizacionExcel item = items.get(i);
                int cantidad = item.getCantidad();
                
                if (cantidad > 0) {
                    itemsConCantidad++;
                    System.out.println("Item " + (i + 1) + ":");
                    System.out.println("  📦 Código: '" + item.getCodigo() + "'");
                    System.out.println("  📝 Descripción: '" + item.getDescripcion() + "'");
                    System.out.println("  🔢 Cantidad: " + cantidad);
                    System.out.println("  📏 Unidad: '" + item.getUnidad() + "'");
                    System.out.println();
                } else {
                    itemsSinCantidad++;
                }
            }
            
            // Contar totales
            itemsConCantidad = 0;
            itemsSinCantidad = 0;
            
            for (ItemCotizacionExcel item : items) {
                int cantidad = item.getCantidad();
                if (cantidad > 0) {
                    itemsConCantidad++;
                } else {
                    itemsSinCantidad++;
                }
            }
            
            System.out.println("📊 ESTADÍSTICAS FINALES:");
            System.out.println("═══════════════════════════");
            System.out.println("✅ Items con cantidad: " + itemsConCantidad);
            System.out.println("❌ Items sin cantidad: " + itemsSinCantidad);
            System.out.println("📈 Porcentaje con cantidad: " + 
                              Math.round((double)itemsConCantidad / items.size() * 100.0) + "%");
            
            // Mostrar ejemplos de cantidades específicas
            System.out.println("\n🎯 EJEMPLOS DE CANTIDADES ESPECÍFICAS:");
            System.out.println("═════════════════════════════════════");
            
            int[] cantidadesBuscadas = {10, 30, 20, 40};
            
            for (int cantidadBuscada : cantidadesBuscadas) {
                boolean encontrada = false;
                for (ItemCotizacionExcel item : items) {
                    if (cantidadBuscada == item.getCantidad()) {
                        System.out.println("🔍 Cantidad " + cantidadBuscada + " encontrada:");
                        System.out.println("   📝 Descripción: " + item.getDescripcion());
                        System.out.println("   📦 Código: " + item.getCodigo());
                        System.out.println("   📏 Unidad: " + item.getUnidad());
                        encontrada = true;
                        break;
                    }
                }
                
                if (!encontrada) {
                    System.out.println("❌ Cantidad " + cantidadBuscada + " no encontrada");
                }
            }
            
        } catch (Exception e) {
            logger.severe("❌ Error durante la prueba: " + e.getMessage());
            e.printStackTrace();
        }
    }
}