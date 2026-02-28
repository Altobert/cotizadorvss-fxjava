package cl.vss.cotizador.service;

import com.aspose.cells.*;
import cl.vss.cotizador.model.RowData;
import cl.vss.cotizador.model.FormatoColumna;
import cl.vss.cotizador.model.BrokerFormato;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    // 🔵 LECTURA DE DATOS
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
    // 🟩 CLONAR HOJA COMPLETA
    // ============================================================
    public Worksheet clonarHoja(int sheetIndex) throws Exception {
        Workbook copiaWorkbook = new Workbook();
        copiaWorkbook.copy(workbookActual);

        this.workbookActual = copiaWorkbook;

        return workbookActual.getWorksheets().get(sheetIndex);
    }

    // ============================================================
    // 🟩 DETECTAR COLUMNA TOTAL PRICE
    // ============================================================
    private int detectarColumnaTotalPrice(BrokerFormato formato) {
        for (FormatoColumna col : formato.getColumnas()) {
            if (col.getNombreColumnaOriginal() != null &&
                col.getNombreColumnaOriginal().toUpperCase().contains("TOTAL")) {
                return col.getIndiceColumna();
            }
        }
        return -1;
    }

    // ============================================================
    // 🟩 ESCRIBIR PRODUCTOS (CORRIMIENTO + FÓRMULA + PROTECCIÓN SUBTOTAL PROVISIONS)
    // ============================================================
    public void escribirDatosEnHojaClonada(
            Worksheet hoja,
            List<RowData> datos,
            int startRow,
            BrokerFormato formato
    ) throws Exception {

        Cells cells = hoja.getCells();

        // ============================================================
        // 🔍 DETECTAR FILA DONDE EMPIEZA BOND (DINÁMICO EN TODAS LAS COLUMNAS)
        // ============================================================
        int filaBondAspose = -1;

        for (int r = 0; r <= cells.getMaxDataRow(); r++) {
            for (int c = 0; c <= cells.getMaxDataColumn(); c++) {
                Cell celda = cells.get(r, c);
                if (celda != null && celda.getStringValue() != null) {
                    String texto = celda.getStringValue().trim().toUpperCase();
                    if ("BOND".equals(texto)) {
                        filaBondAspose = r;
                        logger.info("📌 Fila de BOND detectada dinámicamente en: {}", filaBondAspose + 1);
                        break;
                    }
                }
            }
            if (filaBondAspose != -1) break;
        }

        // Si encontramos BOND, asumimos que el subtotal de PROVISIONS está justo arriba
        int filaSubtotalProvisions = (filaBondAspose != -1) ? filaBondAspose - 1 : -1;

        // ============================================================
        // 🔍 DETECTAR COLUMNA TOTAL PRICE Y FÓRMULA ORIGINAL
        // ============================================================
        int colTotal = detectarColumnaTotalPrice(formato);

        String formulaOriginal = null;
        int filaOriginal = -1;

        if (colTotal != -1) {
            Cell celdaOriginal = cells.get(startRow, colTotal);
            if (celdaOriginal != null && celdaOriginal.isFormula()) {
                formulaOriginal = celdaOriginal.getFormula();

                Pattern p = Pattern.compile("[A-Z]+(\\d+)");
                Matcher m = p.matcher(formulaOriginal);
                if (m.find()) {
                    filaOriginal = Integer.parseInt(m.group(1));
                }
            }
        }

        // ============================================================
        // 🟩 CORRECCIÓN DEL CORRIMIENTO (filaOriginal → Aspose)
        // ============================================================
        if (filaOriginal != -1) {
            startRow = filaOriginal - 1; // Aspose es 0-based
        }

        // ============================================================
        // 🟩 LOOP DE ESCRITURA DE PRODUCTOS
        // ============================================================
        for (int i = 0; i < datos.size(); i++) {

            int filaExcel = startRow + i;

            // 🟩 PROTECCIÓN: si hay BOND y subtotal de PROVISIONS definido,
            // aseguramos que NUNCA pisemos el subtotal.
            if (filaBondAspose != -1 && filaSubtotalProvisions != -1) {
                // Mientras la fila donde queremos escribir alcance o supere
                // la fila del subtotal, insertamos filas en el subtotal
                while (filaExcel >= filaSubtotalProvisions) {
                    logger.info("↕ Insertando fila antes del subtotal PROVISIONS en fila {}", filaSubtotalProvisions + 1);
                    cells.insertRows(filaSubtotalProvisions, 1);

                    // Al insertar:
                    // - el subtotal baja una fila
                    // - BOND baja una fila
                    filaSubtotalProvisions++;
                    filaBondAspose++;
                    filaExcel++;
                }
            }

            // 🛑 Seguridad extra: si por alguna razón alcanzamos BOND, cortamos
            if (filaBondAspose != -1 && filaExcel >= filaBondAspose) {
                logger.info("⛔ Corte dinámico: alcanzamos la fila de BOND en {}", filaBondAspose + 1);
                break;
            }

            RowData rowData = datos.get(i);
            int fila1 = filaExcel + 1; // Para fórmula (1-based)

            for (FormatoColumna columna : formato.getColumnas()) {

                int colIdx = columna.getIndiceColumna();
                Cell cell = cells.get(filaExcel, colIdx);

                // 👉 FÓRMULA DINÁMICA
                if (colIdx == colTotal && formulaOriginal != null && filaOriginal != -1) {

                    String formulaDinamica = formulaOriginal.replaceAll(
                            "(?<=\\D)" + filaOriginal + "(?=\\D|$)",
                            String.valueOf(fila1)
                    );

                    cell.setFormula(formulaDinamica);
                    continue;
                }

                // 👉 VALORES NORMALES
                String valor = rowData.get(columna.getCampoEstandar());
                if (valor == null) valor = "";

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
        }

        logger.info("✅ Datos escritos sin pisar subtotal de PROVISIONS, sin invadir BOND y con fórmula dinámica");
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

   
    public void escribirUnitPriceYRemarks(
        List<RowData> datos,
        BrokerFormato formato
) throws Exception {

    if (workbookActual == null) {
        throw new IllegalStateException("No hay workbook cargado");
    }

    Worksheet sheet = workbookActual.getWorksheets().get(0);
    Cells cells = sheet.getCells();

    // ============================================================
    // 🔵 UNIT PRICE
    // ============================================================
    int colUnitPrice = formato.getColumnas().stream()
            .filter(c -> "UNIT_PRICE".equalsIgnoreCase(c.getCampoEstandar()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("UNIT_PRICE no encontrado"))
            .getIndiceColumna();

    // ============================================================
    // 🔵 REMARKS
    // ============================================================
    FormatoColumna colRemarkObj = formato.getColumnas().stream()
            .filter(c ->
                    "VENDOR_REMARKS".equalsIgnoreCase(c.getCampoEstandar()) ||
                    "NOTES".equalsIgnoreCase(c.getCampoEstandar()) ||
                    "SUPPLIER_COMMENTS".equalsIgnoreCase(c.getCampoEstandar())
            )
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No se encontró columna de remarks"));

    int colRemark = colRemarkObj.getIndiceColumna();
    String campoRemark = colRemarkObj.getCampoEstandar();

    // ============================================================
    // 🔵 Fila REAL donde empiezan los productos (CORREGIDO)
    // ============================================================
    int row = formato.getHeaderRow()-1; // 1-based
    logger.info("Fila de inicio para UNIT_PRICE y REMARKS (1-based): {}", row);
    //row = row - 1; // Aspose usa 0-based → esta es la fila correcta del primer producto
    row++;

    // ============================================================
    // 🔵 LOOP DE ESCRITURA (SIN CORTE POR PART#)
    // ============================================================
    for (int i = 0; i < datos.size(); i++) {

        RowData dato = datos.get(i);

        // UNIT PRICE
        cells.get(row, colUnitPrice).putValue(dato.get("UNIT_PRICE"));

        // REMARKS
        String valorRemark = dato.get(campoRemark);
        cells.get(row, colRemark).putValue(valorRemark != null ? valorRemark : "");

        row++;
    }
}








}
