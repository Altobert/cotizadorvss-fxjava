package cl.vss.cotizador.demo;

import cl.vss.cotizador.util.LoggingConfig;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Demo para probar el sistema de logging
 */
public class LoggingDemo {
    
    private static final Logger logger = LoggingConfig.getLogger(LoggingDemo.class);
    
    public static void main(String[] args) {
        // Inicializar logging
        LoggingConfig.initialize();
        
        logger.info("🚀 Iniciando demo de sistema de logging");
        
        // Probar diferentes niveles de logging
        logger.severe("❌ Ejemplo de mensaje SEVERE");
        logger.warning("⚠️ Ejemplo de mensaje WARNING");
        logger.info("ℹ️ Ejemplo de mensaje INFO");
        logger.fine("🔧 Ejemplo de mensaje FINE (debug)");
        logger.finer("🔍 Ejemplo de mensaje FINER (trace)");
        
        // Cambiar nivel de logging dinámicamente
        logger.info("📊 Cambiando nivel de logging a FINE para ver mensajes de debug");
        LoggingConfig.setApplicationLogLevel(Level.FINE);
        
        logger.fine("🔧 Ahora este mensaje FINE debería aparecer");
        logger.finer("🔍 Este mensaje FINER aún no debería aparecer");
        
        // Probar logging con excepción
        try {
            throw new RuntimeException("Ejemplo de excepción para logging");
        } catch (Exception e) {
            logger.log(Level.WARNING, "⚠️ Ejemplo de logging con excepción", e);
        }
        
        logger.info("✅ Demo de logging completado");
    }
}