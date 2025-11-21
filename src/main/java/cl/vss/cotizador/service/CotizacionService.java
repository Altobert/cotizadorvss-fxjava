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

public class CotizacionService {
    
    private static final Logger logger = LoggingConfig.getLogger(CotizacionService.class);

    public List<ItemCotizacionExcel> leerItemsDesdeExcel(File archivo) {
        logger.info("📂 Iniciando lectura de archivo Excel: " + archivo.getAbsolutePath());
        List<ItemCotizacionExcel> items = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(archivo);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet hoja = workbook.getSheetAt(0);
            Iterator<Row> filas = hoja.iterator();

            logger.info("🔎 Explorando filas para detectar encabezado en archivo: " + archivo.getName());

            Row encabezado = null;
            while (filas.hasNext()) {
                Row fila = filas.next();
                for (Cell celda : fila) {
                    String valor = celda.toString().trim().toLowerCase();
                    logger.fine("Analizando celda: [" + valor + "]");
                    if (valor.contains("item") || valor.contains("item description") || valor.contains("unit of measure")) {
                        encabezado = fila;
                        logger.fine("Encabezado encontrado en fila: " + fila.getRowNum());
                        break;
                    }
                }
                if (encabezado != null) break;
            }

            if (encabezado == null) {
                logger.severe("❌ No se encontró fila de encabezado válida en archivo: " + archivo.getName());
                return items;
            }

            Map<String, Integer> columnas = detectarColumnas(encabezado);

            logger.info("✅ Encabezados detectados: " + columnas.size() + " columnas");
            columnas.forEach((k, v) -> logger.fine("→ " + k + " en columna " + v));

            if (!columnas.containsKey("codigo") || !columnas.containsKey("descripcion")) {
                logger.warning("⚠️ Encabezados clave faltantes: 'codigo' y/o 'descripcion' en archivo: " + archivo.getName());
                return items;
            }

            while (filas.hasNext()) {
                Row fila = filas.next();

                String codigo = obtenerTexto(fila, columnas.get("codigo"));
                logger.info("Leyendo item con código: " + codigo);
                String descripcion = obtenerTexto(fila, columnas.get("descripcion"));

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
                      double precio = resultSet.getDouble("valor_pesos");
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
                      // La vista vista_producto_precio tiene las mismas columnas que producto
                      // Hacer JOIN usando descripcion_es Y descripcion_en para mayor precisión
                      return "SELECT p.descripcion_es, p.descripcion_en, p.unidad_medida, p.valor_pesos, " +
                             "COALESCE(vpp.precio_venta_neto, 0.0) as precio_venta_neto_dolares " +
                             "FROM producto p " +
                             "LEFT JOIN vista_producto_precio vpp ON " +
                             "  (p.descripcion_es = vpp.descripcion_es AND p.descripcion_en = vpp.descripcion_en) " +
                             "WHERE UPPER(p.descripcion_es) LIKE UPPER(?) OR UPPER(p.descripcion_en) LIKE UPPER(?) " +
                             "ORDER BY p.valor_pesos DESC " +
                             "LIMIT 20";
                  }          }
          
      } catch (SQLException e) {
          logger.log(Level.WARNING, "⚠️ Error al construir consulta con vista, usando consulta simple", e);
      }
      
      // Fallback: consulta simple sin JOIN
      return "SELECT descripcion_es, descripcion_en, unidad_medida, valor_pesos, " +
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