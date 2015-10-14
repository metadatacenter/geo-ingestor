package org.metadatacenter.ingestors.geo;

import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.IOUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class SSUtil
{
  public static final String XLS_EXTENSION = ".xls";
  public static final String XLSX_EXTENSION = ".xlsx";

  private static final short HIGHLIGHT_CELL_COLOR = IndexedColors.YELLOW.getIndex();

  public static InputStream openSpreadsheetInputStream(String fileName) throws GEOIngestorException
  {
    checkForXLSOrXLSXExtension(fileName);

    try {
      return new FileInputStream(fileName);
    } catch (IOException e) {
      throw new GEOIngestorException("Error opening spreadsheet " + fileName + ": " + e.getMessage(), e);
    }
  }

  public static void checkForXLSOrXLSXExtension(String fileName) throws GEOIngestorException
  {
    checkForExtension(fileName, XLS_EXTENSION, XLSX_EXTENSION);
  }

  public static void checkForExtension(String fileName, String... allowedExtensions) throws GEOIngestorException
  {
    String upperCase = fileName.toUpperCase(Locale.US);
    for (String extension : allowedExtensions) {
      if (upperCase.endsWith(extension.toUpperCase(Locale.US)))
        return;
    }

    StringBuilder prettyList = new StringBuilder();
    for (String extension : allowedExtensions) {
      prettyList.append(extension);
      prettyList.append(", ");
    }
    if (prettyList.length() > 0)
      prettyList.setLength(prettyList.length() - 2); // trim trailing ", "

    throw new GEOIngestorException(
      String.format("Invalid file extension for '%s'; expected one of: %s", fileName, prettyList));
  }

  public static OutputStream openSpreadsheetOutputStream(String fileName) throws GEOIngestorException
  {
    checkForXLSOrXLSXExtension(fileName);

    try {
      return new FileOutputStream(fileName);
    } catch (IOException e) {
      throw new GEOIngestorException("Error opening spreadsheet " + fileName + ": " + e.getMessage(), e);
    }
  }

  public static Workbook createWorkbook(InputStream inputStream) throws GEOIngestorException
  {
    try {
      return WorkbookFactory.create(inputStream);
    } catch (InvalidFormatException e) {
      throw new GEOIngestorException("Invalid format for workbook " + e.getMessage(), e);
    } catch (IOException e) {
      throw new GEOIngestorException("IO error opening workbook " + e.getMessage(), e);
    }
  }

  public static Workbook createReadonlyWorkbook(InputStream inputStream) throws GEOIngestorException
  {
    try {
      return WorkbookFactory.create(inputStream);
    } catch (InvalidFormatException e) {
      throw new GEOIngestorException("Invalid format for workbook " + e.getMessage(), e);
    } catch (IOException e) {
      throw new GEOIngestorException("IO error opening workbook " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  public static void writeWorkbook(Workbook workbook, String fileName) throws GEOIngestorException
  {
    OutputStream os = openSpreadsheetOutputStream(fileName);

    try {
      workbook.write(os);
    } catch (IOException e) {
      throw new GEOIngestorException("Error writing workbook to file " + fileName + ": " + e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(os);
    }
  }

  public static void highlightCell(Workbook workbook, String sheetName, int columnIndex, int rowNumber)
    throws GEOIngestorException
  {
    Sheet sheet = workbook.getSheet(sheetName);

    if (sheet == null)
      throw new GEOIngestorException("Internal error: no sheet " + sheetName + " in workbook");

    Row row = sheet.getRow(rowNumber);

    if (row == null)
      throw new GEOIngestorException("Internal error: no row " + rowNumber + " in sheet " + sheet.getSheetName());

    Cell cell = row.getCell(columnIndex);

    if (cell == null)
      throw new GEOIngestorException(
        "Internal error: no column " + columnIndex + " at row " + rowNumber + " in sheet " + sheet.getSheetName());

    // setFillBackgroundColor will not take unless setFillForegroundColor is called first!!!
    cell.getCellStyle().setFillForegroundColor(HIGHLIGHT_CELL_COLOR);
    cell.getCellStyle().setFillBackgroundColor(HIGHLIGHT_CELL_COLOR);
  }

  public static String convertIndex2ColString(int columnIndex)
  {
    return CellReference.convertNumToColString(columnIndex);
  }

  public static String convertLocation2String(String sheetName, int columnIndex, int rowIndex)
  {

    return "'" + sheetName + "'!" + convertIndex2ColString(columnIndex) + (rowIndex + 1);
  }

  public static String getCellLocation(Cell cell)
  {
    int columnIndex = cell.getColumnIndex();
    int rowIndex = cell.getRowIndex();

    return convertLocation2String(columnIndex, rowIndex);
  }

  public static String convertLocation2String(int columnIndex, int rowIndex)
  {
    return convertIndex2ColString(columnIndex) + (rowIndex + 1);
  }

  public static Sheet getSheet(Workbook workbook, String sheetName) throws GEOIngestorException
  {
    Sheet sheet = workbook.getSheet(sheetName);

    if (sheet == null)
      throw new GEOIngestorException("Invalid sheet " + sheetName);

    return sheet;
  }

  public static boolean getCellValueAsBoolean(Cell cell, String trueValue) throws GEOIngestorException
  {
    if (cell == null)
      return false;
    else if (isStringCellType(cell)) {
      String value = cell.getStringCellValue().trim();
      return value.equals(trueValue);
    } else if (isBooleanCellType(cell))
      return cell.getBooleanCellValue();
    else
      return false;
  }

  public static String getStringCellValue(Cell cell, String defaultValue) throws GEOIngestorException
  {
    if (cell == null)
      return defaultValue;
    else if (isStringCellType(cell))
      return cell.getStringCellValue();
    else
      return defaultValue;
  }

  public static String getStringCellValue(Cell cell) throws GEOIngestorException
  {
    assertNonNullCell(cell);
    assertStringCellType(cell);

    return cell.getStringCellValue();
  }

  public static void assertNonNullCell(Cell cell) throws GEOIngestorException
  {
    if (cell == null)
      throw new GEOIngestorException("No cell at location " + getCellLocation(cell));
  }

  public static void assertStringCellType(Cell cell) throws GEOIngestorException
  {
    if (!isStringCellType(cell))
      throw new GEOIngestorException(
        "Cell at location " + getCellLocation(cell) + " is not a string; type is " + getCellTypeName(cell));
  }

  public static double getNumericCellValue(Cell cell) throws GEOIngestorException
  {
    assertNumericCellType(cell);

    return cell.getNumericCellValue();
  }

  public static void assertNumericCellType(Cell cell) throws GEOIngestorException
  {
    if (!isNumericCellType(cell))
      throw new GEOIngestorException(
        "Cell at location " + getCellLocation(cell) + " is not a numeric; type is " + getCellTypeName(cell));
  }

  public static boolean isNumericCellType(Cell cell)
  {
    return cell.getCellType() == Cell.CELL_TYPE_NUMERIC;
  }

  public static boolean isBooleanCellType(Cell cell)
  {
    return cell.getCellType() == Cell.CELL_TYPE_BOOLEAN;
  }

  public static boolean isStringCellType(Cell cell)
  {
    return cell.getCellType() == Cell.CELL_TYPE_STRING;
  }

  public static String getCellTypeName(Cell cell)
  {
    if (cell == null)
      return "Cell is NULL";
    else {
      int celltype = cell.getCellType();

      if (celltype == Cell.CELL_TYPE_BLANK)
        return "CELL_TYPE_BLANK";
      else if (celltype == Cell.CELL_TYPE_FORMULA)
        return "CELL_TYPE_BLANK";
      else if (celltype == Cell.CELL_TYPE_STRING)
        return "CELL_TYPE_STRING";
      else if (celltype == Cell.CELL_TYPE_NUMERIC)
        return "CELL_TYPE_NUMERIC";
      else if (celltype == Cell.CELL_TYPE_BOOLEAN)
        return "CELL_TYPE_BOOLEAN";
      else if (celltype == Cell.CELL_TYPE_ERROR)
        return "CELL_TYPE_ERROR";
      else
        return "UNKNOWN CELL TYPE!";
    }
  }

  public static Optional<Integer> findFieldRowNumber(Sheet sheet, String fieldName, int fieldColumnNumber)
    throws GEOIngestorException
  {
    int firstRow = sheet.getFirstRowNum();
    int lastRow = sheet.getLastRowNum();

    for (int currentRow = firstRow; currentRow <= lastRow; currentRow++) {
      Row row = sheet.getRow(currentRow);
      Cell cell = row.getCell(fieldColumnNumber);
      String value = SSUtil.getStringCellValue(cell);
      if (fieldName.equals(value))
        return Optional.of(currentRow);
    }
    return Optional.empty();
  }

  /**
   * A field can have multiple values.
   */
  public static Map<String, List<String>> findFieldValues(Sheet sheet, int fieldNameColumnNumber,
    int fieldValueColumnNumber, int startRowNumber, int finishRowNumber) throws GEOIngestorException
  {
    Map<String, List<String>> field2Values = new HashMap<>();

    for (int currentRow = startRowNumber; currentRow <= finishRowNumber; currentRow++) {
      Row row = sheet.getRow(currentRow);
      Cell fieldNameCell = row.getCell(fieldNameColumnNumber);

      if (fieldNameCell == null)
        throw new GEOIngestorException("empty field name at location " + getCellLocation(fieldNameCell));

      Cell fieldValueCell = row.getCell(fieldValueColumnNumber);

      if (fieldValueCell == null)
        throw new GEOIngestorException("empty field value at location " + getCellLocation(fieldNameCell));

      String fieldName = SSUtil.getStringCellValue(fieldNameCell);
      if (fieldName.isEmpty())
        throw new GEOIngestorException("empty field name at location " + getCellLocation(fieldNameCell));

      String fieldValue = SSUtil.getStringCellValue(fieldNameCell);

      if (fieldValue.isEmpty())
        throw new GEOIngestorException("empty field value at location " + getCellLocation(fieldValueCell));

      if (field2Values.containsKey(fieldName))
        field2Values.get(fieldName).add(fieldValue);
      else {
        List<String> fieldValues = new ArrayList<>();
        fieldValues.add(fieldValue);
        field2Values.put(fieldName, fieldValues);
      }
    }
    return field2Values;
  }

  /**
   * Duplicates not allowed.
   */
  public static Map<String, String> findFieldValue(Sheet sheet, int fieldNameColumnNumber, int fieldValueColumnNumber,
    int startRowNumber, int finishRowNumber) throws GEOIngestorException
  {
    Map<String, String> field2Value = new HashMap<>();

    for (int currentRow = startRowNumber; currentRow <= finishRowNumber; currentRow++) {
      Row row = sheet.getRow(currentRow);
      Cell fieldNameCell = row.getCell(fieldNameColumnNumber);

      if (fieldNameCell == null)
        throw new GEOIngestorException("empty field name at location " + getCellLocation(fieldNameCell));

      Cell fieldValueCell = row.getCell(fieldValueColumnNumber);

      if (fieldValueCell == null)
        throw new GEOIngestorException("empty field value at location " + getCellLocation(fieldNameCell));

      String fieldName = SSUtil.getStringCellValue(fieldNameCell); // Check for null cell
      if (fieldName.isEmpty())
        throw new GEOIngestorException("empty field name at location " + getCellLocation(fieldNameCell));

      String fieldValue = SSUtil.getStringCellValue(fieldNameCell); // Check for null cell

      if (fieldValue.isEmpty())
        throw new GEOIngestorException("empty field value at location " + getCellLocation(fieldValueCell));

      if (field2Value.containsKey(fieldName))
        throw new GEOIngestorException(
          "duplicate field " + fieldName + " value at location " + getCellLocation(fieldValueCell));
      else {
        field2Value.put(fieldName, fieldValue);
      }
    }
    return field2Value;
  }
}
