package copel.sesproductpackage.core.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OriginalStringUtilsTests {

  @Test
  void testConstructor() {
    assertNotNull(new OriginalStringUtils());
  }

  @Test
  void testIsEmpty() {
    // null の場合
    assertTrue(OriginalStringUtils.isEmpty(null));

    // 空文字の場合
    assertTrue(OriginalStringUtils.isEmpty(""));

    // 空白文字の場合
    assertTrue(OriginalStringUtils.isEmpty(" "));

    // "null" という文字列の場合
    assertTrue(OriginalStringUtils.isEmpty("null"));

    // 通常の文字列の場合
    assertFalse(OriginalStringUtils.isEmpty("abc"));

    // 前後空白のある文字列の場合
    assertFalse(OriginalStringUtils.isEmpty(" abc "));
  }
}
