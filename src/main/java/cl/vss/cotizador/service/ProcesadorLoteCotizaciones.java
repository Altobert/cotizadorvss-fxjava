package cl.vss.cotizador.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cl.vss.cotizador.detector.*;
import cl.vss.cotizador.model.Cotizacion;
import cl.vss.cotizador.model.ResultadoProcesamiento;
import cl.vss.cotizador.model.ResultadoProcesamiento.ErrorProcesamiento;
import cl.vss.cotizador.model.ResultadoProcesamiento.EstadisticasProcesamiento;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Servicio para procesar múltiples archivos de cotizaciones
 * Implementa los puntos 2, 3 y 4:
 * - Metadatos de origen
 * - Detectores específicos por formato
 * - Manejo robusto de errores
 */
public class ProcesadorLoteCotizaciones {
    
    private static final Logger logger = LogManager.getLogger(ProcesadorLoteCotizaciones.class);
    private List<DetectorFormatoCotizacion> detectores;
    
    public ProcesadorLoteCotizaciones() {
        inicializarDetectores();
    }
    
    /**
     * Inicializar detectores ordenados por prioridad
     */
    private void inicializarDetectores() {
        detectores = new ArrayList<>();
        
        // Agregar detectores específicos
        detectores.add(new DetectorOneSphere());
        
        // Detector genérico siempre al final (baja prioridad)
        detectores.add(new DetectorGenerico());
        
        // Ordenar por prioridad descendente
        detectores.sort(Comparator.comparingInt(DetectorFormatoCotizacion::getPrioridad).reversed());
        
        logger.info("Detectores inicializados: " + detectores.size());
    }
    
    /**
     * Agregar un detector personalizado
     */
    public void agregarDetector(DetectorFormatoCotizacion detector) {
        detectores.add(detector);
        detectores.sort(Comparator.comparingInt(DetectorFormatoCotizacion::getPrioridad).reversed());
    }
    
