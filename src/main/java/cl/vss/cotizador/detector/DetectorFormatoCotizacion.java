package cl.vss.cotizador.detector;

import cl.vss.cotizador.model.Cotizacion;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.List;

/**
 * Interfaz para detectores de formato de cotizaciones
 * Implementa el patrón Strategy para manejar diferentes formatos de Excel
 */
public interface DetectorFormatoCotizacion {
    
    /**
     * Verifica si este detector puede procesar la hoja dada
     * @param sheet La hoja de Excel a verificar
     * @return true si este detector puede procesar la hoja
     */
    boolean puedeDetectar(Sheet sheet);
    
    /**
     * Extrae cotizaciones de la hoja usando el formato específico
     * @param sheet La hoja de Excel a procesar
     * @param archivoOrigen Nombre del archivo de origen
     * @return Lista de cotizaciones extraídas
     */
    List<Cotizacion> extraerCotizaciones(Sheet sheet, String archivoOrigen);
    
    /**
     * Retorna el nombre del detector (para metadatos)
     * @return Nombre del detector
     */
    String getNombreDetector();
    
    /**
     * Retorna la prioridad del detector (mayor = más prioritario)
     * Detectores más específicos deben tener mayor prioridad
     * @return Nivel de prioridad (0-100)
     */
    int getPrioridad();
}
