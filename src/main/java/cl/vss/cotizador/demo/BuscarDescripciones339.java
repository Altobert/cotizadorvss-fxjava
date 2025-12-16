package cl.vss.cotizador.demo;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Busca específicamente las columnas que contienen descripciones como
 * "BISCUIT NO SUGAR", "FRENCH BREAD", "RYE BREAD"
 */
public class BuscarDescripciones339 {
    private static final Logger logger = Logger.getLogger(BuscarDescripciones339.class.getName());
    
    // Descripciones conocidas que debe contener el archivo
    private static final String[] DESCRIPCIONES_CONOCIDAS = {
        "BISCUIT NO SUGAR",
        "FRENCH BREAD", 
        "RYE BREAD"
    };
    
    public static void main(String[] args) {
        String archivoPath = "/Users/albertosanmartin/Documents/VSS-COTIZACIONES/339-FR250126.xlsx";
        
        try (FileInputStream fis = new FileInputStream(archivoPath);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            
            System.out.println("🔍 BÚSQUEDA DE COLUMNA ITEM DESCRIPTION");
            System.out.println("═══════════════════════════════════════════");
            System.out.println("Buscando descripciones:");
            for (String desc : DESCRIPCIONES_CONOCIDAS) {
                System.out.println("  • " + desc);
            }
            System.out.println();
            
            Sheet sheet = workbook.getSheetAt(0);
            System.out.println("📄 Analizando hoja: " + sheet.getSheetName());
            System.out.println("📊 Total filas: " + (sheet.getLastRowNum() + 1));
            
            // Buscar en todas las columnas y filas
            List<CeldaEncontrada> resultados = new ArrayList<>();
            
            for (int rowNum = 0; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;
                
                for (int colNum = 0; colNum < Math.min(20, row.getLastCellNum()); colNum++) {
                    Cell cell = row.getCell(colNum);
                    if (cell == null) continue;
                    
                    String valor = getCellValueAsString(cell).trim().toUpperCase();
                    
                    // Verificar si esta celda contiene alguna de las descripciones conocidas
                    for (String descripcionBuscada : DESCRIPCIONES_CONOCIDAS) {
                        if (valor.equals(descripcionBuscada.toUpperCase())) {
                            char columnaLetra = (char) ('A' + colNum);
                            resultados.add(new CeldaEncontrada(
                                rowNum + 1, columnaLetra, colNum, valor, descripcionBuscada
                            ));
                        }
                    }
                }
            }
            
            if (resultados.isEmpty()) {
                System.out.println("❌ NO se encontraron las descripciones específicas.");
                System.out.println("\n🔍 Buscando patrones similares...");
                buscarPatronesSimilares(sheet);
            } else {
                System.out.println("✅ ENCONTRADAS " + resultados.size() + " coincidencias:");
                System.out.println();
                
                for (CeldaEncontrada resultado : resultados) {
                    System.out.println("🎯 " + resultado.descripcionOriginal);
                    System.out.println("   └─ Columna " + resultado.columnaLetra + 
                                     " (índice " + resultado.columnaIndice + "), Fila " + resultado.fila);
                }
                
                // Analizar la columna donde se encontraron las descripciones
                if (!resultados.isEmpty()) {
                    int columnaDescripciones = resultados.get(0).columnaIndice;
                    analizarColumnaDescripciones(sheet, columnaDescripciones);
                }
            }
            
        } catch (IOException e) {
            logger.severe("❌ Error al leer el archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void buscarPatronesSimilares(Sheet sheet) {
        // Buscar celdas que contengan palabras relacionadas con alimentos
        String[] palabrasClave = {"BREAD", "BISCUIT", "SUGAR", "FLOUR", "MILK", "BUTTER", "CHEESE"};
        
        System.out.println("Buscando palabras clave relacionadas:");
        
        for (int colNum = 0; colNum < 15; colNum++) {
            char columnaLetra = (char) ('A' + colNum);
            List<String> muestras = new ArrayList<>();
            
            for (int rowNum = 0; rowNum < Math.min(100, sheet.getLastRowNum() + 1); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;
                
                Cell cell = row.getCell(colNum);
                if (cell == null) continue;
                
                String valor = getCellValueAsString(cell).trim().toUpperCase();
                
                // Verificar si contiene palabras clave
                for (String palabra : palabrasClave) {
                    if (valor.contains(palabra) && !muestras.contains(valor) && muestras.size() < 10) {
                        muestras.add(valor);
                    }
                }
            }
            
            if (!muestras.isEmpty()) {
                System.out.println("\n📍 Columna " + columnaLetra + " contiene:");
                for (String muestra : muestras) {
                    System.out.println("   • " + muestra);
                }
            }
        }
    }
    
    private static void analizarColumnaDescripciones(Sheet sheet, int columnaIndice) {
        char columnaLetra = (char) ('A' + columnaIndice);
        System.out.println("\n📋 ANÁLISIS DE COLUMNA " + columnaLetra + " (DESCRIPCIONES):");
        System.out.println("════════════════════════════════════════════════════");
        
        List<String> descripciones = new ArrayList<>();
        
        for (int rowNum = 0; rowNum <= sheet.getLastRowNum() && descripciones.size() < 20; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;
            
            Cell cell = row.getCell(columnaIndice);
            if (cell == null) continue;
            
            String valor = getCellValueAsString(cell).trim();
            if (!valor.isEmpty() && !descripciones.contains(valor)) {
                descripciones.add(valor);
            }
        }
        
        System.out.println("📝 Primeras 20 descripciones encontradas:");
        for (int i = 0; i < descripciones.size(); i++) {
            System.out.println("   " + (i + 1) + ". " + descripciones.get(i));
        }
        
        System.out.println("\n✅ SOLUCIÓN: Usar COLUMN_" + columnaLetra + " para el campo 'descripcion'");
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
    
    static class CeldaEncontrada {
        int fila;
        char columnaLetra;
        int columnaIndice;
        String valorEncontrado;
        String descripcionOriginal;
        
        CeldaEncontrada(int fila, char columnaLetra, int columnaIndice, String valorEncontrado, String descripcionOriginal) {
            this.fila = fila;
            this.columnaLetra = columnaLetra;
            this.columnaIndice = columnaIndice;
            this.valorEncontrado = valorEncontrado;
            this.descripcionOriginal = descripcionOriginal;
        }
    }
}