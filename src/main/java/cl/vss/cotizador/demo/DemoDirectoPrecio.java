package cl.vss.cotizador.demo;

import cl.vss.cotizador.util.DBConnection;
import cl.vss.cotizador.util.LoggingConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

public class DemoDirectoPrecio {
    
    private static final Logger logger = LoggingConfig.getLogger(DemoDirectoPrecio.class);
    
    public static void main(String[] args) {
        System.out.println("=== DEMO DIRECTO CONSULTA PRECIOS ===");
        
        // Prueba de conexión básica
        try (Connection connection = DBConnection.getConnection()) {
            System.out.println("✅ Conexión a la base de datos exitosa");
            
            // Consulta simple para probar la vista
            String sql = "SELECT descripcion_es, descripcion_en, precio_venta_neto FROM vista_producto_precio LIMIT 5";
            
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                
                System.out.println("\n📋 Primeros 5 productos en vista_producto_precio:");
                System.out.println("--------------------------------------------------");
                
                int count = 0;
                while (resultSet.next()) {
                    String descripcionEs = resultSet.getString("descripcion_es");
                    String descripcionEn = resultSet.getString("descripcion_en");
                    double precio = resultSet.getDouble("precio_venta_neto");
                    System.out.printf("%-30s | %-30s | $%.2f%n", 
                        descripcionEs != null ? descripcionEs : "N/A", 
                        descripcionEn != null ? descripcionEn : "N/A", 
                        precio);
                    count++;
                }
                
                if (count == 0) {
                    System.out.println("⚠️ No se encontraron productos en la vista");
                }
                
            }
            
            // Prueba de búsqueda específica
            System.out.println("\n🔍 Prueba de búsqueda específica:");
            System.out.println("----------------------------------");
            
            String sqlBusqueda = "SELECT descripcion_es, descripcion_en, precio_venta_neto FROM vista_producto_precio " +
                                "WHERE UPPER(descripcion_es) LIKE UPPER(?) OR UPPER(descripcion_en) LIKE UPPER(?) LIMIT 3";
            
            String[] busquedas = {"MILK", "BREAD", "WATER"};
            
            for (String busqueda : busquedas) {
                try (PreparedStatement statement = connection.prepareStatement(sqlBusqueda)) {
                    String patron = "%" + busqueda + "%";
                    statement.setString(1, patron);
                    statement.setString(2, patron);
                    
                    try (ResultSet resultSet = statement.executeQuery()) {
                        System.out.printf("\nBúsqueda: '%s'%n", busqueda);
                        
                        boolean encontrado = false;
                        while (resultSet.next()) {
                            String descripcionEs = resultSet.getString("descripcion_es");
                            String descripcionEn = resultSet.getString("descripcion_en");
                            double precio = resultSet.getDouble("precio_venta_neto");
                            String descripcionMostrar = descripcionEs != null ? descripcionEs : 
                                                      (descripcionEn != null ? descripcionEn : "N/A");
                            System.out.printf("  %-35s | $%.2f%n", descripcionMostrar, precio);
                            encontrado = true;
                        }
                        
                        if (!encontrado) {
                            System.out.printf("  No se encontraron productos para '%s'%n", busqueda);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== FIN DEMO ===");
    }
}