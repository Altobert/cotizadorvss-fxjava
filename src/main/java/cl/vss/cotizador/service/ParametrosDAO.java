package cl.vss.cotizador.service;

import java.sql.*;
import java.time.LocalDate;

public class ParametrosDAO {
    private Connection conn;

    public ParametrosDAO(Connection conn) {
        this.conn = conn;
    }

    // 👉 Obtener el último valor de tipo de cambio
    public double getTipoCambioActual() {
        String sql = "SELECT tipo_cambio_usado FROM parametros_comerciales ORDER BY fecha_actualizacion DESC LIMIT 1";
        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    // 👉 Obtener el último valor de porcentaje de utilidad
    public double getPorcentajeUtilidadActual() {
        String sql = "SELECT porcentaje_utilidad FROM parametros_comerciales ORDER BY fecha_actualizacion DESC LIMIT 1";
        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    // 👉 Obtener la última fecha de vigencia
    public LocalDate getFechaVigenciaActual() {
        String sql = "SELECT fecha_vigencia FROM parametros_comerciales ORDER BY fecha_actualizacion DESC LIMIT 1";
        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                return rs.getDate(1).toLocalDate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // 👉 Actualizar parámetros comerciales (inserta un nuevo registro con los valores)
    public void actualizarParametros(double tipoCambio, double utilidad, LocalDate vigencia, Long usuarioId) {
        String sql = "INSERT INTO parametros_comerciales " +
                     "(tipo_cambio_usado, porcentaje_utilidad, fecha_vigencia, usuario_editor_id, fecha_actualizacion) " +
                     "VALUES (?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, tipoCambio);
            ps.setDouble(2, utilidad);
            ps.setDate(3, java.sql.Date.valueOf(vigencia));
            ps.setLong(4, usuarioId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
