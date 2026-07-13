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

class EntityLotBaseTest {

  static class TestEntity extends EntityBase {
    public TestEntity(OriginalDateTime date) {
      super("test-tenant");
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
    public void selectAll(Connection connection, String tenantId) throws SQLException {}

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

    @Override
    protected String getSelectAllSql() {
      return "SELECT * FROM TEST_TABLE";
    }
  }

  @Test
  void testSqlGetters() {
    TestEntityLot lot = new TestEntityLot();
    assertEquals(TestEntityLot.SELECT_SQL, lot.getSelectSql());
    assertEquals(TestEntityLot.SELECT_LIKE_SQL, lot.getSelectLikeSql());
  }

  @Test
  void testToCountSql_doesNotMatchFromInsideColumnNames() {
    String person = "SELECT person_id, from_group, from_id, from_name FROM SES_AI_T_PERSON";
    assertEquals("SELECT COUNT(*) FROM SES_AI_T_PERSON", EntityLotBase.toCountSql(person));

    String join =
        "SELECT COALESCE(p.from_group, s.from_group) AS from_group, COALESCE(p.from_id, s.from_id) AS from_id "
            + "FROM SES_AI_T_SKILLSHEET s INNER JOIN SES_AI_T_PERSON p ON s.file_id = p.file_id";
    assertEquals(
        "SELECT COUNT(*) FROM SES_AI_T_SKILLSHEET s INNER JOIN SES_AI_T_PERSON p ON s.file_id = p.file_id",
        EntityLotBase.toCountSql(join));

    assertNull(EntityLotBase.toCountSql(null));
  }

  @Test
  void testToCountSql_removesOrderByClause() {
    String personWithOrderBy =
        "SELECT person_id, register_date FROM SES_AI_T_PERSON ORDER BY register_date DESC";
    assertEquals(
        "SELECT COUNT(*) FROM SES_AI_T_PERSON", EntityLotBase.toCountSql(personWithOrderBy));

    String jobWithOrderBy =
        "SELECT job_id, register_date FROM SES_AI_T_JOB ORDER BY register_date DESC";
    assertEquals("SELECT COUNT(*) FROM SES_AI_T_JOB", EntityLotBase.toCountSql(jobWithOrderBy));

    String skillsheetWithOrderBy =
        "SELECT file_id, register_date FROM SES_AI_T_SKILLSHEET ORDER BY register_date DESC";
    assertEquals(
        "SELECT COUNT(*) FROM SES_AI_T_SKILLSHEET",
        EntityLotBase.toCountSql(skillsheetWithOrderBy));
  }

  @Test
  void testToCountSql_removesGroupByClause() {
    String withGroupBy = "SELECT category, COUNT(*) FROM SES_AI_T_PERSON GROUP BY category";
    assertEquals("SELECT COUNT(*) FROM SES_AI_T_PERSON", EntityLotBase.toCountSql(withGroupBy));
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

    int count = 0;
    for (TestEntity e : lot) {
      assertNotNull(e);
      count++;
    }
    assertEquals(3, count);

    lot.sort();
    assertEquals(e3, lot.get(0));
    assertEquals(e1, lot.get(1));
    assertEquals(e2, lot.get(2));

    assertNotNull(lot.toString());
  }

  @Test
  void testSelectByQueryWithNulls() throws java.sql.SQLException {
    TestEntityLot lot = new TestEntityLot();
    lot.selectByAndQuery(null, "test-tenant", null);
    lot.selectByOrQuery(null, "test-tenant", null);
    assertTrue(lot.isEmpty());
  }

