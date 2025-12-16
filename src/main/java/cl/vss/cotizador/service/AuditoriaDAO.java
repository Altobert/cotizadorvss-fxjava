package cl.vss.cotizador.service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para manejar la tabla de auditoría de parámetros comerciales.
 */
public class AuditoriaDAO {
    private Connection conn;

    public AuditoriaDAO(Connection conn) {
        this.conn = conn;
    }

    // 👉 Insertar un registro de auditoría
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
            throw new RuntimeException("Error al insertar registro de auditoría", ex);
        }
    }

    // 👉 Listar todos los cambios registrados en auditoría
    public List<AuditoriaRegistro> listarCambios() {
        List<AuditoriaRegistro> lista = new ArrayList<>();
        String sql = "SELECT ap.parametro, ap.valor_anterior, ap.valor_nuevo, " +
                     "u.nombre AS usuario, u.correo AS correo_usuario, ap.fecha " +
                     "FROM auditoria_parametros ap " +
                     "JOIN usuario u ON ap.usuario_id = u.id " +
                     "ORDER BY ap.fecha DESC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new AuditoriaRegistro(
                    rs.getString("parametro"),
                    rs.getString("valor_anterior"),
                    rs.getString("valor_nuevo"),
                    rs.getString("usuario"),
                    rs.getString("correo_usuario"),
                    rs.getTimestamp("fecha").toLocalDateTime()
                ));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error al listar registros de auditoría", ex);
        }
        return lista;
    }
}

