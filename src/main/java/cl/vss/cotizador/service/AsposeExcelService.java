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

    // ============================================================
    // 📂 CARGA Y GESTIÓN DEL WORKBOOK
    // ============================================================
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
    // 📘 LECTURA DE DATOS DESDE LA PLANILLA
    // ============================================================
    public List<RowData> leerDatos(int sheetIndex, int headerRow, BrokerFormato formato) throws Exception {
        if (workbookActual == null) throw new IllegalStateException("No hay workbook cargado");

        List<RowData> datos = new ArrayList<>();
        Worksheet sheet = workbookActual.getWorksheets().get(sheetIndex);
        Cells cells = sheet.getCells();

        int headerRowIndex = headerRow - 1;
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
    // 📄 CLONAR HOJA COMPLETA (PRESERVA TODO)
    // ============================================================
    public Worksheet clonarHoja(int sheetIndex) throws Exception {
        Workbook copiaWorkbook = new Workbook();
        copiaWorkbook.copy(workbookActual);

        this.workbookActual = copiaWorkbook;

        return workbookActual.getWorksheets().get(sheetIndex);
    }

    // ============================================================
    // ✏️ ESCRIBIR SOLO UNIT PRICE Y REMARKS (GOTA DE AGUA)
    // ============================================================
       public void escribirUnitPriceYRemarks(
        List<RowData> datos,
        BrokerFormato formato
) throws Exception {

    if (workbookActual == null) {
        throw new IllegalStateException("No hay workbook cargado");
    }

    Worksheet sheet = workbookActual.getWorksheets().get(0);
    Cells cells = sheet.getCells();

    // ============================
    // COLUMNAS UNIT PRICE
    // ============================
    int colUnitPrice = formato.getColumnas().stream()
            .filter(c -> "UNIT_PRICE".equalsIgnoreCase(c.getCampoEstandar()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("UNIT_PRICE no encontrado"))
            .getIndiceColumna();

    // ============================
    // COLUMNA REMARKS (dinámica)
    // ============================
    FormatoColumna colRemarkObj = formato.getColumnas().stream()
            .filter(c ->
                    "VENDOR_REMARKS".equalsIgnoreCase(c.getCampoEstandar()) ||
                    "NOTES".equalsIgnoreCase(c.getCampoEstandar()) ||
                    "SUPPLIER_COMMENTS".equalsIgnoreCase(c.getCampoEstandar()) ||
                    "OFFICE_REMARKS".equalsIgnoreCase(c.getCampoEstandar())
            )
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No se encontró columna de remarks"));

    int colRemark = colRemarkObj.getIndiceColumna();
    String campoRemark = colRemarkObj.getCampoEstandar();

    // ============================
    // COLUMNA PART NUMBER (con soporte CMA CGM)
    // ============================
    FormatoColumna colPartObj;

    if (formato.getFormatoId() == 3) { // CMA CGM
        colPartObj = formato.getColumnas().stream()
                .filter(c -> "DESCRIPTION".equalsIgnoreCase(c.getCampoEstandar()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No se encontró DESCRIPTION para CMA CGM"));
    } else {
        colPartObj = formato.getColumnas().stream()
                .filter(c ->
                        c.getCampoEstandar().toUpperCase().contains("ITEM") ||
                        c.getCampoEstandar().toUpperCase().contains("PART") ||
                        c.getCampoEstandar().toUpperCase().contains("PRODUCT") ||
                        c.getCampoEstandar().toUpperCase().contains("CODE")
                )
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No se encontró columna de Part Number"));
    }

    String campoPartNumber = colPartObj.getCampoEstandar();
    int colPartExcel = colPartObj.getIndiceColumna();

    // ============================
    // FILAS BASE
    // ============================
    int headerRow = formato.getHeaderRow() - 1; // base 0
    int primeraFilaProducto = headerRow + 1;

    // ============================
    // LOOP PRINCIPAL
    // ============================
    for (RowData dato : datos) {

        String partNumber = dato.get(campoPartNumber);
        if (partNumber == null || partNumber.trim().isEmpty()) continue;

        // Normalización profunda
        partNumber = partNumber
                .replace("\u00A0", " ")
                .replace("\r", "")
                .replace("\n", "")
                .replace("\t", "")
                .trim();

        int filaEncontrada = -1;

        for (int r = primeraFilaProducto; r <= cells.getMaxDataRow(); r++) {

            Cell celdaPart = cells.get(r, colPartExcel);
            if (celdaPart == null) continue;

            String valor = celdaPart.getStringValue();
            if (valor == null) valor = "";

            valor = valor
                    .replace("\u00A0", " ")
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("\t", "")
                    .trim();

            if (valor.equalsIgnoreCase(partNumber)) {
                filaEncontrada = r;
                break;
            }
        }

        // ============================
        // NO MATCH → NO ESCRIBIR NADA
        // ============================
        if (filaEncontrada == -1) {
            logger.warn("⚠ No se encontró Part# {} en el Excel, no se escribe nada", partNumber);
            continue;
        }

        // ============================
        // UNIT PRICE (conversión real)
        // ============================
        String precioStr = dato.get("UNIT_PRICE");

        if (precioStr == null || precioStr.trim().isEmpty()) {
            cells.get(filaEncontrada, colUnitPrice).putValue(0.0);
        } else {
            precioStr = precioStr.replace(",", ".").trim();

            try {
                double precio = Double.parseDouble(precioStr);
                cells.get(filaEncontrada, colUnitPrice).putValue(precio);
            } catch (Exception e) {
                cells.get(filaEncontrada, colUnitPrice).putValue(0.0);
            }
        }

        // ============================
        // REMARKS
        // ============================
        String valorRemark = dato.get(campoRemark);
        cells.get(filaEncontrada, colRemark).putValue(valorRemark != null ? valorRemark : "");
    }

    // ============================
    // RECALCULO
    // ============================
    workbookActual.calculateFormula(true);
    workbookActual.calculateFormula();
    workbookActual.getSettings().setRecalculateBeforeSave(true);
}

    public void guardarWorkbook(String rutaSalida) throws Exception {
        if (workbookActual == null) {
            throw new IllegalStateException("No hay workbook cargado");
        }

        workbookActual.save(rutaSalida);
    }


    // ============================================================
    // 🔧 UTILIDADES
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
}