  @Test
  void testSelectByQueryWithEmptyMap() throws SQLException {
    TestEntityLot lot = new TestEntityLot();
    Connection conn = mock(Connection.class);
    lot.selectByAndQuery(conn, "test-tenant", Map.of());
    lot.selectByOrQuery(conn, "test-tenant", Map.of());
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
    Map<String, Object> query = Map.of("col1", "val1", "col2", "val2");

    lot.selectByAndQuery(conn, "test-tenant", query);
    assertEquals(1, lot.size());

    TestEntityLot lot2 = new TestEntityLot();
    when(rs.next()).thenReturn(true, false);
    lot2.selectByOrQuery(conn, "test-tenant", query);
    assertEquals(1, lot2.size());

    TestEntityLot lotSingle = new TestEntityLot();
    when(rs.next()).thenReturn(true, false);
    lotSingle.selectByAndQuery(conn, "test-tenant", Map.of("col1", "val1"));
    assertEquals(1, lotSingle.size());
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

    lot.searchByField(conn, "test-tenant", "col", "query");
    assertEquals(1, lot.size());

    TestEntityLot lot2 = new TestEntityLot();
    when(rs.next()).thenReturn(true, false);
    java.util.List<copel.sesproductpackage.core.unit.LogicalOperators> queries =
        new java.util.ArrayList<>();
    queries.add(null);
    queries.add(
        new copel.sesproductpackage.core.unit.LogicalOperators(
            copel.sesproductpackage.core.unit.LogicalOperators.論理演算子.AND, "val1"));
    queries.add(null);
    lot2.searchByField(conn, "test-tenant", "col", "query", queries);
    assertEquals(1, lot2.size());

    TestEntityLot lotTwoOps = new TestEntityLot();
    when(rs.next()).thenReturn(true, false);
    java.util.List<copel.sesproductpackage.core.unit.LogicalOperators> twoOps =
        java.util.Arrays.asList(
            new copel.sesproductpackage.core.unit.LogicalOperators(
                copel.sesproductpackage.core.unit.LogicalOperators.論理演算子.AND, "v1"),
            new copel.sesproductpackage.core.unit.LogicalOperators(
                copel.sesproductpackage.core.unit.LogicalOperators.論理演算子.OR, "v2"));
    lotTwoOps.searchByField(conn, "test-tenant", "col", "q", twoOps);
    assertEquals(1, lotTwoOps.size());
    verify(ps, atLeastOnce()).setString(1, "%q%");
    verify(ps, atLeastOnce()).setString(2, "%v1%");
    verify(ps, atLeastOnce()).setString(3, "%v2%");

    TestEntityLot lotEmptyList = new TestEntityLot();
    lotEmptyList.searchByField(conn, "test-tenant", "col", "query", new java.util.ArrayList<>());

    TestEntityLot lotNullList = new TestEntityLot();
    lotNullList.searchByField(conn, "test-tenant", "col", "query", null);

    TestEntityLot lot3 = new TestEntityLot();
    lot3.searchByField(conn, "test-tenant", "col", null);
    assertTrue(lot3.isEmpty());
  }

  @Test
  void testPaginationMetadata() {
    TestEntityLot lot = new TestEntityLot();
    lot.setTotalCount(25);
    lot.setPageSize(10);
    lot.setCurrentPageIndex(1);

    assertEquals(25, lot.getTotalCount());
    assertEquals(10, lot.getPageSize());
    assertEquals(1, lot.getCurrentPageIndex());
    assertEquals(3, lot.getTotalPages());

    lot.setPageSize(0);
    assertEquals(1, lot.getTotalPages());
  }

  @Test
  void testSelectByQueryPaged_Empty() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true); // first next() for COUNT(*)
    when(rs.getLong(1)).thenReturn(0L);

