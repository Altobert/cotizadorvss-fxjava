import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileInputStream;

public class AnalysisOneSphere {
    
    public static void main(String[] args) throws Exception {
        String filePath = args.length > 0 ? args[0] : "cotizaciones/Libro4.xlsx";
        
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            System.out.println("=== FILAS 14-50 DEL ARCHIVO: " + filePath + " ===\n");
            for (int rowIdx = 13; rowIdx <= Math.min(49, sheet.getLastRowNum()); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                System.out.printf("Fila %3d: ", rowIdx + 1);
                
                if (row == null || row.getLastCellNum() <= 0) {
                    System.out.println("[VACÍA]");
                    continue;
                }
                
                StringBuilder line = new StringBuilder();
                for (int colIdx = 0; colIdx < Math.min(6, row.getLastCellNum()); colIdx++) {
                    Cell cell = row.getCell(colIdx);
                    String cellValue = getCellValue(cell);
                    if (!cellValue.isEmpty()) {
                        line.append((char)('A' + colIdx)).append(":");
                        String display = cellValue.length() > 15 ? cellValue.substring(0, 15) + "..." : cellValue;
                        line.append(display).append(" | ");
                    }
                }
                System.out.println(line.toString());
            }
        }
    }
    
    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    }
                    double num = cell.getNumericCellValue();
                    if (num == (long) num) {
                        return String.valueOf((long) num);
                    }
                    return String.valueOf(num);
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    return cell.getCellFormula();
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }
}
