package cl.vss.cotizador.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class DBConnectionTest {

    private static final String EXPECTED_DB_NAME = "sistema_cotizacion_2025";

    @Test
    void shouldLoadProperties() {
        String url = DBConnection.getProperty("db.url");
        String user = DBConnection.getProperty("db.username");
        String dbName = DBConnection.getProperty("db.database", "");
        assertNotNull(url, "db.url no debe ser null");
        assertFalse(url.isBlank(), "db.url no debe estar vacio");
        assertTrue(url.contains(EXPECTED_DB_NAME), "db.url debe apuntar a " + EXPECTED_DB_NAME);
        assertTrue(dbName.equals(EXPECTED_DB_NAME) || dbName.isBlank(), "db.database debe ser " + EXPECTED_DB_NAME + " o estar vacio");
        assertNotNull(user, "db.username no debe ser null");
        assertFalse(user.isBlank(), "db.username no debe estar vacio");
    }

    @Test
    void shouldConnectToDatabase() throws SQLException {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty("db.test.enabled", "true")),
                "Prueba de conexion deshabilitada (db.test.enabled=false)");

        try (Connection conn = DBConnection.getConnection()) {
            assertNotNull(conn, "La conexion no debe ser null");
            assertTrue(conn.isValid(5), "La conexion no es valida");
        }
    }
}
