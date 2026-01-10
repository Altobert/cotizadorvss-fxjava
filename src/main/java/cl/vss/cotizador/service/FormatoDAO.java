package cl.vss.cotizador.service;

import cl.vss.cotizador.model.BrokerFormato;
import cl.vss.cotizador.model.FormatoColumna;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para gestionar formatos de brokers y sus columnas
 */
public class FormatoDAO {
    private static final Logger logger = LogManager.getLogger(FormatoDAO.class);
    private final Connection connection;
    
    public FormatoDAO(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Obtiene el formato activo de un broker con todas sus columnas
     * @param brokerId ID del broker
     * @return BrokerFormato con columnas, o null si no existe
     */
    public BrokerFormato obtenerFormatoPorBrokerId(Integer brokerId) {
        BrokerFormato formato = null;
        
        String sqlFormato = "SELECT bf.formato_id, bf.broker_id, b.broker_name, bf.version, " +
                           "bf.header_row, bf.descripcion, bf.archivo_ejemplo, bf.activo " +
                           "FROM broker_formatos bf " +
                           "JOIN brokers b ON bf.broker_id = b.broker_id " +
                           "WHERE bf.broker_id = ? AND bf.activo = true " +
                           "ORDER BY bf.fecha_creacion DESC " +
                           "LIMIT 1";
        
        try (PreparedStatement stmt = connection.prepareStatement(sqlFormato)) {
            stmt.setInt(1, brokerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    formato = new BrokerFormato();
                    formato.setFormatoId(rs.getInt("formato_id"));
                    formato.setBrokerId(rs.getInt("broker_id"));
                    formato.setBrokerName(rs.getString("broker_name"));
                    formato.setVersion(rs.getString("version"));
                    formato.setHeaderRow(rs.getInt("header_row"));
                    formato.setDescripcion(rs.getString("descripcion"));
                    formato.setArchivoEjemplo(rs.getString("archivo_ejemplo"));
                    formato.setActivo(rs.getBoolean("activo"));
                    
                    // Cargar columnas del formato
                    List<FormatoColumna> columnas = obtenerColumnasPorFormatoId(formato.getFormatoId());
                    formato.setColumnas(columnas);
                    
                    logger.info("Formato cargado para broker ID {}: {} columnas", brokerId, columnas.size());
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error al obtener formato para broker ID: {}", brokerId, e);
        }
        
        return formato;
    }
    
    /**
     * Obtiene todas las columnas de un formato
     * @param formatoId ID del formato
     * @return Lista de columnas ordenadas por índice
     */
    public List<FormatoColumna> obtenerColumnasPorFormatoId(Integer formatoId) {
        List<FormatoColumna> columnas = new ArrayList<>();
        
        String sql = "SELECT columna_id, formato_id, campo_estandar, nombre_columna_original, " +
                    "indice_columna, letra_columna, tipo_dato, requerido, descripcion, " +
                    "color_fondo, color_texto, es_negrita, es_cursiva, tiene_borde " +
                    "FROM formato_columnas " +
                    "WHERE formato_id = ? " +
                    "ORDER BY indice_columna";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, formatoId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    FormatoColumna columna = new FormatoColumna();
                    columna.setColumnaId(rs.getInt("columna_id"));
                    columna.setFormatoId(rs.getInt("formato_id"));
                    columna.setCampoEstandar(rs.getString("campo_estandar"));
                    columna.setNombreColumnaOriginal(rs.getString("nombre_columna_original"));
                    columna.setIndiceColumna(rs.getInt("indice_columna"));
                    columna.setLetraColumna(rs.getString("letra_columna"));
                    columna.setTipoDato(rs.getString("tipo_dato"));
                    columna.setRequerido(rs.getBoolean("requerido"));
                    columna.setDescripcion(rs.getString("descripcion"));
                    columna.setColorFondo(rs.getString("color_fondo"));
                    columna.setColorTexto(rs.getString("color_texto"));
                    columna.setEsNegrita(rs.getBoolean("es_negrita"));
                    columna.setEsCursiva(rs.getBoolean("es_cursiva"));
                    columna.setTieneBorde(rs.getBoolean("tiene_borde"));
                    
                    columnas.add(columna);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error al obtener columnas para formato ID: {}", formatoId, e);
        }
        
        return columnas;
    }
    
    /**
     * Lista todos los formatos activos con sus columnas
     * @return Lista de formatos con columnas
     */
    public List<BrokerFormato> listarFormatosActivos() {
        List<BrokerFormato> formatos = new ArrayList<>();
        
        String sql = "SELECT bf.formato_id, bf.broker_id, b.broker_name, bf.version, " +
                    "bf.header_row, bf.descripcion, bf.archivo_ejemplo, bf.activo " +
                    "FROM broker_formatos bf " +
                    "JOIN brokers b ON bf.broker_id = b.broker_id " +
                    "WHERE bf.activo = true " +
                    "ORDER BY b.broker_name, bf.fecha_creacion DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                BrokerFormato formato = new BrokerFormato();
                formato.setFormatoId(rs.getInt("formato_id"));
                formato.setBrokerId(rs.getInt("broker_id"));
                formato.setBrokerName(rs.getString("broker_name"));
                formato.setVersion(rs.getString("version"));
                formato.setHeaderRow(rs.getInt("header_row"));
                formato.setDescripcion(rs.getString("descripcion"));
                formato.setArchivoEjemplo(rs.getString("archivo_ejemplo"));
                formato.setActivo(rs.getBoolean("activo"));
                
                // Cargar columnas del formato
                List<FormatoColumna> columnas = obtenerColumnasPorFormatoId(formato.getFormatoId());
                formato.setColumnas(columnas);
                
                formatos.add(formato);
            }
            
            logger.info("Se cargaron {} formatos activos", formatos.size());
            
        } catch (SQLException e) {
            logger.error("Error al listar formatos activos", e);
        }
        
        return formatos;
    }
}
