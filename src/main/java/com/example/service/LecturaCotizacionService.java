package com.example.service;

import com.example.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para leer cotizaciones desde archivos Excel
 * Soporta formatos .xlsx y .xls
 */
public class LecturaCotizacionService {
    
    private static LecturaCotizacionService instance;
    
    // Constructor privado para Singleton
    private LecturaCotizacionService() {}
    
    // Obtener instancia singleton
    public static synchronized LecturaCotizacionService getInstance() {
        if (instance == null) {
            instance = new LecturaCotizacionService();
        }
        return instance;
    }
    
    /**
     * Leer cotizaciones desde un archivo Excel
     */
    public List<Cotizacion> leerCotizacionesDesdeExcel(String rutaArchivo) throws IOException {
        List<Cotizacion> cotizaciones = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(rutaArchivo)) {
            Workbook workbook = null;
            
            // Determinar el tipo de archivo
            if (rutaArchivo.toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else if (rutaArchivo.toLowerCase().endsWith(".xls")) {
                workbook = new HSSFWorkbook(fis);
            } else {
                throw new IllegalArgumentException("Formato de archivo no soportado. Use .xlsx o .xls");
            }
            
            // Leer cada hoja del archivo
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                List<Cotizacion> cotizacionesHoja = leerCotizacionesDeHoja(sheet);
                cotizaciones.addAll(cotizacionesHoja);
            }
            
            workbook.close();
        }
        
