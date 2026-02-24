package cl.vss.cotizador.service;

import com.aspose.cells.*;
import cl.vss.cotizador.model.RowData;
import cl.vss.cotizador.model.FormatoColumna;
import cl.vss.cotizador.model.BrokerFormato;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class AsposeExcelService {

    private static final Logger logger = LogManager.getLogger(AsposeExcelService.class);

    private static AsposeExcelService instance;

    private Workbook workbookActual;
    private String rutaArchivoOriginal;

    private AsposeExcelService() {}

    public static synchronized AsposeExcelService getInstance() {
        if (instance == null) {
            instance = new AsposeExcelService();
        }
        return instance;
    }

    public boolean archivoTieneMacros(String ruta) {
        if (ruta == null) return false;
        String r = ruta.toLowerCase();
        return r.endsWith(".xlsm") || r.endsWith(".xlsb");
    }

    public Workbook cargarWorkbook(String ruta) throws Exception {
        logger.info("📂 Cargando archivo con Aspose: {}", ruta);

        workbookActual = new Workbook(ruta);
        rutaArchivoOriginal = ruta;

        if (workbookActual.getVbaProject() != null) {
            logger.info("✅ Macros VBA detectadas");
        }

        return workbookActual;
    }

    public Workbook getWorkbookActual() {
        return workbookActual;
    }

    public boolean hayWorkbookCargado() {
        return workbookActual != null;
    }

    public void limpiarWorkbook() {
        if (workbookActual != null) {
            try { workbookActual.dispose(); } catch (Exception ignored) {}
            workbookActual = null;
            rutaArchivoOriginal = null;
        }
    }

    // ============================================================
    // 🔵 LECTURA DE DATOS (esto estaba bien)
    // ============================================================
    public List<RowData> leerDatos(int sheetIndex, int headerRow, BrokerFormato formato) throws Exception {
        if (workbookActual == null) throw new IllegalStateException("No hay workbook cargado");

        List<RowData> datos = new ArrayList<>();
        Worksheet sheet = workbookActual.getWorksheets().get(sheetIndex);
        Cells cells = sheet.getCells();

        int headerRowIndex = headerRow - 1;   // ✔ convertir 1-index → 0-index
        int lastRow = cells.getMaxDataRow();

        for (int rowIdx = headerRowIndex + 1; rowIdx <= lastRow; rowIdx++) {
            RowData rowData = new RowData();
            boolean tieneContenido = false;

            for (FormatoColumna columna : formato.getColumnas()) {
                int colIdx = columna.getIndiceColumna();
                Cell cell = cells.get(rowIdx, colIdx);

                String valor = obtenerValorCelda(cell);
                if (valor != null && !valor.trim().isEmpty()) tieneContenido = true;

                rowData.set(columna.getCampoEstandar(), valor != null ? valor : "");
            }

            if (tieneContenido) datos.add(rowData);
        }

        return datos;
    }

    // ============================================================
    // 🟩 CLONAR HOJA COMPLETA
    // ============================================================
    public Worksheet clonarHoja(int sheetIndex) throws Exception {
        Workbook copiaWorkbook = new Workbook();
        copiaWorkbook.copy(workbookActual);

        this.workbookActual = copiaWorkbook;

        return workbookActual.getWorksheets().get(sheetIndex);
    }

    // ============================================================
    // 🟩 ESCRIBIR SOLO LOS PRODUCTOS (sin borrar nada)
    // ============================================================
    public void escribirDatosEnHojaClonada(
            Worksheet hoja,
            List<RowData> datos,
            int startRow,
            BrokerFormato formato
    ) throws Exception {

        Cells cells = hoja.getCells();
        int row = startRow;   // ✔ CORREGIDO (antes tenía -1)

        for (RowData rowData : datos) {
            for (FormatoColumna columna : formato.getColumnas()) {

                int colIdx = columna.getIndiceColumna();
                String valor = rowData.get(columna.getCampoEstandar());
                if (valor == null) valor = "";

                Cell cell = cells.get(row, colIdx);

                if (esNumerico(valor) && esColumnaNumérica(columna)) {
                    try {
                        double num = Double.parseDouble(valor.replace(",", "").replace("$", ""));
                        cell.putValue(num);
                    } catch (Exception ex) {
                        cell.putValue(valor);
                    }
                } else {
                    cell.putValue(valor);
                }
            }
            row++;
        }

        logger.info("✅ Datos escritos sin alterar estructura");
    }

    // ============================================================
    // 🔵 GUARDAR
    // ============================================================
    public void guardarWorkbook(String rutaDestino) throws Exception {
        if (workbookActual == null) throw new IllegalStateException("No hay workbook cargado");

        int formato = SaveFormat.XLSX;
        String r = rutaDestino.toLowerCase();

        if (r.endsWith(".xlsm")) formato = SaveFormat.XLSM;
        if (r.endsWith(".xlsb")) formato = SaveFormat.XLSB;
        if (r.endsWith(".xls")) formato = SaveFormat.EXCEL_97_TO_2003;

        workbookActual.save(rutaDestino, formato);
    }

    // ============================================================
    // 🔵 UTILIDADES
    // ============================================================
    private String obtenerValorCelda(Cell cell) {
        if (cell == null) return null;

        switch (cell.getType()) {
            case CellValueType.IS_STRING: return cell.getStringValue();
            case CellValueType.IS_NUMERIC: return String.valueOf(cell.getDoubleValue());
            case CellValueType.IS_BOOL: return String.valueOf(cell.getBoolValue());
            case CellValueType.IS_NULL: return "";
            default:
                try { return cell.getStringValue(); }
                catch (Exception e) { return ""; }
        }
    }

    private boolean esNumerico(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Double.parseDouble(str.replace(",", "").replace("$", ""));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean esColumnaNumérica(FormatoColumna columna) {
        String tipo = columna.getTipoDato();
        if (tipo == null) return false;
        return tipo.equalsIgnoreCase("DECIMAL")
                || tipo.equalsIgnoreCase("INTEGER")
                || tipo.equalsIgnoreCase("NUMERIC");
    }
}
