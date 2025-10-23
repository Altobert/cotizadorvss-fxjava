package com.example.demo;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Analizador de estructura de archivos Excel
 */
public class AnalizadorEstructuraExcel {
    
    public static void main(String[] args) {
        System.out.println("🔍 Analizando estructura de archivos Excel...");
        
        String[] archivos = {
            "cotizaciones/Libro2.xlsx",
            "cotizaciones/Libro3.xlsx", 
            "cotizaciones/Libro4.xlsx",
            "cotizaciones/Libro5.xlsx",
            "cotizaciones/RFQ_excel (1).xlsx"
        };
        
        for (String archivo : archivos) {
            System.out.println("\n📄 Analizando: " + archivo);
            analizarArchivo(archivo);
        }
        
        System.out.println("\n🎉 Análisis completado");
    }
    
    private static void analizarArchivo(String rutaArchivo) {
        try (FileInputStream fis = new FileInputStream(rutaArchivo)) {
            Workbook workbook = null;
            
            if (rutaArchivo.toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else if (rutaArchivo.toLowerCase().endsWith(".xls")) {
                workbook = new HSSFWorkbook(fis);
            }
            
            if (workbook == null) {
                System.out.println("❌ No se pudo abrir el archivo");
                return;
            }
            
            System.out.println("📊 Número de hojas: " + workbook.getNumberOfSheets());
            
            // Analizar cada hoja
            for (int i = 0; i < Math.min(workbook.getNumberOfSheets(), 3); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                System.out.println("   📋 Hoja " + (i + 1) + ": " + sheet.getSheetName());
                System.out.println("      Filas: " + (sheet.getLastRowNum() + 1));
                
                // Analizar primeras filas
                for (int rowNum = 0; rowNum < Math.min(5, sheet.getLastRowNum() + 1); rowNum++) {
                    Row row = sheet.getRow(rowNum);
                    if (row != null) {
                        System.out.println("      Fila " + (rowNum + 1) + ":");
                        for (int cellNum = 0; cellNum < Math.min(10, row.getLastCellNum()); cellNum++) {
                            Cell cell = row.getCell(cellNum);
                            if (cell != null) {
                                String valor = obtenerValorCelda(cell);
                                if (valor != null && !valor.trim().isEmpty()) {
                                    System.out.println("         Col " + (cellNum + 1) + ": " + valor);
                                }
                            }
                        }
                    }
                }
            }
            
            workbook.close();
            
        } catch (IOException e) {
            System.out.println("❌ Error al leer archivo: " + e.getMessage());
        } catch (OutOfMemoryError e) {
            System.out.println("❌ Archivo muy grande para analizar completamente");
        }
    }
    
    private static String obtenerValorCelda(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
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
                return null;
        }
    }
}

