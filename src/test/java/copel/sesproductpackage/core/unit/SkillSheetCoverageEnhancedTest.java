package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.api.gpt.GptAnswer;
import copel.sesproductpackage.core.api.gpt.Transformer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

class SkillSheetCoverageEnhancedTest {

  @Test
  void testDocxProcessing() throws Exception {
    SkillSheet ss = new SkillSheet();
    ss.setFileName("test.docx");

    try (XWPFDocument doc = new XWPFDocument()) {
      XWPFParagraph p = doc.createParagraph();
      p.createRun().setText("Hello Docx");
      XWPFTable table = doc.createTable(1, 1);
      table.getRow(0).getCell(0).setText("CellData");

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      doc.write(out);
      ss.setFileContentFromByte(out.toByteArray());
    }

    assertTrue(ss.getFileContent().contains("Hello Docx"));
    assertTrue(ss.getFileContent().contains("CellData"));
  }

  @Test
  void testXlsxProcessing() throws Exception {
    SkillSheet ss = new SkillSheet();
    ss.setFileName("test.xlsx");

    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet();
      Row row = sheet.createRow(0);
      row.createCell(0).setCellValue("ExcelData");

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      wb.write(out);
      ss.setFileContentFromByte(out.toByteArray());
    }

    assertTrue(ss.getFileContent().contains("ExcelData"));
  }

  @Test
  void testXlsProcessing() throws Exception {
    SkillSheet ss = new SkillSheet();
    ss.setFileName("test.xls");

    try (Workbook wb = new HSSFWorkbook()) {
      Sheet sheet = wb.createSheet();
      Row row = sheet.createRow(0);
      row.createCell(0).setCellValue("HSSFData");

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      wb.write(out);
      ss.setFileContentFromByte(out.toByteArray());
    }

    assertTrue(ss.getFileContent().contains("HSSFData"));
  }

  @Test
  void testUnsupportedFormatAndNulls() throws Exception {
    SkillSheet ss = new SkillSheet();
    ss.setFileName("test.txt");
    ss.setFileContentFromByte(new byte[] {1, 2, 3});
    assertNull(ss.getFileContent());

    ss.setFileName(null);
    ss.setFileContentFromByte(new byte[] {1});
    assertNull(ss.getFileContent());
  }

  @Test
  void testGenerateSummary() throws Exception {
    SkillSheet ss = new SkillSheet();
    Transformer transformer = mock(Transformer.class);

    // Null content case
    assertThrows(IOException.class, () -> ss.generateSummary(transformer));

    // Success case
    ss.setFileContent("Some content");
    GptAnswer answer = mock(GptAnswer.class);
    when(answer.getAnswer())
        .thenReturn(
            "Summary text that is hopefully long enough to test the substring logic if needed, but 1000 chars is a lot.");
    when(transformer.generate(anyString())).thenReturn(answer);

    ss.generateSummary(transformer);
    assertEquals(
        "Summary text that is hopefully long enough to test the substring logic if needed, but 1000 chars is a lot.",
        ss.getFileContentSummary());
  }

  @Test
  void testGetFileUrl() {
    SkillSheet ss = new SkillSheet();
    ss.setFileName("test.docx");
    assertNotNull(ss.getFileUrl());

    ss = new SkillSheet(null, null, null);
    assertNotNull(ss.getFileUrl());
    assertTrue(ss.getFileUrl().contains("null_null"));
  }
}
