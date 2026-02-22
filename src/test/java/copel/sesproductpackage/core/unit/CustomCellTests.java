package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.junit.jupiter.api.Test;

class CustomCellTests {

  @Test
  void testGetValueString() {
    Cell cell = mock(Cell.class);
    when(cell.getCellType()).thenReturn(CellType.STRING);
    when(cell.getStringCellValue()).thenReturn("Hello");

    CustomCell customCell = new CustomCell(cell);
    assertEquals("Hello", customCell.getValue(null));
  }

  @Test
  void testGetValueBoolean() {
    Cell cell = mock(Cell.class);
    when(cell.getCellType()).thenReturn(CellType.BOOLEAN);
    when(cell.getBooleanCellValue()).thenReturn(true);

    CustomCell customCell = new CustomCell(cell);
    assertEquals("true", customCell.getValue(null));
  }

  @Test
  void testGetValueNumeric() {
    Cell cell = mock(Cell.class);
    when(cell.getCellType()).thenReturn(CellType.NUMERIC);
    when(cell.getNumericCellValue()).thenReturn(123.45);

    CustomCell customCell = new CustomCell(cell);
    assertEquals("123.45", customCell.getValue(null));
  }

  @Test
  void testGetValueDate() {
    Cell cell = mock(Cell.class);
    when(cell.getCellType()).thenReturn(CellType.NUMERIC);
    // エクセル上の日付シリアル値(2025/04/01 あたりを想定)
    when(cell.getNumericCellValue()).thenReturn(45748.0);
    Date date = new Date();
    when(cell.getDateCellValue()).thenReturn(date);

    CustomCell customCell = new CustomCell(cell);
    String expected = new SimpleDateFormat("yyyy/MM/dd").format(date);
    assertEquals(expected, customCell.getValue(null));
  }

  @Test
  void testGetValueFormula() {
    Cell cell = mock(Cell.class);
    FormulaEvaluator evaluator = mock(FormulaEvaluator.class);
    when(cell.getCellType()).thenReturn(CellType.FORMULA);

    CellValue cellValue = new CellValue("Result");
    when(evaluator.evaluate(cell)).thenReturn(cellValue);

    CustomCell customCell = new CustomCell(cell);
    assertEquals("\"Result\"", customCell.getValue(evaluator)); // formatAsString はダブルクォートで囲まれる
  }

  @Test
  void testGetValueFormulaException() {
    Cell cell = mock(Cell.class);
    FormulaEvaluator evaluator = mock(FormulaEvaluator.class);
    when(cell.getCellType()).thenReturn(CellType.FORMULA);
    when(cell.toString()).thenReturn("=A1+B1");

    when(evaluator.evaluate(cell)).thenThrow(new NotImplementedException("Not implemented"));

    CustomCell customCell = new CustomCell(cell);
    assertEquals("=A1+B1", customCell.getValue(evaluator));
  }

  @Test
  void testIsExcelDate() {
    Cell cell = mock(Cell.class);
    CustomCell customCell = new CustomCell(cell);

    when(cell.getNumericCellValue()).thenReturn(45000.0);
    assertTrue(customCell.isExcelDate());

    when(cell.getNumericCellValue()).thenReturn(0.5);
    assertFalse(customCell.isExcelDate());

    when(cell.getNumericCellValue()).thenReturn(45000.5);
    assertFalse(customCell.isExcelDate());
  }
}
