package cl.vss.cotizador.service;

import cl.vss.cotizador.model.ItemCotizacionExcel;
import cl.vss.cotizador.model.ProductoSimilar;
import cl.vss.cotizador.util.DBConnection;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import cl.vss.cotizador.util.LoggingConfig;
import java.util.regex.Pattern;

public class CotizacionService {
    
    private static final Logger logger = LoggingConfig.getLogger(CotizacionService.class);
    
    // Configuración de formatos
    private final Map<String, FormatoExcel> formatosConfigurados = new HashMap<>();
    
    /**
     * Clase interna para definir configuraciones de formatos Excel
     */
    private static class FormatoExcel {
        String nombre;
        List<String> patronesArchivo = new ArrayList<>();
        List<String> patronesColumnas = new ArrayList<>();
        Map<String, List<String>> mapeoColumnas = new HashMap<>();
        int filaInicioEncabezado = 0;
        int filaFinEncabezado = 20;
        int columnasMinimas = 3;
        
        public FormatoExcel(String nombre) {
            this.nombre = nombre;
        }
        
        public FormatoExcel patronArchivo(String... patrones) {
            this.patronesArchivo.addAll(Arrays.asList(patrones));
            return this;
        }
        
        public FormatoExcel patronColumna(String... patrones) {
            this.patronesColumnas.addAll(Arrays.asList(patrones));
            return this;
        }
        
        public FormatoExcel mapear(String campoInterno, String... columnasExternas) {
            this.mapeoColumnas.put(campoInterno, Arrays.asList(columnasExternas));
            return this;
        }
        
        public FormatoExcel rango(int inicio, int fin) {
            this.filaInicioEncabezado = inicio;
            this.filaFinEncabezado = fin;
            return this;
        }
        
        public FormatoExcel minColumnas(int min) {
            this.columnasMinimas = min;
            return this;
        }
    }

    /**
     * Constructor - Inicializar configuración de formatos
     */
    public CotizacionService() {
        inicializarFormatosConfigurados();
    }
    
    /**
     * Configuración de formatos soportados
     */
    private void inicializarFormatosConfigurados() {
        
        // QTN_GOF - Formato de cotización marítima
        formatosConfigurados.put("QTN_GOF", 
            new FormatoExcel("QTN_GOF")
                .patronArchivo("QTN_GOF", "QUOTATION")
                .patronColumna("VESSEL COMMENTS", "SUPPLIER COMMENTS")
                .mapear("codigo", "ITEM")
                .mapear("descripcion", "ITEM DESCRIPTION")
                .mapear("cantidad", "QUANTITY ORDER")
                .mapear("precio", "PRICE")
                .mapear("unidad", "UNIT OF MEASURE")
                .mapear("categoria", "FOOD CATEGORIES")
                .mapear("comentarios", "SUPPLIER COMMENTS")
                .mapear("totalbruto", "TOTAL")
                .rango(5, 15)
                .minColumnas(4)
        );
        
        // HMM_BLESSING - Formato provision order
        formatosConfigurados.put("HMM_BLESSING", 
            new FormatoExcel("HMM_BLESSING")
                .patronArchivo("HMM", "BLESSING", "PROVISION ORDER")
                .patronColumna("CANTD", "USD TOTAL", "PRICES")
                .mapear("descripcion", "COLUMN_A") // Columna A contiene descripciones
                .mapear("codigo", "COLUMN_A") // Usar descripción como código
                .mapear("comentarios", "REMARKS")
                .mapear("unidad", "UNIT")
                .mapear("precio", "PRICES")
                .mapear("cantidad", "CANTD")
                .mapear("totalbruto", "USD TOTAL")
                .rango(10, 20)
                .minColumnas(3)
        );
        
        // FERNANDINA - Formato en español
        formatosConfigurados.put("FERNANDINA", 
            new FormatoExcel("FERNANDINA")
                .patronArchivo("FERNANDINA")
                .patronColumna("CÓDIGO", "DESCRIPCIÓN")
                .mapear("codigo", "CÓDIGO", "CODIGO", "COD")
                .mapear("descripcion", "DESCRIPCIÓN", "DESCRIPCION", "DESC")
                .mapear("cantidad", "CANTIDAD", "QTY", "CANT")
                .mapear("precio", "PRECIO", "PRICE")
                .mapear("unidad", "UNIDAD", "UNIT")
                .rango(0, 10)
                .minColumnas(3)
        );
        
        // INVENTARIO - Formato de inventario general
        formatosConfigurados.put("INVENTARIO", 
            new FormatoExcel("INVENTARIO")
                .patronArchivo("INVENTORY", "STOCK", "ALMACÉN")
                .patronColumna("SKU", "PART NUMBER")
                .mapear("codigo", "SKU", "PART NUMBER", "PART #")
                .mapear("descripcion", "PRODUCT NAME", "PRODUCT DESCRIPTION", "NAME")
                .mapear("cantidad", "STOCK", "INVENTORY", "QTY ON HAND")
                .mapear("precio", "COST", "UNIT COST", "WHOLESALE")
                .rango(0, 15)
                .minColumnas(3)
        );
        
        logger.info("📊 Formatos configurados: " + formatosConfigurados.size());
        formatosConfigurados.keySet().forEach(formato -> 
            logger.info("  • " + formato));
    }

