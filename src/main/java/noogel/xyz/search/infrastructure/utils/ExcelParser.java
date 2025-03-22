package noogel.xyz.search.infrastructure.utils;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.util.Pair;

public class ExcelParser {

    /**
     * 解析 Excel 文件并结构化输出
     * @param filePath 文件路径
     * @param hasHeader 是否包含标题行
     * @return Map<Sheet名称, List<结构化数据>> 
     */
    public static Map<String, List<List<Pair<String, Object>>>> parseExcel(String filePath, boolean hasHeader) {
        Map<String, List<List<Pair<String, Object>>>> result = new LinkedHashMap<>();
        
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = createWorkbook(fis, filePath)) {

            // 遍历所有 Sheet
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                List<List<Pair<String, Object>>> sheetData = new ArrayList<>();
                List<String> headers = new ArrayList<>();

                // 处理标题行
                if (hasHeader && sheet.getPhysicalNumberOfRows() > 0) {
                    Row headerRow = sheet.getRow(0);
                    headerRow.forEach(cell -> headers.add(getCellValue(cell).toString()));
                }

                // 遍历数据行
                for (int rowIdx = hasHeader ? 1 : 0; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                    Row row = sheet.getRow(rowIdx);
                    if (row == null) continue;

                    List<Pair<String, Object>> rowData = new ArrayList<>();
                    for (int cellIdx = 0; cellIdx < row.getLastCellNum(); cellIdx++) {
                        Cell cell = row.getCell(cellIdx, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        String headerName = hasHeader && cellIdx < headers.size() ? 
                                            headers.get(cellIdx) : "col_" + (cellIdx + 1);
                        rowData.add(Pair.of(headerName, getCellValue(cell)));
                    }
                    sheetData.add(rowData);
                }
                result.put(sheet.getSheetName(), sheetData);
            }
        } catch (Exception e) {
            throw new RuntimeException("解析 Excel 失败", e);
        }
        return result;
    }

    /**
     * 根据文件类型创建 Workbook
     */
    private static Workbook createWorkbook(FileInputStream fis, String filePath) throws Exception {
        if (filePath.endsWith(".xlsx")) {
            return new XSSFWorkbook(fis); // 处理 .xlsx 文件
        } else if (filePath.endsWith(".xls")) {
            return new HSSFWorkbook(fis); // 处理 .xls 文件
        }
        throw new IllegalArgumentException("不支持的 Excel 格式");
    }

    /**
     * 获取单元格值的通用方法
     */
    private static Object getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return DateUtil.isCellDateFormatted(cell) ? 
                       cell.getDateCellValue() : cell.getNumericCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                return evaluateFormulaCell(cell);
            default:
                return "";
        }
    }

    /**
     * 计算公式单元格的值
     */
    private static Object evaluateFormulaCell(Cell cell) {
        FormulaEvaluator evaluator = cell.getSheet().getWorkbook()
                .getCreationHelper().createFormulaEvaluator();
        CellValue cellValue = evaluator.evaluate(cell);
        switch (cellValue.getCellType()) {
            case NUMERIC: return cellValue.getNumberValue();
            case STRING: return cellValue.getStringValue();
            case BOOLEAN: return cellValue.getBooleanValue();
            default: return "";
        }
    }

}