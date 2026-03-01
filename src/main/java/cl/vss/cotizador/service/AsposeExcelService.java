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
    // 🟦 CLONAR HOJA COMPLETA (respeta colores, bordes, merges, fórmulas, macros)
    // ============================================================
    public Worksheet clonarHoja(int sheetIndex) throws Exception {
    Workbook copiaWorkbook = new Workbook();
    copiaWorkbook.copy(workbookActual);

    this.workbookActual = copiaWorkbook;

    return workbookActual.getWorksheets().get(sheetIndex);
}


    
    // ============================================================
    // 🔍 DETECTAR PRIMERA FILA REAL DE PRODUCTO
    // ============================================================
    private int findFirstProductRow(Worksheet sheet, int headerRow) {
        Cells cells = sheet.getCells();
        int row = headerRow; // headerRow viene 1-based desde BD

        while (true) {
            boolean hasRealData = false;

            for (int col = 0; col < 50; col++) {
                Cell cell = cells.get(row, col);

                if (cell == null) continue;

                // Ignorar fórmulas decorativas (como L19)
                if (cell.isFormula()) continue;

                String val = cell.getDisplayStringValue();
                if (val != null && !val.trim().isEmpty()) {
                    hasRealData = true;
                    break;
                }
            }

            if (hasRealData) return row;

            row++;
        }
    }

    // ============================================================
    // ✏ ESCRITURA SEGURA (MERGES + COLUMNAS OCULTAS)
    // ============================================================
    private void writeValue(Cells cells, int row, int colIndex, Object value) {
        Cell cell = cells.get(row, colIndex);

        if (cell.isMerged()) {
            int r = cell.getMergedRange().getFirstRow();
            int c = cell.getMergedRange().getFirstColumn();
            cell = cells.get(r, c);
        }

        if (value instanceof Number) {
            cell.putValue(((Number) value).doubleValue());
        } else {
            cell.putValue(value != null ? value.toString() : "");
        }
    }

    // ============================================================
    // 🟩 ESCRIBIR TODOS LOS PRODUCTOS (VERSIÓN FINAL)
    // ============================================================
    public void escribirDatosEnHojaClonada(
            Worksheet hoja,
            List<RowData> datos,
            BrokerFormato formato
    ) throws Exception {

        Cells cells = hoja.getCells();

        // 1) Detectar fila real del primer producto
        int startRow = findFirstProductRow(hoja, formato.getHeaderRow());

        // 2) Obtener columnas desde BD (ya corregidas)
        List<FormatoColumna> columnas = formato.getColumnas();

        // 3) Loop principal
        int row = startRow;

        for (RowData dato : datos) {

            for (FormatoColumna col : columnas) {

                int colIndex = col.getIndiceColumna();
                Object value = dato.get(col.getCampoEstandar());

                writeValue(cells, row, colIndex, value);
            }

            row++;
        }

        logger.info("✔ Escritura completada sin interferir con merges, columnas ocultas ni fórmulas decorativas.");
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
