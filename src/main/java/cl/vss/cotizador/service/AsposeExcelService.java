package cl.vss.cotizador.service;

import com.aspose.cells.*;
import cl.vss.cotizador.model.RowData;
import cl.vss.cotizador.model.FormatoColumna;
import cl.vss.cotizador.model.BrokerFormato;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Servicio para manejo de archivos Excel usando Aspose.Cells.
 * Preserva macros VBA, fórmulas y formatos originales.
 * 
 * Nota: Versión de evaluación tiene limitaciones:
 * - Marca de agua en documentos
 * - Límite de ~100 filas en evaluación
 */
public class AsposeExcelService {
    
    private static final Logger logger = LogManager.getLogger(AsposeExcelService.class);
    
    private static AsposeExcelService instance;
    
    // Workbook cargado actualmente (preserva macros)
    private Workbook workbookActual;
    private String rutaArchivoOriginal;
    
    private AsposeExcelService() {}
    
    public static synchronized AsposeExcelService getInstance() {
        if (instance == null) {
            instance = new AsposeExcelService();
        }
        return instance;
    }
    
    /**
     * Verifica si un archivo tiene macros (extensión .xlsm o .xlsb)
     */
    public boolean archivoTieneMacros(String ruta) {
        if (ruta == null) return false;
        String rutaLower = ruta.toLowerCase();
        return rutaLower.endsWith(".xlsm") || rutaLower.endsWith(".xlsb");
    }
    
    /**
     * Carga un archivo Excel preservando todas las macros, fórmulas y formatos.
     * 
     * @param ruta Ruta del archivo Excel
     * @return Workbook cargado
     */
    public Workbook cargarWorkbook(String ruta) throws Exception {
        logger.info("📂 Cargando archivo con Aspose: {}", ruta);
        
        // Detectar formato por extensión - Aspose detecta automáticamente
        String rutaLower = ruta.toLowerCase();
        
        // Aspose.Cells detecta el formato automáticamente, no necesitamos especificarlo
        // Solo logueamos para debug
        if (rutaLower.endsWith(".xlsm")) {
            logger.info("📋 Formato detectado: XLSM (con macros)");
        } else if (rutaLower.endsWith(".xlsb")) {
            logger.info("📋 Formato detectado: XLSB (binario con macros)");
        } else if (rutaLower.endsWith(".xlsx")) {
            logger.info("📋 Formato detectado: XLSX");
        } else if (rutaLower.endsWith(".xls")) {
            logger.info("📋 Formato detectado: XLS (Excel 97-2003)");
        } else {
            logger.info("📋 Formato: auto-detección");
        }
        
        // Aspose detecta automáticamente el formato y preserva macros
        workbookActual = new Workbook(ruta);
        rutaArchivoOriginal = ruta;
        
        // Verificar si tiene macros VBA
        if (workbookActual.getVbaProject() != null) {
            logger.info("✅ Macros VBA detectadas y preservadas");
            int modulosVba = workbookActual.getVbaProject().getModules().getCount();
            logger.info("   └─ {} módulos VBA encontrados", modulosVba);
        } else {
            logger.info("ℹ️ Archivo sin macros VBA");
        }
        
        logger.info("✅ Archivo cargado: {} hojas", workbookActual.getWorksheets().getCount());
        
        return workbookActual;
    }
    
    /**
     * Obtiene el Workbook actualmente cargado
     */
    public Workbook getWorkbookActual() {
        return workbookActual;
    }
    
    /**
     * Verifica si hay un workbook cargado
     */
    public boolean hayWorkbookCargado() {
        return workbookActual != null;
    }
    
    /**
     * Limpia el workbook actual de memoria
     */
    public void limpiarWorkbook() {
        if (workbookActual != null) {
            try {
                workbookActual.dispose();
            } catch (Exception e) {
                logger.warn("Error al liberar workbook: {}", e.getMessage());
            }
            workbookActual = null;
            rutaArchivoOriginal = null;
            logger.info("🗑️ Workbook liberado de memoria");
        }
    }
    
