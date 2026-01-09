package cl.vss.cotizador.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para manejar la tabla de auditoría de parámetros comerciales.
 */
public class AuditoriaDAO {
    private static final Logger logger = LogManager.getLogger(AuditoriaDAO.class);
    private Connection conn;

    public AuditoriaDAO(Connection conn) {
        this.conn = conn;
    }

    // 👉 Insertar un registro de auditoría
    public void insertarCambio(String parametro, String valorAnterior, String valorNuevo, Long usuarioId) {
        String sql = "INSERT INTO auditoria_parametros " +
                     "(parametro, valor_anterior, valor_nuevo, usuario_id, fecha) " +
                     "VALUES (?, ?, ?, ?, NOW())";
        
        logger.info("Insertando cambio de auditoría: parametro={}, valorAnterior={}, valorNuevo={}, usuarioId={}", 
                    parametro, valorAnterior, valorNuevo, usuarioId);
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, parametro);
            ps.setString(2, valorAnterior);
            ps.setString(3, valorNuevo);
            ps.setLong(4, usuarioId);
            ps.executeUpdate();
            
            logger.info("Cambio de auditoría insertado exitosamente para parámetro: {}", parametro);
        } catch (SQLException ex) {
            logger.error("Error al insertar registro de auditoría para parámetro: {}", parametro, ex);
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
        
        logger.debug("Consultando lista de cambios de auditoría");
        
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
            logger.info("Se recuperaron {} registros de auditoría", lista.size());
        } catch (SQLException ex) {
            logger.error("Error al listar registros de auditoría", ex);
            ex.printStackTrace();
            throw new RuntimeException("Error al listar registros de auditoría", ex);
        }
        return lista;
    }
}

