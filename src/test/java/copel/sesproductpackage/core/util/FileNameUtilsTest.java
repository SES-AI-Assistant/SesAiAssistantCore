package copel.sesproductpackage.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for FileNameUtils.
 */
public class FileNameUtilsTest {

  @Test
  public void testDecode_Normal() {
    assertEquals("test.txt", FileNameUtils.decode("test.txt"));
    assertEquals("テスト.xlsx", FileNameUtils.decode("テスト.xlsx"));
  }

  @Test
  public void testDecode_Mime() {
    assertEquals("テスト.txt", FileNameUtils.decode("=?UTF-8?B?44OG44K544OILnR4dA==?="));
    assertEquals("テスト.txt", FileNameUtils.decode("=?ISO-2022-JP?B?GyRCJUYlOSVIGyhCLnR4dA==?="));
  }

  @Test
  public void testDecode_Url() {
    assertEquals("戸越.xlsx", FileNameUtils.decode("%E6%88%B8%E8%B6%8A.xlsx"));
    assertEquals("my file.pdf", FileNameUtils.decode("my%20file.pdf"));
  }

  @Test
  public void testDecode_Rfc5987() {
    assertEquals("スキルシート.pdf", FileNameUtils.decode("UTF-8''%e3%82%b9%e3%82%ad%e3%83%ab%e3%82%b7%e3%83%bc%e3%83%88.pdf"));
  }

  @Test
  public void testDecode_Sanitize() {
    String decoded = FileNameUtils.decode("a\\b/c:d*e?f\"g<h>i|j.txt");
    assertTrue(decoded.contains("_"));
    assertEquals("test_file.txt", FileNameUtils.decode("test\nfile.txt"));
  }

  @Test
  public void testDecode_EdgeCases() {
    assertNull(FileNameUtils.decode(null));
    assertEquals("", FileNameUtils.decode(""));
    assertEquals("a", FileNameUtils.decode("  a  "));
    assertEquals("test", FileNameUtils.decode("test."));
    assertEquals("test", FileNameUtils.decode("test..."));
  }
}