    /**
     * Procesar todos los archivos Excel en un directorio
     */
    public ResultadoProcesamiento procesarDirectorio(String rutaDirectorio) {
        long tiempoInicio = System.currentTimeMillis();
        ResultadoProcesamiento resultado = new ResultadoProcesamiento();
        
        File directorio = new File(rutaDirectorio);
        
        if (!directorio.exists() || !directorio.isDirectory()) {
            resultado.agregarError(new ErrorProcesamiento(
                rutaDirectorio, 
                "El directorio no existe o no es válido"
            ));
            return resultado;
        }
        
        // Filtrar archivos Excel
        File[] archivos = directorio.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".xlsx") || 
            name.toLowerCase().endsWith(".xls")
        );
        
        if (archivos == null || archivos.length == 0) {
            resultado.agregarError(new ErrorProcesamiento(
                rutaDirectorio, 
                "No se encontraron archivos Excel en el directorio"
            ));
            return resultado;
        }
        
        EstadisticasProcesamiento stats = resultado.getEstadisticas();
        stats.setArchivosEncontrados(archivos.length);
        
        logger.info(String.format("Procesando %d archivos en: %s", archivos.length, rutaDirectorio));
        
        // Procesar cada archivo
        for (File archivo : archivos) {
            try {
                logger.info("Procesando: " + archivo.getName());
                List<Cotizacion> cotizaciones = procesarArchivo(archivo.getAbsolutePath());
                
                if (!cotizaciones.isEmpty()) {
                    resultado.getCotizacionesExitosas().addAll(cotizaciones);
                    stats.incrementarArchivosProcesados();
                    logger.info(String.format("✓ %s: %d cotizaciones extraídas", 
                        archivo.getName(), cotizaciones.size()));
                } else {
                    stats.incrementarArchivosConErrores();
                    resultado.agregarError(new ErrorProcesamiento(
                        archivo.getName(), 
                        "No se pudieron extraer cotizaciones del archivo"
                    ));
                    logger.warn("✗ " + archivo.getName() + ": Sin datos extraídos");
                }
                
            } catch (Exception e) {
                stats.incrementarArchivosConErrores();
                ErrorProcesamiento error = new ErrorProcesamiento(
                    archivo.getName(), 
                    "Error al procesar archivo: " + e.getMessage()
                );
                error.setDetallesTecnicos(e.getClass().getName());
                resultado.agregarError(error);
                
                logger.warn( "Error procesando " + archivo.getName(), e);
            }
        }
        
        // Calcular estadísticas finales
        long tiempoFin = System.currentTimeMillis();
        stats.setTiempoProcesamientoMs(tiempoFin - tiempoInicio);
        stats.setCotizacionesExtraidas(resultado.getCotizacionesExitosas().size());
        
        logger.info("Procesamiento completado: " + stats.toString());
        
        return resultado;
    }
    
    /**
     * Procesar un solo archivo
     */
    public List<Cotizacion> procesarArchivo(String rutaArchivo) throws IOException {
        List<Cotizacion> todasLasCotizaciones = new ArrayList<>();
        
        File archivo = new File(rutaArchivo);
        String nombreArchivo = archivo.getName();
        
        try (FileInputStream fis = new FileInputStream(rutaArchivo)) {
            Workbook workbook = null;
            
            // Determinar tipo de archivo
            if (rutaArchivo.toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else if (rutaArchivo.toLowerCase().endsWith(".xls")) {
                workbook = new HSSFWorkbook(fis);
            } else {
                throw new IllegalArgumentException("Formato no soportado: " + rutaArchivo);
            }
            
            logger.info(String.format("  Archivo abierto: %d hojas encontradas", 
                workbook.getNumberOfSheets()));
            
            // Procesar cada hoja
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                logger.info(String.format("  Procesando hoja %d/%d: %s", 
                    i + 1, workbook.getNumberOfSheets(), sheet.getSheetName()));
                
                try {
                    List<Cotizacion> cotizacionesHoja = procesarHoja(sheet, nombreArchivo);
                    todasLasCotizaciones.addAll(cotizacionesHoja);
                    
                    logger.info(String.format("    ✓ %d cotizaciones extraídas", 
                        cotizacionesHoja.size()));
                    
                } catch (Exception e) {
                    logger.warn( 
                        String.format("    ✗ Error en hoja %s: %s", 
                            sheet.getSheetName(), e.getMessage()), e);
                    // Continuar con la siguiente hoja
                }
            }
            
            workbook.close();
            
        } catch (OutOfMemoryError e) {
            logger.error("Error de memoria procesando: " + nombreArchivo);
            throw new IOException("Archivo demasiado grande: " + nombreArchivo, e);
        }
        
        return todasLasCotizaciones;
    }
    
    /**
     * Procesar una hoja usando el detector apropiado
     */
    private List<Cotizacion> procesarHoja(Sheet sheet, String nombreArchivo) {
        // Buscar el detector apropiado
        for (DetectorFormatoCotizacion detector : detectores) {
            if (detector.puedeDetectar(sheet)) {
                logger.info(String.format("    Usando detector: %s", 
                    detector.getNombreDetector()));
                
                List<Cotizacion> cotizaciones = detector.extraerCotizaciones(sheet, nombreArchivo);
                
                // Validar y enriquecer metadatos
                for (Cotizacion cot : cotizaciones) {
                    if (cot.getArchivoOrigen() == null) {
                        cot.setArchivoOrigen(nombreArchivo);
                    }
                    if (cot.getHojaOrigen() == null) {
                        cot.setHojaOrigen(sheet.getSheetName());
                    }
                    if (cot.getTipoDetector() == null) {
                        cot.setTipoDetector(detector.getNombreDetector());
                    }
                }
                
                return cotizaciones;
            }
        }
        
        // Si ningún detector puede procesar, retornar lista vacía
        logger.warn("    No se encontró detector compatible para esta hoja");
        return new ArrayList<>();
    }
    
    /**
     * Obtener reporte detallado del procesamiento
     */
    public String generarReporte(ResultadoProcesamiento resultado) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("\n╔════════════════════════════════════════════════════════════════╗\n");
        sb.append("║          REPORTE DE PROCESAMIENTO DE COTIZACIONES             ║\n");
        sb.append("╚════════════════════════════════════════════════════════════════╝\n\n");
        
        EstadisticasProcesamiento stats = resultado.getEstadisticas();
        
        sb.append("📊 ESTADÍSTICAS\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append(String.format("  Archivos encontrados:    %d\n", stats.getArchivosEncontrados()));
        sb.append(String.format("  Archivos procesados OK:  %d\n", stats.getArchivosProcesadosExitosamente()));
        sb.append(String.format("  Archivos con errores:    %d\n", stats.getArchivosConErrores()));
        sb.append(String.format("  Hojas procesadas:        %d\n", stats.getHojasProcesadas()));
        sb.append(String.format("  Cotizaciones extraídas:  %d\n", stats.getCotizacionesExtraidas()));
        sb.append(String.format("  Tiempo de proceso:       %.2f segundos\n\n", 
            stats.getTiempoProcesamientoMs() / 1000.0));
        
        if (resultado.tieneErrores()) {
            sb.append("❌ ERRORES ENCONTRADOS\n");
            sb.append("───────────────────────────────────────────────────────────────\n");
            for (ErrorProcesamiento error : resultado.getErrores()) {
                sb.append("  • ").append(error.toString()).append("\n");
            }
            sb.append("\n");
        }
        
        if (resultado.getCantidadCotizaciones() > 0) {
            sb.append("✅ COTIZACIONES EXTRAÍDAS POR TIPO\n");
            sb.append("───────────────────────────────────────────────────────────────\n");
            
            // Agrupar por tipo de detector
            resultado.getCotizacionesExitosas().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    c -> c.getTipoDetector() != null ? c.getTipoDetector() : "DESCONOCIDO",
                    java.util.stream.Collectors.counting()
                ))
                .forEach((tipo, count) -> 
                    sb.append(String.format("  %s: %d cotizaciones\n", tipo, count))
                );
            sb.append("\n");
        }
        
        return sb.toString();
    }
}
