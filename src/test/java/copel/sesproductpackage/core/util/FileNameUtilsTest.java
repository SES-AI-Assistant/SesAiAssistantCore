package copel.sesproductpackage.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/** Unit tests for FileNameUtils. */
public class FileNameUtilsTest {

  @BeforeAll
  static void coverPrivateHelpersViaReflection() throws Exception {
    // decodeMimeText(text == null) 分岐を直接踏む（decode(null) だと手前で return してしまうため）
    java.lang.reflect.Method m =
        FileNameUtils.class.getDeclaredMethod("decodeMimeText", String.class);
    m.setAccessible(true);
    assertNull((String) m.invoke(null, new Object[] {null}));
  }

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
  public void testDecode_Mime_QuotedPrintable() {
    // "テ スト" を Q エンコード（_ はスペース扱い）
    assertEquals("テ スト.txt", FileNameUtils.decode("=?UTF-8?Q?=E3=83=86_=E3=82=B9=E3=83=88?=.txt"));
  }

  @Test
  public void testDecode_Mime_Invalid() {
    // 不正 Base64 の場合はデコード失敗し、元の MIME 文字列を保持する（例外を投げない）
    assertEquals("=_UTF-8_B_!!!!_=", FileNameUtils.decode("=?UTF-8?B?!!!!?="));

    // encoding が B/Q 以外のときもそのまま
    assertEquals("=_UTF-8_X_abc_=", FileNameUtils.decode("=?UTF-8?X?abc?="));
  }

  @Test
  public void testDecode_Url() {
    assertEquals("戸越.xlsx", FileNameUtils.decode("%E6%88%B8%E8%B6%8A.xlsx"));
    assertEquals("my file.pdf", FileNameUtils.decode("my%20file.pdf"));
    // 二重エンコード: %2520 -> %20 -> " "
    assertEquals("a b.txt", FileNameUtils.decode("a%2520b.txt"));
  }

  @Test
  public void testDecode_Rfc5987() {
    assertEquals(
        "スキルシート.pdf",
        FileNameUtils.decode("UTF-8''%e3%82%b9%e3%82%ad%e3%83%ab%e3%82%b7%e3%83%bc%e3%83%88.pdf"));
  }

  @Test
  public void testDecode_Rfc5987_EmptyCharset() {
    // charset == "" 分岐（UTF-8 を補完）を踏む
    assertEquals("a b.txt", FileNameUtils.decode("''a%20b.txt"));
  }

  @Test
  public void testDecode_Rfc5987_InvalidCharset() {
    // charset が不正な場合は value 部分をそのまま採用し、例外を投げない
    assertEquals("abc.txt", FileNameUtils.decode("BAD-CHARSET''abc.txt"));
  }

  @Test
  public void testDecode_Sanitize() {
    String decoded = FileNameUtils.decode("a\\b/c:d*e?f\"g<h>i|j.txt");
    assertTrue(decoded.contains("_"));
    assertEquals("test_file.txt", FileNameUtils.decode("test\nfile.txt"));
  }

  @Test
  public void testDecode_UrlDecodeFailureDoesNotThrow() {
    // 不正な % 形式（URLDecoder が例外を投げる）でも、そのまま返却されること
    assertEquals("%E3%", FileNameUtils.decode("%E3%"));
  }

  @Test
  public void testDecode_UrlDecodeNoChangeBranch() throws Exception {
    // decoded.equals(previous) の break 分岐を踏むため、URLDecoder.decode を「変化なし」で返すようモックする
    try (MockedStatic<URLDecoder> mocked = Mockito.mockStatic(URLDecoder.class)) {
      mocked
          .when(
              () ->
                  URLDecoder.decode(Mockito.anyString(), Mockito.eq(StandardCharsets.UTF_8.name())))
          .thenAnswer(inv -> inv.getArgument(0));
      assertEquals("%41", FileNameUtils.decode("%41"));
    }
  }

  @Test
  public void testDecode_Mime_RegexAllowsElseBranch() throws Exception {
    // MIME_PATTERN が [BQ] 固定のため通常は else に到達不能。テスト内で Pattern を差し替えて else 分岐を踏む。
    Field f = FileNameUtils.class.getDeclaredField("MIME_PATTERN");
    f.setAccessible(true);
    Pattern original = (Pattern) f.get(null);

    Pattern anyEncodingPattern = Pattern.compile("=\\?([^?]+)\\?([^?]+)\\?([^?]+)\\?=");
    try {
      putStaticFinalObject(f, anyEncodingPattern);
      assertEquals("=_UTF-8_X_abc_=.txt", FileNameUtils.decode("=?UTF-8?X?abc?=.txt"));
    } finally {
      putStaticFinalObject(f, original);
    }
  }

  @Test
  public void testDecode_Mime_QuotedPrintable_EdgePatterns() {
    // '=' が末尾で i+2 < length を満たさない分岐
    assertEquals("abc=.txt", FileNameUtils.decode("=?UTF-8?Q?abc=?=.txt"));

    // '=' の次が16進でない場合（u/l == -1）分岐
    assertEquals("=GZ.txt", FileNameUtils.decode("=?UTF-8?Q?=GZ?=.txt"));
  }

  @Test
  public void testDecode_Mime_QuotedPrintable_CatchBranch() {
    // decodeQuotedPrintable の catch(Exception ignored) を踏むため、Character.digit を例外送出させる
    try (MockedStatic<Character> mocked =
        Mockito.mockStatic(Character.class, Mockito.CALLS_REAL_METHODS)) {
      mocked.when(() -> Character.digit('E', 16)).thenThrow(new RuntimeException("boom"));
      // Character の static モックは JVM の内部処理へ影響し得るため、結果文字は固定せず「例外なく完走する」ことを優先
      String decoded = FileNameUtils.decode("=?UTF-8?Q?=E3?=.txt");
      assertTrue(decoded.endsWith(".txt"));
    }
  }

  @Test
  public void testDecode_EdgeCases() {
    assertNull(FileNameUtils.decode(null));
    assertEquals("", FileNameUtils.decode(""));
    assertEquals("a", FileNameUtils.decode("  a  "));
    assertEquals("test", FileNameUtils.decode("test."));
    assertEquals("test", FileNameUtils.decode("test..."));
    assertEquals(".", FileNameUtils.decode("."));
  }

  private static void putStaticFinalObject(Field field, Object newValue) throws Exception {
    Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
    unsafeField.setAccessible(true);
    Object unsafe = unsafeField.get(null);

    java.lang.reflect.Method staticFieldBase =
        unsafe.getClass().getMethod("staticFieldBase", Field.class);
    java.lang.reflect.Method staticFieldOffset =
        unsafe.getClass().getMethod("staticFieldOffset", Field.class);
    java.lang.reflect.Method putObject =
        unsafe.getClass().getMethod("putObject", Object.class, long.class, Object.class);

    Object base = staticFieldBase.invoke(unsafe, field);
    long offset = (long) staticFieldOffset.invoke(unsafe, field);
    putObject.invoke(unsafe, base, offset, newValue);
  }
}
