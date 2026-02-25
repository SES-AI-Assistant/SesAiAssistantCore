package copel.sesproductpackage.core.database.base;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EntityLotBaseTests {

  static class TestEntity extends EntityBase {
    public TestEntity(OriginalDateTime date) {
      this.registerDate = date;
    }

    @Override
    public int insert(Connection connection) throws SQLException {
      return 0;
    }

    @Override
    public void selectByPk(Connection connection) throws SQLException {}

    @Override
    public boolean updateByPk(Connection connection) throws SQLException {
      return false;
    }

    @Override
    public boolean deleteByPk(Connection connection) throws SQLException {
      return false;
    }
  }

  static class TestEntityLot extends EntityLotBase<TestEntity> {
    private static final String SELECT_SQL = "SELECT * FROM TEST_TABLE WHERE ";
    private static final String SELECT_LIKE_SQL =
        "SELECT * FROM TEST_TABLE WHERE test_column LIKE ?";

    @Override
    public void selectAll(Connection connection) throws SQLException {}

    @Override
    protected TestEntity mapResultSet(ResultSet resultSet) throws SQLException {
      return new TestEntity(new OriginalDateTime());
    }

    @Override
    protected String getSelectSql() {
      return SELECT_SQL;
    }

    @Override
    protected String getSelectLikeSql() {
      return SELECT_LIKE_SQL;
    }
  }

  @Test
  void testSqlGetters() {
    TestEntityLot lot = new TestEntityLot();
    assertEquals(TestEntityLot.SELECT_SQL, lot.getSelectSql());
    assertEquals(TestEntityLot.SELECT_LIKE_SQL, lot.getSelectLikeSql());
  }

  @Test
  void testLotOperations() {
    TestEntityLot lot = new TestEntityLot();
    assertTrue(lot.isEmpty());
    assertEquals(0, lot.size());

    TestEntity e1 = new TestEntity(new OriginalDateTime("2023-01-01 10:00:00"));
    TestEntity e2 = new TestEntity(new OriginalDateTime("2023-01-02 10:00:00"));
    TestEntity e3 = new TestEntity(new OriginalDateTime("2023-01-01 09:00:00"));

    lot.add(e1);
    lot.add(e2);
    lot.add(e3);

    assertFalse(lot.isEmpty());
    assertEquals(3, lot.size());
    assertEquals(e1, lot.get(0));
    assertEquals(e2, lot.get(1));
    assertEquals(e3, lot.get(2));
    assertNull(lot.get(99));

    // Test iterator
    int count = 0;
    for (TestEntity e : lot) {
      assertNotNull(e);
      count++;
    }
    assertEquals(3, count);

    // Test sort
    lot.sort();
    assertEquals(e3, lot.get(0)); // 09:00
    assertEquals(e1, lot.get(1)); // 10:00 (Jan 1)
    assertEquals(e2, lot.get(2)); // Jan 2

    // Test toString
    assertNotNull(lot.toString());
  }

  @Test
  void testSelectByQueryWithNulls() throws java.sql.SQLException {
    TestEntityLot lot = new TestEntityLot();
    lot.selectByAndQuery(null, null);
    lot.selectByOrQuery(null, null);
    assertTrue(lot.isEmpty());
  }

  @Test
  void testSelectByQueryWithMap() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    TestEntityLot lot = new TestEntityLot();
    Map<String, String> query = Map.of("col1", "val1", "col2", "val2");

    lot.selectByAndQuery(conn, query);
    assertEquals(1, lot.size());

    TestEntityLot lot2 = new TestEntityLot();
    when(rs.next()).thenReturn(true, false);
    lot2.selectByOrQuery(conn, query);
    assertEquals(1, lot2.size());
  }

  @Test
  void testSearchByField() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    TestEntityLot lot = new TestEntityLot();

    // Single query
    lot.searchByField(conn, "col", "query");
    assertEquals(1, lot.size());

    // Multiple queries
    TestEntityLot lot2 = new TestEntityLot();
    when(rs.next()).thenReturn(true, false);
    java.util.List<copel.sesproductpackage.core.unit.LogicalOperators> queries =
        new java.util.ArrayList<>();
    queries.add(null);
    queries.add(
        new copel.sesproductpackage.core.unit.LogicalOperators(
            copel.sesproductpackage.core.unit.LogicalOperators.論理演算子.AND, "val1"));
    queries.add(null);
    lot2.searchByField(conn, "col", "query", queries);
    assertEquals(1, lot2.size());

    // Empty queries list
    TestEntityLot lotEmptyList = new TestEntityLot();
    lotEmptyList.searchByField(conn, "col", "query", new java.util.ArrayList<>());

    // Null queries list
    TestEntityLot lotNullList = new TestEntityLot();
    lotNullList.searchByField(conn, "col", "query", null);

    // Null query
    TestEntityLot lot3 = new TestEntityLot();
    lot3.searchByField(conn, "col", null);
    assertTrue(lot3.isEmpty());
  }
}
