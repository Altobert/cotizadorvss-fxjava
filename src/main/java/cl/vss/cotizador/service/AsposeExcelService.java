package cl.vss.cotizador.service;

import com.aspose.cells.*;
import cl.vss.cotizador.model.RowData;
import cl.vss.cotizador.model.FormatoColumna;
import cl.vss.cotizador.model.BrokerFormato;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

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

    boolean esIfs = formato.getBrokerName() != null
            && formato.getBrokerName().toUpperCase().contains("IFS");
        boolean esTms = formato.getBrokerName() != null
            && formato.getBrokerName().toUpperCase().contains("TMS");

    Worksheet sheet;
    if (esIfs) {
        sheet = workbookActual.getWorksheets().get("PriceRequestDetail");
        if (sheet == null) {
            logger.warn("⚠️ IFS: no existe pestaña 'PriceRequestDetail'. Se usará hoja índice 0 como fallback.");
            sheet = workbookActual.getWorksheets().get(0);
        } else {
            logger.info("📄 IFS detectado: escribiendo UNIT_PRICE/remarks en pestaña 'PriceRequestDetail'");
        }
    } else if (esTms) {
        sheet = workbookActual.getWorksheets().get("Sheet1");
        if (sheet == null) {
            logger.warn("⚠️ TMS: no existe pestaña 'Sheet1'. Se usará hoja índice 0 como fallback.");
            sheet = workbookActual.getWorksheets().get(0);
        } else {
            logger.info("📄 TMS detectado: escribiendo UNIT_PRICE/remarks en pestaña 'Sheet1'");
        }
    } else {
        sheet = workbookActual.getWorksheets().get(0);
    }
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
            .filter(c -> esCampoRemarks(c.getCampoEstandar()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No se encontró columna de remarks"));

    int colRemark = colRemarkObj.getIndiceColumna();
    String campoRemark = colRemarkObj.getCampoEstandar();

        // ============================
        // FILAS BASE
        // ============================
        int headerRow = formato.getHeaderRow() - 1; // base 0

        // Fallback defensivo: priorizar columnas detectadas por encabezado real del Excel
        int colUnitPriceByHeader = detectarColumnaPorHeader(cells, headerRow, h ->
            h.equals("UNITPRICE") || h.equals("UNIT_PRICE") || h.equals("UNIT PRICE"));
        if (colUnitPriceByHeader >= 0 && colUnitPriceByHeader != colUnitPrice) {
        logger.warn("⚠️ UNIT_PRICE por formato={} difiere de header={}. Se usará header.",
            colUnitPrice, colUnitPriceByHeader);
        colUnitPrice = colUnitPriceByHeader;
        }

        int colRemarkByHeader = detectarColumnaPorHeader(cells, headerRow, h ->
            h.equals("ITEMCOMMENTS") || h.equals("ITEM_COMMENTS") || h.equals("ITEM COMMENTS")
            || h.equals("SUPPLIERCOMMENTS") || h.equals("SUPPLIER_COMMENTS") || h.equals("SUPPLIER COMMENTS")
            || h.equals("VENDORREMARKS") || h.equals("VENDOR_REMARKS") || h.equals("VENDOR REMARKS")
            || h.equals("OFFICEREMARKS") || h.equals("OFFICE_REMARKS") || h.equals("OFFICE REMARKS")
            || h.equals("NOTES"));
        if (colRemarkByHeader >= 0 && colRemarkByHeader != colRemark) {
        logger.warn("⚠️ REMARKS por formato={} difiere de header={}. Se usará header.",
            colRemark, colRemarkByHeader);
        colRemark = colRemarkByHeader;
        }

        logger.info("🧭 Aspose write target | hoja='{}' headerRow={} UNIT_PRICE={}({}) REMARKS={}({}) campoRemark='{}'",
            sheet.getName(),
            headerRow + 1,
            colUnitPrice,
            indiceAColumnaExcel(colUnitPrice),
            colRemark,
            indiceAColumnaExcel(colRemark),
            campoRemark);

    // ============================
    // COLUMNA PART NUMBER (con soporte CMA CGM)
    // ============================
        FormatoColumna colPartObj;

        if (formato.getFormatoId() == 3) { // CMA CGM
        colPartObj = formato.getColumnas().stream()
                .filter(c -> "DESCRIPTION".equalsIgnoreCase(c.getCampoEstandar()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No se encontró DESCRIPTION para CMA CGM"));
        } else if (esIfs) {
        colPartObj = formato.getColumnas().stream()
            .filter(c ->
                "ITEM_NAME".equalsIgnoreCase(c.getCampoEstandar()) ||
                "ITEM".equalsIgnoreCase(c.getCampoEstandar()) ||
                "ITEM_DESCRIPTION".equalsIgnoreCase(c.getCampoEstandar()) ||
                "DESCRIPTION".equalsIgnoreCase(c.getCampoEstandar()))
            .findFirst()
            .orElseGet(() -> formato.getColumnas().stream()
                .filter(c ->
                    c.getCampoEstandar().toUpperCase().contains("ITEM") ||
                    c.getCampoEstandar().toUpperCase().contains("PART") ||
                    c.getCampoEstandar().toUpperCase().contains("PRODUCT") ||
                    c.getCampoEstandar().toUpperCase().contains("CODE")
                )
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No se encontró columna de Part/Descripción para IFS")));
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

    int primeraFilaProducto = headerRow + 1;

    // ============================
    // LOOP PRINCIPAL
    // ============================
    int filasSinMatch = 0;
    int filasSinPart = 0;
    int filasTituloOmitidasEnBusqueda = 0;
    int filasPorIndiceOriginal = 0;
    int unitPriceEscritos = 0;
    int unitPriceOmitidosVacios = 0;
    int unitPriceOmitidosNoNumericos = 0;
    int remarksEscritos = 0;
    int remarksVacios = 0;

    for (RowData dato : datos) {

        String partNumber = dato.get(campoPartNumber);
        if (partNumber == null || partNumber.trim().isEmpty()) {
            if (esIfs) {
                partNumber = primerNoVacio(
                        dato,
                        "ITEM_NAME", "ITEM", "ITEM_DESCRIPTION", "DESCRIPTION",
                        "PRODUCT_NAME", "PRODUCT_CODE", "ITEM_CODE", "CODE"
                );
            }
            if (partNumber == null || partNumber.trim().isEmpty()) {
                filasSinPart++;
                continue;
            }
        }

        // Normalización profunda
        partNumber = normalizarTextoComparacion(partNumber);

        int filaEncontrada = -1;

        // Priorizar índice de fila original del Excel (evita desalineación por match ambiguo)
        String filaOriginalStr = dato.get("__EXCEL_ROW_INDEX");
        if (filaOriginalStr != null && !filaOriginalStr.trim().isEmpty()) {
            try {
                int filaOriginal = Integer.parseInt(filaOriginalStr.trim());
                if (filaOriginal >= primeraFilaProducto && filaOriginal <= cells.getMaxDataRow()) {
                    if (!esFilaTituloOEncabezado(cells, filaOriginal, colPartExcel, colUnitPrice, esIfs)) {
                        filaEncontrada = filaOriginal;
                        filasPorIndiceOriginal++;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (filaEncontrada == -1) {
            for (int r = primeraFilaProducto; r <= cells.getMaxDataRow(); r++) {

                if (esFilaTituloOEncabezado(cells, r, colPartExcel, colUnitPrice, esIfs)) {
                    filasTituloOmitidasEnBusqueda++;
                    continue;
                }

                Cell celdaPart = cells.get(r, colPartExcel);
                if (celdaPart == null) continue;

                String valor = celdaPart.getStringValue();
                if (valor == null) valor = "";

                    valor = normalizarTextoComparacion(valor);

                if (valor.equalsIgnoreCase(partNumber)) {
                    filaEncontrada = r;
                    break;
                }
            }
        }

        // ============================
        // NO MATCH → NO ESCRIBIR NADA
        // ============================
        if (filaEncontrada == -1) {
            filasSinMatch++;
            logger.warn("⚠ No se encontró Part# {} en el Excel, no se escribe nada", partNumber);
            continue;
        }

        // ============================
        // UNIT PRICE (conversión real)
        // ============================
        String precioStr = dato.get("UNIT_PRICE");

        if (precioStr == null || precioStr.trim().isEmpty()) {
            unitPriceOmitidosVacios++;
        } else {
            precioStr = precioStr.trim();

            if (precioStr.contains(",") && precioStr.contains(".")) {
                precioStr = precioStr.replace(",", "");
            } else if (precioStr.contains(",")) {
                precioStr = precioStr.replace(",", ".");
            }

            try {
                double precio = Double.parseDouble(precioStr);
                cells.get(filaEncontrada, colUnitPrice).putValue(precio);
                unitPriceEscritos++;
            } catch (Exception e) {
                unitPriceOmitidosNoNumericos++;
            }
        }

        // ============================
        // REMARKS
        // ============================
        String valorRemark = obtenerValorRemark(dato, campoRemark);
        if (valorRemark != null && !valorRemark.trim().isEmpty()) {
            remarksEscritos++;
        } else {
            remarksVacios++;
        }
        cells.get(filaEncontrada, colRemark).putValue(valorRemark != null ? valorRemark : "");
    }

        logger.info("Exportación UNIT_PRICE | escritos={} omitidosVacios={} omitidosNoNumericos={} filasSinPart={} filasSinMatch={} filasTituloOmitidasEnBusqueda={}",
            unitPriceEscritos,
            unitPriceOmitidosVacios,
            unitPriceOmitidosNoNumericos,
            filasSinPart,
            filasSinMatch,
            filasTituloOmitidasEnBusqueda);

        logger.info("Exportación mapeo filas | porIndiceOriginal={} porBusquedaPart={} totalDatos={}",
            filasPorIndiceOriginal,
            (datos != null ? Math.max(datos.size() - filasPorIndiceOriginal - filasSinPart, 0) : 0),
            datos != null ? datos.size() : 0);

        logger.info("Exportación REMARKS | escritos={} vacios={} columna={}({}) campo='{}'",
            remarksEscritos,
            remarksVacios,
            colRemark,
            indiceAColumnaExcel(colRemark),
            campoRemark);

    // ============================
    // RECALCULO
    // ============================
    workbookActual.calculateFormula(true);
    workbookActual.calculateFormula();
    workbookActual.getSettings().setRecalculateBeforeSave(true);
}

    private String primerNoVacio(RowData dato, String... keys) {
        for (String key : keys) {
            String valor = dato.get(key);
            if (valor != null && !valor.trim().isEmpty()) {
                return valor;
            }
        }
        return "";
    }

    private String obtenerValorRemark(RowData dato, String campoRemark) {
        String valorDirecto = dato.get(campoRemark);
        if (valorDirecto != null && !valorDirecto.trim().isEmpty()) {
            return valorDirecto;
        }

        String campoNormalizado = normalizarCampoEstandar(campoRemark);

        String valorNormalizado = dato.get(campoNormalizado);
        if (valorNormalizado != null && !valorNormalizado.trim().isEmpty()) {
            return valorNormalizado;
        }

        String valorConEspacios = dato.get(campoNormalizado.replace('_', ' '));
        if (valorConEspacios != null && !valorConEspacios.trim().isEmpty()) {
            return valorConEspacios;
        }

        switch (campoNormalizado) {
            case "ITEM_COMMENTS":
                return primerNoVacio(dato,
                        "ITEM_COMMENTS", "ITEM COMMENTS", "ITEM_COMMENT",
                        "COMMENTS", "COMENTARIOS", "SUPPLIER_COMMENTS", "SUPPLIER COMMENTS");
            case "SUPPLIER_NOTES":
                return primerNoVacio(dato,
                        "SUPPLIER_NOTES", "SUPPLIER NOTES", "Supplier Notes",
                        "NOTES", "Notes");
            case "SUPPLIER_COMMENTS":
                return primerNoVacio(dato,
                        "SUPPLIER_COMMENTS", "SUPPLIER COMMENTS", "SUPPLIER COMMENT",
                        "Supplier Commnets", "Supplier Comments", "SUPPLIER COMMNETS",
                        "SUPPLIER_NOTES", "SUPPLIER NOTES", "Supplier Notes");
            case "VENDOR_REMARKS":
                return primerNoVacio(dato,
                        "VENDOR_REMARKS", "VENDOR_REMARK", "VENDOR_REMARKS", "VENDOR COMMENTS",
                        "VENDOR_COMMENTS", "VENDOR_COMMENT", "REMARKS");
            case "OFFICE_REMARKS":
                return primerNoVacio(dato,
                        "OFFICE_REMARKS", "OFFICE REMARKS", "OFFICE_REMARK",
                        "OFFICE_COMMENTS", "OFFICE COMMENTS");
            case "NOTES":
                return primerNoVacio(dato, "NOTES", "Notes");
            default:
                return valorDirecto != null ? valorDirecto : "";
        }
    }

    private boolean esCampoRemarks(String campoEstandar) {
        String campo = normalizarCampoEstandar(campoEstandar);
        return "VENDOR_REMARKS".equals(campo)
                || "NOTES".equals(campo)
                || "SUPPLIER_NOTES".equals(campo)
                || "SUPPLIER_COMMENTS".equals(campo)
                || "OFFICE_REMARKS".equals(campo)
                || "ITEM_COMMENTS".equals(campo);
    }

    private String normalizarCampoEstandar(String campo) {
        if (campo == null) {
            return "";
        }
        String normalizado = campo.trim().toUpperCase().replace(' ', '_');

        // Tolerancia a typos frecuentes en BD
        if ("SUPPLIER_COMMNETS".equals(normalizado)
                || "SUPPLIER_COMMETS".equals(normalizado)) {
            return "SUPPLIER_COMMENTS";
        }

        return normalizado;
    }

    private int detectarColumnaPorHeader(Cells cells, int headerRow, Predicate<String> matcher) {
        if (headerRow < 0 || matcher == null) {
            return -1;
        }

        int maxCol = Math.max(cells.getMaxDataColumn(), 0);
        for (int col = 0; col <= maxCol; col++) {
            Cell headerCell = cells.get(headerRow, col);
            if (headerCell == null) {
                continue;
            }
            String header = normalizarHeader(headerCell.getStringValue());
            if (!header.isEmpty() && matcher.test(header)) {
                return col;
            }
        }
        return -1;
    }

    private String normalizarHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.trim().toUpperCase()
                .replace("_", " ")
                .replaceAll("\\s+", " ");
    }

    private String indiceAColumnaExcel(int indice) {
        if (indice < 0) {
            return "?";
        }
        StringBuilder col = new StringBuilder();
        int value = indice;
        while (value >= 0) {
            int rem = value % 26;
            col.insert(0, (char) ('A' + rem));
            value = (value / 26) - 1;
        }
        return col.toString();
    }

    private String normalizarTextoComparacion(String valor) {
        if (valor == null) {
            return "";
        }
        return valor
                .replace("\u00A0", " ")
                .replace("\r", "")
                .replace("\n", "")
                .replace("\t", "")
                .trim();
    }

    private boolean esFilaTituloOEncabezado(Cells cells, int fila, int colPartExcel, int colUnitPrice, boolean esIfs) {
        String valorPart = "";
        String valorUnit = "";

        Cell celdaPart = cells.get(fila, colPartExcel);
        if (celdaPart != null) {
            valorPart = normalizarTextoComparacion(celdaPart.getStringValue()).toUpperCase();
        }

        Cell celdaUnit = cells.get(fila, colUnitPrice);
        if (celdaUnit != null) {
            valorUnit = normalizarTextoComparacion(celdaUnit.getStringValue()).toUpperCase();
        }

        if (valorPart.isEmpty() && valorUnit.isEmpty()) {
            return true;
        }

        if (esIfs && "YOUR PRICE".equals(valorUnit)) {
            return true;
        }

        return valorPart.equals("PROVISIONS")
                || valorPart.equals("PROVISION")
                || valorPart.equals("ITEMS")
                || valorPart.equals("PRODUCTS")
                || valorPart.equals("DESCRIPTION")
                || valorPart.equals("ITEM DESCRIPTION")
                || valorPart.equals("PRODUCT LIST")
                || valorPart.equals("PRODUCT CODE")
                || valorPart.equals("ITEM CODE")
                || valorPart.startsWith("----")
                || valorPart.startsWith("====");
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
