package copel.sesproductpackage.core.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OriginalStringUtilsTest {

  @Test
  void testConstructor() {
    assertNotNull(new OriginalStringUtils());
  }

  @Test
  void testIsEmpty() {
    assertTrue(OriginalStringUtils.isEmpty(null));

    assertTrue(OriginalStringUtils.isEmpty(""));

    assertTrue(OriginalStringUtils.isEmpty(" "));

    assertTrue(OriginalStringUtils.isEmpty("null"));

    assertFalse(OriginalStringUtils.isEmpty("abc"));

    assertFalse(OriginalStringUtils.isEmpty(" abc "));

    assertTrue(OriginalStringUtils.isEmpty("\t"));
    assertTrue(OriginalStringUtils.isEmpty("\n"));
  }

  @Test
  void testTruncateByCodePoints() {
    assertNull(OriginalStringUtils.truncateByCodePoints(null, 3));
    assertEquals("", OriginalStringUtils.truncateByCodePoints("", 3));
    assertEquals("あいう", OriginalStringUtils.truncateByCodePoints("あいう", 3));
    assertEquals("あいう", OriginalStringUtils.truncateByCodePoints("あいうえ", 3));
    // サロゲートペア（𠮷）はUTF-16で2単位だが1文字として数え、分断しないこと
    assertEquals("𠮷𠮷𠮷", OriginalStringUtils.truncateByCodePoints("𠮷𠮷𠮷𠮷", 3));
    assertEquals("a𠮷", OriginalStringUtils.truncateByCodePoints("a𠮷b", 2));
  }

  @Test
  void testIsUnspecified() {
    assertTrue(OriginalStringUtils.isUnspecified(null));
    assertTrue(OriginalStringUtils.isUnspecified(""));
    assertTrue(OriginalStringUtils.isUnspecified("null"));
    assertTrue(OriginalStringUtils.isUnspecified("Unknown"));
    assertTrue(OriginalStringUtils.isUnspecified(" UNKNOWN "));
    assertFalse(OriginalStringUtils.isUnspecified("広木駅"));
  }
}
