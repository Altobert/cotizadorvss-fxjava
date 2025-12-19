package cl.vss.cotizador.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Modelo para almacenar metadatos de la cabecera del archivo Excel
 * Incluye información del cliente, número de cotización, fecha, etc.
 */
public class CabeceraCotizacion {
    
    private String nombreCliente;
    private String idCliente;
    private String empresaCliente;
    private String numeroCotizacion;
    private String referencia;
    private LocalDateTime fecha;
    private String observaciones;
    private String estado;
    
    // Almacenar datos adicionales encontrados
    private Map<String, String> datosAdicionales;
    
    // Constructor vacío
    public CabeceraCotizacion() {
        this.datosAdicionales = new HashMap<>();
    }
    
    // Getters y Setters
    public String getNombreCliente() {
        return nombreCliente;
    }
    
    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }
    
    public String getIdCliente() {
        return idCliente;
    }
    
    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente;
    }
    
    public String getEmpresaCliente() {
        return empresaCliente;
    }
    
    public void setEmpresaCliente(String empresaCliente) {
        this.empresaCliente = empresaCliente;
    }
    
    public String getNumeroCotizacion() {
        return numeroCotizacion;
    }
    
    public void setNumeroCotizacion(String numeroCotizacion) {
        this.numeroCotizacion = numeroCotizacion;
    }
    
    public String getReferencia() {
        return referencia;
    }
    
    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }
    
    public LocalDateTime getFecha() {
        return fecha;
    }
    
    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
    
    public String getObservaciones() {
        return observaciones;
    }
    
    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
    
    public String getEstado() {
        return estado;
    }
    
    public void setEstado(String estado) {
        this.estado = estado;
    }
    
    public Map<String, String> getDatosAdicionales() {
        return datosAdicionales;
    }
    
    public void agregarDatoAdicional(String clave, String valor) {
        this.datosAdicionales.put(clave, valor);
    }
    
    public String obtenerDatoAdicional(String clave) {
        return this.datosAdicionales.get(clave);
    }
    
    public boolean tieneData() {
        return nombreCliente != null || idCliente != null || 
               empresaCliente != null || numeroCotizacion != null ||
               !datosAdicionales.isEmpty();
    }
    
    @Override
    public String toString() {
        return "CabeceraCotizacion{" +
                "nombreCliente='" + nombreCliente + '\'' +
                ", idCliente='" + idCliente + '\'' +
                ", empresaCliente='" + empresaCliente + '\'' +
                ", numeroCotizacion='" + numeroCotizacion + '\'' +
                ", referencia='" + referencia + '\'' +
                ", fecha=" + fecha +
                ", observaciones='" + observaciones + '\'' +
                ", estado='" + estado + '\'' +
                ", datosAdicionales=" + datosAdicionales +
                '}';
    }
}
