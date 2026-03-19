package copel.sesproductpackage.core.api.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class ExternalFileLinkLotTest {

  private HttpClient mockHttpClient;

  @BeforeEach
  void setUp() {
    mockHttpClient = mock(HttpClient.class);
  }

  @Test
  void testConstructor_NullText_EmptyList() {
    ExternalFileLinkLot lot = new ExternalFileLinkLot(null);
    assertEquals(0, lot.size());
  }

  @Test
  void testConstructor_BlankText_EmptyList() {
    ExternalFileLinkLot lot = new ExternalFileLinkLot("   ");
    assertEquals(0, lot.size());
  }

  @Test
  void testConstructor_NoUrl_EmptyList() {
    ExternalFileLinkLot lot = new ExternalFileLinkLot("URLが含まれないテキストです。");
    assertEquals(0, lot.size());
  }

  @Test
  void testConstructor_SingleUrl_SizeOne() {
    ExternalFileLinkLot lot =
        new ExternalFileLinkLot("スキルシートのURL：https://drive.google.com/file/d/TESTID/view");
    assertEquals(1, lot.size());
  }

  @Test
  void testConstructor_MultipleUrls_ExtractsAll() {
    String text =
        "１つ目：https://drive.google.com/file/d/ABC/view\n"
            + "２つ目：https://www.dropbox.com/s/HASH/skill.xlsx?dl=0";
    ExternalFileLinkLot lot = new ExternalFileLinkLot(text);
    assertEquals(2, lot.size());
  }

  @Test
  void testHasDownloadableLinks_EmptyList_ReturnsFalse() {
    ExternalFileLinkLot lot = new ExternalFileLinkLot(null);
    assertFalse(lot.hasDownloadableLinks());
  }

  @Test
  void testHasDownloadableLinks_UnsupportedUrl_ReturnsFalse() throws Exception {
    ExternalFileLinkLot lot =
        new ExternalFileLinkLot("サポートされていないURL：https://www.example.com/page.html です。");
    // This will hit the unresolved/unsupported branch in hasDownloadableLinks
    assertFalse(lot.hasDownloadableLinks());
  }

  @Test
  void testDownloadAll_EmptyList_NoOp() {
    ExternalFileLinkLot lot = new ExternalFileLinkLot(null);
    assertDoesNotThrow(lot::downloadAll);
    assertEquals(0, lot.getDownloadedLinks().size());
  }

  @Test
  void testDownloadAll_NonDownloadableUrl_SkipsAll() {
    ExternalFileLinkLot lot = new ExternalFileLinkLot("テスト https://www.example.com/no-support");
    assertDoesNotThrow(lot::downloadAll);
    assertEquals(0, lot.getDownloadedLinks().size());
  }

  @Test
  void testGetDownloadedLinks_NothingDownloaded_ReturnsEmpty() {
    ExternalFileLinkLot lot = new ExternalFileLinkLot(null);
    assertTrue(lot.getDownloadedLinks().isEmpty());
  }

  @Test
  void testSize_EmptyText_ReturnsZero() {
    ExternalFileLinkLot lot = new ExternalFileLinkLot("");
    assertEquals(0, lot.size());
  }

  @Test
  void testSize_TextWithUrls_ReturnsCount() {
    String text = "https://drive.google.com/file/d/A/view と https://1drv.ms/b/s!B 等が含まれます。";
    ExternalFileLinkLot lot = new ExternalFileLinkLot(text);
    assertEquals(2, lot.size());
  }

  @Test
  void testUrlPattern_MatchesHttpAndHttps() {
    String text = "http://example.com/file.pdf と https://secure.com/doc.docx";
    ExternalFileLinkLot lot = new ExternalFileLinkLot(text);
    assertEquals(2, lot.size());
  }

  @Test
  void testUrlPattern_DoesNotMatchFtp() {
    ExternalFileLinkLot lot = new ExternalFileLinkLot("ftp://example.com/file.pdf");
    assertEquals(0, lot.size());
  }

  private HttpResponse<Void> createMockVoidResponse(final String resolvedUrl) {
    HttpResponse<Void> response = (HttpResponse<Void>) mock(HttpResponse.class);
    when(response.uri()).thenReturn(URI.create(resolvedUrl));
    return response;
  }
}
