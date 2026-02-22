package cl.vss.cotizador.service;

import com.aspose.cells.Cells;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Valida que el broker detectado en el archivo Excel coincida
 * con el broker seleccionado en el ComboBox.
 * 
 * Escanea las primeras filas del Excel (zona de metadata/cabecera)
 * buscando texto que identifique al broker.
 */
public class BrokerValidator {

    private static final Logger logger = LogManager.getLogger(BrokerValidator.class);

    /**
     * Resultado de la validación
     */
    public static class ResultadoValidacion {
        private final boolean valido;
        private final String brokerDetectado;
        private final String mensaje;

        public ResultadoValidacion(boolean valido, String brokerDetectado, String mensaje) {
            this.valido = valido;
            this.brokerDetectado = brokerDetectado;
            this.mensaje = mensaje;
        }

        public boolean isValido() { return valido; }
        public String getBrokerDetectado() { return brokerDetectado; }
        public String getMensaje() { return mensaje; }
    }

    /**
     * Patrones de identificación para cada broker.
     * Se buscan en las primeras filas del Excel (zona de metadata).
     * Cada entrada: { nombreBroker, patron1, patron2, ... }
     */
    private static final String[][] PATRONES_BROKER = {
        { "MCTC",        "MCTC", "MCTC MARINE" },
        { "OCEANIC",     "OCEANIC", "OCEANIC CATERING" },
        { "CMA CGM",     "CMA CGM", "CMA-CGM" },
        { "GARRETS",     "GARRETS", "GARRETS INTERNATIONAL" },
        { "PROCURESHIP", "PROCURESHIP" },
        { "BSM",         "BSM", "BSM CATERING" },
    };

    /**
     * Cantidad máxima de filas a escanear buscando el nombre del broker.
     */
    private static final int MAX_FILAS_ESCANEO = 30;

    /**
     * Valida que el broker del archivo Excel coincida con el seleccionado.
     *
     * @param workbook          Workbook de Aspose ya cargado
     * @param brokerSeleccionado Nombre del broker elegido en el ComboBox
     * @return ResultadoValidacion con el resultado
     */
    public ResultadoValidacion validar(Workbook workbook, String brokerSeleccionado) {
        if (workbook == null || brokerSeleccionado == null || brokerSeleccionado.trim().isEmpty()) {
            return new ResultadoValidacion(false, null,
                "No se puede validar: workbook o broker seleccionado es nulo.");
        }

        String brokerDetectado = detectarBrokerDesdeExcel(workbook);

        if (brokerDetectado == null) {
            logger.warn("⚠️ No se pudo detectar el broker desde el archivo Excel");
            // Si no se detecta, permitir continuar pero con advertencia
            return new ResultadoValidacion(true, null,
                "No se pudo identificar el broker en el archivo. Se continuará con el broker seleccionado.");
        }

        String seleccionadoUpper = brokerSeleccionado.toUpperCase().trim();
        String detectadoUpper = brokerDetectado.toUpperCase().trim();

        // Validar coincidencia: el nombre del broker seleccionado debe contener
        // el broker detectado, o viceversa
        boolean coincide = seleccionadoUpper.contains(detectadoUpper)
                        || detectadoUpper.contains(seleccionadoUpper);

        if (coincide) {
            logger.info("✅ Broker validado: seleccionado='{}', detectado='{}'",
                brokerSeleccionado, brokerDetectado);
            return new ResultadoValidacion(true, brokerDetectado,
                "Broker validado correctamente: " + brokerDetectado);
        } else {
            logger.warn("❌ Broker NO coincide: seleccionado='{}', detectado='{}'",
                brokerSeleccionado, brokerDetectado);
            return new ResultadoValidacion(false, brokerDetectado,
                "El archivo pertenece al broker \"" + brokerDetectado
                + "\" pero se seleccionó \"" + brokerSeleccionado + "\".\n\n"
                + "Por favor seleccione el broker correcto antes de cargar el archivo.");
        }
    }

    /**
     * Escanea las primeras filas del Excel buscando texto que identifique al broker.
     *
     * @param workbook Workbook de Aspose
     * @return Nombre del broker detectado, o null si no se identifica
     */
    public String detectarBrokerDesdeExcel(Workbook workbook) {
        try {
            Worksheet sheet = workbook.getWorksheets().get(0);
            Cells cells = sheet.getCells();

            int maxRow = Math.min(cells.getMaxDataRow(), MAX_FILAS_ESCANEO);
            int maxCol = Math.min(cells.getMaxDataColumn(), 20); // Limitar columnas

            // Recorrer las primeras filas buscando patrones de broker
            for (int row = 0; row <= maxRow; row++) {
                for (int col = 0; col <= maxCol; col++) {
                    com.aspose.cells.Cell cell = cells.get(row, col);
                    if (cell == null) continue;

                    String valor = obtenerTexto(cell);
                    if (valor == null || valor.isEmpty()) continue;

                    String valorUpper = valor.toUpperCase();

                    // Buscar patrones de cada broker
                    for (String[] patron : PATRONES_BROKER) {
                        String nombreBroker = patron[0];
                        for (int p = 1; p < patron.length; p++) {
                            if (valorUpper.contains(patron[p])) {
                                logger.info("🔍 Broker detectado: '{}' en fila {} col {} (texto: '{}')",
                                    nombreBroker, row + 1, col, valor.trim());
                                return nombreBroker;
                            }
                        }
                    }
                }
            }

            logger.info("🔍 No se detectó broker en las primeras {} filas", maxRow + 1);
            return null;

        } catch (Exception e) {
            logger.error("Error al detectar broker desde Excel", e);
            return null;
        }
    }

    /**
     * Obtiene el texto de una celda Aspose de forma segura.
     */
    private String obtenerTexto(com.aspose.cells.Cell cell) {
        try {
            int type = cell.getType();
            if (type == com.aspose.cells.CellValueType.IS_STRING) {
                return cell.getStringValue();
            } else {
                String val = cell.getStringValue();
                return val != null ? val.trim() : null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
