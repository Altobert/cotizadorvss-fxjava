package cl.vss.cotizador.service;

import cl.vss.cotizador.model.ItemCotizacionExcel;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

public class CotizacionService {

    public List<ItemCotizacionExcel> leerItemsDesdeExcel(File archivo) {
        List<ItemCotizacionExcel> items = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(archivo);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet hoja = workbook.getSheetAt(0);
            Iterator<Row> filas = hoja.iterator();

            System.out.println("🔎 Explorando filas para detectar encabezado...");

            Row encabezado = null;
            while (filas.hasNext()) {
                Row fila = filas.next();
                for (Cell celda : fila) {
                    String valor = celda.toString().trim().toLowerCase();
                    System.out.print("[" + valor + "] ");
                    if (valor.contains("item") || valor.contains("item description") || valor.contains("unit of measure")) {
                        encabezado = fila;
                        break;
                    }
                }
                System.out.println();
                if (encabezado != null) break;
            }

            if (encabezado == null) {
                System.err.println("❌ No se encontró fila de encabezado válida.");
                return items;
            }

            Map<String, Integer> columnas = detectarColumnas(encabezado);

            System.out.println("✅ Encabezados detectados:");
            columnas.forEach((k, v) -> System.out.println("→ " + k + " en columna " + v));

            if (!columnas.containsKey("codigo") || !columnas.containsKey("descripcion")) {
                System.err.println("⚠️ Encabezados clave faltantes: 'codigo' y/o 'descripcion'");
                return items;
            }

            while (filas.hasNext()) {
                Row fila = filas.next();

                String codigo = obtenerTexto(fila, columnas.get("codigo"));
                String descripcion = obtenerTexto(fila, columnas.get("descripcion"));
                int cantidad = obtenerEntero(fila, columnas.get("cantidad"));
                double precio = obtenerDecimal(fila, columnas.get("precio"));
                String unidad = columnas.containsKey("unidad") ? obtenerTexto(fila, columnas.get("unidad")) : "";
                String categoria = columnas.containsKey("categoria") ? obtenerTexto(fila, columnas.get("categoria")) : "";

                double descuento = columnas.containsKey("descuento") ? obtenerDecimal(fila, columnas.get("descuento")) : 0.0;
                double totalNeto = columnas.containsKey("totalneto") ? obtenerDecimal(fila, columnas.get("totalneto")) : 0.0;
                String comentarios = columnas.containsKey("comentarios") ? obtenerTexto(fila, columnas.get("comentarios")) : "";
                int disponibilidad = columnas.containsKey("disponibilidad") ? obtenerEntero(fila, columnas.get("disponibilidad")) : 0;
                double totalBruto = columnas.containsKey("totalbruto") ? obtenerDecimal(fila, columnas.get("totalbruto")) : 0.0;

                if (codigo.isEmpty() && descripcion.isEmpty()) continue;

                ItemCotizacionExcel item = new ItemCotizacionExcel(
                    codigo, descripcion, cantidad, precio, unidad, categoria,
                    descuento, totalNeto, comentarios, disponibilidad, totalBruto
                );
                items.add(item);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return items;
    }

    public boolean exportarItemsAExcel(List<ItemCotizacionExcel> items, File archivo) {
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

            return true;
        } catch (Exception e) {
            e.printStackTrace();
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

    private Map<String, Integer> detectarColumnas(Row encabezado) {
        Map<String, Integer> mapa = new HashMap<>();
        Map<String, List<String>> sinonimos = new HashMap<>();
        sinonimos.put("codigo", List.of("item code", "code", "item", "codigo", "código"));
        sinonimos.put("descripcion", List.of("description", "desc", "item description", "descripción"));
        sinonimos.put("cantidad", List.of("quantity", "qty", "cantidad", "quantity order"));
        sinonimos.put("precio", List.of("price", "unit price", "precio", "unit cost", "precio unitario"));
        sinonimos.put("unidad", List.of("unit", "unit of measure", "unidad"));
        sinonimos.put("categoria", List.of("category", "food categories", "categoría"));
        sinonimos.put("descuento", List.of("discount", "descuento"));
        sinonimos.put("totalneto", List.of("total net", "net total", "totalneto"));
        sinonimos.put("comentarios", List.of("comments", "supplier comments", "comentarios"));
        sinonimos.put("disponibilidad", List.of("availability", "disponibilidad"));
        sinonimos.put("totalbruto", List.of("total", "gross total", "total bruto"));

        for (Cell celda : encabezado) {
            String valor = celda.toString().trim().toLowerCase().replaceAll("[^a-z0-9 ]", "");

            for (Map.Entry<String, List<String>> entry : sinonimos.entrySet()) {
                for (String alias : entry.getValue()) {
                    String normalizado = alias.toLowerCase().replaceAll("[^a-z0-9 ]", "");
                    if (valor.contains(normalizado)) {
                        mapa.put(entry.getKey(), celda.getColumnIndex());
                    }
                }
            }
        }

        return mapa;
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
            System.err.println("Error leyendo cantidad: " + e.getMessage());
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
        System.err.println("Error leyendo decimal: " + e.getMessage());
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

}