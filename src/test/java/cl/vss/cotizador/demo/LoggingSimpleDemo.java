package cl.vss.cotizador.demo;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.SimpleFormatter;

/**
 * Demo simple para probar logging básico
 */
public class LoggingSimpleDemo {
    
    public static void main(String[] args) {
        // Configurar logger simple
        Logger logger = Logger.getLogger("cl.vss.cotizador.demo");
        
        // Configurar handler para consola
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFormatter(new SimpleFormatter());
        
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false); // No usar handlers padre
        
        System.out.println("=== DEMO DE LOGGING ===");
        
        // Probar diferentes niveles
        logger.severe("❌ SEVERE: Este es un error crítico");
        logger.warning("⚠️ WARNING: Esta es una advertencia");
        logger.info("ℹ️ INFO: Esta es información general");
        logger.fine("🔧 FINE: Este es un mensaje de debug");
        logger.finer("🔍 FINER: Este es un mensaje de trace detallado");
        logger.finest("🔬 FINEST: Este es el mensaje más detallado");
        
        // Logging con excepción
        try {
            throw new RuntimeException("Excepción de prueba");
        } catch (Exception e) {
            logger.log(Level.WARNING, "⚠️ Capturada excepción de prueba", e);
        }
        
        System.out.println("=== FIN DEMO ===");
    }
}