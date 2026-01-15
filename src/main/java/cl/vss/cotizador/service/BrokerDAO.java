package cl.vss.cotizador.service;

import cl.vss.cotizador.model.Broker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para gestionar operaciones con la tabla brokers
 */
public class BrokerDAO {
    private static final Logger logger = LogManager.getLogger(BrokerDAO.class);
    private final Connection connection;
    
    public BrokerDAO(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Lista todos los brokers activos del sistema
     * @return Lista de brokers activos
     */
    public List<Broker> listarBrokersActivos() {
        List<Broker> brokers = new ArrayList<>();
        String sql = "SELECT broker_id, broker_name, descripcion, contacto, email, " +
                     "activo, fecha_creacion, fecha_actualizacion " +
                     "FROM brokers WHERE activo = true " +
                     "ORDER BY broker_name";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Broker broker = new Broker();
                broker.setBrokerId(rs.getInt("broker_id"));
                broker.setBrokerName(rs.getString("broker_name"));
                broker.setDescripcion(rs.getString("descripcion"));
                broker.setContacto(rs.getString("contacto"));
                broker.setEmail(rs.getString("email"));
                broker.setActivo(rs.getBoolean("activo"));
                
                Timestamp tsCreacion = rs.getTimestamp("fecha_creacion");
                if (tsCreacion != null) {
                    broker.setFechaCreacion(tsCreacion.toLocalDateTime());
                }
                
                Timestamp tsActualizacion = rs.getTimestamp("fecha_actualizacion");
                if (tsActualizacion != null) {
                    broker.setFechaActualizacion(tsActualizacion.toLocalDateTime());
                }
                
                brokers.add(broker);
            }
            
            logger.info("Se cargaron {} brokers activos", brokers.size());
            
        } catch (SQLException e) {
            logger.error("Error al listar brokers activos", e);
        }
        
        return brokers;
    }
    
    /**
     * Lista todos los brokers (activos e inactivos)
     * @return Lista de todos los brokers
     */
    public List<Broker> listarTodosBrokers() {
        List<Broker> brokers = new ArrayList<>();
        String sql = "SELECT broker_id, broker_name, descripcion, contacto, email, " +
                     "activo, fecha_creacion, fecha_actualizacion " +
                     "FROM brokers " +
                     "ORDER BY broker_name";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Broker broker = new Broker();
                broker.setBrokerId(rs.getInt("broker_id"));
                broker.setBrokerName(rs.getString("broker_name"));
                broker.setDescripcion(rs.getString("descripcion"));
                broker.setContacto(rs.getString("contacto"));
                broker.setEmail(rs.getString("email"));
                broker.setActivo(rs.getBoolean("activo"));
                
                Timestamp tsCreacion = rs.getTimestamp("fecha_creacion");
                if (tsCreacion != null) {
                    broker.setFechaCreacion(tsCreacion.toLocalDateTime());
                }
                
                Timestamp tsActualizacion = rs.getTimestamp("fecha_actualizacion");
                if (tsActualizacion != null) {
                    broker.setFechaActualizacion(tsActualizacion.toLocalDateTime());
                }
                
                brokers.add(broker);
            }
            
            logger.info("Se cargaron {} brokers en total", brokers.size());
            
        } catch (SQLException e) {
            logger.error("Error al listar todos los brokers", e);
        }
        
        return brokers;
    }
    
    /**
     * Obtiene un broker por su ID
     * @param brokerId ID del broker
     * @return Broker encontrado o null
     */
    public Broker obtenerBrokerPorId(Integer brokerId) {
        String sql = "SELECT broker_id, broker_name, descripcion, contacto, email, " +
                     "activo, fecha_creacion, fecha_actualizacion " +
                     "FROM brokers WHERE broker_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, brokerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Broker broker = new Broker();
                    broker.setBrokerId(rs.getInt("broker_id"));
                    broker.setBrokerName(rs.getString("broker_name"));
                    broker.setDescripcion(rs.getString("descripcion"));
                    broker.setContacto(rs.getString("contacto"));
                    broker.setEmail(rs.getString("email"));
                    broker.setActivo(rs.getBoolean("activo"));
                    
                    Timestamp tsCreacion = rs.getTimestamp("fecha_creacion");
                    if (tsCreacion != null) {
                        broker.setFechaCreacion(tsCreacion.toLocalDateTime());
                    }
                    
                    Timestamp tsActualizacion = rs.getTimestamp("fecha_actualizacion");
                    if (tsActualizacion != null) {
                        broker.setFechaActualizacion(tsActualizacion.toLocalDateTime());
                    }
                    
                    return broker;
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error al obtener broker por ID: {}", brokerId, e);
        }
        
        return null;
    }
}