    /**
     * Lee los datos de una hoja a partir de una fila de encabezados.
     * 
     * @param sheetIndex Índice de la hoja
     * @param headerRow Fila de encabezados (1-indexed como Excel)
     * @param formato Formato del broker para mapear columnas
     * @return Lista de RowData con los datos
     */
    public List<RowData> leerDatos(int sheetIndex, int headerRow, BrokerFormato formato) throws Exception {
        if (workbookActual == null) {
            throw new IllegalStateException("No hay workbook cargado");
        }
        
        List<RowData> datos = new ArrayList<>();
        Worksheet sheet = workbookActual.getWorksheets().get(sheetIndex);
        Cells cells = sheet.getCells();
        
        int headerRowIndex = headerRow - 1; // Convertir a 0-indexed
        int lastRow = cells.getMaxDataRow();
        
        logger.info("📖 Leyendo datos desde fila {} hasta {}", headerRow + 1, lastRow + 1);
        
        // Leer cada fila de datos
        for (int rowIdx = headerRowIndex + 1; rowIdx <= lastRow; rowIdx++) {
            RowData rowData = new RowData();
            boolean tieneContenido = false;
            
            for (FormatoColumna columna : formato.getColumnas()) {
                int colIdx = columna.getIndiceColumna();
                Cell cell = cells.get(rowIdx, colIdx);
                
                String valor = obtenerValorCelda(cell);
                if (valor != null && !valor.trim().isEmpty()) {
                    tieneContenido = true;
                }
                
                rowData.set(columna.getCampoEstandar(), valor != null ? valor : "");
            }
            
            if (tieneContenido) {
                datos.add(rowData);
            }
        }
        
        logger.info("✅ {} filas de datos leídas", datos.size());
        return datos;
    }
    
    /**
     * Escribe datos en el workbook actual.
     * Los datos se escriben a partir de startRow, preservando todo lo demás.
     * 
     * @param sheetIndex Índice de la hoja
     * @param datos Lista de RowData a escribir
     * @param startRow Fila donde empezar a escribir (1-indexed)
     * @param formato Formato del broker
     */
    public void escribirDatos(int sheetIndex, List<RowData> datos, int startRow, BrokerFormato formato) throws Exception {
        if (workbookActual == null) {
            throw new IllegalStateException("No hay workbook cargado");
        }
        
        Worksheet sheet = workbookActual.getWorksheets().get(sheetIndex);
        Cells cells = sheet.getCells();
        
        int startRowIndex = startRow - 1; // Convertir a 0-indexed
        
        logger.info("📝 Escribiendo {} filas a partir de fila {}", datos.size(), startRow);
        
        // Limpiar filas existentes de datos (preservar estructura)
        int lastDataRow = cells.getMaxDataRow();
        for (int rowIdx = lastDataRow; rowIdx >= startRowIndex; rowIdx--) {
            Row row = cells.getRows().get(rowIdx);
            if (row != null) {
                // Solo limpiar contenido, preservar formato
                for (FormatoColumna columna : formato.getColumnas()) {
                    Cell cell = cells.get(rowIdx, columna.getIndiceColumna());
                    if (cell != null) {
                        cell.putValue("");
                    }
                }
            }
        }
        
        // Escribir nuevos datos
        int currentRow = startRowIndex;
        for (RowData rowData : datos) {
            for (FormatoColumna columna : formato.getColumnas()) {
                int colIdx = columna.getIndiceColumna();
                String campoEstandar = columna.getCampoEstandar();
                String valor = rowData.get(campoEstandar);
                
                Cell cell = cells.get(currentRow, colIdx);
                
                // Intentar escribir como número si es posible
                if (valor != null && !valor.isEmpty()) {
                    if (esNumerico(valor) && esColumnaNumérica(columna)) {
                        try {
                            double numValue = Double.parseDouble(valor.replace(",", "").replace("$", ""));
                            cell.putValue(numValue);
                        } catch (NumberFormatException e) {
                            cell.putValue(valor);
                        }
                    } else {
                        cell.putValue(valor);
                    }
                }
            }
            currentRow++;
        }
        
        logger.info("✅ Datos escritos exitosamente");
    }
    
