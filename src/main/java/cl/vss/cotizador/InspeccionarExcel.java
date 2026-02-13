package cl.vss.cotizador;

import com.aspose.cells.*;

/**
 * Utilidad para inspeccionar archivos Excel y ver su estructura.
 */
public class InspeccionarExcel {
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: InspeccionarExcel <ruta-archivo.xlsx>");
            return;
        }
        
        String rutaArchivo = args[0];
        System.out.println("📂 Inspeccionando: " + rutaArchivo);
        System.out.println("=".repeat(80));
        
        try {
            Workbook workbook = new Workbook(rutaArchivo);
            Worksheet sheet = workbook.getWorksheets().get(0);
            Cells cells = sheet.getCells();
            
            int maxRow = Math.min(cells.getMaxDataRow(), 30); // Primeras 30 filas
            int maxCol = Math.min(cells.getMaxDataColumn(), 20); // Primeras 20 columnas
            
            System.out.println("\n📊 Revisando filas 1-" + (maxRow + 1) + " (columnas A-" + (char)('A' + maxCol) + "):\n");
            
            // Mostrar filas relevantes (15-25 para BSM)
            for (int row = 14; row <= Math.min(25, maxRow); row++) {
                System.out.println("--- FILA " + (row + 1) + " ---");
                
                for (int col = 0; col <= maxCol; col++) {
                    Cell cell = cells.get(row, col);
                    if (cell != null && cell.getValue() != null) {
                        String colLetter = String.valueOf((char)('A' + col));
                        if (col >= 26) {
                            colLetter = "A" + (char)('A' + col - 26);
                        }
                        
                        String valor = "";
                        String tipo = "";
                        String formula = "";
                        
                        if (cell.isFormula()) {
                            formula = " [FORMULA: " + cell.getFormula() + "]";
                            tipo = "FORMULA";
                        } else {
                            tipo = String.valueOf(cell.getType());
                        }
                        
                        try {
                            valor = cell.getStringValue();
                            if (valor.length() > 50) {
                                valor = valor.substring(0, 50) + "...";
                            }
                        } catch (Exception e) {
                            valor = cell.getValue().toString();
                        }
                        
                        // Verificar color de fondo
                        Style style = cell.getStyle();
                        String colorInfo = "";
                        if (style != null) {
                            Color bgColor = style.getBackgroundColor();
                            Color fgColor = style.getForegroundColor();
                            if (bgColor != null && !bgColor.isEmpty()) {
                                colorInfo = " [BG: R=" + (bgColor.getR() & 0xFF) + ",G=" + (bgColor.getG() & 0xFF) + ",B=" + (bgColor.getB() & 0xFF) + "]";
                            }
                            if (fgColor != null && !fgColor.isEmpty()) {
                                colorInfo += " [FG: R=" + (fgColor.getR() & 0xFF) + ",G=" + (fgColor.getG() & 0xFF) + ",B=" + (fgColor.getB() & 0xFF) + "]";
                            }
                        }
                        
                        System.out.println("  " + colLetter + (row + 1) + ": '" + valor + "' (" + tipo + ")" + formula + colorInfo);
                    }
                }
                System.out.println();
            }
            
            // Buscar específicamente columna TOTAL (columna P = índice 15)
            System.out.println("\n📝 COLUMNA P (TOTAL) - Filas 18-25:");
            for (int row = 17; row <= 24; row++) {
                Cell cell = cells.get(row, 15); // Columna P
                String info = "Fila " + (row + 1) + " (P" + (row + 1) + "): ";
                if (cell != null) {
                    if (cell.isFormula()) {
                        info += "FORMULA = " + cell.getFormula();
                    } else if (cell.getValue() != null) {
                        info += "VALOR = " + cell.getStringValue();
                    } else {
                        info += "(vacía)";
                    }
                } else {
                    info += "(null)";
                }
                System.out.println("  " + info);
            }
            
            System.out.println("\n✅ Inspección completada.");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
