package cl.vss.cotizador.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo que representa el formato de un broker con sus columnas
 */
public class BrokerFormato {
    private Integer formatoId;
    private Integer brokerId;
    private String brokerName;
    private String version;
    private Integer headerRow;
    private String descripcion;
    private String archivoEjemplo;
    private String rutaPlantilla;
    private Boolean activo;
    private List<FormatoColumna> columnas;
    
    public BrokerFormato() {
        this.columnas = new ArrayList<>();
    }
    
    // Getters y Setters
    public Integer getFormatoId() {
        return formatoId;
    }
    
    public void setFormatoId(Integer formatoId) {
        this.formatoId = formatoId;
    }
    
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
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public Integer getHeaderRow() {
        return headerRow;
    }
    
    public void setHeaderRow(Integer headerRow) {
        this.headerRow = headerRow;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public String getArchivoEjemplo() {
        return archivoEjemplo;
    }
    
    public void setArchivoEjemplo(String archivoEjemplo) {
        this.archivoEjemplo = archivoEjemplo;
    }
    
    public String getRutaPlantilla() {
        return rutaPlantilla;
    }
    
    public void setRutaPlantilla(String rutaPlantilla) {
        this.rutaPlantilla = rutaPlantilla;
    }
    
    public Boolean getActivo() {
        return activo;
    }
    
    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
    
    public List<FormatoColumna> getColumnas() {
        return columnas;
    }
    
    public void setColumnas(List<FormatoColumna> columnas) {
        this.columnas = columnas;
    }
    
    public void agregarColumna(FormatoColumna columna) {
        this.columnas.add(columna);
    }
    
    @Override
    public String toString() {
        return "BrokerFormato{" +
                "formatoId=" + formatoId +
                ", brokerName='" + brokerName + '\'' +
                ", version='" + version + '\'' +
                ", headerRow=" + headerRow +
                ", columnas=" + columnas.size() +
                '}';
    }
}
