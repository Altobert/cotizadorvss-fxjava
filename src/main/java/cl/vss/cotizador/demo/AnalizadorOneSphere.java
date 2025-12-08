package cl.vss.cotizador.demo;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Analizador para el archivo de cotización de M/V One Sphere
 */
public class AnalizadorOneSphere {
    private static final Logger logger = Logger.getLogger(AnalizadorOneSphere.class.getName());
    
    public static void main(String[] args) {
        String archivoPath = "/Users/albertosanmartin/Documents/VSS-COTIZACIONES/759191248_ONE Sphere_A25008819-02_Valparaiso Ship Services_2025_05_17_(P) Provision_RFQ.xlsx";
        
        try (FileInputStream fis = new FileInputStream(archivoPath);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            
            System.out.println("🚢 ANÁLISIS DE COTIZACIÓN M/V ONE SPHERE");
            System.out.println("═══════════════════════════════════════");
            System.out.println("📂 Archivo: " + archivoPath.substring(archivoPath.lastIndexOf("/") + 1));
            System.out.println("📊 Total hojas: " + workbook.getNumberOfSheets());
            
            // Analizar todas las hojas
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                System.out.println("\n📄 HOJA " + (sheetIndex + 1) + ": " + sheet.getSheetName());
                System.out.println("══════════════════════════════════");
                System.out.println("📏 Filas: " + (sheet.getLastRowNum() + 1));
                
                // Buscar patrones típicos de cotización
                buscarPatronesHeader(sheet);
                
                // Mostrar estructura de las primeras 20 filas
                System.out.println("\n🔤 ESTRUCTURA DE ENCABEZADOS (primeras 20 filas):");
                System.out.println("═════════════════════════════════════════════════");
                
                for (int rowNum = 0; rowNum < Math.min(20, sheet.getLastRowNum() + 1); rowNum++) {
                    Row row = sheet.getRow(rowNum);
                    if (row == null) continue;
                    
                    boolean hasContent = false;
                    StringBuilder rowContent = new StringBuilder();
                    rowContent.append("Fila ").append(rowNum + 1).append(": ");
                    
                    for (int colNum = 0; colNum < Math.min(15, row.getLastCellNum()); colNum++) {
                        Cell cell = row.getCell(colNum);
                        String valor = getCellValueAsString(cell);
                        
                        if (!valor.trim().isEmpty()) {
                            if (hasContent) rowContent.append(" | ");
                            rowContent.append((char)('A' + colNum)).append(":\"").append(valor.trim()).append("\"");
                            hasContent = true;
                        }
                    }
                    
                    if (hasContent) {
                        System.out.println(rowContent.toString());
                    }
                }
                
                // Buscar datos típicos de productos
                buscarDatosProductos(sheet);
            }
            
        } catch (IOException e) {
            logger.severe("❌ Error al leer el archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void buscarPatronesHeader(Sheet sheet) {
        System.out.println("\n🔍 BÚSQUEDA DE PATRONES HEADER:");
        System.out.println("═════════════════════════════════");
        
        String[] patronesBuscar = {
            "ITEM", "DESCRIPTION", "QUANTITY", "QTY", "PRICE", "UNIT", "TOTAL",
            "CODE", "CODIGO", "DESCRIPCION", "CANTIDAD", "PRECIO", "UNIDAD",
            "SKU", "PRODUCT", "VESSEL", "SHIP", "ONE SPHERE", "PROVISION",
            "ORDER", "REQUEST", "RFQ", "QUOTATION", "QUOTE"
        };
        
        for (int rowNum = 0; rowNum < Math.min(25, sheet.getLastRowNum() + 1); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;
            
            for (int colNum = 0; colNum < Math.min(20, row.getLastCellNum()); colNum++) {
                Cell cell = row.getCell(colNum);
                String valor = getCellValueAsString(cell).trim().toUpperCase();
                
                for (String patron : patronesBuscar) {
                    if (valor.contains(patron) || valor.equals(patron)) {
                        char columnaLetra = (char) ('A' + colNum);
                        System.out.println("✅ '" + patron + "' encontrado en " + 
                                         columnaLetra + (rowNum + 1) + ": \"" + valor + "\"");
                    }
                }
            }
        }
    }
    
    private static void buscarDatosProductos(Sheet sheet) {
        System.out.println("\n📦 BÚSQUEDA DE DATOS DE PRODUCTOS:");
        System.out.println("══════════════════════════════════");
        
        // Buscar filas que parezcan contener productos
        List<Integer> filasConProductos = new ArrayList<>();
        
        for (int rowNum = 5; rowNum < Math.min(50, sheet.getLastRowNum() + 1); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;
            
            int celdasConDatos = 0;
            boolean tieneNumero = false;
            boolean tieneTexto = false;
            
            for (int colNum = 0; colNum < Math.min(10, row.getLastCellNum()); colNum++) {
                Cell cell = row.getCell(colNum);
                String valor = getCellValueAsString(cell).trim();
                
                if (!valor.isEmpty()) {
                    celdasConDatos++;
                    
                    // Verificar si es un número (posible precio/cantidad)
                    try {
                        Double.parseDouble(valor);
                        tieneNumero = true;
                    } catch (NumberFormatException e) {
                        if (valor.length() > 3) { // Texto descriptivo
                            tieneTexto = true;
                        }
                    }
                }
            }
            
            // Si tiene al menos 3 celdas con datos, un número y texto, probablemente es un producto
            if (celdasConDatos >= 3 && tieneNumero && tieneTexto) {
                filasConProductos.add(rowNum);
            }
        }
        
        System.out.println("🎯 Filas que parecen contener productos: " + filasConProductos.size());
        
        // Mostrar ejemplos de las primeras filas de productos
        for (int i = 0; i < Math.min(5, filasConProductos.size()); i++) {
            int rowNum = filasConProductos.get(i);
            Row row = sheet.getRow(rowNum);
            
            System.out.println("\n📋 EJEMPLO FILA " + (rowNum + 1) + ":");
            for (int colNum = 0; colNum < Math.min(10, row.getLastCellNum()); colNum++) {
                Cell cell = row.getCell(colNum);
                String valor = getCellValueAsString(cell);
                if (!valor.trim().isEmpty()) {
                    char columnaLetra = (char) ('A' + colNum);
                    System.out.println("   " + columnaLetra + ": \"" + valor.trim() + "\"");
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