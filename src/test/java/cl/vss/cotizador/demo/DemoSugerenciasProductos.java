package cl.vss.cotizador.demo;

import cl.vss.cotizador.service.CotizacionService;
import cl.vss.cotizador.util.LoggingConfig;

import java.util.logging.Logger;

/**
 * Clase de demostración para probar las sugerencias de productos
 * cuando se consulta precio con descripción vacía o nula
 */
public class DemoSugerenciasProductos {
    
    private static final Logger logger = LoggingConfig.getLogger(DemoSugerenciasProductos.class);
    
    public static void main(String[] args) {
        // Configurar logging
        LoggingConfig.initialize();
        
        logger.info("🚀 Iniciando demo de sugerencias de productos");
        logger.info("=".repeat(60));
        
        CotizacionService servicio = new CotizacionService();
        
        // Caso 1: Descripción nula
        logger.info("\n📝 CASO 1: Descripción nula");
        logger.info("-".repeat(30));
        double precio1 = servicio.consultarPrecioPorDescripcion(null);
        logger.info("Precio retornado: $" + precio1);
        
        // Caso 2: Descripción vacía
        logger.info("\n📝 CASO 2: Descripción vacía");
        logger.info("-".repeat(30));
        double precio2 = servicio.consultarPrecioPorDescripcion("");
        logger.info("Precio retornado: $" + precio2);
        
        // Caso 3: Descripción solo espacios
        logger.info("\n📝 CASO 3: Descripción solo espacios");
        logger.info("-".repeat(30));
        double precio3 = servicio.consultarPrecioPorDescripcion("   ");
        logger.info("Precio retornado: $" + precio3);
        
        // Caso 4: Descripción parcial para ver sugerencias inteligentes
        logger.info("\n📝 CASO 4: Descripción con palabras clave para sugerencias");
        logger.info("-".repeat(30));
        double precio4 = servicio.consultarPrecioPorDescripcion("ACEITE VEGETAL");
        logger.info("Precio retornado: $" + precio4);
        
        // Caso 5: Comparación con búsqueda válida
        logger.info("\n📝 CASO 5: Búsqueda válida para comparar");
        logger.info("-".repeat(30));
        double precio5 = servicio.consultarPrecioPorDescripcion("AJINOMOTO");
        logger.info("Precio retornado: $" + precio5);
        
        logger.info("\n" + "=".repeat(60));
        logger.info("✅ Demo completado");
    }
}