    TestEntityLot lot = new TestEntityLot();
    lot.selectByQueryPaged(conn, "test-tenant", "SELECT * FROM TEST_TABLE", Map.of(), true, 1, 10);
    assertTrue(lot.isEmpty());
    assertEquals(0, lot.getTotalCount());
  }

  @Test
  void testSelectByQueryPaged_WithData() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);

    // First query is for count
    // Second query is for actual data
    when(rs.next()).thenReturn(true, true, false); // true for count, then true, false for data
    when(rs.getLong(1)).thenReturn(100L);

    TestEntityLot lot = new TestEntityLot();
    lot.selectByQueryPaged(
        conn, "test-tenant", "SELECT * FROM TEST_TABLE", Map.of("id", "1"), true, 1, 10);
    assertEquals(1, lot.size());
    assertEquals(100, lot.getTotalCount());
    assertEquals(10, lot.getPageSize());
    assertEquals(1, lot.getCurrentPageIndex());
  }

  @Test
  void testAddTenantIdFilter_WithoutTenantId() {
    TestEntityLot lot = new TestEntityLot();
    String sql = "SELECT * FROM TEST_TABLE WHERE col1 = ?";
    String result = lot.addTenantIdFilter(sql, "tenant1");
    assertEquals("SELECT * FROM TEST_TABLE WHERE col1 = ? AND tenant_id = ?", result);
  }

  @Test
  void testAddTenantIdFilter_AlreadyHasTenantId() {
    TestEntityLot lot = new TestEntityLot();
    String sql = "SELECT * FROM TEST_TABLE WHERE col1 = ? AND tenant_id = ?";
    String result = lot.addTenantIdFilter(sql, "tenant1");
    assertEquals(sql, result);
  }

  @Test
  void testAddTenantIdFilter_WithWhitespace() {
    TestEntityLot lot = new TestEntityLot();
    String sql = "SELECT * FROM TEST_TABLE WHERE col1 = ?  \n  ";
    String result = lot.addTenantIdFilter(sql, "tenant1");
    assertEquals("SELECT * FROM TEST_TABLE WHERE col1 = ? AND tenant_id = ?", result);
  }

  @Test
  void testAddTenantIdFilter_NullTenantId() {
    TestEntityLot lot = new TestEntityLot();
    String sql = "SELECT * FROM TEST_TABLE";
    assertThrows(
        IllegalArgumentException.class,
        () -> lot.addTenantIdFilter(sql, null),
        "TenantId must not be null or empty");
  }

  @Test
  void testAddTenantIdFilter_EmptyTenantId() {
    TestEntityLot lot = new TestEntityLot();
    String sql = "SELECT * FROM TEST_TABLE";
    assertThrows(
        IllegalArgumentException.class,
        () -> lot.addTenantIdFilter(sql, ""),
        "TenantId must not be null or empty");
  }

  @Test
  void testAddTenantIdFilter_NullSql() {
    TestEntityLot lot = new TestEntityLot();
    assertThrows(
        IllegalArgumentException.class,
        () -> lot.addTenantIdFilter(null, "tenant1"),
        "baseSql must not be null");
  }

  @Test
  void testAddTenantIdFilter_SelectExists() {
    TestEntityLot lot = new TestEntityLot();
    String sql = "SELECT EXISTS (SELECT 1 FROM SES_AI_M_SENDER WHERE from_id = ?)";
    String result = lot.addTenantIdFilter(sql, "tenant1");
    assertEquals(
        "SELECT EXISTS (SELECT 1 FROM SES_AI_M_SENDER WHERE from_id = ? AND tenant_id = ?)",
        result);
  }

  @Test
  void testAddTenantIdFilter_SelectExistsWithoutWhere() {
    TestEntityLot lot = new TestEntityLot();
    String sql = "SELECT EXISTS (SELECT 1 FROM SES_AI_M_SENDER)";
    String result = lot.addTenantIdFilter(sql, "tenant1");
    assertEquals("SELECT EXISTS (SELECT 1 FROM SES_AI_M_SENDER) WHERE tenant_id = ?", result);
  }

  @Test
  void testSetTenantIdParameter() throws SQLException {
    TestEntityLot lot = new TestEntityLot();
    PreparedStatement ps = mock(PreparedStatement.class);
    lot.setTenantIdParameter(ps, 1, "tenant1");
    verify(ps, times(1)).setString(1, "tenant1");
  }

  @Test
  void testSetTenantIdParameter_NullTenantId() throws SQLException {
    TestEntityLot lot = new TestEntityLot();
    PreparedStatement ps = mock(PreparedStatement.class);
    assertThrows(
        IllegalArgumentException.class,
        () -> lot.setTenantIdParameter(ps, 1, null),
        "TenantId must not be null or empty");
  }

  @Test
  void testSetTenantIdParameter_EmptyTenantId() throws SQLException {
    TestEntityLot lot = new TestEntityLot();
    PreparedStatement ps = mock(PreparedStatement.class);
    assertThrows(
        IllegalArgumentException.class,
        () -> lot.setTenantIdParameter(ps, 1, ""),
        "TenantId must not be null or empty");
  }

  @Test
  void testExecuteQuery_WithNullConnection() throws SQLException {
    TestEntityLot lot = new TestEntityLot();
    var results =
        lot.executeQuery(
            null,
            "SELECT * FROM TEST_TABLE",
            "tenant1",
            rs -> new TestEntity(new OriginalDateTime()),
            (stmt, idx) -> idx);
    assertTrue(results.isEmpty());
  }

  @Test
  void testExecuteQuery_WithData() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    TestEntityLot lot = new TestEntityLot();
    var results =
        lot.executeQuery(
            conn,
            "SELECT * FROM TEST_TABLE",
            "tenant1",
            rs2 -> new TestEntity(new OriginalDateTime()),
            (stmt, idx) -> {
              stmt.setString(idx, "value1");
              return idx + 1;
            });

    assertEquals(1, results.size());
    verify(ps, times(1)).setString(1, "value1");
    verify(ps, times(1)).setString(2, "tenant1");
  }

  @Test
  void testExecuteQuery_AddsTenantIdFilter() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(false);

    TestEntityLot lot = new TestEntityLot();
    lot.executeQuery(
        conn,
        "SELECT * FROM TEST_TABLE WHERE col1 = ?",
        "tenant1",
        rs2 -> new TestEntity(new OriginalDateTime()),
        (stmt, idx) -> {
          stmt.setString(idx, "value1");
          return idx + 1;
        });

    verify(conn, times(1))
        .prepareStatement("SELECT * FROM TEST_TABLE WHERE col1 = ? AND tenant_id = ?");
  }

  @Test
  void testExecuteQuery_NullTenantId() throws SQLException {
    Connection conn = mock(Connection.class);
    TestEntityLot lot = new TestEntityLot();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            lot.executeQuery(
                conn,
                "SELECT * FROM TEST_TABLE",
                null,
                rs -> new TestEntity(new OriginalDateTime()),
                (stmt, idx) -> idx),
        "TenantId must not be null or empty");
  }

  @Test
  void testExecuteQueryWithoutTenantFilter_WithNullConnection() throws SQLException {
    TestEntityLot lot = new TestEntityLot();
    var results =
        lot.executeQueryWithoutTenantFilter(
            null,
            "SELECT * FROM TEST_TABLE",
            rs -> new TestEntity(new OriginalDateTime()),
            (stmt, idx) -> idx);
    assertTrue(results.isEmpty());
  }

  @Test
  void testExecuteQueryWithoutTenantFilter_WithData() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    TestEntityLot lot = new TestEntityLot();
    var results =
        lot.executeQueryWithoutTenantFilter(
            conn,
            "SELECT * FROM TEST_TABLE",
            rs2 -> new TestEntity(new OriginalDateTime()),
            (stmt, idx) -> {
              stmt.setString(idx, "value1");
              return idx + 1;
            });

    assertEquals(1, results.size());
    verify(ps, times(1)).setString(1, "value1");
    verify(conn, times(1)).prepareStatement("SELECT * FROM TEST_TABLE");
  }

  @Test
  void testExecuteQueryWithoutTenantFilter_NoTenantIdBound() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(false);

    TestEntityLot lot = new TestEntityLot();
    lot.executeQueryWithoutTenantFilter(
        conn,
        "SELECT * FROM TEST_TABLE",
        rs2 -> new TestEntity(new OriginalDateTime()),
        (stmt, idx) -> idx);

    verify(ps, never()).setString(anyInt(), eq("tenant-any"));
  }

  @Test
  void testSelectByAndQuery_AfterRefactoring() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    TestEntityLot lot = new TestEntityLot();
    Map<String, Object> query = Map.of("col1", "val1");
    lot.selectByAndQuery(conn, "test-tenant", query);

    assertEquals(1, lot.size());
    // tenant_id がバインドされていることを確認（最後のパラメータ）
    verify(ps).setString(anyInt(), eq("test-tenant"));
    verify(ps).setString(anyInt(), eq("val1"));
  }

  @Test
  void testSelectByOrQuery_AfterRefactoring() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    TestEntityLot lot = new TestEntityLot();
    lot.selectByOrQuery(conn, "test-tenant", Map.of("col1", "val1"));

    assertEquals(1, lot.size());
    verify(ps).setString(1, "val1");
    verify(ps).setString(2, "test-tenant");
  }

  @Test
  void testSelectByLikeQuery_AfterRefactoring() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    TestEntityLot lot = new TestEntityLot();
    lot.selectByLikeQuery(
        conn, "test-tenant", "SELECT * FROM TEST_TABLE WHERE ", "col1", "search", null);

    assertEquals(1, lot.size());
    verify(ps).setString(1, "%search%");
    verify(ps).setString(2, "test-tenant");
  }

  @Test
  void testSelectByLikeQuery_WithLogicalOperators() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    TestEntityLot lot = new TestEntityLot();
    java.util.List<copel.sesproductpackage.core.unit.LogicalOperators> operators =
        java.util.Arrays.asList(
            new copel.sesproductpackage.core.unit.LogicalOperators(
                copel.sesproductpackage.core.unit.LogicalOperators.論理演算子.AND, "val1"));

    lot.selectByLikeQuery(
        conn, "test-tenant", "SELECT * FROM TEST_TABLE WHERE ", "col1", "search", operators);

    assertEquals(1, lot.size());
    verify(ps).setString(1, "%search%");
    verify(ps).setString(2, "%val1%");
    verify(ps).setString(3, "test-tenant");
  }

  @Test
  void testCountByQuery_AfterRefactoring() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true);
    when(rs.getLong(1)).thenReturn(100L);

    TestEntityLot lot = new TestEntityLot();
    long count =
        lot.countByQuery(
            conn, "test-tenant", "SELECT * FROM TEST_TABLE", Map.of("col1", "val1"), true);

    assertEquals(100L, count);
    verify(ps).setString(1, "val1");
    verify(ps).setString(2, "test-tenant");
  }

  @Test
  void testSelectByQueryPaged_AfterRefactoring() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, true, false);
    when(rs.getLong(1)).thenReturn(50L);

    TestEntityLot lot = new TestEntityLot();
    lot.selectByQueryPaged(
        conn, "test-tenant", "SELECT * FROM TEST_TABLE", Map.of("col1", "val1"), true, 1, 10);

    assertEquals(1, lot.size());
    assertEquals(50L, lot.getTotalCount());
  }

  @Test
  void testSelectByLikeQueryPaged_AfterRefactoring() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, true, false);
    when(rs.getLong(1)).thenReturn(25L);

    TestEntityLot lot = new TestEntityLot();
    lot.selectByLikeQueryPaged(
        conn, "test-tenant", "SELECT * FROM TEST_TABLE WHERE ", "col1", "search", null, 1, 10);

    assertEquals(1, lot.size());
    assertEquals(25L, lot.getTotalCount());
  }

  @Test
  void testSelectByDynamicWherePaged_AfterRefactoring() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, true, false);
    when(rs.getLong(1)).thenReturn(75L);

    TestEntityLot lot = new TestEntityLot();
    lot.selectByDynamicWherePaged(
        conn,
        "test-tenant",
        "SELECT * FROM TEST_TABLE WHERE ",
        "(col1 LIKE ?)",
        java.util.Arrays.asList("search"),
        1,
        10);

    assertEquals(1, lot.size());
    assertEquals(75L, lot.getTotalCount());
  }
}
