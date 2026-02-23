package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.api.gpt.GptAnswer;
import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.util.Properties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.poifs.filesystem.NotOLE2FileException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SkillSheetTests {

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void setupProperties() throws Exception {
        Field propertiesField = Properties.class.getDeclaredField("properties");
        propertiesField.setAccessible(true);
        Map<String, String> propertiesMap = (Map<String, String>) propertiesField.get(null);
        propertiesMap.put("SKILLSHEET_SUMMARIZE_PROMPT", "Summarize: ");
        propertiesMap.put("SES_AI_T_SKILLSHEET_MAX_RAW_CONTENT_LENGTH", "100");
        propertiesMap.put("S3_BUCKET_NAME", "test-bucket");
    }
    
    @Test
    void testFileParsingExceptions() {
        SkillSheet ss = new SkillSheet();
        byte[] badData = "this is not a valid office file".getBytes();
        
        ss.setFileName("test.docx");
        assertThrows(NotOfficeXmlFileException.class, () -> ss.setFileContentFromByte(badData));

        ss.setFileName("test.doc");
        assertThrows(IllegalArgumentException.class, () -> ss.setFileContentFromByte(badData));

        ss.setFileName("test.pdf");
        assertThrows(IOException.class, () -> ss.setFileContentFromByte(badData));

        ss.setFileName("test.xlsx");
        assertThrows(NotOfficeXmlFileException.class, () -> ss.setFileContentFromByte(badData));

        ss.setFileName("test.xls");
        assertThrows(org.apache.poi.poifs.filesystem.NotOLE2FileException.class, () -> ss.setFileContentFromByte(badData));
    }

    @Test
    void testUnsupportedAndNulls() throws IOException {
        SkillSheet ss = new SkillSheet(); // This creates ss with fileContent = null
        ss.setFileId("id");
        ss.setFileName("test.txt");
        ss.setFileContentFromByte(new byte[]{1, 2, 3});
        assertNull(ss.getFileContent());

        // Create a new instance to ensure fileContent is null
        SkillSheet ss2 = new SkillSheet();
        ss2.setFileName(null);
        ss2.setFileContentFromByte(new byte[]{1, 2, 3});
        assertNull(ss2.getFileContent());

        SkillSheet ss3 = new SkillSheet();
        ss3.setFileName("test.docx");
        ss3.setFileContentFromByte(null);
        assertNull(ss3.getFileContent());
    }

    @Test
    void testSuccessfulParsing() throws IOException {
        try (XWPFDocument docx = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            docx.createParagraph().createRun().setText("Hello Docx");
            docx.write(out);
            SkillSheet ss = new SkillSheet();
            ss.setFileName("test.docx");
            ss.setFileContentFromByte(out.toByteArray());
            assertTrue(ss.getFileContent().contains("Hello Docx"));
        }
        
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            pdf.addPage(new PDPage());
            pdf.save(out);
            SkillSheet ss = new SkillSheet();
            ss.setFileName("test.pdf");
            ss.setFileContentFromByte(out.toByteArray());
            assertNotNull(ss.getFileContent());
        }

        try (Workbook xlsx = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            xlsx.createSheet("Sheet1").createRow(0).createCell(0).setCellValue("Hello Xlsx");
            xlsx.write(out);
            SkillSheet ss = new SkillSheet();
            ss.setFileName("test.xlsx");
            ss.setFileContentFromByte(out.toByteArray());
            assertTrue(ss.getFileContent().contains("Hello Xlsx"));
        }

        try (Workbook xls = new HSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            xls.createSheet("Sheet1").createRow(0).createCell(0).setCellValue("Hello Xls");
            xls.write(out);
            SkillSheet ss = new SkillSheet();
            ss.setFileName("test.xls");
            ss.setFileContentFromByte(out.toByteArray());
            assertTrue(ss.getFileContent().contains("Hello Xls"));
        }
    }

    @Test
    void testGenerateSummary() throws Exception {
        Transformer transformer = mock(Transformer.class);
        GptAnswer answer = mock(GptAnswer.class, RETURNS_DEEP_STUBS);
        when(answer.getAnswer()).thenReturn("a".repeat(1500));
        when(transformer.generate(anyString())).thenReturn(answer);

        SkillSheet ss = new SkillSheet("1", "t.txt", "Some Content");
        ss.generateSummary(transformer);
        assertEquals(1000, ss.getFileContentSummary().length());
        
        ss.setFileContent(null);
        assertThrows(IOException.class, () -> ss.generateSummary(transformer));
    }

    @Test
    void testGetters() throws Exception {
        SkillSheet ss = new SkillSheet("id1", "name1.pdf", "content");
        assertNotNull(ss.getFileUrl());
        assertEquals("id1_name1.pdf", ss.getObjectKey());
        
        Field field = SkillSheet.class.getDeclaredField("SES_AI_T_SKILLSHEET_MAX_RAW_CONTENT_LENGTH");
        field.setAccessible(true);
        int maxLength = (int) field.get(null);
        
        ss.setFileContent("a".repeat(maxLength + 10));
        assertEquals(maxLength - 1, ss.getFileContent().length());
        
        ss.setFileContent(null);
        assertNull(ss.getFileContent());
    }
}
