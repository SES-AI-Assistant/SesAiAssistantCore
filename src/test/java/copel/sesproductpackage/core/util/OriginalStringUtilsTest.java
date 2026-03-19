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
}
