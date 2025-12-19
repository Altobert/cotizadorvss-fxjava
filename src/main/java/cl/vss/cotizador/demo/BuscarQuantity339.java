package cl.vss.cotizador.demo;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Busca específicamente la columna QUANTITY en el archivo 339-FR250126.xlsx
 */
public class BuscarQuantity339 {
    private static final Logger logger = Logger.getLogger(BuscarQuantity339.class.getName());
    
    public static void main(String[] args) {
        String archivoPath = "/Users/albertosanmartin/Documents/VSS-COTIZACIONES/339-FR250126.xlsx";
        
        try (FileInputStream fis = new FileInputStream(archivoPath);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            
            System.out.println("🔍 BÚSQUEDA DE COLUMNA QUANTITY");
            System.out.println("═══════════════════════════════════");
            
            Sheet sheet = workbook.getSheetAt(0);
            System.out.println("📄 Analizando hoja: " + sheet.getSheetName());
            System.out.println("📊 Total filas: " + (sheet.getLastRowNum() + 1));
            
            System.out.println("\n🔤 BUSCANDO 'QUANTITY' EN ENCABEZADOS:");
            System.out.println("════════════════════════════════════════");
            
            boolean foundQuantity = false;
            
            // Buscar en todas las filas la palabra "QUANTITY"
            for (int rowNum = 0; rowNum < Math.min(15, sheet.getLastRowNum() + 1); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;
                
                for (int colNum = 0; colNum < Math.min(20, row.getLastCellNum()); colNum++) {
                    Cell cell = row.getCell(colNum);
                    if (cell == null) continue;
                    
                    String valor = getCellValueAsString(cell).trim().toUpperCase();
                    
                    if (valor.contains("QUANTITY") || valor.contains("QTY") || valor.equals("QUANTITY")) {
                        char columnaLetra = (char) ('A' + colNum);
                        System.out.println("✅ Encontrada '" + valor + "' en:");
                        System.out.println("   └─ Columna " + columnaLetra + 
                                         " (índice " + colNum + "), Fila " + (rowNum + 1));
                        foundQuantity = true;
                        
                        // Mostrar algunos valores de esa columna
                        System.out.println("📝 Valores en esta columna:");
                        for (int dataRow = rowNum + 1; dataRow < Math.min(rowNum + 10, sheet.getLastRowNum() + 1); dataRow++) {
                            Row dataRowObj = sheet.getRow(dataRow);
                            if (dataRowObj != null) {
                                Cell dataCell = dataRowObj.getCell(colNum);
                                String dataValue = getCellValueAsString(dataCell);
                                if (!dataValue.trim().isEmpty()) {
                                    System.out.println("    Fila " + (dataRow + 1) + ": '" + dataValue + "'");
                                }
                            }
                        }
                        System.out.println();
                    }
                }
            }
            
            if (!foundQuantity) {
                System.out.println("❌ No se encontró 'QUANTITY' explícitamente.");
                System.out.println("\n🔍 BUSCANDO PATRONES NUMÉRICOS QUE PODRÍAN SER CANTIDADES:");
                System.out.println("═════════════════════════════════════════════════════════");
                buscarPatronesNumericos(sheet);
            }
            
            System.out.println("\n📋 ANÁLISIS DE TODAS LAS COLUMNAS (primeras 15 filas):");
            System.out.println("═════════════════════════════════════════════════════");
            
            for (int colNum = 0; colNum < 20; colNum++) {
                char columnaLetra = (char) ('A' + colNum);
                System.out.println("\n🔤 Columna " + columnaLetra + ":");
                
                int samplesShown = 0;
                for (int rowNum = 0; rowNum < 15 && samplesShown < 8; rowNum++) {
                    Row row = sheet.getRow(rowNum);
                    if (row != null) {
                        Cell cell = row.getCell(colNum);
                        String valor = getCellValueAsString(cell);
                        if (!valor.trim().isEmpty()) {
                            System.out.println("   Fila " + (rowNum + 1) + ": '" + valor + "'");
                            samplesShown++;
                        }
                    }
                }
                
                if (samplesShown == 0) {
                    System.out.println("   (vacía)");
                }
            }
            
        } catch (IOException e) {
            logger.severe("❌ Error al leer el archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void buscarPatronesNumericos(Sheet sheet) {
        // Buscar columnas que contengan principalmente números (posibles cantidades)
        for (int colNum = 0; colNum < 20; colNum++) {
            char columnaLetra = (char) ('A' + colNum);
            List<String> muestrasNumericas = new ArrayList<>();
            int totalCeldas = 0;
            int celdasNumericas = 0;
            
            for (int rowNum = 1; rowNum < Math.min(50, sheet.getLastRowNum() + 1); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;
                
                Cell cell = row.getCell(colNum);
                if (cell == null) continue;
                
                String valor = getCellValueAsString(cell).trim();
                if (!valor.isEmpty()) {
                    totalCeldas++;
                    
                    // Verificar si es un número
                    try {
                        double num = Double.parseDouble(valor);
                        celdasNumericas++;
                        if (muestrasNumericas.size() < 5 && num > 0 && num < 1000) {
                            muestrasNumericas.add(valor);
                        }
                    } catch (NumberFormatException e) {
                        // No es un número
                    }
                }
            }
            
            // Si más del 70% son números, podría ser cantidad
            if (totalCeldas > 10 && celdasNumericas > (totalCeldas * 0.7)) {
                System.out.println("\n🔢 Columna " + columnaLetra + " contiene principalmente números:");
                System.out.println("   📊 " + celdasNumericas + "/" + totalCeldas + " celdas son numéricas (" + 
                                 Math.round((double)celdasNumericas/totalCeldas*100) + "%)");
                System.out.println("   📝 Muestras: " + String.join(", ", muestrasNumericas));
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