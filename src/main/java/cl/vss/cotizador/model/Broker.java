package cl.vss.cotizador.model;

import java.time.LocalDateTime;

/**
 * Modelo que representa un Broker del sistema
 */
public class Broker {
    private Integer brokerId;
    private String brokerName;
    private String descripcion;
    private String contacto;
    private String email;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    
    // Constructor vacío
    public Broker() {
    }
    
    // Constructor completo
    public Broker(Integer brokerId, String brokerName, String descripcion, 
                  String contacto, String email, Boolean activo,
                  LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
        this.brokerId = brokerId;
        this.brokerName = brokerName;
        this.descripcion = descripcion;
        this.contacto = contacto;
        this.email = email;
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }
    
    // Getters y Setters
    public Integer getBrokerId() {
        return brokerId;
    }
    
    public void setBrokerId(Integer brokerId) {
        this.brokerId = brokerId;
    }
    
    public String getBrokerName() {
        return brokerName;
    }
    
    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getContacto() {
        return contacto;
    }
    
    public void setContacto(String contacto) {
        this.contacto = contacto;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Boolean getActivo() {
        return activo;
    }
    
    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
    
    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
    
    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
    
    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
    
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
    
    @Override
    public String toString() {
        return brokerName; // Para mostrar en el ComboBox
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Broker broker = (Broker) o;
        return brokerId != null && brokerId.equals(broker.brokerId);
    }
    
    @Override
    public int hashCode() {
        return brokerId != null ? brokerId.hashCode() : 0;
    }
}
