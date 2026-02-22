package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LogicalOperatorsTests {

  @Test
  void testConstructorAndGetValue() {
    LogicalOperators op = new LogicalOperators(LogicalOperators.論理演算子.AND, "value");
    assertEquals("value", op.getValue());
  }

  @Test
  void testGetLikeQuery() {
    LogicalOperators op = new LogicalOperators(LogicalOperators.論理演算子.AND, "col1", "val1");
    assertEquals(" AND col1 LIKE ?", op.getLikeQuery());

    op = new LogicalOperators(LogicalOperators.論理演算子.OR, "val1");
    op.setColumnName("col2");
    assertEquals(" OR col2 LIKE ?", op.getLikeQuery());
  }

  @Test
  void testGetLikeQueryNullCases() {
    LogicalOperators op = new LogicalOperators(null, "val");
    assertNull(op.getLikeQuery());

    op = new LogicalOperators(LogicalOperators.論理演算子.AND, "val");
    assertNull(op.getLikeQuery());
  }

  @Test
  void testEnum() {
    assertNotNull(LogicalOperators.論理演算子.AND);
    assertNotNull(LogicalOperators.論理演算子.OR);
    assertNotNull(LogicalOperators.論理演算子.NOT);
    assertNotNull(LogicalOperators.論理演算子.NOR);
    assertNotNull(LogicalOperators.論理演算子.XOR);
  }
}