    public List<ItemCotizacionExcel> leerItemsDesdeExcel(File archivo) {
        logger.info("📂 Iniciando lectura de archivo Excel: " + archivo.getAbsolutePath());
        logger.info("📊 Tamaño del archivo: " + archivo.length() + " bytes");
        logger.info("📝 Nombre del archivo: " + archivo.getName());
        logger.info("✅ Archivo existe: " + archivo.exists());
        logger.info("📖 Archivo es legible: " + archivo.canRead());
        
        List<ItemCotizacionExcel> items = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(archivo);
             Workbook workbook = new XSSFWorkbook(fis)) {
             
            logger.info("✅ Archivo Excel abierto exitosamente");

            Sheet hoja = workbook.getSheetAt(0);

            logger.info("🔎 Explorando filas para detectar encabezado en archivo: " + archivo.getName());

            Row encabezado = null;
            int filaEncabezado = -1;
            
            // Buscar encabezados en las primeras 25 filas (algunos archivos tienen encabezados muy abajo)
            for (int i = 0; i <= Math.min(25, hoja.getLastRowNum()); i++) {
                Row fila = hoja.getRow(i);
                if (fila == null) continue;
                
                int columnasRelevantes = 0;
                Set<String> columnasEncontradas = new HashSet<>();
                
                // Contar cuántas columnas relevantes encontramos
                for (Cell celda : fila) {
                    String valor = celda.toString().trim().toLowerCase();
                    columnasEncontradas.add(valor);
                    
                    // Patrones expandidos para diferentes formatos
                    if (valor.contains("item description") || valor.contains("item") || 
                        valor.contains("description") || valor.contains("quantity") || 
                        valor.contains("price") || valor.contains("unit of measure") ||
                        valor.contains("food categories") || valor.contains("total") ||
                        valor.contains("supplier comments") || valor.contains("remarks") ||
                        valor.contains("unit") || valor.contains("prices") || valor.contains("cantd") ||
                        valor.contains("usd total") || valor.equals("unit") || valor.equals("prices")) {
                        columnasRelevantes++;
                    }
                }
                
                // Verificación específica para formato HMM BLESSING
                boolean esFormatoHMM = columnasEncontradas.contains("unit") && 
                                       columnasEncontradas.contains("prices") && 
                                       columnasEncontradas.contains("cantd");
                
                // Si encontramos al menos 3 columnas relevantes, es probablemente el encabezado
                if (columnasRelevantes >= 3 || esFormatoHMM) {
                    encabezado = fila;
                    filaEncabezado = i;
                    logger.info("✅ Encabezado detectado en fila " + (i + 1) + " con " + columnasRelevantes + " columnas relevantes");
                    if (esFormatoHMM) {
                        logger.info("💡 Formato HMM BLESSING detectado por patrones específicos");
                    }
                    break;
                }
            }

            if (encabezado == null) {
                logger.severe("❌ No se encontró fila de encabezado válida en archivo: " + archivo.getName());
                logger.severe("🔍 DIAGNÓSTICO DETALLADO:");
                
                // Mostrar todas las filas para diagnóstico
                for (int i = 0; i <= Math.min(15, hoja.getLastRowNum()); i++) {
                    Row filaDebug = hoja.getRow(i);
                    if (filaDebug != null) {
                        logger.severe("   Fila " + (i + 1) + ":");
                        for (int j = 0; j < Math.min(10, filaDebug.getLastCellNum()); j++) {
                            Cell celdaDebug = filaDebug.getCell(j);
                            if (celdaDebug != null) {
                                String valorDebug = celdaDebug.toString().trim();
                                if (!valorDebug.isEmpty()) {
                                    logger.severe("     Col " + (char)('A' + j) + ": [" + valorDebug + "]");
                                }
                            }
                        }
                    }
                }
                return items;
            }

            Map<String, Integer> columnas = detectarColumnas(encabezado);
            
            // Aplicar mapeo dinámico basado en patrones detectados
            String formatoDetectado = detectarFormato(archivo, encabezado);
            if (!"GENERICO".equals(formatoDetectado)) {
                logger.info("🎯 Aplicando mapeo específico para formato: " + formatoDetectado);
                columnas = aplicarMapeoEspecifico(encabezado, columnas, formatoDetectado);
            }

            logger.info("✅ Encabezados detectados: " + columnas.size() + " columnas");
            columnas.forEach((k, v) -> logger.info("→ " + k + " en columna " + (char)('A' + v)));

            // Verificar que al menos tengamos descripción o código
            if (!columnas.containsKey("codigo") && !columnas.containsKey("descripcion")) {
                logger.warning("⚠️ Encabezados clave faltantes: 'codigo' y/o 'descripcion' en archivo: " + archivo.getName());
                logger.warning("🔍 Columnas detectadas: " + columnas.keySet());
                logger.warning("💡 Intentando usar cualquier columna de texto como descripción...");
                
                // Buscar cualquier columna que pueda servir como descripción
                for (Cell celda : encabezado) {
                    String valor = celda.toString().trim().toLowerCase();
                    if (valor.contains("desc") || valor.contains("name") || valor.contains("product") || 
                        valor.contains("item") || valor.contains("article") || valor.contains("provision")) {
                        columnas.put("descripcion", celda.getColumnIndex());
                        logger.info("💡 Usando '" + celda.toString() + "' como descripción");
                        break;
                    }
                }
                
                // Si aún no tenemos descripción, usar la primera columna no vacía
                if (!columnas.containsKey("descripcion")) {
                    for (Cell celda : encabezado) {
                        String valor = celda.toString().trim();
                        if (!valor.isEmpty()) {
                            columnas.put("descripcion", celda.getColumnIndex());
                            logger.info("💡 Usando '" + valor + "' como descripción por defecto");
                            break;
                        }
                    }
                }
                
                if (!columnas.containsKey("codigo") && !columnas.containsKey("descripcion")) {
                    logger.severe("❌ No se pueden procesar items sin código o descripción");
                    return items;
                }
            }
            
            logger.info("🚀 Iniciando procesamiento de datos. Filas esperadas: " + (hoja.getLastRowNum() - filaEncabezado));

            // Procesar filas de datos empezando después del encabezado
            for (int i = filaEncabezado + 1; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                if (fila == null) continue;

                String codigo = obtenerTexto(fila, columnas.get("codigo"));
                String descripcion = obtenerTexto(fila, columnas.get("descripcion"));
                
                // Si no hay código ni descripción, saltar esta fila
                if ((codigo == null || codigo.trim().isEmpty()) && 
                    (descripcion == null || descripcion.trim().isEmpty())) {
                    continue;
                }
                
                logger.info("📝 Procesando fila " + (i + 1) + ": " + codigo + " - " + descripcion);

                int cantidad = obtenerEntero(fila, columnas.get("cantidad"));
                
                // utilizar metodo consultarPrecioPorDescripcion()
                double precioPorDescripcion = consultarPrecioPorDescripcion(descripcion);
                logger.info("Leyendo item con descripción: " + descripcion);
                                
                //double precio = obtenerDecimal(fila, columnas.get("precio"));
                double precio = precioPorDescripcion;
                double precioCalculado = precioPorDescripcion;

                String unidad = columnas.containsKey("unidad") ? obtenerTexto(fila, columnas.get("unidad")) : "";
                String categoria = columnas.containsKey("categoria") ? obtenerTexto(fila, columnas.get("categoria")) : "";

                double descuento = columnas.containsKey("descuento") ? obtenerDecimal(fila, columnas.get("descuento")) : 0.0;
                double totalNeto = columnas.containsKey("totalneto") ? obtenerDecimal(fila, columnas.get("totalneto")) : 0.0;
                
                String comentarios = columnas.containsKey("comentarios") ? obtenerTexto(fila, columnas.get("comentarios")) : "";
                int disponibilidad = columnas.containsKey("disponibilidad") ? obtenerEntero(fila, columnas.get("disponibilidad")) : 0;
                
                //double totalBruto = columnas.containsKey("totalbruto") ? obtenerDecimal(fila, columnas.get("totalbruto")) : 0.0;
                double precioTotalBruto = precioCalculado * cantidad;
                double totalBruto = precioTotalBruto;

                if (codigo.isEmpty() && descripcion.isEmpty()) continue;

                ItemCotizacionExcel item = new ItemCotizacionExcel(
                    codigo, descripcion, cantidad, precio, unidad, categoria,
                    descuento, totalNeto, comentarios, disponibilidad, totalBruto
                );
                
                // Marcar si no se encontró precio en la base de datos (precio = 0.0)
                // para colorear la fila en amarillo
                item.setPrecioNoEncontrado(precio == 0.0);
                
                items.add(item);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error al leer archivo Excel: " + archivo.getName(), e);
        }

        logger.info("✅ Procesamiento completado. Items leídos: " + items.size());
        return items;
    }

    public boolean exportarItemsAExcel(List<ItemCotizacionExcel> items, File archivo) {
        logger.info("📤 Iniciando exportación de " + items.size() + " items a: " + archivo.getName());
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet hoja = workbook.createSheet("Cotización");

            Row encabezado = hoja.createRow(0);
            encabezado.createCell(0).setCellValue("Código");
            encabezado.createCell(1).setCellValue("Descripción");
            encabezado.createCell(2).setCellValue("Cantidad");
            encabezado.createCell(3).setCellValue("Precio");
            encabezado.createCell(4).setCellValue("Total");
            encabezado.createCell(5).setCellValue("Descuento");
            encabezado.createCell(6).setCellValue("Total Neto");
            encabezado.createCell(7).setCellValue("Comentarios");
            encabezado.createCell(8).setCellValue("Disponibilidad");
            encabezado.createCell(9).setCellValue("Total Bruto");

            for (int i = 0; i < items.size(); i++) {
                ItemCotizacionExcel item = items.get(i);
                Row fila = hoja.createRow(i + 1);
                fila.createCell(0).setCellValue(item.getCodigo());
                fila.createCell(1).setCellValue(item.getDescripcion());
                fila.createCell(2).setCellValue(item.getCantidad());
                fila.createCell(3).setCellValue(item.getPrecio());
                fila.createCell(4).setCellValue(item.getTotal());
                fila.createCell(5).setCellValue(item.getDescuento());
                fila.createCell(6).setCellValue(item.getTotalNeto());
                fila.createCell(7).setCellValue(item.getComentarios());
                fila.createCell(8).setCellValue(item.getDisponibilidad());
                fila.createCell(9).setCellValue(item.getTotalBruto());
            }

            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                workbook.write(fos);
            }

