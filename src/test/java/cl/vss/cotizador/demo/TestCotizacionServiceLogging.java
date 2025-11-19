package cl.vss.cotizador.demo;

import cl.vss.cotizador.util.LoggingConfig;
import java.util.logging.Logger;

public class TestCotizacionServiceLogging {
    
    private static final Logger logger = LoggingConfig.getLogger(TestCotizacionServiceLogging.class);
    
    public static void main(String[] args) {
        System.out.println("=== TEST LOGGING COTIZACIÓN SERVICE ===");
        
        // Probar logging desde esta clase
        logger.severe("❌ SEVERE desde TestCotizacionServiceLogging");
        logger.warning("⚠️ WARNING desde TestCotizacionServiceLogging");
        logger.info("ℹ️ INFO desde TestCotizacionServiceLogging");
        logger.fine("🔧 FINE desde TestCotizacionServiceLogging");
        
        // Verificar nivel del logger simulando CotizacionService
        Logger cotizacionLogger = LoggingConfig.getLogger("cl.vss.cotizador.service.CotizacionService");
        System.out.println("Logger de CotizacionService - Nivel: " + cotizacionLogger.getLevel());
        System.out.println("Logger de CotizacionService - Handlers: " + cotizacionLogger.getHandlers().length);
        
        // Probar directamente con el logger de CotizacionService
        cotizacionLogger.severe("❌ SEVERE directo desde logger CotizacionService");
        cotizacionLogger.warning("⚠️ WARNING directo desde logger CotizacionService");
        cotizacionLogger.info("ℹ️ INFO directo desde logger CotizacionService");
        cotizacionLogger.fine("🔧 FINE directo desde logger CotizacionService");
        
        // Probar específicamente el caso que mencionas
        String codigo = "TEST123";
        cotizacionLogger.info("Leyendo item con código: " + codigo);
        
        System.out.println("=== FIN TEST ===");
    }
}