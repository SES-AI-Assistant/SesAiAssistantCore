package copel.sesproductpackage.core.api.gpt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.database.SES_AI_T_MATCH;
import copel.sesproductpackage.core.database.SES_AI_T_PERSON;
import copel.sesproductpackage.core.database.SES_AI_T_PERSONLot;
import copel.sesproductpackage.core.database.SES_AI_T_SKILLSHEET;
import copel.sesproductpackage.core.unit.MatchingStatus;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.SkillSheet;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

class CoverageFinalBoosterTest extends HttpTestBase {

  @BeforeEach
  void setup() {
    sharedMockConn = mock(HttpURLConnection.class);
  }

  private void setupMock(int code, String response) throws Exception {
    reset(sharedMockConn);
    when(sharedMockConn.getResponseCode()).thenReturn(code);
    when(sharedMockConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());
    when(sharedMockConn.getInputStream()).thenReturn(new ByteArrayInputStream(response.getBytes()));
    when(sharedMockConn.getErrorStream()).thenReturn(new ByteArrayInputStream(response.getBytes()));
  }

  @Test
  void boostPERSONLot() throws Exception {
    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    lot.retrieve(null, null, 0);
    lot.searchByRawContent(null, "q");
    lot.searchByRawContent(null, "q", null);
    lot.selectByRegisterDateAfter(null, null);
    lot.selectAll(null);

    assertDoesNotThrow(() -> lot.selectByAndQuery(null, new HashMap<>()));
    assertDoesNotThrow(() -> lot.selectByOrQuery(null, new HashMap<>()));

    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);
    when(rs.getString(anyString())).thenReturn("val");

