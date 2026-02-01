package cl.vss.cotizador.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static final Properties properties = new Properties();
    private static final String PROPERTIES_FILE = "/db.properties";
    private static final String EXTERNAL_CONFIG_FILE = "cotizador-config.properties";
    
    // Variables de configuración cargadas desde properties
    private static String URL;
    private static String USER;
    private static String PASSWORD;
    private static String DRIVER;
    private static int CONNECTION_TIMEOUT;
    private static boolean DEBUG;
    
    // Bloque estático para cargar propiedades al inicializar la clase
    static {
        loadProperties();
        initializeConnectionProperties();
    }
    
    /**
     * Carga las propiedades desde un archivo externo si existe,
     * de lo contrario usa el archivo interno del JAR
     */
    private static void loadProperties() {
        boolean loaded = false;
        
        // 1. Intentar cargar desde archivo externo (mismo directorio que el JAR)
        File externalFile = new File(EXTERNAL_CONFIG_FILE);
        if (externalFile.exists()) {
            try (FileInputStream fis = new FileInputStream(externalFile)) {
                properties.load(fis);
                System.out.println("✅ Configuración cargada desde archivo externo: " + EXTERNAL_CONFIG_FILE);
                loaded = true;
            } catch (IOException e) {
                System.err.println("⚠️ Error al cargar archivo externo: " + e.getMessage());
            }
        }
        
        // 2. Si no hay archivo externo, usar el archivo interno del JAR
        if (!loaded) {
            try (InputStream input = DBConnection.class.getResourceAsStream(PROPERTIES_FILE)) {
                if (input == null) {
                    System.err.println("❌ No se pudo encontrar el archivo " + PROPERTIES_FILE);
                    setDefaultProperties();
                    return;
                }
                
                properties.load(input);
                System.out.println("✅ Configuración cargada desde JAR (archivo interno)");
                
            } catch (IOException e) {
                System.err.println("❌ Error al cargar propiedades: " + e.getMessage());
                setDefaultProperties();
            }
        }
    }
    
    /**
     * Inicializa las variables de conexión desde las propiedades
     */
   private static void initializeConnectionProperties() {
        URL = properties.getProperty("db.url", "jdbc:postgresql://localhost:5432/sistema_cotizacion_2025");
        USER = properties.getProperty("db.username", "albertosanmartin");
        PASSWORD = properties.getProperty("db.password", "");
        DRIVER = properties.getProperty("db.driver", "org.postgresql.Driver");
        CONNECTION_TIMEOUT = Integer.parseInt(properties.getProperty("db.connection.timeout", "30000"));
        DEBUG = Boolean.parseBoolean(properties.getProperty("app.debug", "false"));
        
        if (DEBUG) {
            System.out.println("🔧 Configuración de BD cargada:");
            System.out.println("   URL: " + URL);
            System.out.println("   Usuario: " + USER);
            System.out.println("   Driver: " + DRIVER);
            System.out.println("   Timeout: " + CONNECTION_TIMEOUT + "ms");
        }

      }   
    /**
     * Establece valores por defecto en caso de error al cargar propiedades
     */
    private static void setDefaultProperties() {
        properties.setProperty("db.url", "jdbc:postgresql://localhost:5432/sistema_cotizacion_2025");
        properties.setProperty("db.username", "albertosanmartin");
        properties.setProperty("db.password", "");
        properties.setProperty("db.driver", "org.postgresql.Driver");
        properties.setProperty("db.connection.timeout", "30000");
        properties.setProperty("app.debug", "false");
        System.out.println("⚠️************ Usando configuración por defecto************");
    }
    
    /**
     * Obtiene una conexión a la base de datos
     * @return Connection objeto de conexión a la BD
     * @throws SQLException si hay error en la conexión
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Cargar el driver explícitamente
            Class.forName(DRIVER);
            
            if (DEBUG) {
                System.out.println("🔗 Estableciendo conexión con: " + URL);
            }
            
            return DriverManager.getConnection(URL, USER, PASSWORD);
            
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Driver de PostgreSQL no encontrado: " + e.getMessage());
            throw new SQLException("Driver de base de datos no disponible", e);
        }
    }
    
    /**
     * Obtiene una propiedad específica
     * @param key clave de la propiedad
     * @return valor de la propiedad
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * Obtiene una propiedad con valor por defecto
     * @param key clave de la propiedad
     * @param defaultValue valor por defecto
     * @return valor de la propiedad o valor por defecto
     */
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Verifica si la conexión está funcionando
     * @return true si la conexión es exitosa
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            boolean isValid = conn.isValid(5); // 5 segundos timeout
            if (DEBUG) {
                System.out.println(isValid ? "✅ Conexión a BD exitosa" : "❌ Conexión a BD falló");
            }
            return isValid;
        } catch (SQLException e) {
            if (DEBUG) {
                System.err.println("❌ Error al probar conexión: " + e.getMessage());
            }
            return false;
        }
    }
}