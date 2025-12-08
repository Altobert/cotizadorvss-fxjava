package cl.vss.cotizador.detector;

import cl.vss.cotizador.model.Cliente;
import cl.vss.cotizador.model.Cotizacion;
import cl.vss.cotizador.model.ItemCotizacion;
import org.apache.poi.ss.usermodel.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Detector especializado para archivos de ONE Sphere
 */
public class DetectorOneSphere extends DetectorBase {
    
    @Override
    public boolean puedeDetectar(Sheet sheet) {
        // Buscar indicadores específicos de ONE Sphere en las primeras 20 filas
        for (int i = 0; i < Math.min(20, sheet.getLastRowNum() + 1); i++) {
            Row fila = sheet.getRow(i);
            if (filaContiene(fila, "ONE SPHERE", "ONESPHERE", "ONE_SPHERE")) {
                return true;
            }
        }
        
        // También buscar por estructura típica de RFQ de navieras
        Cell cell = buscarCeldaConTexto(sheet, "RFQ", 15);
        if (cell != null) {
            // Verificar si tiene columnas típicas de provision
            for (int i = 0; i < Math.min(25, sheet.getLastRowNum() + 1); i++) {
                Row fila = sheet.getRow(i);
                if (filaContiene(fila, "Item No", "Material Code", "Material Description", "Requested Qty")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    @Override
    public List<Cotizacion> extraerCotizaciones(Sheet sheet, String archivoOrigen) {
        List<Cotizacion> cotizaciones = new ArrayList<>();
        
        // Buscar fila de encabezado
        int filaHeader = encontrarFilaHeader(sheet);
        if (filaHeader == -1) {
            return cotizaciones;
        }
        
        Row headerRow = sheet.getRow(filaHeader);
        
        // Mapear columnas
        int colItemNo = encontrarColumna(headerRow, "Item No", "Item", "#");
        int colMaterialCode = encontrarColumna(headerRow, "Material Code", "Code", "Código");
        int colDescription = encontrarColumna(headerRow, "Material Description", "Description", "Descripción");
        int colQty = encontrarColumna(headerRow, "Requested Qty", "Qty", "Quantity", "Cantidad");
        int colUOM = encontrarColumna(headerRow, "UOM", "Unit", "Unidad");
        int colUOMDesc = encontrarColumna(headerRow, "UOM Description");
        
        // Extraer nombre del barco
        String nombreBarco = extraerNombreBarco(sheet);
        
        // Leer items
        for (int i = filaHeader + 1; i <= Math.min(sheet.getLastRowNum(), filaHeader + 1000); i++) {
            Row fila = sheet.getRow(i);
            if (esFilaVacia(fila)) continue;
            
            ItemCotizacion item = new ItemCotizacion();
            item.setFilaOrigen(i + 1); // +1 para numeración humana
            
            // Extraer datos
            if (colItemNo >= 0) {
                item.setCodigo(obtenerValorCelda(fila.getCell(colItemNo)));
            }
            
            if (colMaterialCode >= 0) {
                String materialCode = obtenerValorCelda(fila.getCell(colMaterialCode));
                if (item.getCodigo() == null || item.getCodigo().isEmpty()) {
                    item.setCodigo(materialCode);
                }
            }
            
            if (colDescription >= 0) {
                item.setDescripcion(obtenerValorCelda(fila.getCell(colDescription)));
            }
            
            if (colQty >= 0) {
                Double qty = obtenerValorNumerico(fila.getCell(colQty));
                if (qty != null) {
                    item.setCantidad(qty.intValue());
                }
            }
            
            if (colUOM >= 0) {
                item.setUnidad(obtenerValorCelda(fila.getCell(colUOM)));
            }
            
            if (colUOMDesc >= 0) {
                String uomDesc = obtenerValorCelda(fila.getCell(colUOMDesc));
                if (item.getUnidad() == null || item.getUnidad().isEmpty()) {
                    item.setUnidad(uomDesc);
                }
            }
            
            // Validar que el item tenga datos mínimos
            if (item.getDescripcion() != null && !item.getDescripcion().isEmpty()) {
                // Crear cotización para este item
                Cotizacion cot = new Cotizacion();
                cot.setArchivoOrigen(archivoOrigen);
                cot.setHojaOrigen(sheet.getSheetName());
                cot.setFilaOrigen(i + 1);
                cot.setTipoDetector(getNombreDetector());
                
                if (nombreBarco != null) {
                    cot.setCliente(new Cliente(nombreBarco, ""));
                }
                
                cot.agregarItem(item);
                cotizaciones.add(cot);
            }
        }
        
        return cotizaciones;
    }
    
    private int encontrarFilaHeader(Sheet sheet) {
        for (int i = 0; i < Math.min(30, sheet.getLastRowNum() + 1); i++) {
            Row fila = sheet.getRow(i);
            if (filaContiene(fila, "Item No", "Material Code", "Material Description")) {
                return i;
            }
        }
        return -1;
    }
    
    private String extraerNombreBarco(Sheet sheet) {
        // Buscar en las primeras filas información del barco
        for (int i = 0; i < Math.min(15, sheet.getLastRowNum() + 1); i++) {
            Row fila = sheet.getRow(i);
            if (fila == null) continue;
            
            for (Cell cell : fila) {
                String valor = obtenerValorCelda(cell);
                if (valor.toUpperCase().contains("VESSEL") || 
                    valor.toUpperCase().contains("SHIP NAME") ||
                    valor.toUpperCase().contains("M/V")) {
                    // Buscar el valor en la celda siguiente o en la misma
                    if (valor.contains(":")) {
                        String[] partes = valor.split(":");
                        if (partes.length > 1) {
                            return partes[1].trim();
                        }
                    }
                    // Intentar celda siguiente
                    Cell nextCell = fila.getCell(cell.getColumnIndex() + 1);
                    if (nextCell != null) {
                        String nombreBarco = obtenerValorCelda(nextCell);
                        if (!nombreBarco.isEmpty()) {
                            return nombreBarco;
                        }
                    }
                }
            }
        }
        
        // Si no se encuentra, intentar extraer de nombre de archivo
        return null;
    }
    
    @Override
    public String getNombreDetector() {
        return "ONE_SPHERE";
    }
    
    @Override
    public int getPrioridad() {
        return 80; // Alta prioridad - formato muy específico
    }
}
