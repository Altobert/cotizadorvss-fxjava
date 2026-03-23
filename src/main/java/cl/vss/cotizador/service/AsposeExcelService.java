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

    boolean esIfs = formato.getBrokerName() != null
            && formato.getBrokerName().toUpperCase().contains("IFS");

    Worksheet sheet;
    if (esIfs) {
        sheet = workbookActual.getWorksheets().get("PriceRequestDetail");
        if (sheet == null) {
            logger.warn("⚠️ IFS: no existe pestaña 'PriceRequestDetail'. Se usará hoja índice 0 como fallback.");
            sheet = workbookActual.getWorksheets().get(0);
        } else {
            logger.info("📄 IFS detectado: escribiendo UNIT_PRICE/remarks en pestaña 'PriceRequestDetail'");
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

    // ============================
    // FILAS BASE
    // ============================
    int headerRow = formato.getHeaderRow() - 1; // base 0
    int primeraFilaProducto = headerRow + 1;

    // ============================
    // LOOP PRINCIPAL
    // ============================
    int filasSinMatch = 0;
    int filasSinPart = 0;
    int filasTituloOmitidasEnBusqueda = 0;
    int unitPriceEscritos = 0;
    int unitPriceOmitidosVacios = 0;
    int unitPriceOmitidosNoNumericos = 0;

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
        String valorRemark = dato.get(campoRemark);
        cells.get(filaEncontrada, colRemark).putValue(valorRemark != null ? valorRemark : "");
    }

        logger.info("Exportación UNIT_PRICE | escritos={} omitidosVacios={} omitidosNoNumericos={} filasSinPart={} filasSinMatch={} filasTituloOmitidasEnBusqueda={}",
            unitPriceEscritos,
            unitPriceOmitidosVacios,
            unitPriceOmitidosNoNumericos,
            filasSinPart,
            filasSinMatch,
            filasTituloOmitidasEnBusqueda);

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

    //public void guardarWorkbook(String rutaSalida) throws Exception {
      //  if (workbookActual == null) {
        //    throw new IllegalStateException("No hay workbook cargado");
       // }

        //workbookActual.save(rutaSalida);
    //}
    public void guardarWorkbook(String rutaSalida) throws Exception {
    if (workbookActual == null) {
        throw new IllegalStateException("No hay workbook cargado");
    }

    if (rutaArchivoOriginal == null) {
        throw new IllegalStateException("No se conoce la ruta del archivo original");
    }

    // Detectar el formato REAL del archivo original
    FileFormatInfo info = FileFormatUtil.detectFileFormat(rutaArchivoOriginal);
    int format = info.getFileFormatType();

    String extensionDetectada = FileFormatUtil.loadFormatToExtension(format); // ej: ".xlsx", ".xlsm"
    logger.info("💾 Guardando archivo. Formato real detectado: {} ({})",
            format, extensionDetectada);

    // Determinar SaveFormat y extensión correcta
    int saveFormat;
    if (format == FileFormatType.XLSM) {
        saveFormat = SaveFormat.XLSM;
    } else if (format == FileFormatType.XLSX) {
        saveFormat = SaveFormat.XLSX;
    } else if (format == FileFormatType.EXCEL_97_TO_2003) {
        saveFormat = SaveFormat.EXCEL_97_TO_2003;
    } else {
        logger.warn("⚠ Formato desconocido o híbrido. Guardando como XLSX por seguridad.");
        saveFormat = SaveFormat.XLSX;
        extensionDetectada = ".xlsx";
    }

    // Asegurar que la ruta de salida tenga la extensión correcta según el formato real
    String rutaNormalizada = ajustarExtensionSegunFormato(rutaSalida, extensionDetectada);
    if (!rutaNormalizada.equals(rutaSalida)) {
        logger.info("🔁 Ajustando extensión de salida: '{}' → '{}'", rutaSalida, rutaNormalizada);
    }

    workbookActual.save(rutaNormalizada, saveFormat);

    logger.info("✅ Archivo guardado correctamente en {}", rutaNormalizada);
}

// Helper privado dentro de AsposeExcelService
private String ajustarExtensionSegunFormato(String rutaSalida, String extensionConPunto) {
    if (rutaSalida == null || rutaSalida.isEmpty()) {
        return rutaSalida;
    }

    int idx = rutaSalida.lastIndexOf('.');
    if (idx == -1) {
        // No tenía extensión, se la agregamos
        return rutaSalida + extensionConPunto;
    }

    // Reemplazamos la extensión existente por la correcta
    return rutaSalida.substring(0, idx) + extensionConPunto;
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
