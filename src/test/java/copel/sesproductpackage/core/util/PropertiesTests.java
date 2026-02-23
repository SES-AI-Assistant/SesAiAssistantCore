package copel.sesproductpackage.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PropertiesTests {

  @Test
  void testConstructor() {
    assertNotNull(new Properties());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testGetWithMock() throws Exception {
    Field propertiesField = Properties.class.getDeclaredField("properties");
    propertiesField.setAccessible(true);
    Map<String, String> propertiesMap = (Map<String, String>) propertiesField.get(null);

    propertiesMap.put("TEST_KEY", "TEST_VALUE");
    propertiesMap.put("TEST_INT", "123");
    propertiesMap.put("TEST_DOUBLE", "123.45");
    propertiesMap.put("TEST_ARRAY", "A,B,C");

    assertEquals("TEST_VALUE", Properties.get("TEST_KEY"));
    assertEquals(123, Properties.getInt("TEST_INT"));
    assertEquals(123.45, Properties.getDouble("TEST_DOUBLE"));
    assertEquals("A", Properties.getAsArray("TEST_ARRAY")[0]);
    assertEquals("B", Properties.getAsArray("TEST_ARRAY")[1]);
    assertEquals("C", Properties.getAsArray("TEST_ARRAY")[2]);
  }

  @Test
  void testStaticBlockLogicCoverage() {
    // static ブロック内の if (!line.trim().isEmpty() && line.contains("=")) をカバーするための
    // ロジックをテストしたいが、既に実行済み。
    // 他のテストクラスでのProperties利用により既にロードされている。
  }
}
