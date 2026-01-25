package cl.vss.cotizador.service;

import cl.vss.cotizador.model.BrokerMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO para gestionar operaciones con la tabla broker_metadata
 */
public class BrokerMetadataDAO {
    private static final Logger logger = LogManager.getLogger(BrokerMetadataDAO.class);
    private final Connection connection;
    
    public BrokerMetadataDAO(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Obtiene toda la metadata de un formato específico
     * @param formatoId ID del formato
     * @return Lista de metadata del formato
     */
    public List<BrokerMetadata> obtenerMetadataPorFormato(int formatoId) {
        List<BrokerMetadata> metadataList = new ArrayList<>();
        String sql = "SELECT metadata_id, formato_id, seccion, campo_nombre, campo_valor, " +
                     "fila_origen, columna_origen, letra_columna, fecha_creacion " +
                     "FROM broker_metadata " +
                     "WHERE formato_id = ? " +
                     "ORDER BY seccion, metadata_id";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, formatoId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BrokerMetadata metadata = new BrokerMetadata();
                    metadata.setMetadataId(rs.getInt("metadata_id"));
                    metadata.setFormatoId(rs.getInt("formato_id"));
                    metadata.setSeccion(rs.getString("seccion"));
                    metadata.setCampoNombre(rs.getString("campo_nombre"));
                    metadata.setCampoValor(rs.getString("campo_valor"));
                    metadata.setFilaOrigen(rs.getInt("fila_origen"));
                    metadata.setColumnaOrigen(rs.getInt("columna_origen"));
                    metadata.setLetraColumna(rs.getString("letra_columna"));
                    
                    Timestamp ts = rs.getTimestamp("fecha_creacion");
                    if (ts != null) {
                        metadata.setFechaCreacion(ts.toLocalDateTime());
                    }
                    
                    metadataList.add(metadata);
                }
            }
            
            logger.info("Se cargaron {} registros de metadata para formato ID {}", metadataList.size(), formatoId);
            
        } catch (SQLException e) {
            logger.error("Error al obtener metadata del formato ID {}", formatoId, e);
        }
        
        return metadataList;
    }
    
    /**
     * Obtiene la metadata organizada por secciones
     * @param formatoId ID del formato
     * @return Mapa con secciones como clave y lista de metadata como valor
     */
    public Map<String, List<BrokerMetadata>> obtenerMetadataPorSeccion(int formatoId) {
        Map<String, List<BrokerMetadata>> metadataPorSeccion = new HashMap<>();
        List<BrokerMetadata> todaMetadata = obtenerMetadataPorFormato(formatoId);
        
        for (BrokerMetadata metadata : todaMetadata) {
            String seccion = metadata.getSeccion();
            if (seccion != null) {
                metadataPorSeccion.computeIfAbsent(seccion, k -> new ArrayList<>()).add(metadata);
            }
        }
        
        logger.info("Metadata organizada en {} secciones para formato ID {}", metadataPorSeccion.size(), formatoId);
        
        return metadataPorSeccion;
    }
    
    /**
     * Obtiene la metadata de una sección específica
     * @param formatoId ID del formato
     * @param seccion Nombre de la sección
     * @return Lista de metadata de la sección
     */
    public List<BrokerMetadata> obtenerMetadataPorSeccionEspecifica(int formatoId, String seccion) {
        List<BrokerMetadata> metadataList = new ArrayList<>();
        String sql = "SELECT metadata_id, formato_id, seccion, campo_nombre, campo_valor, " +
                     "fila_origen, columna_origen, letra_columna, fecha_creacion " +
                     "FROM broker_metadata " +
                     "WHERE formato_id = ? AND seccion = ? " +
                     "ORDER BY metadata_id";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, formatoId);
            stmt.setString(2, seccion);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BrokerMetadata metadata = new BrokerMetadata();
                    metadata.setMetadataId(rs.getInt("metadata_id"));
                    metadata.setFormatoId(rs.getInt("formato_id"));
                    metadata.setSeccion(rs.getString("seccion"));
                    metadata.setCampoNombre(rs.getString("campo_nombre"));
                    metadata.setCampoValor(rs.getString("campo_valor"));
                    metadata.setFilaOrigen(rs.getInt("fila_origen"));
                    metadata.setColumnaOrigen(rs.getInt("columna_origen"));
                    metadata.setLetraColumna(rs.getString("letra_columna"));
                    
                    Timestamp ts = rs.getTimestamp("fecha_creacion");
                    if (ts != null) {
                        metadata.setFechaCreacion(ts.toLocalDateTime());
                    }
                    
                    metadataList.add(metadata);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error al obtener metadata de la sección {} del formato ID {}", seccion, formatoId, e);
        }
        
        return metadataList;
    }
    
    /**
     * Obtiene un valor específico de metadata por nombre de campo
     * @param formatoId ID del formato
     * @param campoNombre Nombre del campo
     * @return Valor del campo o null si no existe
     */
    public String obtenerValorCampo(int formatoId, String campoNombre) {
        String sql = "SELECT campo_valor FROM broker_metadata " +
                     "WHERE formato_id = ? AND campo_nombre = ? " +
                     "LIMIT 1";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, formatoId);
            stmt.setString(2, campoNombre);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("campo_valor");
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error al obtener valor del campo {} del formato ID {}", campoNombre, formatoId, e);
        }
        
        return null;
    }
}
