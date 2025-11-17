package cl.vss.cotizador.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // Ajusta estos valores a tu entorno real
    private static final String URL = "jdbc:postgresql://localhost:5432/sistema_cotizacion";
    private static final String USER = "postgres";
    private static final String PASSWORD = "031244";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}