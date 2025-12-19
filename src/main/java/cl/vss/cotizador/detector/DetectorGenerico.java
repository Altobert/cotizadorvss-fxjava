package cl.vss.cotizador.detector;

import cl.vss.cotizador.model.Cliente;
import cl.vss.cotizador.model.Cotizacion;
import cl.vss.cotizador.model.ItemCotizacion;
import org.apache.poi.ss.usermodel.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Detector genérico que intenta leer cualquier formato de cotización
 * Tiene baja prioridad y se usa cuando otros detectores no funcionan
 */
public class DetectorGenerico extends DetectorBase {
    
    @Override
    public boolean puedeDetectar(Sheet sheet) {
        // Este detector siempre puede intentar procesar cualquier hoja
        // pero tiene baja prioridad
        return sheet != null && sheet.getLastRowNum() > 0;
    }
    
    @Override
    public List<Cotizacion> extraerCotizaciones(Sheet sheet, String archivoOrigen) {
        List<Cotizacion> cotizaciones = new ArrayList<>();
        
        // Intentar encontrar una fila de encabezado
        int filaHeader = encontrarFilaHeader(sheet);
        if (filaHeader == -1) {
            return cotizaciones;
        }
        
        Row headerRow = sheet.getRow(filaHeader);
        
        // Mapear columnas con nombres comunes
        int colCodigo = encontrarColumna(headerRow, 
            "código", "codigo", "code", "sku", "item", "no", "número", "numero", "#");
        int colDescripcion = encontrarColumna(headerRow, 
            "descripción", "descripcion", "description", "producto", "product", "material");
        int colCantidad = encontrarColumna(headerRow, 
            "cantidad", "qty", "quantity", "requested", "cant");
        int colPrecio = encontrarColumna(headerRow, 
            "precio", "price", "unit price", "precio unitario", "valor");
        int colUnidad = encontrarColumna(headerRow, 
            "unidad", "unit", "uom", "u/m");
        int colTotal = encontrarColumna(headerRow, 
            "total", "amount", "subtotal");
        
        // Leer datos
        for (int i = filaHeader + 1; i <= Math.min(sheet.getLastRowNum(), filaHeader + 1000); i++) {
            Row fila = sheet.getRow(i);
            if (esFilaVacia(fila)) continue;
            
            ItemCotizacion item = new ItemCotizacion();
            item.setFilaOrigen(i + 1);
            
            // Extraer datos
            if (colCodigo >= 0) {
                item.setCodigo(obtenerValorCelda(fila.getCell(colCodigo)));
            }
            
            if (colDescripcion >= 0) {
                item.setDescripcion(obtenerValorCelda(fila.getCell(colDescripcion)));
            }
            
            if (colCantidad >= 0) {
                Double qty = obtenerValorNumerico(fila.getCell(colCantidad));
                if (qty != null) {
                    item.setCantidad(qty.intValue());
                }
            }
            
            if (colPrecio >= 0) {
                Double precio = obtenerValorNumerico(fila.getCell(colPrecio));
                if (precio != null) {
                    item.setPrecioUnitario(precio);
                }
            }
            
            if (colUnidad >= 0) {
                item.setUnidad(obtenerValorCelda(fila.getCell(colUnidad)));
            }
            
            // Validar datos mínimos
            if ((item.getDescripcion() != null && !item.getDescripcion().isEmpty()) ||
                (item.getCodigo() != null && !item.getCodigo().isEmpty())) {
                
                Cotizacion cot = new Cotizacion();
                cot.setArchivoOrigen(archivoOrigen);
                cot.setHojaOrigen(sheet.getSheetName());
                cot.setFilaOrigen(i + 1);
                cot.setTipoDetector(getNombreDetector());
                
                cot.agregarItem(item);
                cotizaciones.add(cot);
            }
        }
        
        return cotizaciones;
    }
    
    private int encontrarFilaHeader(Sheet sheet) {
        // Buscar palabras clave típicas en headers
        String[] keywords = {
            "descripción", "descripcion", "description", 
            "código", "codigo", "code",
            "cantidad", "qty", "quantity",
            "precio", "price",
            "item", "producto", "product"
        };
        
        for (int i = 0; i < Math.min(30, sheet.getLastRowNum() + 1); i++) {
            Row fila = sheet.getRow(i);
            if (fila == null) continue;
            
            int matchCount = 0;
            for (Cell cell : fila) {
                String valor = obtenerValorCelda(cell).toLowerCase();
                for (String keyword : keywords) {
                    if (valor.contains(keyword)) {
                        matchCount++;
                        break;
                    }
                }
            }
            
            // Si encontramos al menos 2 keywords típicos, probablemente es el header
            if (matchCount >= 2) {
                return i;
            }
        }
        
        // Si no se encuentra header, intentar usar la primera fila
        if (sheet.getLastRowNum() > 0) {
            return 0;
        }
        
        return -1;
    }
    
    @Override
    public String getNombreDetector() {
        return "GENERICO";
    }
    
    @Override
    public int getPrioridad() {
        return 10; // Baja prioridad - es el fallback
    }
}
