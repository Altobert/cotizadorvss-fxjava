package cl.vss.cotizador.detector;

import org.apache.poi.ss.usermodel.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Clase base con utilidades comunes para todos los detectores
 */
public abstract class DetectorBase implements DetectorFormatoCotizacion {
    
    /**
     * Obtener valor de celda como String
     */
    protected String obtenerValorCelda(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    // Si es un número entero, no mostrar decimales
                    if (numValue == Math.floor(numValue)) {
                        return String.valueOf((long) numValue);
                    }
                    return String.valueOf(numValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception ex) {
                        return "";
                    }
                }
            default:
                return "";
        }
    }
    
    /**
     * Obtener valor numérico de celda
     */
    protected Double obtenerValorNumerico(Cell cell) {
        if (cell == null) return null;
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    String valor = cell.getStringCellValue().trim();
                    // Limpiar caracteres no numéricos excepto . y ,
                    valor = valor.replaceAll("[^\\d.,\\-]", "");
                    valor = valor.replace(",", ".");
                    if (!valor.isEmpty()) {
                        return Double.parseDouble(valor);
                    }
                    return null;
                case FORMULA:
                    return cell.getNumericCellValue();
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Obtener fecha de celda
     */
    protected LocalDateTime obtenerFecha(Cell cell) {
        if (cell == null) return null;
        
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            }
        } catch (Exception e) {
            // Ignorar errores de fecha
        }
        
        return null;
    }
    
    /**
     * Buscar texto en una fila
     */
    protected boolean filaContiene(Row fila, String... textos) {
        if (fila == null) return false;
        
        for (Cell cell : fila) {
            String valor = obtenerValorCelda(cell).toLowerCase();
            for (String texto : textos) {
                if (valor.contains(texto.toLowerCase())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Buscar una celda que contenga un texto específico
     */
    protected Cell buscarCeldaConTexto(Sheet sheet, String texto, int filaMax) {
        for (int i = 0; i < Math.min(filaMax, sheet.getLastRowNum() + 1); i++) {
            Row fila = sheet.getRow(i);
            if (fila == null) continue;
            
            for (Cell cell : fila) {
                String valor = obtenerValorCelda(cell);
                if (valor.toLowerCase().contains(texto.toLowerCase())) {
                    return cell;
                }
            }
        }
        return null;
    }
    
    /**
     * Encontrar índice de columna por nombre de header
     */
    protected int encontrarColumna(Row headerRow, String... nombresAlternativos) {
        if (headerRow == null) return -1;
        
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            String valor = obtenerValorCelda(cell).toLowerCase();
            
            for (String nombre : nombresAlternativos) {
                if (valor.contains(nombre.toLowerCase())) {
                    return i;
                }
            }
        }
        
        return -1;
    }
    
    /**
     * Verificar si una fila está vacía o casi vacía
     */
    protected boolean esFilaVacia(Row fila) {
        if (fila == null) return true;
        
        int celdasConDatos = 0;
        for (Cell cell : fila) {
            String valor = obtenerValorCelda(cell);
            if (!valor.isEmpty()) {
                celdasConDatos++;
            }
        }
        
        return celdasConDatos < 2; // Menos de 2 celdas con datos
    }
}
