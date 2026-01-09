package cl.vss.cotizador.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.LocalDate;

public class ParametrosDAO {
    private static final Logger logger = LogManager.getLogger(ParametrosDAO.class);
    private Connection conn;

    public ParametrosDAO(Connection conn) {
        this.conn = conn;
    }

    // 👉 Validación de parámetros comerciales
    private void validarParametros(double tipoCambio, double utilidad, LocalDate vigencia) {
        if (tipoCambio <= 0) {
            throw new IllegalArgumentException("El tipo de cambio debe ser mayor a 0");
        }
        if (utilidad < 0) {
            throw new IllegalArgumentException("La utilidad no puede ser negativa");
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
        logger.debug("Consultando tipo de cambio actual");
        String sql = "SELECT tipo_cambio_usado FROM parametros_comerciales ORDER BY fecha_actualizacion DESC LIMIT 1";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                double valor = rs.getDouble(1);
                logger.info("Tipo de cambio recuperado: {}", valor);
                return valor;
            }
        } catch (SQLException ex) {
            logger.error("Error al obtener tipo de cambio actual", ex);
            ex.printStackTrace();
        }
        logger.warn("No se encontró tipo de cambio, retornando 0");
        return 0;
    }

    // 👉 Obtener el último valor de porcentaje de utilidad
    public double getPorcentajeUtilidadActual() {
        logger.debug("Consultando porcentaje de utilidad actual");
        String sql = "SELECT porcentaje_utilidad FROM parametros_comerciales ORDER BY fecha_actualizacion DESC LIMIT 1";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                double valor = rs.getDouble(1);
                logger.info("Porcentaje de utilidad recuperado: {}", valor);
                return valor;
            }
        } catch (SQLException ex) {
            logger.error("Error al obtener porcentaje de utilidad actual", ex);
            ex.printStackTrace();
        }
        logger.warn("No se encontró porcentaje de utilidad, retornando 0");
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
        logger.info("Actualizando parámetros comerciales: tipoCambio={}, utilidad={}, vigencia={}, usuarioId={}", 
                    tipoCambio, utilidad, vigencia, usuarioId);
        
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
            logger.info("Parámetros comerciales actualizados exitosamente");
        } catch (SQLException ex) {
            logger.error("Error al actualizar parámetros comerciales", ex);
            ex.printStackTrace();
            throw new RuntimeException("Error al actualizar parámetros comerciales", ex);
        }
    }
}
