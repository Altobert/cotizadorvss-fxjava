package cl.vss.cotizador.service;

import com.aspose.cells.*;
import cl.vss.cotizador.model.RowData;
import cl.vss.cotizador.model.FormatoColumna;
import cl.vss.cotizador.model.BrokerFormato;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    // ============================================================
    // 📂 CARGA DE ARCHIVO
    // ============================================================
    public Workbook cargarWorkbook(String ruta) throws Exception {
        logger.info("📂 Cargando archivo con Aspose: {}", ruta);

        workbookActual = new Workbook(ruta);
        rutaArchivoOriginal = ruta;

        if (workbookActual.getVbaProject() != null) {
            logger.info("🔧 Macros VBA detectadas");
        }

        return workbookActual;
    }

    public Workbook getWorkbookActual() {
        return workbookActual;
    }

    public void limpiarWorkbook() {
        if (workbookActual != null) {
            try { workbookActual.dispose(); } catch (Exception ignored) {}
            workbookActual = null;
            rutaArchivoOriginal = null;
        }
    }

    // ============================================================
    // 🟦 CLONAR HOJA COMPLETA
    // ============================================================
    public Worksheet clonarHoja(int sheetIndex) throws Exception {
        Workbook copiaWorkbook = new Workbook();
        copiaWorkbook.copy(workbookActual);

        this.workbookActual = copiaWorkbook;

        return workbookActual.getWorksheets().get(sheetIndex);
    }

    // ============================================================
    // 🔵 UTILIDADES
    // ============================================================
    private boolean esNumerico(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Double.parseDouble(str.replace(",", "").replace("$", ""));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ============================================================
    // 🟩 ESCRIBIR PRODUCTOS + UNIT_PRICE + REMARKS (VERSIÓN FINAL)
    // ============================================================
   public void escribirDatosEnHojaClonada(
        Worksheet hoja,
        List<RowData> datos,
        int startRow,
        BrokerFormato formato
) throws Exception {

    Cells cells = hoja.getCells();

    // ============================================================
    // 🔍 DETECTAR FILA DONDE EMPIEZA BOND
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

    int filaSubtotalProvisions = (filaBondAspose != -1) ? filaBondAspose - 1 : -1;

    // ============================================================
    // 🔍 DETECTAR COLUMNA TOTAL PRICE Y FÓRMULA ORIGINAL
    // ============================================================
    int colTotal = -1;
    for (FormatoColumna col : formato.getColumnas()) {
        if (col.getNombreColumnaOriginal() != null &&
            col.getNombreColumnaOriginal().toUpperCase().contains("TOTAL")) {
            colTotal = col.getIndiceColumna();
            break;
        }
    }

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

    if (filaOriginal != -1) {
        startRow = filaOriginal - 1;
    }

    // ============================================================
    // 🟩 LOOP PRINCIPAL
    // ============================================================
    for (int i = 0; i < datos.size(); i++) {

        int filaExcel = startRow + i;

        // 🟦 PROTECCIÓN: no pisar subtotal PROVISIONS
       
    if (filaBondAspose != -1 && filaSubtotalProvisions != -1) {

    // Solo insertar si el producto cae EXACTAMENTE sobre el subtotal
    if (filaExcel == filaSubtotalProvisions) {

        logger.info("↕ Insertando fila antes del subtotal PROVISIONS en fila {}", filaSubtotalProvisions + 1);

        cells.insertRows(filaSubtotalProvisions, 1);

        filaSubtotalProvisions++;
        filaBondAspose++;
        filaExcel++;
    }
}


        // 🛑 No invadir BOND
        if (filaBondAspose != -1 && filaExcel >= filaBondAspose) {
            logger.info("⛔ Corte dinámico: alcanzamos la fila de BOND en {}", filaBondAspose + 1);
            break;
        }

        RowData rowData = datos.get(i);
        int fila1 = filaExcel + 1;

        // ============================================================
        // 🟩 ESCRITURA MODULAR POR COLUMNA
        // ============================================================
        for (FormatoColumna columna : formato.getColumnas()) {

            int colIdx = columna.getIndiceColumna();
            String campo = columna.getCampoEstandar();
            Cell cell = cells.get(filaExcel, colIdx);

            // 👉 FÓRMULA DINÁMICA TOTAL PRICE
            if (colIdx == colTotal && formulaOriginal != null && filaOriginal != -1) {

                String formulaDinamica = formulaOriginal.replaceAll(
                        "(?<=\\D)" + filaOriginal + "(?=\\D|$)",
                        String.valueOf(fila1)
                );

                cell.setFormula(formulaDinamica);
                continue;
            }

            // 👉 VALOR A ESCRIBIR (UNIT_PRICE, REMARKS, ETC.)
            String valor = rowData.get(campo);
            if (valor == null) valor = "";

            // 👉 MERGES
            if (cell.isMerged()) {
                int r = cell.getMergedRange().getFirstRow();
                int c = cell.getMergedRange().getFirstColumn();
                cell = cells.get(r, c);
            }

            // ============================================================
            // 🟩 NÚMEROS (con protección tipoDato null)
            // ============================================================
            String tipo = columna.getTipoDato();

            boolean esNumero = tipo != null && (
                    tipo.equalsIgnoreCase("DECIMAL") ||
                    tipo.equalsIgnoreCase("INTEGER") ||
                    tipo.equalsIgnoreCase("NUMERIC")
            );

            if (esNumerico(valor) && esNumero) {

                try {
                    double num = Double.parseDouble(valor.replace(",", "").replace("$", ""));
                    cell.putValue(num);
                } catch (Exception ex) {
                    cell.putValue(valor);
                }

            } else {
                // 👉 TEXTO (incluye REMARKS)
                cell.putValue(valor);
            }
        }
    }

    logger.info("✅ Datos escritos sin pisar subtotal de PROVISIONS, sin invadir BOND y con fórmula dinámica + UNIT_PRICE + REMARKS");
}


    // ============================================================
    // 💾 GUARDAR ARCHIVO
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
}
