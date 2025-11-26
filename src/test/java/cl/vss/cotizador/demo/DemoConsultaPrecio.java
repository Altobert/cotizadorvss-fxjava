package cl.vss.cotizador.demo;

import cl.vss.cotizador.service.CotizacionService;
import cl.vss.cotizador.util.LoggingConfig;
import java.util.logging.Logger;

public class DemoConsultaPrecio {
    
    private static final Logger logger = LoggingConfig.getLogger(DemoConsultaPrecio.class);
    
    public static void main(String[] args) {
        System.out.println("=== DEMO CONSULTA DE PRECIOS ===");
        
        CotizacionService service = new CotizacionService();
        
        // Ejemplos de descripcionesp ara probar
        String[] descripcionesEjemplo = {
            "MILK LONG LIFE PASTERIZED",
            "YOGHURT PLAIN PASTERIZED", 
            "EGGS FRESH LARGE",
            "BREAD LOAVES",
            "ORANGE NATURAL JUICE",
            "MINERAL WATER",
            "producto_inexistente"
        };
        
        System.out.println("\n🔍 Probando búsqueda parcial (LIKE):");
        for (String descripcion : descripcionesEjemplo) {
            double precio = service.consultarPrecioPorDescripcion(descripcion);
            System.out.printf("Descripción: %-30s | Precio: $%.2f%n", descripcion, precio);
        }
        
        System.out.println("\n🎯 Probando búsqueda exacta:");
        for (String descripcion : descripcionesEjemplo) {
            double precioExacto = service.consultarPrecioExactoPorDescripcion(descripcion);
            System.out.printf("Descripción: %-30s | Precio: $%.2f%n", descripcion, precioExacto);
        }
        
        // Ejemplo de búsqueda con palabras parciales
        System.out.println("\n🔎 Probando búsquedas parciales:");
        String[] busquedasParciales = {
            "MILK",
            "BREAD", 
            "JUICE",
            "WATER"
        };
        
        for (String busqueda : busquedasParciales) {
            double precio = service.consultarPrecioPorDescripcion(busqueda);
            System.out.printf("Búsqueda: %-15s | Precio encontrado: $%.2f%n", busqueda, precio);
        }
        
        System.out.println("\n=== FIN DEMO ===");
    }
}