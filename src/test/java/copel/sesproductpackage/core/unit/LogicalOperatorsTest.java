package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LogicalOperatorsTest {

  @Test
  void testConstructorAndGetValue() {
    LogicalOperators op = new LogicalOperators(LogicalOperators.LogicalOperatorType.AND, "value");
    assertEquals("value", op.getValue());
  }

  @Test
  void testGetLikeQuery() {
    LogicalOperators op =
        new LogicalOperators(LogicalOperators.LogicalOperatorType.AND, "col1", "val1");
    assertEquals(" AND col1 LIKE ?", op.getLikeQuery());

    op = new LogicalOperators(LogicalOperators.LogicalOperatorType.OR, "val1");
    op.setColumnName("col2");
    assertEquals(" OR col2 LIKE ?", op.getLikeQuery());
  }

  @Test
  void testGetLikeQueryNullCases() {
    LogicalOperators op = new LogicalOperators(null, "val");
    assertNull(op.getLikeQuery());

    op = new LogicalOperators(LogicalOperators.LogicalOperatorType.AND, "val");
    assertNull(op.getLikeQuery());
  }

  @Test
  void testEnum() {
    assertNotNull(LogicalOperators.LogicalOperatorType.AND);
    assertNotNull(LogicalOperators.LogicalOperatorType.OR);
    assertNotNull(LogicalOperators.LogicalOperatorType.NOT);
    assertNotNull(LogicalOperators.LogicalOperatorType.NOR);
    assertNotNull(LogicalOperators.LogicalOperatorType.XOR);
  }
}
