package cl.vss.cotizador.demo;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * 🔍 ANALIZADOR DE PLANILLAS EXCEL
 * ================================
 * 
 * Herramienta para analizar la estructura de archivos Excel
 * y generar automáticamente el mapeo de columnas necesario.
 */
public class AnalizadorPlanilla {
    
    public static void main(String[] args) {
        String rutaArchivo = "/Users/albertosanmartin/Documents/VSS-COTIZACIONES/339-FR250126.xlsx";
        
        System.out.println("🔍 ANALIZADOR DE PLANILLA EXCEL");
        System.out.println("===============================");
        System.out.println("📄 Archivo: " + rutaArchivo);
        System.out.println();
        
        analizarEstructura(rutaArchivo);
    }
    
    /**
     * Analiza la estructura completa del archivo Excel
     */
    public static void analizarEstructura(String rutaArchivo) {
        try {
            File archivo = new File(rutaArchivo);
            if (!archivo.exists()) {
                System.out.println("❌ ERROR: El archivo no existe: " + rutaArchivo);
                return;
            }
            
            FileInputStream fis = new FileInputStream(archivo);
            XSSFWorkbook workbook = new XSSFWorkbook(fis);
            
            System.out.println("📊 INFORMACIÓN GENERAL");
            System.out.println("----------------------");
            System.out.println("Número de hojas: " + workbook.getNumberOfSheets());
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet hoja = workbook.getSheetAt(i);
                System.out.println("  Hoja " + (i + 1) + ": \"" + hoja.getSheetName() + "\" (" + 
                                 (hoja.getLastRowNum() + 1) + " filas)");
            }
            
            // Analizar la primera hoja (generalmente contiene los datos)
            Sheet hoja = workbook.getSheetAt(0);
            analizarHoja(hoja, archivo.getName());
            
            workbook.close();
            fis.close();
            
        } catch (Exception e) {
            System.out.println("❌ ERROR analizando archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Analiza una hoja específica del Excel
     */
    private static void analizarHoja(Sheet hoja, String nombreArchivo) {
        System.out.println();
        System.out.println("📋 ANÁLISIS DE HOJA: \"" + hoja.getSheetName() + "\"");
        System.out.println("=====================================");
        
        // Buscar posibles encabezados en las primeras 20 filas
        System.out.println();
        System.out.println("🔎 BUSCANDO ENCABEZADOS...");
        System.out.println("---------------------------");
        
        Map<Integer, List<String>> posiblesEncabezados = new HashMap<>();
        int maxColumnas = 0;
        
        for (int filaNum = 0; filaNum <= Math.min(20, hoja.getLastRowNum()); filaNum++) {
            Row fila = hoja.getRow(filaNum);
            if (fila == null) continue;
            
            List<String> columnas = new ArrayList<>();
            for (int colNum = 0; colNum < Math.max(20, fila.getLastCellNum()); colNum++) {
                Cell celda = fila.getCell(colNum);
                String valor = celda != null ? celda.toString().trim() : "";
                columnas.add(valor);
            }
            
            // Solo considerar filas que tengan al menos 3 columnas con texto
            long columnasConTexto = columnas.stream().filter(c -> !c.isEmpty()).count();
            if (columnasConTexto >= 3) {
                posiblesEncabezados.put(filaNum, columnas);
                maxColumnas = Math.max(maxColumnas, columnas.size());
            }
        }
        
        // Mostrar posibles encabezados
        posiblesEncabezados.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                int fila = entry.getKey();
                List<String> cols = entry.getValue();
                long textoCount = cols.stream().filter(c -> !c.isEmpty()).count();
                
                System.out.println("📍 Fila " + (fila + 1) + " (" + textoCount + " columnas con texto):");
                for (int i = 0; i < Math.min(cols.size(), 10); i++) {
                    if (!cols.get(i).isEmpty()) {
                        char letra = (char) ('A' + i);
                        System.out.println("    " + letra + ": \"" + cols.get(i) + "\"");
                    }
                }
                System.out.println();
            });
        
        // Detectar el encabezado más probable
        detectarEncabezadoPrincipal(posiblesEncabezados);
        
        // Analizar contenido de datos
        analizarContenidoDatos(hoja, posiblesEncabezados);
        
        // Generar mapeo sugerido
        generarMapeoSugerido(nombreArchivo, posiblesEncabezados);
    }
    