        return cotizaciones;
    }
    
    /**
     * Leer cotizaciones de una hoja específica
     */
    private List<Cotizacion> leerCotizacionesDeHoja(Sheet sheet) {
        List<Cotizacion> cotizaciones = new ArrayList<>();
        
        // Buscar la estructura de datos en la hoja
        int filaInicio = encontrarFilaInicio(sheet);
        if (filaInicio == -1) {
            return cotizaciones; // No se encontró estructura válida
        }
        
        // Leer encabezados
        Row filaEncabezados = sheet.getRow(filaInicio);
        List<String> encabezados = leerEncabezados(filaEncabezados);
        
        // Leer datos de cotizaciones (limitar a 1000 filas para evitar problemas de memoria)
        int limiteFilas = Math.min(sheet.getLastRowNum(), filaInicio + 1000);
        for (int i = filaInicio + 1; i <= limiteFilas; i++) {
            Row fila = sheet.getRow(i);
            if (fila == null) continue;
            
            Cotizacion cotizacion = crearCotizacionDesdeFila(fila, encabezados);
            if (cotizacion != null) {
                cotizaciones.add(cotizacion);
            }
        }
        
        return cotizaciones;
    }
    
    /**
     * Encontrar la fila donde comienzan los datos
     */
    private int encontrarFilaInicio(Sheet sheet) {
        // Buscar patrones específicos de los archivos de cotizaciones
        for (int i = 0; i <= Math.min(10, sheet.getLastRowNum()); i++) {
            Row fila = sheet.getRow(i);
            if (fila == null) continue;
            
            // Buscar palabras clave que indiquen el inicio de datos
            for (Cell cell : fila) {
                String valor = obtenerValorCelda(cell);
                if (valor != null) {
                    String valorLower = valor.toLowerCase();
                    if (valorLower.contains("item no") ||
                        valorLower.contains("material code") ||
                        valorLower.contains("material description") ||
                        valorLower.contains("requested qty") ||
                        valorLower.contains("unit price") ||
                        valorLower.contains("número") ||
                        valorLower.contains("numero") ||
                        valorLower.contains("cliente") ||
                        valorLower.contains("fecha") ||
                        valorLower.contains("total") ||
                        valorLower.contains("cot") ||
                        valorLower.contains("coffee") ||
                        valorLower.contains("provisions")) {
                        return i;
                    }
                }
            }
        }
        
        // Si no se encuentra por palabras clave, buscar la primera fila con datos numéricos en la primera columna
        for (int i = 0; i <= Math.min(5, sheet.getLastRowNum()); i++) {
            Row fila = sheet.getRow(i);
            if (fila == null) continue;
            
            Cell primeraCelda = fila.getCell(0);
            if (primeraCelda != null && primeraCelda.getCellType() == CellType.NUMERIC) {
                double valor = primeraCelda.getNumericCellValue();
                if (valor == 1.0) { // Típicamente las listas empiezan con 1
                    return i;
                }
            }
        }
        
        // Fallback: asumir que la primera fila tiene los encabezados
        if (sheet.getLastRowNum() >= 0) {
            return 0;
        }
        
        return -1;
    }
    
    /**
     * Leer encabezados de una fila
     */
    private List<String> leerEncabezados(Row fila) {
        List<String> encabezados = new ArrayList<>();
        for (Cell cell : fila) {
            encabezados.add(obtenerValorCelda(cell));
        }
        return encabezados;
    }
    
    /**
     * Crear cotización desde una fila de datos
     */
    private Cotizacion crearCotizacionDesdeFila(Row fila, List<String> encabezados) {
        try {
            Cotizacion cotizacion = new Cotizacion();
            
            // Mapear columnas por nombre
            for (int i = 0; i < encabezados.size() && i < fila.getLastCellNum(); i++) {
                String encabezado = encabezados.get(i).toLowerCase();
                Cell celda = fila.getCell(i);
                String valor = obtenerValorCelda(celda);
                
                if (valor == null || valor.trim().isEmpty()) continue;
                
                switch (encabezado) {
                    case "número":
                    case "numero":
                    case "número de cotización":
                    case "numero de cotizacion":
                    case "item no":
                        cotizacion.setNumeroCotizacion(valor);
                        break;
                        
                    case "cliente":
                    case "nombre cliente":
                    case "empresa":
                    case "material code":
                    case "cot":
                        // Crear cliente básico
                        Cliente cliente = new Cliente(valor, "");
                        cotizacion.setCliente(cliente);
                        break;
                        
                    case "fecha":
                    case "fecha creación":
                    case "fecha creacion":
                        try {
                            if (celda.getCellType() == CellType.NUMERIC) {
                                LocalDateTime fecha = celda.getDateCellValue()
                                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                                cotizacion.setFechaCreacion(fecha);
                            }
                        } catch (Exception e) {
                            // Ignorar errores de fecha
                        }
                        break;
                        
                    case "estado":
                    case "status":
                        cotizacion.setEstado(valor);
                        break;
                        
                    case "subtotal":
                    case "requested qty":
                        try {
                            double subtotal = Double.parseDouble(valor.replaceAll("[^\\d.,]", ""));
                            cotizacion.setSubtotal(subtotal);
                        } catch (NumberFormatException e) {
                            // Ignorar errores de número
                        }
                        break;
                        
                    case "descuento":
                    case "discount(%)":
                    case "extra discount(%)":
                        try {
                            double descuento = Double.parseDouble(valor.replaceAll("[^\\d.,]", ""));
                            cotizacion.setDescuento(descuento);
                        } catch (NumberFormatException e) {
                            // Ignorar errores de número
                        }
                        break;
                        
                    case "impuestos":
                    case "iva":
                        try {
                            double impuestos = Double.parseDouble(valor.replaceAll("[^\\d.,]", ""));
                            cotizacion.setImpuestos(impuestos);
                        } catch (NumberFormatException e) {
                            // Ignorar errores de número
                        }
                        break;
                        
                    case "total":
                    case "unit price":
                        try {
                            double total = Double.parseDouble(valor.replaceAll("[^\\d.,]", ""));
                            cotizacion.setTotal(total);
                        } catch (NumberFormatException e) {
                            // Ignorar errores de número
                        }
                        break;
                        
                    case "notas":
                    case "observaciones":
                    case "material description":
                        cotizacion.setNotas(valor);
                        break;
                        
                    case "condiciones":
                    case "términos":
                    case "terminos":
                    case "uom":
                    case "uom description":
                        cotizacion.setCondiciones(valor);
                        break;
                }
            }
            
            // Validar que la cotizacion tenga datos mínimos
            if (cotizacion.getNumeroCotizacion() != null && !cotizacion.getNumeroCotizacion().trim().isEmpty()) {
                return cotizacion;
            }
            
            // También aceptar si tiene cliente válido
            if (cotizacion.getCliente() != null && 
                cotizacion.getCliente().getNombre() != null && 
                !cotizacion.getCliente().getNombre().trim().isEmpty()) {
                return cotizacion;
            }
            
            // Para archivos de productos, aceptar si tiene descripción válida
            if (cotizacion.getNotas() != null && !cotizacion.getNotas().trim().isEmpty()) {
                return cotizacion;
            }
            
            // Para archivos con precios, aceptar si tiene total válido
            if (cotizacion.getTotal() > 0) {
                return cotizacion;
            }
            
        } catch (Exception e) {
            // Ignorar errores en filas individuales
        }
        
        return null;
    }
    
    /**
     * Obtener valor de una celda como String
     */
    private String obtenerValorCelda(Cell cell) {
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
    
    /**
     * Leer items de cotización desde una hoja separada
     */
    public List<ItemCotizacion> leerItemsDesdeHoja(Sheet sheet, String numeroCotizacion) {
        List<ItemCotizacion> items = new ArrayList<>();
        
        int filaInicio = encontrarFilaInicioItems(sheet);
        if (filaInicio == -1) return items;
        
        Row filaEncabezados = sheet.getRow(filaInicio);
        List<String> encabezados = leerEncabezados(filaEncabezados);
        
        for (int i = filaInicio + 1; i <= sheet.getLastRowNum(); i++) {
            Row fila = sheet.getRow(i);
            if (fila == null) continue;
            
            ItemCotizacion item = crearItemDesdeFila(fila, encabezados);
            if (item != null) {
                items.add(item);
            }
        }
        
        return items;
    }
    
    /**
     * Encontrar fila de inicio para items
     */
    private int encontrarFilaInicioItems(Sheet sheet) {
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row fila = sheet.getRow(i);
            if (fila == null) continue;
            
            for (Cell cell : fila) {
                String valor = obtenerValorCelda(cell);
                if (valor != null && (
                    valor.toLowerCase().contains("código") ||
                    valor.toLowerCase().contains("codigo") ||
                    valor.toLowerCase().contains("descripción") ||
                    valor.toLowerCase().contains("descripcion") ||
                    valor.toLowerCase().contains("cantidad") ||
                    valor.toLowerCase().contains("precio")
                )) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    /**
     * Crear item desde una fila
     */
    private ItemCotizacion crearItemDesdeFila(Row fila, List<String> encabezados) {
        try {
            ItemCotizacion item = new ItemCotizacion();
            
            for (int i = 0; i < encabezados.size() && i < fila.getLastCellNum(); i++) {
                String encabezado = encabezados.get(i).toLowerCase();
                Cell celda = fila.getCell(i);
                String valor = obtenerValorCelda(celda);
                
                if (valor == null || valor.trim().isEmpty()) continue;
                
                switch (encabezado) {
                    case "código":
                    case "codigo":
                    case "sku":
                        item.setCodigo(valor);
                        break;
                        
                    case "descripción":
                    case "descripcion":
                    case "producto":
                        item.setDescripcion(valor);
                        break;
                        
                    case "cantidad":
                    case "qty":
                        try {
                            int cantidad = (int) Double.parseDouble(valor);
                            item.setCantidad(cantidad);
                        } catch (NumberFormatException e) {
                            item.setCantidad(1);
                        }
                        break;
                        
                    case "precio":
                    case "precio unitario":
                    case "precio unit":
                        try {
                            double precio = Double.parseDouble(valor.replaceAll("[^\\d.,]", ""));
                            item.setPrecioUnitario(precio);
                        } catch (NumberFormatException e) {
                            // Ignorar errores
                        }
                        break;
                        
                    case "categoría":
                    case "categoria":
                    case "tipo":
                        item.setCategoria(valor);
                        break;
                        
                    case "unidad":
                    case "uom":
                        item.setUnidad(valor);
                        break;
                }
            }
            
            // Validar que el item tenga datos mínimos
            if (item.getDescripcion() != null && item.getPrecioUnitario() > 0) {
                return item;
            }
            
        } catch (Exception e) {
            // Ignorar errores
        }
        
        return null;
    }
}