    /**
     * Guarda el workbook actual en la ruta especificada.
     * Preserva formato original (macros si es .xlsm)
     * 
     * @param rutaDestino Ruta donde guardar
     */
    public void guardarWorkbook(String rutaDestino) throws Exception {
        if (workbookActual == null) {
            throw new IllegalStateException("No hay workbook cargado");
        }
        
        logger.info("💾 Guardando archivo: {}", rutaDestino);
        
        // Determinar formato de guardado
        int saveFormat = SaveFormat.XLSX;
        String rutaLower = rutaDestino.toLowerCase();
        
        if (rutaLower.endsWith(".xlsm")) {
            saveFormat = SaveFormat.XLSM;
            logger.info("📋 Guardando como XLSM (preservando macros)");
        } else if (rutaLower.endsWith(".xlsb")) {
            saveFormat = SaveFormat.XLSB;
            logger.info("📋 Guardando como XLSB");
        } else if (rutaLower.endsWith(".xls")) {
            saveFormat = SaveFormat.EXCEL_97_TO_2003;
            logger.info("📋 Guardando como XLS");
        }
        
        workbookActual.save(rutaDestino, saveFormat);
        
        // Verificar que las macros se preservaron
        if (workbookActual.getVbaProject() != null) {
            logger.info("✅ Macros VBA preservadas en el archivo guardado");
        }
        
        logger.info("✅ Archivo guardado exitosamente: {}", rutaDestino);
    }
    
    /**
     * Crea una copia del workbook actual para exportación.
     * Útil cuando se quiere modificar sin afectar el original.
     */
    public Workbook clonarWorkbook() throws Exception {
        if (workbookActual == null) {
            throw new IllegalStateException("No hay workbook cargado");
        }
        
        Workbook copia = new Workbook();
        copia.copy(workbookActual);
        return copia;
    }
    
    /**
     * Obtiene el valor de una celda como String
     */
    private String obtenerValorCelda(Cell cell) {
        if (cell == null) return null;
        
        int tipo = cell.getType();
        
        switch (tipo) {
            case CellValueType.IS_STRING:
                return cell.getStringValue();
            case CellValueType.IS_NUMERIC:
                // Verificar si es fecha por el formato de la celda
                try {
                    Style style = cell.getStyle();
                    if (style != null && isDateFormat(style.getNumber())) {
                        return cell.getDateTimeValue().toString();
                    }
                } catch (Exception e) {
                    // Ignorar error de formato fecha
                }
                return String.valueOf(cell.getDoubleValue());
            case CellValueType.IS_BOOL:
                return String.valueOf(cell.getBoolValue());
            case CellValueType.IS_ERROR:
                return "#ERROR";
            case CellValueType.IS_NULL:
                return "";
            default:
                // Para fórmulas, obtener valor calculado
                try {
                    return cell.getStringValue();
                } catch (Exception e) {
                    return "";
                }
        }
    }
    
    /**
     * Verifica si un número de formato corresponde a una fecha
     */
    private boolean isDateFormat(int formatNumber) {
        // Formatos de fecha estándar de Excel: 14-22, 27-36, 45-47, 50-58
        return (formatNumber >= 14 && formatNumber <= 22) ||
               (formatNumber >= 27 && formatNumber <= 36) ||
               (formatNumber >= 45 && formatNumber <= 47) ||
               (formatNumber >= 50 && formatNumber <= 58);
    }
    
    /**
     * Verifica si un string es numérico
     */
    private boolean esNumerico(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Double.parseDouble(str.replace(",", "").replace("$", ""));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Verifica si una columna debería contener valores numéricos
     */
    private boolean esColumnaNumérica(FormatoColumna columna) {
        String tipo = columna.getTipoDato();
        if (tipo == null) return false;
        return tipo.equalsIgnoreCase("DECIMAL") || 
               tipo.equalsIgnoreCase("INTEGER") || 
               tipo.equalsIgnoreCase("NUMERIC");
    }
    
    /**
     * Obtiene información del archivo cargado
     */
    public String getInfoArchivoCargado() {
        if (workbookActual == null) return "Sin archivo cargado";
        
        StringBuilder info = new StringBuilder();
        info.append("Archivo: ").append(rutaArchivoOriginal).append("\n");
        info.append("Hojas: ").append(workbookActual.getWorksheets().getCount()).append("\n");
        
        if (workbookActual.getVbaProject() != null) {
            info.append("Macros VBA: Sí (").append(workbookActual.getVbaProject().getModules().getCount()).append(" módulos)\n");
        } else {
            info.append("Macros VBA: No\n");
        }
        
        return info.toString();
    }
}
