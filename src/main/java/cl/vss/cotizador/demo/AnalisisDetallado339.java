package cl.vss.cotizador.demo;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Analiza las primeras filas del archivo 339-FR250126.xlsx
 * para identificar correctamente qué columna contiene las descripciones
 */
public class AnalisisDetallado339 {
    private static final Logger logger = Logger.getLogger(AnalisisDetallado339.class.getName());
    
    public static void main(String[] args) {
        String archivoPath = "/Users/albertosanmartin/Documents/VSS-COTIZACIONES/339-FR250126.xlsx";
        
        try (FileInputStream fis = new FileInputStream(archivoPath);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            
            System.out.println("🔍 ANÁLISIS DETALLADO DEL ARCHIVO 339-FR250126.xlsx");
            System.out.println("═══════════════════════════════════════════════════");
            
            // Obtener la primera hoja
            Sheet sheet = workbook.getSheetAt(0);
            System.out.println("📄 Hoja: " + sheet.getSheetName());
            System.out.println("📊 Total filas con datos: " + (sheet.getLastRowNum() + 1));
            
            System.out.println("\n🔤 ANÁLISIS DE ENCABEZADOS (si existen):");
            System.out.println("════════════════════════════════════════════");
            
            // Verificar si la primera fila son encabezados
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                System.out.println("Fila 0 (posibles encabezados):");
                for (int i = 0; i < Math.min(10, headerRow.getLastCellNum()); i++) {
                    Cell cell = headerRow.getCell(i);
                    String valor = getCellValueAsString(cell);
                    char columnaLetra = (char) ('A' + i);
                    System.out.println("  " + columnaLetra + ": '" + valor + "'");
                }
            }
            
            System.out.println("\n📋 PRIMERAS 5 FILAS DE DATOS:");
            System.out.println("═══════════════════════════════");
            
            // Analizar las primeras filas para entender la estructura
            for (int rowNum = 0; rowNum < Math.min(6, sheet.getLastRowNum() + 1); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;
                
                System.out.println("\nFila " + (rowNum + 1) + ":");
                
                for (int colNum = 0; colNum < Math.min(8, row.getLastCellNum()); colNum++) {
                    Cell cell = row.getCell(colNum);
                    String valor = getCellValueAsString(cell);
                    char columnaLetra = (char) ('A' + colNum);
                    
                    // Mostrar solo si tiene contenido
                    if (!valor.trim().isEmpty()) {
                        System.out.println("  " + columnaLetra + ": '" + valor + "'");
                    }
                }
            }
            
            System.out.println("\n🎯 BÚSQUEDA DE COLUMNA 'ITEM DESCRIPTION':");
            System.out.println("═══════════════════════════════════════════");
            
            // Buscar en todas las filas una columna que pueda ser "Item Description"
            boolean foundDescription = false;
            for (int rowNum = 0; rowNum < Math.min(10, sheet.getLastRowNum() + 1); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;
                
                for (int colNum = 0; colNum < Math.min(15, row.getLastCellNum()); colNum++) {
                    Cell cell = row.getCell(colNum);
                    String valor = getCellValueAsString(cell);
                    
                    if (valor.toUpperCase().contains("ITEM") && 
                        valor.toUpperCase().contains("DESCRIPTION")) {
                        char columnaLetra = (char) ('A' + colNum);
                        System.out.println("✅ Encontrada columna ITEM DESCRIPTION en: " + 
                                         columnaLetra + " (fila " + (rowNum + 1) + ")");
                        foundDescription = true;
                        
                        // Mostrar algunos valores de esa columna
                        System.out.println("📝 Valores en esta columna:");
                        for (int dataRow = rowNum + 1; dataRow < Math.min(rowNum + 6, sheet.getLastRowNum() + 1); dataRow++) {
                            Row dataRowObj = sheet.getRow(dataRow);
                            if (dataRowObj != null) {
                                Cell dataCell = dataRowObj.getCell(colNum);
                                String dataValue = getCellValueAsString(dataCell);
                                if (!dataValue.trim().isEmpty()) {
                                    System.out.println("    Fila " + (dataRow + 1) + ": '" + dataValue + "'");
                                }
                            }
                        }
                        break;
                    }
                }
                if (foundDescription) break;
            }
            
            if (!foundDescription) {
                System.out.println("⚠️  No se encontró una columna específica 'ITEM DESCRIPTION'");
                System.out.println("🔍 Analizando patrones de contenido para inferir descripciones...");
                
                // Analizar patrones en las primeras columnas
                analyzeContentPatterns(sheet);
            }
            
        } catch (IOException e) {
            logger.severe("❌ Error al leer el archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void analyzeContentPatterns(Sheet sheet) {
        System.out.println("\n🧩 ANÁLISIS DE PATRONES DE CONTENIDO:");
        System.out.println("══════════════════════════════════════");
        
        for (int colNum = 0; colNum < 8; colNum++) {
            char columnaLetra = (char) ('A' + colNum);
            System.out.println("\nColumna " + columnaLetra + ":");
            
            int samplesShown = 0;
            for (int rowNum = 1; rowNum < Math.min(20, sheet.getLastRowNum() + 1) && samplesShown < 5; rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row != null) {
                    Cell cell = row.getCell(colNum);
                    String valor = getCellValueAsString(cell);
                    if (!valor.trim().isEmpty()) {
                        System.out.println("  • '" + valor + "'");
                        samplesShown++;
                    }
                }
            }
        }
    }
    
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    if (numValue == Math.floor(numValue)) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            default:
                return "";
        }
    }
}