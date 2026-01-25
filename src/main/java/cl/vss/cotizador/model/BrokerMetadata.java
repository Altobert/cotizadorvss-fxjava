package cl.vss.cotizador.model;

import java.time.LocalDateTime;

/**
 * Modelo que representa la metadata de un broker
 * Contiene información adicional como datos de compañía, RFQ, vendor, etc.
 */
public class BrokerMetadata {
    private Integer metadataId;
    private Integer formatoId;
    private String seccion;
    private String campoNombre;
    private String campoValor;
    private Integer filaOrigen;
    private Integer columnaOrigen;
    private String letraColumna;
    private LocalDateTime fechaCreacion;
    
    // Constructor vacío
    public BrokerMetadata() {
    }
    
    // Constructor completo
    public BrokerMetadata(Integer metadataId, Integer formatoId, String seccion, 
                         String campoNombre, String campoValor, Integer filaOrigen,
                         Integer columnaOrigen, String letraColumna, LocalDateTime fechaCreacion) {
        this.metadataId = metadataId;
        this.formatoId = formatoId;
        this.seccion = seccion;
        this.campoNombre = campoNombre;
        this.campoValor = campoValor;
        this.filaOrigen = filaOrigen;
        this.columnaOrigen = columnaOrigen;
        this.letraColumna = letraColumna;
        this.fechaCreacion = fechaCreacion;
    }
    
    // Getters y Setters
    public Integer getMetadataId() {
        return metadataId;
    }
    
    public void setMetadataId(Integer metadataId) {
        this.metadataId = metadataId;
    }
    
    public Integer getFormatoId() {
        return formatoId;
    }
    
    public void setFormatoId(Integer formatoId) {
        this.formatoId = formatoId;
    }
    
    public String getSeccion() {
        return seccion;
    }
    
    public void setSeccion(String seccion) {
        this.seccion = seccion;
    }
    
    public String getCampoNombre() {
        return campoNombre;
    }
    
    public void setCampoNombre(String campoNombre) {
        this.campoNombre = campoNombre;
    }
    
    public String getCampoValor() {
        return campoValor;
    }
    
    public void setCampoValor(String campoValor) {
        this.campoValor = campoValor;
    }
    
    public Integer getFilaOrigen() {
        return filaOrigen;
    }
    
    public void setFilaOrigen(Integer filaOrigen) {
        this.filaOrigen = filaOrigen;
    }
    
    public Integer getColumnaOrigen() {
        return columnaOrigen;
    }
    
    public void setColumnaOrigen(Integer columnaOrigen) {
        this.columnaOrigen = columnaOrigen;
    }
    
    public String getLetraColumna() {
        return letraColumna;
    }
    
    public void setLetraColumna(String letraColumna) {
        this.letraColumna = letraColumna;
    }
    
    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
    
    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
    
    @Override
    public String toString() {
        return "BrokerMetadata{" +
                "metadataId=" + metadataId +
                ", formatoId=" + formatoId +
                ", seccion='" + seccion + '\'' +
                ", campoNombre='" + campoNombre + '\'' +
                ", campoValor='" + campoValor + '\'' +
                ", filaOrigen=" + filaOrigen +
                ", columnaOrigen=" + columnaOrigen +
                ", letraColumna='" + letraColumna + '\'' +
                ", fechaCreacion=" + fechaCreacion +
                '}';
    }
}
