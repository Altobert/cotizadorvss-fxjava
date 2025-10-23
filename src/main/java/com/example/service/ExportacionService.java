package com.example.service;

import com.example.model.Cotizacion;
import com.example.model.ItemCotizacion;
import com.example.model.Cliente;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio para exportar cotizaciones a diferentes formatos
 * Utiliza Apache POI para generar archivos Excel
 */
public class ExportacionService {
    
    private static ExportacionService instance;
    
    // Constructor privado para Singleton
    private ExportacionService() {}
    
    // Obtener instancia singleton
    public static synchronized ExportacionService getInstance() {
        if (instance == null) {
            instance = new ExportacionService();
        }
        return instance;
    }
    
    /**
     * Exportar cotización a Excel
     */
    public String exportarCotizacionAExcel(Cotizacion cotizacion, String rutaArchivo) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Cotización");
        
        // Crear estilos
        CellStyle headerStyle = crearEstiloEncabezado(workbook);
        CellStyle titleStyle = crearEstiloTitulo(workbook);
        CellStyle dataStyle = crearEstiloDatos(workbook);
        CellStyle currencyStyle = crearEstiloMoneda(workbook);
        
        int rowNum = 0;
        
        // Encabezado de la empresa
        Row headerRow = sheet.createRow(rowNum++);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue("COTIZACIÓN");
        headerCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        
        // Información de la cotización
        rowNum++;
        Row infoRow1 = sheet.createRow(rowNum++);
        infoRow1.createCell(0).setCellValue("Número:");
        infoRow1.createCell(1).setCellValue(cotizacion.getNumeroCotizacion());
        infoRow1.createCell(3).setCellValue("Fecha:");
        infoRow1.createCell(4).setCellValue(cotizacion.getFechaCreacion()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        
        Row infoRow2 = sheet.createRow(rowNum++);
        infoRow2.createCell(0).setCellValue("Estado:");
        infoRow2.createCell(1).setCellValue(cotizacion.getEstado());
        infoRow2.createCell(3).setCellValue("Válida hasta:");
        if (cotizacion.getFechaVencimiento() != null) {
            infoRow2.createCell(4).setCellValue(cotizacion.getFechaVencimiento()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        
        // Información del cliente
        rowNum++;
        Row clienteHeaderRow = sheet.createRow(rowNum++);
        clienteHeaderRow.createCell(0).setCellValue("CLIENTE");
        clienteHeaderRow.getCell(0).setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 5));
        
        if (cotizacion.getCliente() != null) {
            Cliente cliente = cotizacion.getCliente();
            Row clienteRow1 = sheet.createRow(rowNum++);
            clienteRow1.createCell(0).setCellValue("Nombre:");
            clienteRow1.createCell(1).setCellValue(cliente.getNombre());
            clienteRow1.createCell(3).setCellValue("Email:");
            clienteRow1.createCell(4).setCellValue(cliente.getEmail());
            
            Row clienteRow2 = sheet.createRow(rowNum++);
            clienteRow2.createCell(0).setCellValue("Teléfono:");
            clienteRow2.createCell(1).setCellValue(cliente.getTelefono());
            clienteRow2.createCell(3).setCellValue("Dirección:");
            clienteRow2.createCell(4).setCellValue(cliente.getDireccionCompleta());
        }
        
        // Tabla de items
        rowNum++;
        Row itemsHeaderRow = sheet.createRow(rowNum++);
        itemsHeaderRow.createCell(0).setCellValue("ITEMS DE LA COTIZACIÓN");
        itemsHeaderRow.getCell(0).setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 5));
        
        // Encabezados de la tabla
        Row tableHeaderRow = sheet.createRow(rowNum++);
        String[] headers = {"Código", "Descripción", "Cantidad", "Precio Unit.", "Descuento", "Subtotal"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = tableHeaderRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Datos de los items
        for (ItemCotizacion item : cotizacion.getItems()) {
            Row itemRow = sheet.createRow(rowNum++);
            
            itemRow.createCell(0).setCellValue(item.getCodigo() != null ? item.getCodigo() : "");
            itemRow.createCell(1).setCellValue(item.getDescripcion());
            
            Cell cantidadCell = itemRow.createCell(2);
            cantidadCell.setCellValue(item.getCantidad());
            cantidadCell.setCellStyle(dataStyle);
            
            Cell precioCell = itemRow.createCell(3);
            precioCell.setCellValue(item.getPrecioUnitario());
            precioCell.setCellStyle(currencyStyle);
            
            Cell descuentoCell = itemRow.createCell(4);
            descuentoCell.setCellValue(item.getDescuentoTotal());
            descuentoCell.setCellStyle(currencyStyle);
            
            Cell subtotalCell = itemRow.createCell(5);
            subtotalCell.setCellValue(item.getSubtotal());
            subtotalCell.setCellStyle(currencyStyle);
        }
        
        // Totales
        rowNum++;
        Row subtotalRow = sheet.createRow(rowNum++);
        subtotalRow.createCell(4).setCellValue("Subtotal:");
        Cell subtotalCell = subtotalRow.createCell(5);
        subtotalCell.setCellValue(cotizacion.getSubtotal());
        subtotalCell.setCellStyle(currencyStyle);
        
        Row descuentoRow = sheet.createRow(rowNum++);
        descuentoRow.createCell(4).setCellValue("Descuento:");
        Cell descuentoCell = descuentoRow.createCell(5);
        descuentoCell.setCellValue(cotizacion.getDescuento());
        descuentoCell.setCellStyle(currencyStyle);
        
        Row impuestosRow = sheet.createRow(rowNum++);
        impuestosRow.createCell(4).setCellValue("Impuestos:");
        Cell impuestosCell = impuestosRow.createCell(5);
        impuestosCell.setCellValue(cotizacion.getImpuestos());
        impuestosCell.setCellStyle(currencyStyle);
        
        Row totalRow = sheet.createRow(rowNum++);
        totalRow.createCell(4).setCellValue("TOTAL:");
        Cell totalCell = totalRow.createCell(5);
        totalCell.setCellValue(cotizacion.getTotal());
        totalCell.setCellStyle(currencyStyle);
        
        // Notas y condiciones
        if (cotizacion.getNotas() != null && !cotizacion.getNotas().isEmpty()) {
            rowNum++;
            Row notasRow = sheet.createRow(rowNum++);
            notasRow.createCell(0).setCellValue("NOTAS:");
            notasRow.getCell(0).setCellStyle(headerStyle);
            
            Row notasContentRow = sheet.createRow(rowNum++);
            notasContentRow.createCell(0).setCellValue(cotizacion.getNotas());
            sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 5));
        }
        
        if (cotizacion.getCondiciones() != null && !cotizacion.getCondiciones().isEmpty()) {
            rowNum++;
            Row condicionesRow = sheet.createRow(rowNum++);
            condicionesRow.createCell(0).setCellValue("CONDICIONES:");
            condicionesRow.getCell(0).setCellStyle(headerStyle);
            
            Row condicionesContentRow = sheet.createRow(rowNum++);
            condicionesContentRow.createCell(0).setCellValue(cotizacion.getCondiciones());
            sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 5));
        }
        
        // Ajustar ancho de columnas
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
        
        // Guardar archivo
        try (FileOutputStream fileOut = new FileOutputStream(rutaArchivo)) {
            workbook.write(fileOut);
        }
        
        workbook.close();
        return rutaArchivo;
    }
    
    /**
     * Exportar múltiples cotizaciones a Excel
     */
    public String exportarCotizacionesAExcel(List<Cotizacion> cotizaciones, String rutaArchivo) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Cotizaciones");
        
        CellStyle headerStyle = crearEstiloEncabezado(workbook);
        CellStyle dataStyle = crearEstiloDatos(workbook);
        CellStyle currencyStyle = crearEstiloMoneda(workbook);
        
        int rowNum = 0;
        
        // Encabezados
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Número", "Cliente", "Fecha", "Estado", "Subtotal", "Descuento", "Impuestos", "Total"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Datos
        for (Cotizacion cotizacion : cotizaciones) {
            Row dataRow = sheet.createRow(rowNum++);
            
            dataRow.createCell(0).setCellValue(cotizacion.getNumeroCotizacion());
            dataRow.createCell(1).setCellValue(cotizacion.getCliente() != null ? 
                cotizacion.getCliente().getNombre() : "Sin cliente");
            dataRow.createCell(2).setCellValue(cotizacion.getFechaCreacion()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            dataRow.createCell(3).setCellValue(cotizacion.getEstado());
            
            Cell subtotalCell = dataRow.createCell(4);
            subtotalCell.setCellValue(cotizacion.getSubtotal());
            subtotalCell.setCellStyle(currencyStyle);
            
            Cell descuentoCell = dataRow.createCell(5);
            descuentoCell.setCellValue(cotizacion.getDescuento());
            descuentoCell.setCellStyle(currencyStyle);
            
            Cell impuestosCell = dataRow.createCell(6);
            impuestosCell.setCellValue(cotizacion.getImpuestos());
            impuestosCell.setCellStyle(currencyStyle);
            
            Cell totalCell = dataRow.createCell(7);
            totalCell.setCellValue(cotizacion.getTotal());
            totalCell.setCellStyle(currencyStyle);
        }
        
        // Ajustar ancho de columnas
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        // Guardar archivo
        try (FileOutputStream fileOut = new FileOutputStream(rutaArchivo)) {
            workbook.write(fileOut);
        }
        
        workbook.close();
        return rutaArchivo;
    }
    
    // Métodos para crear estilos
    private CellStyle crearEstiloEncabezado(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
    
    private CellStyle crearEstiloTitulo(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
    
    private CellStyle crearEstiloDatos(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
    
    private CellStyle crearEstiloMoneda(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("$#,##0.00"));
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
}
