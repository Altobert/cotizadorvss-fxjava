package cl.vss.cotizador.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Utilidad para configurar el sistema de logging del aplicativo
 */
public class LoggingConfig {
    
    private static final Logger logger = Logger.getLogger(LoggingConfig.class.getName());
    private static boolean initialized = false;
    
    /**
     * Inicializa la configuración de logging
     */
    public static void initialize() {
        if (initialized) {
            return;
        }
        
        try {
            // Cargar configuración desde archivo de propiedades
            InputStream configStream = LoggingConfig.class.getResourceAsStream("/logging.properties");
            
            if (configStream != null) {
                LogManager.getLogManager().readConfiguration(configStream);
                logger.info("✅ Configuración de logging cargada desde logging.properties");
            } else {
                // Configuración por defecto si no existe el archivo
                setupDefaultLogging();
                logger.warning("⚠️ Archivo logging.properties no encontrado, usando configuración por defecto");
            }
            
            initialized = true;
            logger.info("🔧 Sistema de logging inicializado correctamente");
            
        } catch (IOException e) {
            System.err.println("❌ Error al configurar logging: " + e.getMessage());
            setupDefaultLogging();
        }
    }
    
    /**
     * Configura logging por defecto si no se puede cargar desde archivo
     */
    private static void setupDefaultLogging() {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);
        
        // Configurar logger específico para nuestro paquete
        Logger appLogger = Logger.getLogger("cl.vss.cotizador");
        appLogger.setLevel(Level.FINE);
    }
    
    /**
     * Obtiene un logger configurado para una clase específica
     * @param clazz la clase para la que se requiere el logger
     * @return Logger configurado
     */
    public static Logger getLogger(Class<?> clazz) {
        initialize();
        return Logger.getLogger(clazz.getName());
    }
    
    /**
     * Obtiene un logger configurado por nombre
     * @param name nombre del logger
     * @return Logger configurado
     */
    public static Logger getLogger(String name) {
        initialize();
        return Logger.getLogger(name);
    }
    
    /**
     * Cambia el nivel de logging dinámicamente
     * @param loggerName nombre del logger
     * @param level nuevo nivel
     */
    public static void setLogLevel(String loggerName, Level level) {
        Logger targetLogger = Logger.getLogger(loggerName);
        targetLogger.setLevel(level);
        logger.info("📊 Nivel de logging cambiado para " + loggerName + " a " + level);
    }
    
    /**
     * Cambia el nivel de logging para toda la aplicación
     * @param level nuevo nivel
     */
    public static void setApplicationLogLevel(Level level) {
        setLogLevel("cl.vss.cotizador", level);
        logger.info("📊 Nivel de logging de aplicación cambiado a " + level);
    }
}