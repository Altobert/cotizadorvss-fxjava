package cl.vss.cotizador.demo;

import cl.vss.cotizador.service.CotizacionService;
import cl.vss.cotizador.util.LoggingConfig;

import java.util.logging.Logger;

/**
 * Clase de demostración para probar el método consultarPrecioNetoPorDescripcion
 */
public class DemoPrecioNeto {
    
    private static final Logger logger = LoggingConfig.getLogger(DemoPrecioNeto.class);
    
    public static void main(String[] args) {
        // Configurar logging
        LoggingConfig.initialize();
        
        logger.info("🚀 Iniciando demo de consulta de precio neto");
        logger.info("=".repeat(60));
        
        CotizacionService servicio = new CotizacionService();
        
        // Caso 1: Producto conocido
        logger.info("\n📝 CASO 1: Producto conocido - AJINOMOTO");
        logger.info("-".repeat(40));
        double precio1 = servicio.consultarPrecioNetoPorDescripcion("AJINOMOTO");
        logger.info("💰 Precio neto retornado: $" + precio1);
        
        // Caso 2: Búsqueda parcial
        logger.info("\n📝 CASO 2: Búsqueda parcial - ACEITE");
        logger.info("-".repeat(40));
        double precio2 = servicio.consultarPrecioNetoPorDescripcion("ACEITE");
        logger.info("💰 Precio neto retornado: $" + precio2);
        
        // Caso 3: Descripción inexistente
        logger.info("\n📝 CASO 3: Producto inexistente - PRODUCTO_FALSO");
        logger.info("-".repeat(40));
        double precio3 = servicio.consultarPrecioNetoPorDescripcion("PRODUCTO_FALSO");
        logger.info("💰 Precio neto retornado: $" + precio3);
        
        // Caso 4: Descripción vacía
        logger.info("\n📝 CASO 4: Descripción vacía");
        logger.info("-".repeat(40));
        double precio4 = servicio.consultarPrecioNetoPorDescripcion("");
        logger.info("💰 Precio neto retornado: $" + precio4);
        
        // Caso 5: Descripción nula
        logger.info("\n📝 CASO 5: Descripción nula");
        logger.info("-".repeat(40));
        double precio5 = servicio.consultarPrecioNetoPorDescripcion(null);
        logger.info("💰 Precio neto retornado: $" + precio5);
        
        // Caso 6: Búsqueda con múltiples palabras
        logger.info("\n📝 CASO 6: Múltiples palabras - ALIÑO COMPLETO");
        logger.info("-".repeat(40));
        double precio6 = servicio.consultarPrecioNetoPorDescripcion("ALIÑO COMPLETO");
        logger.info("💰 Precio neto retornado: $" + precio6);
        
        logger.info("\n" + "=".repeat(60));
        logger.info("✅ Demo de precio neto completado");
        logger.info("📊 Resumen de precios encontrados:");
        logger.info("   • AJINOMOTO: $" + precio1);
        logger.info("   • ACEITE: $" + precio2);
        logger.info("   • PRODUCTO_FALSO: $" + precio3);
        logger.info("   • [vacío]: $" + precio4);
        logger.info("   • [nulo]: $" + precio5);
        logger.info("   • ALIÑO COMPLETO: $" + precio6);
    }
}