            logger.info("✅ Exportación completada exitosamente: " + archivo.getName());
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error al exportar items a Excel: " + archivo.getName(), e);
            return false;
        }
    }

    public boolean actualizarArchivoConPrecios(File archivo) {
        List<ItemCotizacionExcel> items = leerItemsDesdeExcel(archivo);

        for (ItemCotizacionExcel item : items) {
            double nuevoPrecio = item.getPrecio() * 1.10;
            item.setPrecio(nuevoPrecio);
        }

        return exportarItemsAExcel(items, archivo);
    }

    /**
     * Analiza completamente la estructura de un archivo Excel
     */
    public void analizarEstructuraExcel(File archivo) {
        logger.info("🔍 ANALIZADOR DE ESTRUCTURA EXCEL: " + archivo.getName());
        
        try (FileInputStream fis = new FileInputStream(archivo);
             Workbook workbook = new XSSFWorkbook(fis)) {

            logger.info("📊 Número de hojas: " + workbook.getNumberOfSheets());
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet hoja = workbook.getSheetAt(i);
                logger.info("\n📋 === HOJA " + (i+1) + ": " + hoja.getSheetName() + " ===");
                logger.info("   Filas totales: " + (hoja.getLastRowNum() + 1));
                
                // Analizar las primeras 10 filas para detectar encabezados
                for (int rowNum = 0; rowNum <= Math.min(10, hoja.getLastRowNum()); rowNum++) {
                    Row fila = hoja.getRow(rowNum);
                    if (fila != null) {
                        logger.info("   Fila " + (rowNum + 1) + ":");
                        
                        for (int colNum = 0; colNum < Math.min(20, fila.getLastCellNum()); colNum++) {
                            Cell celda = fila.getCell(colNum);
                            if (celda != null) {
                                String valor = celda.toString().trim();
                                if (!valor.isEmpty()) {
                                    logger.info("     Col " + (char)('A' + colNum) + ": [" + valor + "]");
                                }
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error analizando estructura del Excel", e);
        }
    }

    private Map<String, Integer> detectarColumnas(Row encabezado) {
        Map<String, Integer> mapa = new HashMap<>();
        Map<String, List<String>> sinonimos = new HashMap<>();
        
        // Sinónimos expandidos para diferentes formatos de Excel
        sinonimos.put("codigo", List.of("item code", "code", "item", "codigo", "código", "part number", "sku", "reference"));
        sinonimos.put("descripcion", List.of("description", "desc", "item description", "descripción", "product name", "name", "product description"));
        sinonimos.put("cantidad", List.of("quantity", "qty", "cantidad", "quantity order", "amount", "qte", "quantité"));
        sinonimos.put("precio", List.of("price", "unit price", "precio", "unit cost", "precio unitario", "cost", "rate", "valor"));
        sinonimos.put("unidad", List.of("unit", "unit of measure", "unidad", "uom", "measure", "medida"));
        sinonimos.put("categoria", List.of("category", "food categories", "categoría", "type", "grupo", "classification"));
        sinonimos.put("descuento", List.of("discount", "descuento", "rebate", "reduction"));
        sinonimos.put("totalneto", List.of("total net", "net total", "totalneto", "net amount", "subtotal"));
        sinonimos.put("comentarios", List.of("comments", "supplier comments", "comentarios", "notes", "remarks", "observations"));
        sinonimos.put("disponibilidad", List.of("availability", "disponibilidad", "stock", "inventory", "available"));
        sinonimos.put("totalbruto", List.of("total", "gross total", "total bruto", "grand total", "final total"));

        // Log detallado de todas las columnas encontradas
        logger.info("🔍 Analizando encabezados en fila " + (encabezado.getRowNum() + 1) + ":");
        
        for (Cell celda : encabezado) {
            String valorOriginal = celda.toString().trim();
            String valor = valorOriginal.toLowerCase().replaceAll("[^a-z0-9 ]", "");
            
            logger.info("   Columna " + (char)('A' + celda.getColumnIndex()) + ": [" + valorOriginal + "] -> [" + valor + "]");

            for (Map.Entry<String, List<String>> entry : sinonimos.entrySet()) {
                for (String alias : entry.getValue()) {
                    String normalizado = alias.toLowerCase().replaceAll("[^a-z0-9 ]", "");
                    if (valor.contains(normalizado) || normalizado.contains(valor)) {
                        mapa.put(entry.getKey(), celda.getColumnIndex());
                        logger.info("     ✅ MATCH: '" + entry.getKey() + "' -> Columna " + (char)('A' + celda.getColumnIndex()));
                        break;
                    }
                }
            }
        }
        
        logger.info("📊 Columnas detectadas finales:");
        mapa.forEach((k, v) -> logger.info("   " + k + " -> Columna " + (char)('A' + v)));

        return mapa;
    }

    /**
     * Detecta el formato del archivo Excel basándose en configuración
     */
    private String detectarFormato(File archivo, Row encabezado) {
        String nombreArchivo = archivo.getName().toUpperCase();
        
        // Recopilar todas las columnas del encabezado
        Set<String> columnasEncontradas = new HashSet<>();
        for (Cell celda : encabezado) {
            columnasEncontradas.add(celda.toString().trim().toUpperCase());
        }
        
        logger.info("🔍 Detectando formato. Columnas encontradas: " + columnasEncontradas);
        
        // Verificar cada formato configurado
        for (Map.Entry<String, FormatoExcel> entry : formatosConfigurados.entrySet()) {
            String nombreFormato = entry.getKey();
            FormatoExcel formato = entry.getValue();
            
            boolean coincideArchivo = false;
            boolean coincideColumnas = false;
            
            // Verificar patrones de nombre de archivo
            for (String patron : formato.patronesArchivo) {
                if (nombreArchivo.contains(patron.toUpperCase())) {
                    coincideArchivo = true;
                    break;
                }
            }
            
            // Verificar patrones de columnas
            int columnasCoincidentes = 0;
            for (String patron : formato.patronesColumnas) {
                if (columnasEncontradas.contains(patron.toUpperCase())) {
                    columnasCoincidentes++;
                }
            }
            
            if (columnasCoincidentes >= Math.min(formato.columnasMinimas, formato.patronesColumnas.size())) {
                coincideColumnas = true;
            }
            
            // Si coincide archivo O columnas, es este formato
            if (coincideArchivo || coincideColumnas) {
                logger.info("✅ Formato detectado: " + nombreFormato + 
                          " (archivo:" + coincideArchivo + ", columnas:" + coincideColumnas + ")");
                return nombreFormato;
            }
        }
        
        // Verificar formatos personalizados
        for (FormatoPersonalizado formato : formatosPersonalizados.values()) {
            // Verificar patrones de archivo
            boolean coincideArchivo = Arrays.stream(formato.patronesArchivo)
                    .anyMatch(patron -> nombreArchivo.contains(patron.toUpperCase()));
            
            // Verificar patrones de columna
            boolean coincideColumna = Arrays.stream(formato.patronesColumna)
                    .anyMatch(patron -> columnasEncontradas.contains(patron.toUpperCase()));
            
            if (coincideArchivo || coincideColumna) {
                logger.info("🎯 Formato personalizado detectado: " + formato.nombre);
                return formato.nombre;
            }
        }
        
        logger.info("📋 Formato no reconocido, usando mapeo genérico");
        return "GENERICO";
    }
    
    /**
     * Aplica mapeo específico basado en la configuración del formato
     */
    private Map<String, Integer> aplicarMapeoEspecifico(Row encabezado, Map<String, Integer> columnasBase, String formato) {
        logger.info("🎯 Aplicando mapeo para formato: " + formato);
        
        FormatoExcel configuracion = formatosConfigurados.get(formato);
        if (configuracion == null) {
            logger.info("ℹ️ No hay configuración específica, usando mapeo genérico");
            return columnasBase;
        }
        
        Map<String, Integer> mapeoEspecifico = new HashMap<>(columnasBase);
        
        // Aplicar mapeo basado en configuración
        for (Map.Entry<String, List<String>> mapeo : configuracion.mapeoColumnas.entrySet()) {
            String campoInterno = mapeo.getKey();
            List<String> columnasExternas = mapeo.getValue();
            
            for (Cell celda : encabezado) {
                String valorCelda = celda.toString().trim().toUpperCase();
                
                // Verificar si esta celda coincide con alguna de las columnas externas
                for (String columnaExterna : columnasExternas) {
                    if (columnaExterna.equals("COLUMN_A") && celda.getColumnIndex() == 0) {
                        // Caso especial para columna A (HMM BLESSING)
                        mapeoEspecifico.put(campoInterno, 0);
                        logger.info("   ✅ Columna A -> " + campoInterno);
                        break;
                    } else if (valorCelda.equals(columnaExterna.toUpperCase()) || 
                               valorCelda.contains(columnaExterna.toUpperCase())) {
                        mapeoEspecifico.put(campoInterno, celda.getColumnIndex());
                        logger.info("   ✅ " + valorCelda + " -> " + campoInterno + " (columna " + (char)('A' + celda.getColumnIndex()) + ")");
                        break;
                    }
                }
            }
        }
        
        logger.info("🔄 Mapeo " + formato + " completado. Columnas mapeadas: " + mapeoEspecifico.size());
        return mapeoEspecifico;
    }
    
    /**
     * Mapeo específico para archivos formato QTN_GOF
     */
    private Map<String, Integer> aplicarMapeoQTN_GOF(Row encabezado, Map<String, Integer> columnas) {
        logger.info("🎯 Aplicando mapeo específico QTN_GOF...");
        
        Map<String, Integer> mapeoEspecifico = new HashMap<>(columnas);
        
        // Mapeo directo basado en el análisis del archivo QTN_GOF_233.xlsx
        for (Cell celda : encabezado) {
            String valor = celda.toString().trim().toUpperCase();
            int col = celda.getColumnIndex();
            
            switch (valor) {
                case "ITEM":
                    mapeoEspecifico.put("codigo", col);
                    logger.info("   ✅ ITEM -> codigo (columna " + (char)('A' + col) + ")");
                    break;
                case "ITEM DESCRIPTION":
                    mapeoEspecifico.put("descripcion", col);
                    logger.info("   ✅ ITEM DESCRIPTION -> descripcion (columna " + (char)('A' + col) + ")");
                    break;
                case "QUANTITY ORDER":
                    mapeoEspecifico.put("cantidad", col);
                    logger.info("   ✅ QUANTITY ORDER -> cantidad (columna " + (char)('A' + col) + ")");
                    break;
                case "PRICE":
                    mapeoEspecifico.put("precio", col);
                    logger.info("   ✅ PRICE -> precio (columna " + (char)('A' + col) + ")");
                    break;
                case "UNIT OF MEASURE":
                    mapeoEspecifico.put("unidad", col);
                    logger.info("   ✅ UNIT OF MEASURE -> unidad (columna " + (char)('A' + col) + ")");
                    break;
                case "FOOD CATEGORIES":
                    mapeoEspecifico.put("categoria", col);
                    logger.info("   ✅ FOOD CATEGORIES -> categoria (columna " + (char)('A' + col) + ")");
                    break;
                case "SUPPLIER COMMENTS":
                    mapeoEspecifico.put("comentarios", col);
                    logger.info("   ✅ SUPPLIER COMMENTS -> comentarios (columna " + (char)('A' + col) + ")");
                    break;
                case "TOTAL":
                    mapeoEspecifico.put("totalbruto", col);
                    logger.info("   ✅ TOTAL -> totalbruto (columna " + (char)('A' + col) + ")");
                    break;
                case "VESSEL COMMENTS":
                    // Esta columna es específica del formato, podríamos agregarla al modelo si es necesaria
                    logger.info("   ℹ️ VESSEL COMMENTS encontrado en columna " + (char)('A' + col) + " (no mapeado)");
                    break;
                case "MCTC'S REF NO":
                case "MCTC S REF NO":
                    // Referencia específica del formato, podría ser útil
                    logger.info("   ℹ️ MCTC REF NO encontrado en columna " + (char)('A' + col) + " (no mapeado)");
                    break;
                case "AA":
                    // Columna de numeración
                    logger.info("   ℹ️ AA (numeración) encontrado en columna " + (char)('A' + col) + " (no mapeado)");
                    break;
            }
        }
        
        logger.info("🔄 Mapeo QTN_GOF completado. Columnas mapeadas: " + mapeoEspecifico.size());
        return mapeoEspecifico;
    }
    
    /**
     * Mapeo específico para archivos formato PROVISION ORDER
     */
    private Map<String, Integer> aplicarMapeoPROVISION_ORDER(Row encabezado, Map<String, Integer> columnas) {
        logger.info("🚢 Aplicando mapeo específico PROVISION ORDER...");
        
        Map<String, Integer> mapeoEspecifico = new HashMap<>(columnas);
        
        for (Cell celda : encabezado) {
            String valor = celda.toString().trim().toUpperCase();
            int col = celda.getColumnIndex();
            
            switch (valor) {
                case "ITEM CODE":
                case "CODE":
                case "PART NO":
                case "PART NUMBER":
                case "NO.":
                    mapeoEspecifico.put("codigo", col);
                    logger.info("   ✅ " + valor + " -> codigo (columna " + (char)('A' + col) + ")");
                    break;
                case "DESCRIPTION":
                case "ITEM DESCRIPTION":
                case "PROVISION DESCRIPTION":
                case "PRODUCT DESCRIPTION":
                case "DESC":
                    mapeoEspecifico.put("descripcion", col);
                    logger.info("   ✅ " + valor + " -> descripcion (columna " + (char)('A' + col) + ")");
                    break;
                case "QUANTITY":
                case "QTY":
                case "AMOUNT":
                case "ORDERED QTY":
                case "ORDER QTY":
                    mapeoEspecifico.put("cantidad", col);
                    logger.info("   ✅ " + valor + " -> cantidad (columna " + (char)('A' + col) + ")");
                    break;
                case "UNIT PRICE":
                case "PRICE":
                case "RATE":
                case "UNIT COST":
                case "COST":
                    mapeoEspecifico.put("precio", col);
                    logger.info("   ✅ " + valor + " -> precio (columna " + (char)('A' + col) + ")");
                    break;
                case "UNIT":
                case "UOM":
                case "UNIT OF MEASURE":
                case "U/M":
                    mapeoEspecifico.put("unidad", col);
                    logger.info("   ✅ " + valor + " -> unidad (columna " + (char)('A' + col) + ")");
                    break;
                case "TOTAL":
                case "TOTAL AMOUNT":
                case "LINE TOTAL":
                case "AMOUNT TOTAL":
                    mapeoEspecifico.put("totalbruto", col);
                    logger.info("   ✅ " + valor + " -> totalbruto (columna " + (char)('A' + col) + ")");
                    break;
                case "REMARKS":
                case "COMMENTS":
                case "NOTES":
                case "OBSERVATION":
                    mapeoEspecifico.put("comentarios", col);
                    logger.info("   ✅ " + valor + " -> comentarios (columna " + (char)('A' + col) + ")");
                    break;
                default:
                    // Buscar patrones más flexibles
                    if (valor.contains("DESCRIP") || valor.contains("DETAIL")) {
                        mapeoEspecifico.put("descripcion", col);
                        logger.info("   ✅ " + valor + " -> descripcion (columna " + (char)('A' + col) + ")");
                    } else if (valor.contains("QTY") || valor.contains("QUANT")) {
                        mapeoEspecifico.put("cantidad", col);
                        logger.info("   ✅ " + valor + " -> cantidad (columna " + (char)('A' + col) + ")");
                    } else if (valor.contains("PRICE") || valor.contains("COST")) {
                        mapeoEspecifico.put("precio", col);
                        logger.info("   ✅ " + valor + " -> precio (columna " + (char)('A' + col) + ")");
                    } else if (valor.contains("TOTAL")) {
                        mapeoEspecifico.put("totalbruto", col);
                        logger.info("   ✅ " + valor + " -> totalbruto (columna " + (char)('A' + col) + ")");
                    }
                    break;
            }
        }
        
        logger.info("🔄 Mapeo PROVISION ORDER completado. Columnas mapeadas: " + mapeoEspecifico.size());
        return mapeoEspecifico;
    }
    
    /**
     * Mapeo específico para archivos formato HMM BLESSING
     */
    private Map<String, Integer> aplicarMapeoHMM_BLESSING(Row encabezado, Map<String, Integer> columnas) {
        logger.info("😢 Aplicando mapeo específico HMM BLESSING...");
        
        Map<String, Integer> mapeoEspecifico = new HashMap<>();
        
        // Mapeo basado en el diagnóstico del archivo real
        // Columna A = Descripción (datos empiezan en fila 14)
        // Columna B = Remarks 
        // Columna C = UNIT
        // Columna D = PRICES
        // Columna F = CANTD (cantidad)
        // Columna G = USD TOTAL
        
        for (Cell celda : encabezado) {
            String valor = celda.toString().trim().toUpperCase();
            int col = celda.getColumnIndex();
            char colChar = (char)('A' + col);
            
            logger.info("🔍 Analizando columna " + colChar + ": [" + valor + "]");
            
            switch (valor) {
                case "REMARKS":
                    mapeoEspecifico.put("comentarios", col);
                    logger.info("   ✅ REMARKS -> comentarios (columna " + colChar + ")");
                    break;
                case "UNIT":
                    mapeoEspecifico.put("unidad", col);
                    logger.info("   ✅ UNIT -> unidad (columna " + colChar + ")");
                    break;
                case "PRICES":
                    mapeoEspecifico.put("precio", col);
                    logger.info("   ✅ PRICES -> precio (columna " + colChar + ")");
                    break;
                case "CANTD":
                    mapeoEspecifico.put("cantidad", col);
                    logger.info("   ✅ CANTD -> cantidad (columna " + colChar + ")");
                    break;
                case "USD TOTAL":
                    mapeoEspecifico.put("totalbruto", col);
                    logger.info("   ✅ USD TOTAL -> totalbruto (columna " + colChar + ")");
                    break;
            }
        }
        
        // La columna A contiene las descripciones (aunque no está en el encabezado)
        mapeoEspecifico.put("descripcion", 0); // Columna A = índice 0
        logger.info("   ✅ Columna A -> descripcion (productos)");
        
        // No hay códigos explícitos, usar la descripción como código también
        mapeoEspecifico.put("codigo", 0); // Usar descripción como código
        logger.info("   ℹ️ Usando descripción como código (no hay columna de códigos separada)");
        
        logger.info("🔄 Mapeo HMM BLESSING completado. Columnas mapeadas: " + mapeoEspecifico.size());
        logger.info("📊 Mapeo final: " + mapeoEspecifico);
        return mapeoEspecifico;
    }

    /**
     * Mapeo específico para archivos formato FERNANDINA
     */
    private Map<String, Integer> aplicarMapeoFERNANDINA(Row encabezado, Map<String, Integer> columnas) {
        logger.info("🐠 Aplicando mapeo específico FERNANDINA...");
        
        Map<String, Integer> mapeoEspecifico = new HashMap<>(columnas);
        
        for (Cell celda : encabezado) {
            String valor = celda.toString().trim().toUpperCase();
            int col = celda.getColumnIndex();
            
            switch (valor) {
                case "CÓDIGO":
                case "CODIGO":
                case "COD":
                    mapeoEspecifico.put("codigo", col);
                    logger.info("   ✅ " + valor + " -> codigo (columna " + (char)('A' + col) + ")");
                    break;
                case "DESCRIPCIÓN":
                case "DESCRIPCION":
                case "DESC":
                    mapeoEspecifico.put("descripcion", col);
                    logger.info("   ✅ " + valor + " -> descripcion (columna " + (char)('A' + col) + ")");
                    break;
                case "CANTIDAD":
                case "QTY":
                case "CANT":
                    mapeoEspecifico.put("cantidad", col);
                    logger.info("   ✅ " + valor + " -> cantidad (columna " + (char)('A' + col) + ")");
                    break;
            }
        }
        
        logger.info("🔄 Mapeo FERNANDINA completado. Columnas mapeadas: " + mapeoEspecifico.size());
        return mapeoEspecifico;
    }
    
    /**
     * Mapeo específico para archivos formato INVENTARIO
     */
    private Map<String, Integer> aplicarMapeoINVENTARIO(Row encabezado, Map<String, Integer> columnas) {
        logger.info("📦 Aplicando mapeo específico INVENTARIO...");
        
        Map<String, Integer> mapeoEspecifico = new HashMap<>(columnas);
        
        for (Cell celda : encabezado) {
            String valor = celda.toString().trim().toUpperCase();
            int col = celda.getColumnIndex();
            
            switch (valor) {
                case "SKU":
                case "PART NUMBER":
                case "PART #":
                    mapeoEspecifico.put("codigo", col);
                    logger.info("   ✅ " + valor + " -> codigo (columna " + (char)('A' + col) + ")");
                    break;
                case "PRODUCT NAME":
                case "PRODUCT DESCRIPTION":
                case "NAME":
                    mapeoEspecifico.put("descripcion", col);
                    logger.info("   ✅ " + valor + " -> descripcion (columna " + (char)('A' + col) + ")");
                    break;
                case "STOCK":
                case "INVENTORY":
                case "QTY ON HAND":
                    mapeoEspecifico.put("cantidad", col);
                    logger.info("   ✅ " + valor + " -> cantidad (columna " + (char)('A' + col) + ")");
                    break;
                case "COST":
                case "UNIT COST":
                case "WHOLESALE":
                    mapeoEspecifico.put("precio", col);
                    logger.info("   ✅ " + valor + " -> precio (columna " + (char)('A' + col) + ")");
                    break;
            }
        }
        
        logger.info("🔄 Mapeo INVENTARIO completado. Columnas mapeadas: " + mapeoEspecifico.size());
        return mapeoEspecifico;
    }
    
    /**
     * Mapeo específico para archivos formato FACTURA
     */
    private Map<String, Integer> aplicarMapeoFACTURA(Row encabezado, Map<String, Integer> columnas) {
        logger.info("🧾 Aplicando mapeo específico FACTURA...");
        
        Map<String, Integer> mapeoEspecifico = new HashMap<>(columnas);
        
        for (Cell celda : encabezado) {
            String valor = celda.toString().trim().toUpperCase();
            int col = celda.getColumnIndex();
            
            switch (valor) {
                case "INVOICE #":
                case "INVOICE NUMBER":
                case "BILL #":
                    mapeoEspecifico.put("codigo", col);
                    logger.info("   ✅ " + valor + " -> codigo (columna " + (char)('A' + col) + ")");
                    break;
                case "LINE DESCRIPTION":
                case "SERVICE":
                case "PRODUCT":
                    mapeoEspecifico.put("descripcion", col);
                    logger.info("   ✅ " + valor + " -> descripcion (columna " + (char)('A' + col) + ")");
                    break;
                case "QTY":
                case "HOURS":
                case "UNITS":
                    mapeoEspecifico.put("cantidad", col);
                    logger.info("   ✅ " + valor + " -> cantidad (columna " + (char)('A' + col) + ")");
                    break;
                case "RATE":
                case "UNIT PRICE":
                case "HOURLY RATE":
                    mapeoEspecifico.put("precio", col);
                    logger.info("   ✅ " + valor + " -> precio (columna " + (char)('A' + col) + ")");
                    break;
                case "AMOUNT":
                case "LINE TOTAL":
                case "SUBTOTAL":
                    mapeoEspecifico.put("totalbruto", col);
                    logger.info("   ✅ " + valor + " -> totalbruto (columna " + (char)('A' + col) + ")");
                    break;
            }
        }
        
        logger.info("🔄 Mapeo FACTURA completado. Columnas mapeadas: " + mapeoEspecifico.size());
        return mapeoEspecifico;
    }
    
    /**
     * Lista todos los formatos de Excel soportados
     */
    public List<String> getFormatosSoportados() {
        List<String> formatos = new ArrayList<>(formatosConfigurados.keySet());
        formatos.add("GENERICO");
        return formatos;
    }
    
    /**
     * Proporciona información sobre qué columnas espera cada formato
     */
    public void mostrarInformacionFormatos() {
        logger.info("📊 FORMATOS DE EXCEL CONFIGURADOS:");
        
        for (Map.Entry<String, FormatoExcel> entry : formatosConfigurados.entrySet()) {
            FormatoExcel formato = entry.getValue();
            logger.info("🎯 " + entry.getKey() + ":");
            logger.info("   📁 Patrones de archivo: " + formato.patronesArchivo);
            logger.info("   📋 Patrones de columnas: " + formato.patronesColumnas);
            
            StringBuilder mapeos = new StringBuilder("   🔗 Mapeos: ");
            formato.mapeoColumnas.forEach((interno, externo) -> 
                mapeos.append(interno).append("=").append(externo).append(", "));
            logger.info(mapeos.toString());
        }
        
        logger.info("📋 GENÉRICO: Cualquier combinación de columnas comunes detectadas automáticamente");
    }
    
    /**
     * Agregar nuevo formato dinámicamente
     */
    public void agregarFormato(String nombre, FormatoExcel configuracion) {
        formatosConfigurados.put(nombre, configuracion);
        logger.info("✅ Nuevo formato agregado: " + nombre);
    }
    
    /**
     * Método de utilidad para crear configuraciones de formato desde código
     */
    public static FormatoExcel crearFormato(String nombre) {
        return new FormatoExcel(nombre);
    }
    
    // Registro de formatos personalizados
    private final Map<String, FormatoPersonalizado> formatosPersonalizados = new HashMap<>();
    
    /**
     * Clase para definir formatos personalizados
     */
    public static class FormatoPersonalizado {
        public final String nombre;
        public final String[] patronesArchivo;
        public final String[] patronesColumna;
        public final Map<String, String[]> mapeoColumnas;
        
        public FormatoPersonalizado(String nombre, String[] patronesArchivo, 
                                   String[] patronesColumna, Map<String, String[]> mapeoColumnas) {
            this.nombre = nombre;
            this.patronesArchivo = patronesArchivo;
            this.patronesColumna = patronesColumna;
            this.mapeoColumnas = mapeoColumnas;
        }
    }
    
    /**
     * Agrega un formato personalizado al sistema
     */
    public void agregarFormatoPersonalizado(String nombre, String[] patronesArchivo, 
                                           String[] patronesColumna, Map<String, String[]> mapeoColumnas) {
        FormatoPersonalizado formato = new FormatoPersonalizado(nombre, patronesArchivo, patronesColumna, mapeoColumnas);
        formatosPersonalizados.put(nombre, formato);
        logger.info("✅ Formato personalizado registrado: " + nombre);
        logger.info("📂 Patrones de archivo: " + Arrays.toString(patronesArchivo));
        logger.info("📊 Patrones de columna: " + Arrays.toString(patronesColumna));
        logger.info("🔄 Mapeos: " + mapeoColumnas.size() + " columnas configuradas");
    }

    private String obtenerTexto(Row fila, Integer index) {
        if (index == null) return "";
        Cell celda = fila.getCell(index);
        return (celda != null) ? celda.toString().trim() : "";
    }

    private int obtenerEntero(Row fila, Integer index) {
        if (index == null) return 0;
        Cell celda = fila.getCell(index);
        if (celda == null) return 0;

        try {
            if (celda.getCellType() == CellType.NUMERIC) {
                return (int) celda.getNumericCellValue();
            } else if (celda.getCellType() == CellType.STRING) {
                return Integer.parseInt(celda.getStringCellValue().trim());
            } else if (celda.getCellType() == CellType.FORMULA) {
                return (int) celda.getNumericCellValue();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "⚠️ Error leyendo cantidad en índice " + index, e);
        }
        return 0;
    }

    private double obtenerDecimal(Row fila, Integer index) {
    if (index == null) return 0.0;
    Cell celda = fila.getCell(index);
    if (celda == null) return 0.0;

    try {
        if (celda.getCellType() == CellType.NUMERIC) {
            return celda.getNumericCellValue();
        } else if (celda.getCellType() == CellType.STRING) {
            return Double.parseDouble(celda.getStringCellValue().trim().replace(",", "."));
        } else if (celda.getCellType() == CellType.FORMULA) {
            return celda.getNumericCellValue();
        }
    } catch (Exception e) {
        logger.log(Level.WARNING, "⚠️ Error leyendo decimal en índice " + index, e);
    }

    return 0.0;
  }

  /**
   * Exporta una lista de ItemCotizacionExcel a un archivo Excel
   */
  public void exportarCotizacion(List<ItemCotizacionExcel> items, File archivo) throws Exception {
      try (Workbook workbook = new XSSFWorkbook()) {
          Sheet sheet = workbook.createSheet("Cotización");
          
          // Crear estilos
          CellStyle headerStyle = workbook.createCellStyle();
          Font headerFont = workbook.createFont();
          headerFont.setBold(true);
          headerFont.setColor(IndexedColors.WHITE.getIndex());
          headerStyle.setFont(headerFont);
          headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
          headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
          headerStyle.setBorderBottom(BorderStyle.THIN);
          headerStyle.setBorderTop(BorderStyle.THIN);
          headerStyle.setBorderRight(BorderStyle.THIN);
          headerStyle.setBorderLeft(BorderStyle.THIN);
          
          // Crear encabezados
          Row headerRow = sheet.createRow(0);
          String[] headers = {"Código", "Descripción", "Cantidad", "Precio", "Total", 
                            "Descuento", "Total Neto", "Comentarios", "Disponibilidad", "Total Bruto"};
          
          for (int i = 0; i < headers.length; i++) {
              Cell cell = headerRow.createCell(i);
              cell.setCellValue(headers[i]);
              cell.setCellStyle(headerStyle);
          }
          
          // Agregar datos
          int rowNum = 1;
          for (ItemCotizacionExcel item : items) {
              Row row = sheet.createRow(rowNum++);
              
              row.createCell(0).setCellValue(item.getCodigo());
              row.createCell(1).setCellValue(item.getDescripcion());
              row.createCell(2).setCellValue(item.getCantidad());
              row.createCell(3).setCellValue(item.getPrecio());
              row.createCell(4).setCellValue(item.getTotal());
              row.createCell(5).setCellValue(item.getDescuento());
              row.createCell(6).setCellValue(item.getTotalNeto());
              row.createCell(7).setCellValue(item.getComentarios());
              row.createCell(8).setCellValue(item.getDisponibilidad());
              row.createCell(9).setCellValue(item.getTotalBruto());
          }
          
          // Ajustar ancho de columnas
          for (int i = 0; i < headers.length; i++) {
              sheet.autoSizeColumn(i);
          }
          
          // Escribir archivo
          try (FileOutputStream fos = new FileOutputStream(archivo)) {
              workbook.write(fos);
          }
      }
  }

  /**
   * Consulta el precio de venta neto de un producto desde la vista vista_producto_precio
   * @param descripcion descripción del producto a buscar
   * @return precio de venta neto, o 0.0 si no se encuentra
   */
  public double consultarPrecioPorDescripcion(String descripcion) {
      logger.info("💰 Consultando precio para descripción: " + descripcion);
      
      if (descripcion == null || descripcion.trim().isEmpty()) {
          logger.warning("⚠️ Descripción vacía o nula para consulta de precio");
          return 0.0;
      }
      
      // Buscar en ambas columnas de descripción (español e inglés) en tabla producto
      /*String sql = "SELECT valor_pesos FROM producto " +
                  "WHERE UPPER(descripcion_es) LIKE UPPER(?) OR UPPER(descripcion_en) LIKE UPPER(?) " +
                  "LIMIT 1";*/

        String sql = "SELECT precio_venta_neto FROM vista_producto_precio " +
                  "WHERE UPPER(descripcion_es) LIKE UPPER(?) OR UPPER(descripcion_en) LIKE UPPER(?) " +
                  "LIMIT 1";
      
      try (Connection connection = DBConnection.getConnection();
           PreparedStatement statement = connection.prepareStatement(sql)) {
          
          // Preparar el parámetro con wildcards para búsqueda parcial
          String descripcionBusqueda = "%" + descripcion.trim() + "%";
          statement.setString(1, descripcionBusqueda);
          statement.setString(2, descripcionBusqueda);
          
          logger.fine("🔍 Ejecutando consulta SQL: " + sql);
          logger.fine("🔍 Parámetro búsqueda: " + descripcionBusqueda);
          
          try (ResultSet resultSet = statement.executeQuery()) {
              if (resultSet.next()) {
                  double precio = resultSet.getDouble("precio_venta_neto");
                  logger.info("✅ Precio encontrado: $" + precio + " para descripción: " + descripcion);
                  return precio;
              } else {
                  logger.warning("⚠️ No se encontró precio para descripción: " + descripcion);
                  return 0.0;
              }
          }
          
      } catch (SQLException e) {
          logger.log(Level.SEVERE, "❌ Error al consultar precio para descripción: " + descripcion, e);
          return 0.0;
      }
  }
  
  /**
   * Consulta el precio de venta neto de un producto con búsqueda exacta
   * @param descripcion descripción exacta del producto
   * @return precio de venta neto, o 0.0 si no se encuentra
   */
  public double consultarPrecioExactoPorDescripcion(String descripcion) {
      logger.info("💰 Consultando precio exacto para descripción: " + descripcion);
      
      if (descripcion == null || descripcion.trim().isEmpty()) {
          logger.warning("⚠️ Descripción vacía o nula para consulta de precio exacto");
          return 0.0;
      }
      
      // Buscar coincidencia exacta en ambas columnas de descripción en tabla producto
      String sql = "SELECT valor_pesos FROM producto " +
                  "WHERE UPPER(descripcion_es) = UPPER(?) OR UPPER(descripcion_en) = UPPER(?) " +
                  "LIMIT 1";
      
      try (Connection connection = DBConnection.getConnection();
           PreparedStatement statement = connection.prepareStatement(sql)) {
          
          statement.setString(1, descripcion.trim());
          statement.setString(2, descripcion.trim());
          
          logger.fine("🔍 Ejecutando consulta SQL exacta: " + sql);
          logger.fine("🔍 Parámetro: " + descripcion.trim());
          
          try (ResultSet resultSet = statement.executeQuery()) {
              if (resultSet.next()) {
                  double precio = resultSet.getDouble("valor_pesos");
                  logger.info("✅ Precio exacto encontrado: $" + precio + " para descripción: " + descripcion);
                  return precio;
              } else {
                  logger.warning("⚠️ No se encontró precio exacto para descripción: " + descripcion);
                  return 0.0;
              }
          }
          
      } catch (SQLException e) {
          logger.log(Level.SEVERE, "❌ Error al consultar precio exacto para descripción: " + descripcion, e);
          return 0.0;
      }
  }



  /**
   * Busca productos similares por descripción y retorna una lista con todos los datos
   * @param descripcion descripción del producto a buscar
   * @return lista de productos similares encontrados
   */
  public List<ProductoSimilar> buscarProductosSimilares(String descripcion) {
      logger.info("🔍 Buscando productos similares para descripción: " + descripcion);
      List<ProductoSimilar> productos = new ArrayList<>();
      
      if (descripcion == null || descripcion.trim().isEmpty()) {
          logger.warning("⚠️ Descripción vacía o nula para búsqueda de productos similares");
          return productos;
      }
      
      // Construir consulta dinámicamente basada en la estructura de la vista
      String sql = construirConsultaConVista();
      
      try (Connection connection = DBConnection.getConnection()) {
          logger.info("✅ Conexión establecida para búsqueda de productos similares");
          
          // Primero verificar si hay datos en la tabla producto
          String countSql = "SELECT COUNT(*) as total FROM producto";
          try (PreparedStatement countStatement = connection.prepareStatement(countSql);
               ResultSet countResult = countStatement.executeQuery()) {
              if (countResult.next()) {
                  int totalProductos = countResult.getInt("total");
                  logger.info("📊 Total productos en tabla: " + totalProductos);
                  if (totalProductos == 0) {
                      logger.warning("⚠️ La tabla producto está vacía");
                      return productos;
                  }
              }
          }
          
          try (PreparedStatement statement = connection.prepareStatement(sql)) {
              // Preparar parámetros con wildcards para búsqueda parcial
              String descripcionBusqueda = "%" + descripcion.trim() + "%";
              statement.setString(1, descripcionBusqueda);
              statement.setString(2, descripcionBusqueda);
              
              logger.info("🔍 Ejecutando consulta SQL productos similares: " + sql);
              logger.info("🔍 Parámetro búsqueda: " + descripcionBusqueda);
          
              try (ResultSet resultSet = statement.executeQuery()) {
                  logger.info("🔄 Ejecutando consulta...");
                  int contador = 0;
                  while (resultSet.next()) {
                      contador++;
                      String descEs = resultSet.getString("descripcion_es");
                      String descEn = resultSet.getString("descripcion_en");
                      String unidad = resultSet.getString("unidad_medida");
                      //double precio = resultSet.getDouble("valor_pesos");                    
                      double precio = resultSet.getDouble("precio_venta_neto");
                      double precioDolares = resultSet.getDouble("precio_venta_neto_dolares");
                      
                      logger.info("📦 Producto " + contador + ": ES=" + descEs + ", EN=" + descEn + ", Precio=" + precio + ", PrecioDolares=" + precioDolares);
                      
                      ProductoSimilar producto = new ProductoSimilar(descEs, descEn, unidad, precio, precioDolares);
                      productos.add(producto);
                  }
                  
                  if (contador == 0) {
                      logger.warning("⚠️ La consulta no devolvió ningún resultado");
                      // Hacer una consulta de prueba más simple
                      String testSql = "SELECT descripcion_es FROM producto LIMIT 5";
                      try (PreparedStatement testStatement = connection.prepareStatement(testSql);
                           ResultSet testResult = testStatement.executeQuery()) {
                          logger.info("🧪 Probando consulta simple...");
                          while (testResult.next()) {
                              logger.info("📋 Producto encontrado: " + testResult.getString("descripcion_es"));
                          }
                      }
                  }
                  
                  logger.info("✅ Encontrados " + productos.size() + " productos similares para: " + descripcion);
              
              }
          }
          
      } catch (SQLException e) {
          logger.log(Level.SEVERE, "❌ Error al buscar productos similares para descripción: " + descripcion, e);
          logger.log(Level.SEVERE, "❌ Detalles del error SQL: " + e.getSQLState() + " - " + e.getErrorCode());
      }
      
      return productos;
  }

  /**
   * Consulta el precio de venta neto de un producto desde la vista vista_producto_precio
   * Método público que busca coincidencias parciales en las descripciones
   * @param descripcion descripción del producto a buscar
   * @return precio de venta neto, o 0.0 si no se encuentra
   */
  public double consultarPrecioNetoPorDescripcion(String descripcion) {
      logger.info("💰 Consultando precio neto para descripción: " + descripcion);
      
      if (descripcion == null || descripcion.trim().isEmpty()) {
          logger.warning("⚠️ Descripción vacía o nula para consulta de precio neto");
          return 0.0;
      }
      
      // Buscar coincidencias parciales en ambas columnas de descripción en tabla producto
      String sql = "SELECT valor_pesos FROM producto " +
                  "WHERE UPPER(descripcion_es) LIKE UPPER(?) OR UPPER(descripcion_en) LIKE UPPER(?) " +
                  "ORDER BY valor_pesos DESC " +
                  "LIMIT 1";
      
      try (Connection connection = DBConnection.getConnection();
           PreparedStatement statement = connection.prepareStatement(sql)) {
          
          // Preparar parámetros con wildcards para búsqueda parcial
          String descripcionBusqueda = "%" + descripcion.trim() + "%";
          statement.setString(1, descripcionBusqueda);
          statement.setString(2, descripcionBusqueda);
          
          logger.fine("🔍 Ejecutando consulta SQL precio neto: " + sql);
          logger.fine("🔍 Parámetro búsqueda: " + descripcionBusqueda);
          
          try (ResultSet resultSet = statement.executeQuery()) {
              if (resultSet.next()) {
                  double precioNeto = resultSet.getDouble("valor_pesos");
                  logger.info("✅ Precio neto encontrado: $" + precioNeto + " para descripción: " + descripcion);
                  return precioNeto;
              } else {
                  logger.warning("⚠️ No se encontró precio neto para descripción: " + descripcion);
                  return 0.0;
              }
          }
          
      } catch (SQLException e) {
          logger.log(Level.SEVERE, "❌ Error al consultar precio neto para descripción: " + descripcion, e);
          return 0.0;
      }
  }

  /**
   * Método de diagnóstico para verificar la estructura y contenido de la tabla producto
   */
  public void diagnosticarTablaProducto() {
      logger.info("🔧 Iniciando diagnóstico de tabla producto");
      
      try (Connection connection = DBConnection.getConnection()) {
          // Verificar si la tabla existe
          String checkTableSql = "SELECT table_name FROM information_schema.tables WHERE table_name = 'producto'";
          try (PreparedStatement statement = connection.prepareStatement(checkTableSql);
               ResultSet resultSet = statement.executeQuery()) {
              if (resultSet.next()) {
                  logger.info("✅ Tabla 'producto' existe");
              } else {
                  logger.severe("❌ Tabla 'producto' NO existe");
                  return;
              }
          }
          
          // Verificar columnas
          String columnsSql = "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'producto'";
          try (PreparedStatement statement = connection.prepareStatement(columnsSql);
               ResultSet resultSet = statement.executeQuery()) {
              logger.info("📋 Columnas de la tabla producto:");
              while (resultSet.next()) {
                  logger.info("  - " + resultSet.getString("column_name") + " (" + resultSet.getString("data_type") + ")");
              }
          }
          
          // Verificar datos de muestra
          String sampleSql = "SELECT descripcion_es, descripcion_en, unidad_medida, valor_pesos FROM producto LIMIT 3";
          try (PreparedStatement statement = connection.prepareStatement(sampleSql);
               ResultSet resultSet = statement.executeQuery()) {
              logger.info("📊 Datos de muestra:");
              int count = 0;
              while (resultSet.next()) {
                  count++;
                  logger.info("  " + count + ". ES: " + resultSet.getString("descripcion_es") + 
                            ", EN: " + resultSet.getString("descripcion_en") + 
                            ", Unidad: " + resultSet.getString("unidad_medida") + 
                            ", Precio: " + resultSet.getDouble("valor_pesos"));
              }
              if (count == 0) {
                  logger.warning("⚠️ No hay datos en la tabla producto");
              }
          }
          
      } catch (SQLException e) {
          logger.log(Level.SEVERE, "❌ Error en diagnóstico de tabla producto", e);
      }
  }

  /**
   * Construye dinámicamente una consulta SQL basada en la estructura de vista_producto_precio
   */
  private String construirConsultaConVista() {
      logger.info("🔧 Construyendo consulta SQL dinámica basada en vista_producto_precio");
      try (Connection connection = DBConnection.getConnection()) {
          // Verificar si la vista existe y qué columnas tiene
          String checkColumnsSql = "SELECT column_name FROM information_schema.columns " +
                                  "WHERE table_name = 'vista_producto_precio' " +
                                  "AND column_name IN ('descripcion', 'descripcion_es', 'descripcion_en', 'precio_venta_neto')";
          
          try (PreparedStatement statement = connection.prepareStatement(checkColumnsSql);
               ResultSet result = statement.executeQuery()) {
              
              boolean tienePrecioVentaNeto = false;
              
              while (result.next()) {
                  String columnName = result.getString("column_name");
                  if ("precio_venta_neto".equals(columnName)) {
                      tienePrecioVentaNeto = true;
                      break; // Solo necesitamos verificar esta columna
                  }
              }
              
              if (tienePrecioVentaNeto) {
                  // La vista vista_producto_precio tiene precio_venta_neto, pero producto solo tiene valor_pesos
                  // Usar los datos de la vista cuando esté disponible
                  return "SELECT p.descripcion_es, p.descripcion_en, p.unidad_medida, " +
                         "COALESCE(vpp.precio_venta_neto, 0.0) as precio_venta_neto, " +
                         "COALESCE(vpp.precio_venta_neto, 0.0) as precio_venta_neto_dolares " +
                         "FROM producto p " +
                         "LEFT JOIN vista_producto_precio vpp ON " +
                         "  (p.descripcion_es = vpp.descripcion_es AND p.descripcion_en = vpp.descripcion_en) " +
                         "WHERE UPPER(p.descripcion_es) LIKE UPPER(?) OR UPPER(p.descripcion_en) LIKE UPPER(?) " +
                         "ORDER BY COALESCE(vpp.precio_venta_neto, 0.0) DESC " +
                         "LIMIT 20";
              }
          }
          
      } catch (SQLException e) {
          logger.log(Level.WARNING, "⚠️ Error al construir consulta con vista, usando consulta simple", e);
      }
      
      // Fallback: consulta simple sin JOIN
      return "SELECT descripcion_es, descripcion_en, unidad_medida, valor_pesos as precio_venta_neto, " +
             "0.0 as precio_venta_neto_dolares " +
             "FROM producto " +
             "WHERE UPPER(descripcion_es) LIKE UPPER(?) OR UPPER(descripcion_en) LIKE UPPER(?) " +
             "ORDER BY valor_pesos DESC " +
             "LIMIT 20";
  }

  /**
   * Diagnostica la estructura de la vista vista_producto_precio
   */
  public void diagnosticarVistaProductoPrecio() {
      logger.info("🔍 Diagnosticando estructura de vista_producto_precio...");
      
      try (Connection connection = DBConnection.getConnection()) {
          // Primero verificar si la vista existe
          String checkVistaSql = "SELECT table_name FROM information_schema.views WHERE table_name = 'vista_producto_precio'";
          try (PreparedStatement checkStatement = connection.prepareStatement(checkVistaSql);
               ResultSet checkResult = checkStatement.executeQuery()) {
              
              if (checkResult.next()) {
                  logger.info("✅ Vista vista_producto_precio existe");
                  
                  // Obtener estructura de columnas
                  String columnsSql = "SELECT column_name, data_type FROM information_schema.columns " +
                                    "WHERE table_name = 'vista_producto_precio' ORDER BY ordinal_position";
                  try (PreparedStatement columnsStatement = connection.prepareStatement(columnsSql);
                       ResultSet columnsResult = columnsStatement.executeQuery()) {
                      
                      logger.info("📋 Columnas de vista_producto_precio:");
                      while (columnsResult.next()) {
                          String columnName = columnsResult.getString("column_name");
                          String dataType = columnsResult.getString("data_type");
                          logger.info("   → " + columnName + " (" + dataType + ")");
                      }
                  }
                  
                  // Obtener una muestra de datos
                  String sampleSql = "SELECT * FROM vista_producto_precio LIMIT 3";
                  try (PreparedStatement sampleStatement = connection.prepareStatement(sampleSql);
                       ResultSet sampleResult = sampleStatement.executeQuery()) {
                      
                      logger.info("📊 Muestra de datos de vista_producto_precio:");
                      int rowCount = 0;
                      while (sampleResult.next() && rowCount < 3) {
                          rowCount++;
                          StringBuilder row = new StringBuilder("   Fila " + rowCount + ": ");
                          
                          // Obtener metadatos para saber cuántas columnas hay
                          java.sql.ResultSetMetaData metaData = sampleResult.getMetaData();
                          for (int i = 1; i <= metaData.getColumnCount(); i++) {
                              String columnName = metaData.getColumnName(i);
                              String value = sampleResult.getString(i);
                              row.append(columnName).append("=").append(value).append(", ");
                          }
                          logger.info(row.toString());
                      }
                  }
                  
              } else {
                  logger.warning("❌ Vista vista_producto_precio no existe");
                  
                  // Listar todas las vistas disponibles
                  String allViewsSql = "SELECT table_name FROM information_schema.views";
                  try (PreparedStatement allViewsStatement = connection.prepareStatement(allViewsSql);
                       ResultSet allViewsResult = allViewsStatement.executeQuery()) {
                      
                      logger.info("📋 Vistas disponibles en la base de datos:");
                      while (allViewsResult.next()) {
                          logger.info("   → " + allViewsResult.getString("table_name"));
                      }
                  }
              }
              
          }
          
      } catch (SQLException e) {
          logger.log(Level.SEVERE, "❌ Error en diagnóstico de vista_producto_precio", e);
      }
  }

}