    /**
     * Detecta cuál es el encabezado principal más probable
     */
    private static void detectarEncabezadoPrincipal(Map<Integer, List<String>> encabezados) {
        System.out.println("🎯 DETECCIÓN DE ENCABEZADO PRINCIPAL");
        System.out.println("------------------------------------");
        
        // Palabras clave que indican encabezados de cotización
        String[] palabrasClave = {
            "CODIGO", "CODE", "ITEM", "SKU", "PART",
            "DESCRIPCION", "DESCRIPTION", "PRODUCT", "NAME",
            "CANTIDAD", "QTY", "QUANTITY", "CANT",
            "PRECIO", "PRICE", "COST", "UNIT",
            "TOTAL", "AMOUNT", "SUBTOTAL",
            "UNIDAD", "UNIT", "UOM", "MEASURE"
        };
        
        int mejorFila = -1;
        int mejorPuntaje = 0;
        
        for (Map.Entry<Integer, List<String>> entry : encabezados.entrySet()) {
            int fila = entry.getKey();
            List<String> columnas = entry.getValue();
            
            int puntaje = 0;
            for (String columna : columnas) {
                String colUpper = columna.toUpperCase();
                for (String palabra : palabrasClave) {
                    if (colUpper.contains(palabra)) {
                        puntaje++;
                        break;
                    }
                }
            }
            
            if (puntaje > mejorPuntaje) {
                mejorPuntaje = puntaje;
                mejorFila = fila;
            }
        }
        
        if (mejorFila >= 0) {
            System.out.println("✅ Encabezado principal detectado: FILA " + (mejorFila + 1));
            System.out.println("📊 Puntaje de coincidencia: " + mejorPuntaje + "/" + palabrasClave.length);
            System.out.println("🔤 Columnas detectadas:");
            
            List<String> columnasDetectadas = encabezados.get(mejorFila);
            for (int i = 0; i < columnasDetectadas.size() && i < 15; i++) {
                if (!columnasDetectadas.get(i).isEmpty()) {
                    char letra = (char) ('A' + i);
                    System.out.println("    " + letra + ": " + columnasDetectadas.get(i));
                }
            }
        } else {
            System.out.println("⚠️ No se pudo detectar un encabezado principal claro");
        }
        
        System.out.println();
    }
    
    /**
     * Analiza el contenido de los datos para entender el formato
     */
    private static void analizarContenidoDatos(Sheet hoja, Map<Integer, List<String>> encabezados) {
        System.out.println("📈 ANÁLISIS DE CONTENIDO");
        System.out.println("------------------------");
        
        if (encabezados.isEmpty()) {
            System.out.println("⚠️ No hay encabezados detectados para analizar contenido");
            return;
        }
        
        // Usar el primer encabezado encontrado para analizar
        int filaEncabezado = encabezados.keySet().iterator().next();
        int filaInicioDatos = filaEncabezado + 1;
        
        System.out.println("📍 Analizando datos desde fila " + (filaInicioDatos + 1) + "...");
        
        int filasConDatos = 0;
        int maxColumnasUsadas = 0;
        
        for (int filaNum = filaInicioDatos; filaNum <= hoja.getLastRowNum() && filaNum < filaInicioDatos + 10; filaNum++) {
            Row fila = hoja.getRow(filaNum);
            if (fila == null) continue;
            
            int columnasEnUso = 0;
            StringBuilder muestra = new StringBuilder();
            muestra.append("    Fila ").append(filaNum + 1).append(": ");
            
            for (int colNum = 0; colNum < Math.min(10, fila.getLastCellNum()); colNum++) {
                Cell celda = fila.getCell(colNum);
                String valor = celda != null ? celda.toString().trim() : "";
                
                if (!valor.isEmpty()) {
                    columnasEnUso++;
                    char letra = (char) ('A' + colNum);
                    muestra.append(letra).append("=\"").append(valor.substring(0, Math.min(valor.length(), 20)))
                           .append(valor.length() > 20 ? "...\"" : "\"").append(" ");
                }
            }
            
            if (columnasEnUso > 0) {
                filasConDatos++;
                maxColumnasUsadas = Math.max(maxColumnasUsadas, columnasEnUso);
                System.out.println(muestra.toString());
            }
        }
        
        System.out.println();
        System.out.println("📊 Resumen del contenido:");
        System.out.println("  • Filas con datos: " + filasConDatos);
        System.out.println("  • Máximo columnas usadas: " + maxColumnasUsadas);
        System.out.println("  • Fila de datos inicia en: " + (filaInicioDatos + 1));
        System.out.println();
    }
    
