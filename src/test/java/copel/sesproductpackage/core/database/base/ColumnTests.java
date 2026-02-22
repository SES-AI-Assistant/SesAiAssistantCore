package copel.sesproductpackage.core.database.base;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class ColumnTests {

  static class TestClass {
    @Column(physicalName = "p_name", logicalName = "l_name", required = true, primary = true)
    private String field1;

    @Column // defaults
    private String field2;
  }

  @Test
  void testColumnAnnotation() throws NoSuchFieldException {
    Field f1 = TestClass.class.getDeclaredField("field1");
    Column c1 = f1.getAnnotation(Column.class);
    assertNotNull(c1);
    assertEquals("p_name", c1.physicalName());
    assertEquals("l_name", c1.logicalName());
    assertTrue(c1.required());
    assertTrue(c1.primary());

    Field f2 = TestClass.class.getDeclaredField("field2");
    Column c2 = f2.getAnnotation(Column.class);
    assertNotNull(c2);
    assertEquals("", c2.physicalName());
    assertEquals("", c2.logicalName());
    assertFalse(c2.required());
    assertFalse(c2.primary());
  }
}
