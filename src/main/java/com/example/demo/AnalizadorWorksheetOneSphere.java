package com.example.demo;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;

public class AnalizadorWorksheetOneSphere {
    
    public static void main(String[] args) throws Exception {
        String filePath = args.length > 0 ? args[0] : "cotizaciones/Libro4.xlsx";
        
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            System.out.println("=== ANÁLISIS DEL WORKSHEET ===");
            System.out.println("Total de filas: " + sheet.getLastRowNum());
            System.out.println("");
            
            // Analiza las primeras 30 filas para encontrar encabezados
            System.out.println("=== CONTENIDO DE FILAS 14-40 ===");
            for (int rowIdx = 13; rowIdx <= Math.min(39, sheet.getLastRowNum()); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row != null) {
                    System.out.print("Fila " + (rowIdx + 1) + ": ");
                    
                    // Imprime los primeros 8 valores de la fila
                    StringBuilder sb = new StringBuilder();
                    for (int colIdx = 0; colIdx < Math.min(8, row.getLastCellNum()); colIdx++) {
                        Cell cell = row.getCell(colIdx);
                        String cellValue = getCellValue(cell);
                        sb.append("[Col ").append((char)('A' + colIdx)).append(": ");
                        sb.append(cellValue.substring(0, Math.min(20, cellValue.length())));
                        if (cellValue.length() > 20) sb.append("...");
                        sb.append("] ");
                    }
                    System.out.println(sb.toString());
                    
                    // Si encuentra "ITEM DESCRIPTION", marca esta fila como especial
                    String firstCell = getCellValue(row.getCell(0));
                    if (firstCell.contains("ITEM") || firstCell.contains("DESCRIPTION") || 
                        firstCell.contains("UNIT") || firstCell.contains("PRICE")) {
                        System.out.println("  ⭐ POSIBLE FILA DE ENCABEZADOS");
                    }
                }
            }
            
            System.out.println("\n=== BÚSQUEDA ESPECÍFICA DE ENCABEZADOS ===");
            boolean encontradoEncabezado = false;
            for (int rowIdx = 0; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row != null) {
                    for (int colIdx = 0; colIdx < row.getLastCellNum(); colIdx++) {
                        Cell cell = row.getCell(colIdx);
                        String val = getCellValue(cell);
                        if (val.equalsIgnoreCase("ITEM DESCRIPTION") || 
                            val.equalsIgnoreCase("UNIT") ||
                            val.equalsIgnoreCase("PRICE") ||
                            val.equalsIgnoreCase("MIN. QUANTITY")) {
                            if (!encontradoEncabezado) {
                                System.out.println("✅ Encabezados encontrados en fila " + (rowIdx + 1));
                                encontradoEncabezado = true;
                            }
                            System.out.println("  - Columna " + (char)('A' + colIdx) + (rowIdx + 1) + 
                                             ": " + val);
                        }
                    }
                }
            }
            
            if (!encontradoEncabezado) {
                System.out.println("❌ No se encontraron encabezados estándar");
            }
        }
    }
    
    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double num = cell.getNumericCellValue();
                if (num == (long) num) {
                    return String.valueOf((long) num);
                }
                return String.valueOf(num);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
