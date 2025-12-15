package cl.vss.cotizador.service;

import java.sql.*;
import java.time.LocalDate;

public class ParametrosDAO {
    private Connection conn;

    public ParametrosDAO(Connection conn) {
        this.conn = conn;
    }

    // 👉 Validación de parámetros comerciales
    private void validarParametros(double tipoCambio, double utilidad, LocalDate vigencia) {
        if (tipoCambio <= 0) {
            throw new IllegalArgumentException("El tipo de cambio debe ser mayor a 0");
        }
        if (utilidad < 0 || utilidad > 100) {
            throw new IllegalArgumentException("La utilidad debe estar entre 0 y 100");
        }
        if (vigencia == null) {
            throw new IllegalArgumentException("La fecha de vigencia es obligatoria");
        }
        if (vigencia.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de vigencia no puede ser anterior a hoy");
        }
    }

    // 👉 Obtener el último valor de tipo de cambio
    public double getTipoCambioActual() {
        String sql = "SELECT tipo_cambio_usado FROM parametros_comerciales ORDER BY fecha_actualizacion DESC LIMIT 1";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
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
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
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
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                Date fecha = rs.getDate(1);
                return fecha != null ? fecha.toLocalDate() : null;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // 👉 Actualizar parámetros comerciales (inserta un nuevo registro con los valores)
    public void actualizarParametros(double tipoCambio, double utilidad, LocalDate vigencia, Long usuarioId) {
        // Validación antes de insertar
        validarParametros(tipoCambio, utilidad, vigencia);

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
            throw new RuntimeException("Error al actualizar parámetros comerciales", ex);
        }
    }
}
