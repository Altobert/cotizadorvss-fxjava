package cl.vss.cotizador.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado del procesamiento de archivos de cotizaciones
 * Incluye estadísticas, errores y datos extraídos
 */
public class ResultadoProcesamiento {
    
    private List<Cotizacion> cotizacionesExitosas;
    private List<ErrorProcesamiento> errores;
    private EstadisticasProcesamiento estadisticas;
    
    public ResultadoProcesamiento() {
        this.cotizacionesExitosas = new ArrayList<>();
        this.errores = new ArrayList<>();
        this.estadisticas = new EstadisticasProcesamiento();
    }
    
    public void agregarCotizacion(Cotizacion cotizacion) {
        this.cotizacionesExitosas.add(cotizacion);
    }
    
    public void agregarError(ErrorProcesamiento error) {
        this.errores.add(error);
    }
    
    public List<Cotizacion> getCotizacionesExitosas() {
        return cotizacionesExitosas;
    }
    
    public List<ErrorProcesamiento> getErrores() {
        return errores;
    }
    
    public EstadisticasProcesamiento getEstadisticas() {
        return estadisticas;
    }
    
    public void setEstadisticas(EstadisticasProcesamiento estadisticas) {
        this.estadisticas = estadisticas;
    }
    
    public boolean tieneErrores() {
        return !errores.isEmpty();
    }
    
    public int getCantidadCotizaciones() {
        return cotizacionesExitosas.size();
    }
    
    public int getCantidadErrores() {
        return errores.size();
    }
    
    /**
     * Clase interna para errores de procesamiento
     */
    public static class ErrorProcesamiento {
        private String archivo;
        private String hoja;
        private Integer fila;
        private String tipo;
        private String mensaje;
        private String detallesTecnicos;
        
        public ErrorProcesamiento(String archivo, String mensaje) {
            this.archivo = archivo;
            this.mensaje = mensaje;
            this.tipo = "ERROR_ARCHIVO";
        }
        
        public ErrorProcesamiento(String archivo, String hoja, String mensaje) {
            this.archivo = archivo;
            this.hoja = hoja;
            this.mensaje = mensaje;
            this.tipo = "ERROR_HOJA";
        }
        
        public ErrorProcesamiento(String archivo, String hoja, Integer fila, String mensaje) {
            this.archivo = archivo;
            this.hoja = hoja;
            this.fila = fila;
            this.mensaje = mensaje;
            this.tipo = "ERROR_FILA";
        }
        
        // Getters y Setters
        public String getArchivo() { return archivo; }
        public void setArchivo(String archivo) { this.archivo = archivo; }
        
        public String getHoja() { return hoja; }
        public void setHoja(String hoja) { this.hoja = hoja; }
        
        public Integer getFila() { return fila; }
        public void setFila(Integer fila) { this.fila = fila; }
        
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        
        public String getMensaje() { return mensaje; }
        public void setMensaje(String mensaje) { this.mensaje = mensaje; }
        
        public String getDetallesTecnicos() { return detallesTecnicos; }
        public void setDetallesTecnicos(String detallesTecnicos) { this.detallesTecnicos = detallesTecnicos; }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(tipo).append("] ");
            sb.append(archivo);
            if (hoja != null) sb.append(" - Hoja: ").append(hoja);
            if (fila != null) sb.append(" - Fila: ").append(fila);
            sb.append(" - ").append(mensaje);
            return sb.toString();
        }
    }
    
    /**
     * Clase interna para estadísticas de procesamiento
     */
    public static class EstadisticasProcesamiento {
        private int archivosEncontrados;
        private int archivosProcesadosExitosamente;
        private int archivosConErrores;
        private int hojasProcesadas;
        private int cotizacionesExtraidas;
        private long tiempoProcesamientoMs;
        
        public int getArchivosEncontrados() { return archivosEncontrados; }
        public void setArchivosEncontrados(int archivosEncontrados) { 
            this.archivosEncontrados = archivosEncontrados; 
        }
        
        public int getArchivosProcesadosExitosamente() { return archivosProcesadosExitosamente; }
        public void setArchivosProcesadosExitosamente(int archivosProcesadosExitosamente) { 
            this.archivosProcesadosExitosamente = archivosProcesadosExitosamente; 
        }
        
        public int getArchivosConErrores() { return archivosConErrores; }
        public void setArchivosConErrores(int archivosConErrores) { 
            this.archivosConErrores = archivosConErrores; 
        }
        
        public int getHojasProcesadas() { return hojasProcesadas; }
        public void setHojasProcesadas(int hojasProcesadas) { 
            this.hojasProcesadas = hojasProcesadas; 
        }
        
        public int getCotizacionesExtraidas() { return cotizacionesExtraidas; }
        public void setCotizacionesExtraidas(int cotizacionesExtraidas) { 
            this.cotizacionesExtraidas = cotizacionesExtraidas; 
        }
        
        public long getTiempoProcesamientoMs() { return tiempoProcesamientoMs; }
        public void setTiempoProcesamientoMs(long tiempoProcesamientoMs) { 
            this.tiempoProcesamientoMs = tiempoProcesamientoMs; 
        }
        
        public void incrementarArchivosProcesados() {
            archivosProcesadosExitosamente++;
        }
        
        public void incrementarArchivosConErrores() {
            archivosConErrores++;
        }
        
        public void incrementarHojasProcesadas() {
            hojasProcesadas++;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Archivos: %d encontrados, %d exitosos, %d con errores | " +
                "Hojas: %d | Cotizaciones: %d | Tiempo: %dms",
                archivosEncontrados, archivosProcesadosExitosamente, 
                archivosConErrores, hojasProcesadas, cotizacionesExtraidas,
                tiempoProcesamientoMs
            );
        }
    }
}
