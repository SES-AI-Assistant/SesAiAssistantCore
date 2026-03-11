package copel.sesproductpackage.core.api.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class ExternalFileLinkTests {

  private HttpClient mockHttpClient;

  @BeforeEach
  void setUp() {
    mockHttpClient = mock(HttpClient.class);
  }

  // ============================================================
  // isDownloadable() テスト
  // ============================================================

  @Test
  void testIsDownloadable_GoogleDrive_ReturnsTrue() throws Exception {
    HttpResponse<Void> mockResponse =
        createMockVoidResponse("https://drive.google.com/file/d/TESTID/view");
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    ExternalFileLink link =
        new ExternalFileLink("https://drive.google.com/file/d/TESTID/view", mockHttpClient);
    assertTrue(link.isDownloadable());
    // directUrl が変換されているか確認
    assertTrue(link.getDirectUrl().contains("uc?export=download&id=TESTID"));
  }

  @Test
  void testIsDownloadable_GoogleSheets_ReturnsTrue() throws Exception {
    HttpResponse<Void> mockResponse =
        createMockVoidResponse("https://docs.google.com/spreadsheets/d/SHEETID/edit");
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    ExternalFileLink link =
        new ExternalFileLink("https://docs.google.com/spreadsheets/d/SHEETID/edit", mockHttpClient);
    assertTrue(link.isDownloadable());
    assertTrue(link.getDirectUrl().contains("/export?format=xlsx"));
  }

  @Test
  void testIsDownloadable_GoogleDocs_ReturnsTrue() throws Exception {
    HttpResponse<Void> mockResponse =
        createMockVoidResponse("https://docs.google.com/document/d/DOCID/edit");
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    ExternalFileLink link =
        new ExternalFileLink("https://docs.google.com/document/d/DOCID/edit", mockHttpClient);
    assertTrue(link.isDownloadable());
    assertTrue(link.getDirectUrl().contains("/export?format=docx"));
  }

  @Test
  void testIsDownloadable_GoogleSlides_ReturnsTrue() throws Exception {
    HttpResponse<Void> mockResponse =
        createMockVoidResponse("https://docs.google.com/presentation/d/SLIDEID/edit");
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    ExternalFileLink link =
        new ExternalFileLink("https://docs.google.com/presentation/d/SLIDEID/edit", mockHttpClient);
    assertTrue(link.isDownloadable());
    assertTrue(link.getDirectUrl().contains("/export?format=pdf"));
  }

  @Test
  void testIsDownloadable_Dropbox_ReturnsTrue() throws Exception {
    HttpResponse<Void> mockResponse =
        createMockVoidResponse("https://www.dropbox.com/s/HASH/file.xlsx?dl=0");
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    ExternalFileLink link =
        new ExternalFileLink("https://www.dropbox.com/s/HASH/file.xlsx?dl=0", mockHttpClient);
    assertTrue(link.isDownloadable());
    assertTrue(link.getDirectUrl().endsWith("?dl=1"));
  }

  @Test
  void testIsDownloadable_DropboxScl_ReturnsTrue() throws Exception {
    HttpResponse<Void> mockResponse =
        createMockVoidResponse("https://www.dropbox.com/scl/fi/HASH/file.xlsx?dl=0");
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    ExternalFileLink link =
        new ExternalFileLink("https://www.dropbox.com/scl/fi/HASH/file.xlsx?dl=0", mockHttpClient);
    assertTrue(link.isDownloadable());
    assertTrue(link.getDirectUrl().endsWith("?dl=1"));
  }

  @Test
  void testIsDownloadable_OneDrive_ReturnsTrue() throws Exception {
    HttpResponse<Void> mockResponse = createMockVoidResponse("https://1drv.ms/b/s!ABCDEF");
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    ExternalFileLink link = new ExternalFileLink("https://1drv.ms/b/s!ABCDEF", mockHttpClient);
    assertTrue(link.isDownloadable());
    assertTrue(link.getDirectUrl().contains("download=1"));
  }

  @Test
  void testIsDownloadable_SharePoint_ReturnsTrue() throws Exception {
    HttpResponse<Void> mockResponse =
        createMockVoidResponse("https://company.sharepoint.com/sites/shared/file.xlsx");
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    ExternalFileLink link =
        new ExternalFileLink(
            "https://company.sharepoint.com/sites/shared/file.xlsx", mockHttpClient);
    assertTrue(link.isDownloadable());
    assertTrue(link.getDirectUrl().contains("download=1"));
  }

  @Test
  void testIsDownloadable_SharePoint_WithQueryString_AppendsAmpersand() throws Exception {
    HttpResponse<Void> mockResponse =
        createMockVoidResponse("https://company.sharepoint.com/sites/shared/file.xlsx?param=1");
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    ExternalFileLink link =
        new ExternalFileLink(
            "https://company.sharepoint.com/sites/shared/file.xlsx?param=1", mockHttpClient);
    assertTrue(link.isDownloadable());
    assertTrue(link.getDirectUrl().contains("&download=1"));
  }

  @Test
  void testIsDownloadable_UnsupportedUrl_ReturnsFalse() throws Exception {
    HttpResponse<Void> mockResponse = createMockVoidResponse("https://www.example.com/page.html");
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    ExternalFileLink link =
        new ExternalFileLink("https://www.example.com/page.html", mockHttpClient);
    assertFalse(link.isDownloadable());
  }

  @Test
  void testIsDownloadable_NullUrl_ReturnsFalse() {
    ExternalFileLink link = new ExternalFileLink(null, mockHttpClient);
    assertFalse(link.isDownloadable());
  }

  @Test
  void testIsDownloadable_BlankUrl_ReturnsFalse() {
    ExternalFileLink link = new ExternalFileLink("", mockHttpClient);
    assertFalse(link.isDownloadable());
  }

  @Test
  void testIsDownloadable_ExceptionThrown_ReturnsFalse() throws Exception {
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new IOException("network error"));

    ExternalFileLink link =
        new ExternalFileLink("https://drive.google.com/file/d/ID/view", mockHttpClient);
    assertFalse(link.isDownloadable());
  }

  @Test
  void testIsDownloadable_InterruptedException_ReturnsFalse() throws Exception {
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new InterruptedException("interrupted"));

    ExternalFileLink link =
        new ExternalFileLink("https://drive.google.com/file/d/ID/view", mockHttpClient);
    assertFalse(link.isDownloadable());
  }

  // ============================================================
  // download() テスト
  // ============================================================

  @Test
  void testDownload_Success() throws Exception {
    // isDownloadable()をまず成功させる
    HttpResponse<Void> headResponse =
        createMockVoidResponse("https://drive.google.com/file/d/TESTID/view");
    when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.discarding())))
        .thenReturn(headResponse);

    byte[] expectedData = "PDF content".getBytes();
    HttpResponse<byte[]> getResponse =
        createMockByteResponse(
            200, expectedData, "application/pdf", "attachment; filename=\"skill.pdf\"");
    when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofByteArray())))
        .thenReturn(getResponse);

    ExternalFileLink link =
        new ExternalFileLink("https://drive.google.com/file/d/TESTID/view", mockHttpClient);
    link.isDownloadable();
    link.download();

    assertNotNull(link.getFileData());
    assertArrayEquals(expectedData, link.getFileData());
    assertEquals("skill.pdf", link.getFileName());
  }

  @Test
  void testDownload_HttpError_ThrowsIOException() throws Exception {
    HttpResponse<Void> headResponse =
        createMockVoidResponse("https://drive.google.com/file/d/TESTID/view");
    when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.discarding())))
        .thenReturn(headResponse);

    HttpResponse<byte[]> getResponse = createMockByteResponse(403, new byte[0], "text/html", "");
    when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofByteArray())))
        .thenReturn(getResponse);

    ExternalFileLink link =
        new ExternalFileLink("https://drive.google.com/file/d/TESTID/view", mockHttpClient);
    link.isDownloadable();
    assertThrows(IOException.class, link::download);
  }

  @Test
  void testDownload_HtmlContentType_ThrowsIOException() throws Exception {
    HttpResponse<Void> headResponse =
        createMockVoidResponse("https://drive.google.com/file/d/TESTID/view");
    when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.discarding())))
        .thenReturn(headResponse);

    HttpResponse<byte[]> getResponse =
        createMockByteResponse(
            200, "<html>Confirm</html>".getBytes(), "text/html; charset=utf-8", "");
    when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofByteArray())))
        .thenReturn(getResponse);

    ExternalFileLink link =
        new ExternalFileLink("https://drive.google.com/file/d/TESTID/view", mockHttpClient);
    link.isDownloadable();
    assertThrows(IOException.class, link::download);
  }

  @Test
  void testDownload_FileSizeExceedsLimit_ThrowsIOException() throws Exception {
    HttpResponse<Void> headResponse =
        createMockVoidResponse("https://drive.google.com/file/d/TESTID/view");
    when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.discarding())))
        .thenReturn(headResponse);

    // 21MB のダミーデータ
    byte[] largeData = new byte[(int) (ExternalFileLink.MAX_FILE_SIZE_BYTES + 1)];
    HttpResponse<byte[]> getResponse =
        createMockByteResponse(200, largeData, "application/pdf", "");
    when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofByteArray())))
        .thenReturn(getResponse);

    ExternalFileLink link =
        new ExternalFileLink("https://drive.google.com/file/d/TESTID/view", mockHttpClient);
    link.isDownloadable();
    assertThrows(IOException.class, link::download);
  }

  @Test
  void testDownload_DirectUrlNotSet_ThrowsIOException() {
    ExternalFileLink link =
        new ExternalFileLink("https://drive.google.com/file/d/TESTID/view", mockHttpClient);
    // isDownloadable() を呼ばずにdownload()
    assertThrows(IOException.class, link::download);
  }

  // ============================================================
  // convertToDirectUrl() テスト
  // ============================================================

  @Test
  void testConvertToDirectUrl_Null_ReturnsNull() {
    ExternalFileLink link = new ExternalFileLink("https://example.com", mockHttpClient);
    assertNull(link.convertToDirectUrl(null));
  }

  @Test
  void testConvertToDirectUrl_UnknownDomain_ReturnsSameUrl() {
    ExternalFileLink link = new ExternalFileLink("https://example.com", mockHttpClient);
    assertEquals(
        "https://example.com/file.pdf", link.convertToDirectUrl("https://example.com/file.pdf"));
  }

  // ============================================================
  // isSupportedDomain() テスト
  // ============================================================

  @Test
  void testIsSupportedDomain_NullUrl_ReturnsFalse() {
    ExternalFileLink link = new ExternalFileLink("https://example.com", mockHttpClient);
    assertFalse(link.isSupportedDomain(null));
  }

  @Test
  void testIsSupportedDomain_AllSupportedDomains() {
    ExternalFileLink link = new ExternalFileLink("https://example.com", mockHttpClient);
    assertTrue(link.isSupportedDomain("https://drive.google.com/file/d/ID/view"));
    assertTrue(link.isSupportedDomain("https://docs.google.com/spreadsheets/d/ID/edit"));
    assertTrue(link.isSupportedDomain("https://www.dropbox.com/s/HASH/file.xlsx"));
    assertTrue(link.isSupportedDomain("https://company.sharepoint.com/file.xlsx"));
    assertTrue(link.isSupportedDomain("https://1drv.ms/b/s!ABC"));
    assertFalse(link.isSupportedDomain("https://www.example.com/file.pdf"));
  }

  // ============================================================
  // extractFileName() テスト
  // ============================================================

  @Test
  void testExtractFileName_FromContentDisposition_Quoted() throws Exception {
    ExternalFileLink link =
        new ExternalFileLink("https://drive.google.com/file/d/ID/view", mockHttpClient);
    HttpResponse<byte[]> response =
        createMockByteResponse(
            200, new byte[0], "application/pdf", "attachment; filename=\"mySkillSheet.pdf\"");
    assertEquals("mySkillSheet.pdf", link.extractFileName(response, "https://example.com/file"));
  }

  @Test
  void testExtractFileName_FromContentDisposition_Rfc5987() throws Exception {
    ExternalFileLink link =
        new ExternalFileLink("https://drive.google.com/file/d/ID/view", mockHttpClient);
    HttpResponse<byte[]> response =
        createMockByteResponse(
            200,
            new byte[0],
            "application/pdf",
            "attachment; filename*=UTF-8''%E3%82%B9%E3%82%AD%E3%83%AB.pdf");
    String name = link.extractFileName(response, "https://example.com/file");
    assertFalse(name.isBlank());
  }

  @Test
  void testExtractFileName_FromUrlPath() throws Exception {
    ExternalFileLink link =
        new ExternalFileLink("https://drive.google.com/file/d/ID/view", mockHttpClient);
    HttpResponse<byte[]> response = createMockByteResponse(200, new byte[0], "application/pdf", "");
    assertEquals(
        "resume.pdf", link.extractFileName(response, "https://example.com/path/resume.pdf"));
  }

  @Test
  void testExtractFileName_FallbackToTimestamp() throws Exception {
    ExternalFileLink link =
        new ExternalFileLink("https://drive.google.com/file/d/ID/view", mockHttpClient);
    HttpResponse<byte[]> response = createMockByteResponse(200, new byte[0], "application/pdf", "");
    // パスが空(エントリがルート"/")となるURLでフォールバックをテスト
    String name = link.extractFileName(response, "https://example.com/");
    assertTrue(name.startsWith("skillsheet_"));
  }

  @Test
  void testExtractFileName_FallbackWhenInvalidUri() throws Exception {
    ExternalFileLink link =
        new ExternalFileLink("https://drive.google.com/file/d/ID/view", mockHttpClient);
    HttpResponse<byte[]> response = createMockByteResponse(200, new byte[0], "application/pdf", "");
    // URLにスペースが含まれるなどURIパースが失敗するケース
    String name = link.extractFileName(response, "not a valid url with spaces");
    assertTrue(name.startsWith("skillsheet_"));
  }

  // ============================================================
  // extractId() テスト
  // ============================================================

  @Test
  void testExtractId_Found() {
    ExternalFileLink link = new ExternalFileLink("https://example.com", mockHttpClient);
    assertEquals(
        "TESTID123",
        link.extractId("https://drive.google.com/file/d/TESTID123/view", "/file/d/([^/?]+)"));
  }

  @Test
  void testExtractId_NotFound_ReturnsEmpty() {
    ExternalFileLink link = new ExternalFileLink("https://example.com", mockHttpClient);
    assertEquals("", link.extractId("https://example.com/no-match", "/file/d/([^/?]+)"));
  }

  // ============================================================
  // getFileData() クローン確認
  // ============================================================

  @Test
  void testGetFileData_Null_ReturnsNull() {
    ExternalFileLink link = new ExternalFileLink("https://example.com", mockHttpClient);
    assertNull(link.getFileData());
  }

  // ============================================================
  // ゲッター確認
  // ============================================================

  @Test
  void testGetRawUrl() {
    ExternalFileLink link = new ExternalFileLink("https://example.com/raw", mockHttpClient);
    assertEquals("https://example.com/raw", link.getRawUrl());
  }

  @Test
  void testGetFileName_Null() {
    ExternalFileLink link = new ExternalFileLink("https://example.com", mockHttpClient);
    assertNull(link.getFileName());
  }

  @Test
  void testGetResolvedUrl_BeforeDownloadable_Null() {
    ExternalFileLink link = new ExternalFileLink("https://example.com", mockHttpClient);
    assertNull(link.getResolvedUrl());
  }

  // ============================================================
  // ヘルパーメソッド
  // ============================================================

  private HttpResponse<Void> createMockVoidResponse(final String resolvedUrl) {
    HttpResponse<Void> response = mock(HttpResponse.class);
    when(response.uri()).thenReturn(URI.create(resolvedUrl));
    return response;
  }

  private HttpResponse<byte[]> createMockByteResponse(
      final int statusCode, final byte[] body, final String contentType, final String disposition) {
    HttpResponse<byte[]> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(statusCode);
    when(response.body()).thenReturn(body);

    Map<String, List<String>> headersMap = new java.util.HashMap<>();
    if (!contentType.isBlank()) {
      headersMap.put("Content-Type", List.of(contentType));
    }
    if (!disposition.isBlank()) {
      headersMap.put("Content-Disposition", List.of(disposition));
    }
    HttpHeaders headers = HttpHeaders.of(headersMap, (k, v) -> true);
    when(response.headers()).thenReturn(headers);
    when(response.uri()).thenReturn(URI.create("https://example.com/file"));
    return response;
  }
}
