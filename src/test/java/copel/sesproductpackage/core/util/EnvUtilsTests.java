package copel.sesproductpackage.core.util;

import org.junit.jupiter.api.Test;

class EnvUtilsTests {
  @Test
  void testGet() {
    // Just verify it doesn't crash and returns something or null
    EnvUtils.get("PATH");
    EnvUtils.get("NON_EXISTENT_VAR_XYZ");
  }

  @Test
  void testConstructor() {
    // Cover the implicit constructor
    new EnvUtils();
  }
}