    /**
     * Genera una sugerencia de mapeo para CotizacionService
     */
    private static void generarMapeoSugerido(String nombreArchivo, Map<Integer, List<String>> encabezados) {
        System.out.println("🛠️ MAPEO SUGERIDO PARA COTIZACIONSERVICE");
        System.out.println("=========================================");
        
        if (encabezados.isEmpty()) {
            System.out.println("❌ No hay encabezados detectados para generar mapeo");
            return;
        }
        
        // Tomar el encabezado con más columnas como principal
        List<String> encabezadoPrincipal = encabezados.values().stream()
            .max((a, b) -> Long.compare(
                a.stream().filter(s -> !s.isEmpty()).count(),
                b.stream().filter(s -> !s.isEmpty()).count()
            ))
            .orElse(Collections.emptyList());
        
        System.out.println("📝 CÓDIGO JAVA PARA AGREGAR AL COTIZACIONSERVICE:");
        System.out.println();
        
        // Generar nombre de formato basado en el archivo
        String nombreFormato = nombreArchivo.toUpperCase()
            .replace(".XLSX", "")
            .replace(".XLS", "")
            .replace("-", "_")
            .replace(" ", "_");
        
        System.out.println("// Agregar este código al método inicializarFormatosConfigurados()");
        System.out.println("// o usar agregarFormatoPersonalizado()");
        System.out.println();
        System.out.println("service.agregarFormatoPersonalizado(");
        System.out.println("    \"" + nombreFormato + "\",");
        System.out.println("    new String[]{\"" + nombreFormato.substring(0, Math.min(nombreFormato.length(), 10)) + "\", \"" + 
                         nombreArchivo.substring(0, Math.min(nombreArchivo.length(), 15)) + "\"}, // patrones de archivo");
        
        // Detectar palabras clave para patrones de columna
        Set<String> patronesColumna = new HashSet<>();
        for (String col : encabezadoPrincipal) {
            if (!col.isEmpty() && col.length() > 2) {
                patronesColumna.add(col.toUpperCase());
            }
        }
        
        System.out.println("    new String[]{" + 
            String.join(", ", patronesColumna.stream()
                .limit(3)
                .map(s -> "\"" + s + "\"")
                .toArray(String[]::new)) + "}, // patrones de columna");
        
        System.out.println("    Map.of(");
        
        // Mapear columnas basado en palabras clave
        Map<String, String> mapeoSugerido = new HashMap<>();
        
        for (int i = 0; i < encabezadoPrincipal.size(); i++) {
            String columna = encabezadoPrincipal.get(i).toUpperCase().trim();
            if (columna.isEmpty()) continue;
            
            String campoInterno = detectarCampoInterno(columna);
            if (campoInterno != null) {
                mapeoSugerido.put(campoInterno, encabezadoPrincipal.get(i));
            }
        }
        
        // Imprimir mapeos
        String[] orden = {"codigo", "descripcion", "cantidad", "precio", "unidad", "totalbruto", "categoria", "comentarios"};
        boolean primero = true;
        
        for (String campo : orden) {
            if (mapeoSugerido.containsKey(campo)) {
                if (!primero) System.out.println(",");
                System.out.print("        \"" + campo + "\", new String[]{\"" + 
                               mapeoSugerido.get(campo) + "\"}");
                primero = false;
            }
        }
        
        System.out.println();
        System.out.println("    )");
        System.out.println(");");
        System.out.println();
        
        System.out.println("🎯 RESUMEN DEL MAPEO:");
        mapeoSugerido.forEach((campo, columna) -> 
            System.out.println("  • " + campo + " ← \"" + columna + "\""));
        
        System.out.println();
        System.out.println("💡 INSTRUCCIONES:");
        System.out.println("1. Copia el código Java generado");
        System.out.println("2. Ejecútalo en tu aplicación o agrégalo a CotizacionService");
        System.out.println("3. Usa 'Analizar Estructura Excel' en la app para verificar");
        System.out.println("4. Carga tu archivo con 'Cargar Excel'");
        System.out.println();
    }
    
    /**
     * Detecta el campo interno basado en el nombre de columna
     */
    private static String detectarCampoInterno(String nombreColumna) {
        String upper = nombreColumna.toUpperCase();
        
        if (upper.contains("CODIGO") || upper.contains("CODE") || upper.contains("ITEM") || 
            upper.contains("SKU") || upper.contains("PART")) {
            return "codigo";
        }
        if (upper.contains("DESCRIPCION") || upper.contains("DESCRIPTION") || 
            upper.contains("PRODUCT") || upper.contains("NAME")) {
            return "descripcion";
        }
        if (upper.contains("CANTIDAD") || upper.contains("QTY") || upper.contains("QUANTITY") || 
            upper.contains("CANT")) {
            return "cantidad";
        }
        if (upper.contains("PRECIO") || upper.contains("PRICE") || upper.contains("COST") || 
            upper.contains("UNIT")) {
            return "precio";
        }
        if (upper.contains("UNIDAD") || upper.contains("UOM") || upper.contains("MEASURE")) {
            return "unidad";
        }
        if (upper.contains("TOTAL") || upper.contains("AMOUNT") || upper.contains("SUBTOTAL")) {
            return "totalbruto";
        }
        if (upper.contains("CATEGORIA") || upper.contains("CATEGORY") || upper.contains("TYPE")) {
            return "categoria";
        }
        if (upper.contains("COMENTARIO") || upper.contains("COMMENT") || upper.contains("REMARK") || 
            upper.contains("NOTE")) {
            return "comentarios";
        }
        
        return null;
    }
}