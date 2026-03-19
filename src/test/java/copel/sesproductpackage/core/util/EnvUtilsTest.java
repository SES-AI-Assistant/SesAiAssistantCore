package copel.sesproductpackage.core.util;

import org.junit.jupiter.api.Test;

class EnvUtilsTest {
  @Test
  void testGet() {
    EnvUtils.get("PATH");
    EnvUtils.get("NON_EXISTENT_VAR_XYZ");
  }

  @Test
  void testConstructor() {
    new EnvUtils();
  }
}
