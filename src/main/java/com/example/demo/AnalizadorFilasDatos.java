package com.example.demo;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;

public class AnalizadorFilasDatos {
    
    public static void main(String[] args) throws Exception {
        String filePath = args.length > 0 ? args[0] : "cotizaciones/Libro4.xlsx";
        
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            System.out.println("=== FILAS 16-35 ===\n");
            for (int rowIdx = 15; rowIdx <= Math.min(34, sheet.getLastRowNum()); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                System.out.print("Fila " + (rowIdx + 1) + ": ");
                
                if (row == null || row.getLastCellNum() <= 0) {
                    System.out.println("[VACÍA]");
                    continue;
                }
                
                for (int colIdx = 0; colIdx < Math.min(10, row.getLastCellNum()); colIdx++) {
                    Cell cell = row.getCell(colIdx);
                    String cellValue = getCellValue(cell);
                    if (!cellValue.isEmpty()) {
                        System.out.print((char)('A' + colIdx) + ":");
                        System.out.print(cellValue.substring(0, Math.min(18, cellValue.length())));
                        if (cellValue.length() > 18) System.out.print("...");
                        System.out.print(" | ");
                    }
                }
                System.out.println();
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
