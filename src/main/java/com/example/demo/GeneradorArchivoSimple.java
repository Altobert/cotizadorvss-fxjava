package com.example.demo;

import com.example.service.ExportacionService;
import com.example.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Generador de archivo Excel simple para pruebas de importación
 */
public class GeneradorArchivoSimple {
    
    public static void main(String[] args) {
        System.out.println("📄 Generando archivo Excel simple para pruebas...");
        
        try {
            // Crear workbook
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Cotizaciones");
            
            // Crear fila de encabezados
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Número", "Cliente", "Fecha", "Estado", "Subtotal", "Descuento", "Impuestos", "Total"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                
                // Aplicar estilo a los encabezados
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                cell.setCellStyle(headerStyle);
            }
            
            // Crear datos de ejemplo
            Object[][] data = {
                {"COT-001", "Empresa Demo S.A.", LocalDateTime.now(), "PENDIENTE", 100000, 5000, 19000, 114000},
                {"COT-002", "Cliente Test Ltda.", LocalDateTime.now().minusDays(1), "APROBADA", 200000, 10000, 38000, 228000},
                {"COT-003", "Corporación ABC", LocalDateTime.now().minusDays(2), "PENDIENTE", 150000, 7500, 28500, 171000}
            };
            
            // Llenar datos
            for (int i = 0; i < data.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < data[i].length; j++) {
                    Cell cell = row.createCell(j);
                    Object value = data[i][j];
                    
                    if (value instanceof String) {
                        cell.setCellValue((String) value);
                    } else if (value instanceof LocalDateTime) {
                        cell.setCellValue((LocalDateTime) value);
                    } else if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    }
                }
            }
            
            // Autoajustar columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Guardar archivo
            String fileName = "cotizaciones_simple_para_importar.xlsx";
            try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
                workbook.write(fileOut);
            }
            
            workbook.close();
            
            System.out.println("✅ Archivo generado exitosamente: " + fileName);
            System.out.println("📋 Contiene " + data.length + " cotizaciones de ejemplo");
            System.out.println("🚀 Estructura simple compatible con el sistema de importación");
            
        } catch (Exception e) {
            System.out.println("❌ Error al generar archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