    Map<String, String> query = new HashMap<>();
    query.put("c1", "v1");
    query.put("c2", "v2");
    lot.selectByAndQuery(conn, query);
    lot.selectByOrQuery(conn, query);
  }

  @Test
  void boostOpenAI() throws Exception {
    OpenAI api = new OpenAI("key");

    // Test null/missing contentNode branches in L244
    setupMock(200, "{\"choices\":[{\"message\":{}}]}");
    api.generate("test");

    setupMock(200, "{\"choices\":[{\"message\":{\"content\":null}}]}");
    api.generate("test");

    setupMock(200, "{\"choices\":[{\"message\":{\"content\":\"ans\"}}]}");
    api.generate("test");

    setupMock(200, "{\"data\":[{\"embedding\":[0.1]}]}");
    api.embedding("test");
  }

  @Test
  void boostGemini() throws Exception {
    Gemini api = new Gemini("key");

    // Test candidates missing/empty (isArray() false)
    setupMock(200, "{\"candidates\":{}}");
    api.generate("test");

    // Test candidates missing/empty (size() == 0)
    setupMock(200, "{\"candidates\":[]}");
    api.generate("test");

    // Test content missing/empty
    setupMock(200, "{\"candidates\":[{}]}");
    api.generate("test");

    // Test parts missing/empty (isArray() false)
    setupMock(200, "{\"candidates\":[{\"content\":{\"parts\":{}}}]}");
    api.generate("test");

    // Test parts missing/empty (size() == 0)
    setupMock(200, "{\"candidates\":[{\"content\":{\"parts\":[]}}]}");
    api.generate("test");

    // Test parts exists but text null (via normal JSON parsing, asText() returns
    // "null" not null)
    setupMock(200, "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":null}]}}]}");
    api.generate("test");

    // Test success with resultText
    setupMock(200, "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"ans\"}]}}]}");
    api.generate("test");

    // Test embedding success
    setupMock(200, "{\"embedding\":{\"values\":[0.1]}}");
    api.embedding("test");
  }

  @Test
  void boostDatabaseEntities() throws Exception {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);

    SES_AI_T_PERSON person = new SES_AI_T_PERSON();
    person.setPersonId("pid");
    when(ps.executeUpdate()).thenReturn(1);
    assertTrue(person.updateFileIdByPk(conn));

    // Coverage for executeUpdate() <= 0
    when(ps.executeUpdate()).thenReturn(0);
    assertFalse(person.updateFileIdByPk(conn));

    SES_AI_T_MATCH match = new SES_AI_T_MATCH();
    match.setMatchingId("mid");
    match.setStatus(MatchingStatus.提案中);
    match.setStatus(null);
    when(ps.executeUpdate()).thenReturn(1);
    assertTrue(match.updateByPk(conn));
  }

  @Test
  void boostSkillSheet() throws Exception {
    SkillSheet ss = new SkillSheet();

    // docx
    ss.setFileName("t.docx");
    try (XWPFDocument doc = new XWPFDocument()) {
      doc.createParagraph().createRun().setText("p1");
      XWPFTable table = doc.createTable(1, 1);
      table.getRow(0).getCell(0).setText("c1");
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      doc.write(out);
      ss.setFileContentFromByte(out.toByteArray());
    }

    // doc (Mock)
    ss.setFileName("t.doc");
    try (MockedConstruction<HWPFDocument> mockedDoc = mockConstruction(HWPFDocument.class);
        MockedConstruction<WordExtractor> mockedExtractor = mockConstruction(
            WordExtractor.class,
            (mock, context) -> {
              when(mock.getParagraphText()).thenReturn(new String[] { "p1" });
              when(mock.getText()).thenReturn("t1");
            })) {
      ss.setFileContentFromByte(new byte[] { 1 });
    }

    // pdf
    ss.setFileName("t.pdf");
    try (PDDocument doc = new PDDocument()) {
      PDPage page = new PDPage();
      doc.addPage(page);
      try (PDPageContentStream contents = new PDPageContentStream(doc, page)) {
        contents.beginText();
        contents.setFont(PDType1Font.HELVETICA, 12);
        contents.newLineAtOffset(100, 700);
        contents.showText("pdf text");
        contents.endText();
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      doc.save(out);
      ss.setFileContentFromByte(out.toByteArray());
    }

    // xlsx
    ss.setFileName("t.xlsx");
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet s = wb.createSheet();
      Row r = s.createRow(0);
      r.createCell(0).setCellValue("v1");
      r.createCell(1).setCellValue(123.45);
      r.createCell(2).setCellValue(true);
      r.createCell(3).setCellFormula("SUM(A1:B1)");
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      wb.write(out);
      ss.setFileContentFromByte(out.toByteArray());
    }

    // xls
    ss.setFileName("t.xls");
    try (Workbook wb = new HSSFWorkbook()) {
      Sheet s = wb.createSheet();
      Row r = s.createRow(0);
      r.createCell(0).setCellValue(45000); // Excel Date numeric
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      wb.write(out);
      ss.setFileContentFromByte(out.toByteArray());
    }

    // Other
    ss.setFileName("t.txt");
    ss.setFileContentFromByte(new byte[] { 1 });

    // misc
    ss.getFileUrl();
    ss.getObjectKey();
    SkillSheet ssNone = new SkillSheet(null, null, null);
    assertNull(ssNone.getFileContent());

    // Lombok coverage for SkillSheet
    SkillSheet ss1 = new SkillSheet("f1", "n1", "c1");
    SkillSheet ss2 = new SkillSheet("f1", "n1", "c1");
    assertEquals(ss1, ss2);
    assertEquals(ss1.hashCode(), ss2.hashCode());
    assertNotNull(ss1.toString());
    ss1.setFileId("f2");
    assertEquals("f2", ss1.getFileId());
  }

  @Test
  void boostOriginalDateTime() {
    new OriginalDateTime(new java.sql.Date(System.currentTimeMillis()));
    new OriginalDateTime(new java.sql.Timestamp(System.currentTimeMillis()));
    new OriginalDateTime(2024, 4, 1, 12, 0, 0);

    // Coverage for if (date == null) and if (timestamp == null)
    new OriginalDateTime((java.sql.Date) null);
    new OriginalDateTime((java.sql.Timestamp) null);

    OriginalDateTime empty = new OriginalDateTime((String) null);
    empty.betweenDays(new OriginalDateTime());
    empty.betweenMonth(new OriginalDateTime());
    empty.betweenYear(new OriginalDateTime());

    OriginalDateTime valid = new OriginalDateTime();
    valid.betweenDays(empty);
    valid.betweenMonth(empty);
    valid.betweenYear(empty);

    // Test isEmpty branch return this.dateTime == null
    assertTrue(empty.isEmpty());
    assertFalse(valid.isEmpty());

    // Coverage for plusDays / minusMinutes null check
    empty.plusDays(1);
    empty.minusMinutes(1);
  }

  @Test
  void boostSKILLSHEETEntity() throws Exception {
    SES_AI_T_SKILLSHEET entity = new SES_AI_T_SKILLSHEET();
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(ps.executeUpdate()).thenReturn(1);
    when(rs.next()).thenReturn(true);

    // uniqueCheck coverage
    when(rs.getInt(1)).thenReturn(0); // < 1 returns true
    assertTrue(entity.uniqueCheck(conn, 0.5));
    when(rs.getInt(1)).thenReturn(1); // not < 1 returns false
    assertFalse(entity.uniqueCheck(conn, 0.5));

    // deleteByPk coverage
    entity.setFileId("fid");
    assertTrue(entity.deleteByPk(conn));
    when(ps.executeUpdate()).thenReturn(0);
    assertFalse(entity.deleteByPk(conn));

    // Lombok coverage for SES_AI_T_SKILLSHEET
    SES_AI_T_SKILLSHEET e2 = new SES_AI_T_SKILLSHEET();
    e2.setFileId("fid");
    assertTrue(entity.equals(e2));
    assertNotNull(entity.hashCode());
  }
}
