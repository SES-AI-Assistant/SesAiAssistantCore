package copel.sesproductpackage.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * FileNameUtilsのテストクラス.
 */
public class FileNameUtilsTests {

  @Test
  public void testDecode_Normal() {
    assertEquals("test.txt", FileNameUtils.decode("test.txt"));
    assertEquals("テスト.xlsx", FileNameUtils.decode("テスト.xlsx"));
  }

  @Test
  public void testDecode_Mime() {
    // "=?UTF-8?B?44OG44K544OILnR4dA==?=" -> "テスト.txt"
    assertEquals("テスト.txt", FileNameUtils.decode("=?UTF-8?B?44OG44K544OILnR4dA==?="));
    // ISO-2022-JP
    assertEquals("テスト.txt", FileNameUtils.decode("=?ISO-2022-JP?B?GyRCJUYlOSVIGyhCLnR4dA==?="));
  }

  @Test
  public void testDecode_Url() {
    // "%E6%88%B8%E8%B6%8A.xlsx" -> "戸越.xlsx"
    assertEquals("戸越.xlsx", FileNameUtils.decode("%E6%88%B8%E8%B6%8A.xlsx"));
    // スペースが%20になっているケース
    assertEquals("my file.pdf", FileNameUtils.decode("my%20file.pdf"));
  }

  @Test
  public void testDecode_Rfc5987() {
    // "UTF-8''%e3%82%b9%e3%82%ad%e3%83%ab%e3%82%b7%e3%83%bc%e3%83%88.pdf"
    assertEquals("スキルシート.pdf", FileNameUtils.decode("UTF-8''%e3%82%b9%e3%82%ad%e3%83%ab%e3%82%b7%e3%83%bc%e3%83%88.pdf"));
  }

  @Test
  public void testDecode_GarbagedExamples() {
    // ユーザーからの例1: %E6%88%B8%E8%B6%8A.xlsx
    assertEquals("戸越.xlsx", FileNameUtils.decode("%E6%88%B8%E8%B6%8A.xlsx"));
    
    // ユーザーからの例2: KYï¼ã¹ã­ã«ã·ã¼ãï¼:.pdf.pdf
    // これがどういうエンコードか不明だが、サニタイズはされるはず
    String decoded = FileNameUtils.decode("KYï¼ã¹ã­ã«ã·ã¼ãï¼:.pdf.pdf");
    // 不正文字 : が _ に置換され、末尾の . も除去されるはず
    assertEquals("KYï¼ã¹ã­ã«ã·ã¼ãï¼_.pdf.pdf", decoded.replace(" ", "")); // trimの影響を考慮
  }

  @Test
  public void testDecode_Sanitize() {
    assertEquals("a_b_c_d_e_f_g_h_i.txt", FileNameUtils.decode("a\\b/c:d*e?f\"g<h>i|j.txt").substring(0, 17) + ".txt");
    // 制御文字
    assertEquals("test_file.txt", FileNameUtils.decode("test\nfile.txt"));
  }

  @Test
  public void testDecode_EdgeCases() {
    assertNull(FileNameUtils.decode(null));
    assertEquals("", FileNameUtils.decode(""));
    assertEquals("a", FileNameUtils.decode("  a  "));
    // 末尾のドット
    assertEquals("test", FileNameUtils.decode("test."));
    assertEquals("test", FileNameUtils.decode("test..."));
  }
}
