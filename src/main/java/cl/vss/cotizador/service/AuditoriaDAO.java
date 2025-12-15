package cl.vss.cotizador.service;

import java.sql.*;

public class AuditoriaDAO {
    private Connection conn;

    public AuditoriaDAO(Connection conn) {
        this.conn = conn;
    }

    // Insertar un registro de auditoría
    public void insertarCambio(String parametro, String valorAnterior, String valorNuevo, Long usuarioId) {
        String sql = "INSERT INTO auditoria_parametros " +
                     "(parametro, valor_anterior, valor_nuevo, usuario_id, fecha) " +
                     "VALUES (?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, parametro);
            ps.setString(2, valorAnterior);
            ps.setString(3, valorNuevo);
            ps.setLong(4, usuarioId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
