package copel.sesproductpackage.core.database.base;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

class EntityBaseTest {

  static class TestEntity extends EntityBase {
    public TestEntity() {
      super("test-tenant");
    }

    @Override
    public int insert(Connection connection) throws SQLException {
      return 0;
    }

    @Override
    public void selectByPk(Connection connection) throws SQLException {}

    @Override
    public boolean updateByPk(Connection connection) throws SQLException {
      return true;
    }

    @Override
    public boolean deleteByPk(Connection connection) throws SQLException {
      return true;
    }

    @Override
    public String toString() {
      return "TestEntity";
    }
  }

  static class TestEntityLot extends EntityLotBase<TestEntity> {
    @Override
    public void selectAll(Connection connection, String tenantId) throws SQLException {}

    @Override
    protected TestEntity mapResultSet(ResultSet resultSet) throws SQLException {
      return new TestEntity();
    }

    @Override
    protected String getSelectAllSql() {
      return "SELECT * FROM TEST_TABLE";
    }
  }

  @Test
  void testEntityBaseBasic() {
    TestEntity entity = new TestEntity();
    OriginalDateTime now = new OriginalDateTime();
    entity.setRegisterDate(now);
    entity.setRegisterUser("user1");

    assertEquals(now, entity.getRegisterDate());
    assertEquals("user1", entity.getRegisterUser());
  }

  @Test
  void testEntityBaseCompareTo() {
    TestEntity e1 = new TestEntity();
    TestEntity e2 = new TestEntity();

    assertEquals(0, e1.compareTo(null));

    OriginalDateTime d1 = new OriginalDateTime("2026-01-01 00:00:00");
    OriginalDateTime d2 = new OriginalDateTime("2026-01-02 00:00:00");

    e1.setRegisterDate(d1);
    e2.setRegisterDate(d2);

    assertTrue(e1.compareTo(e2) < 0);
    assertTrue(e2.compareTo(e1) > 0);
    assertEquals(0, e1.compareTo(e1));

    TestEntity e3 = new TestEntity();
    TestEntity e4 = new TestEntity();
    e4.setRegisterDate(d1);
    assertEquals(-1, e3.compareTo(e4));
    assertEquals(0, e3.compareTo(new TestEntity()));
    assertEquals(1, e4.compareTo(e3));
    assertEquals(1, e4.compareTo(null));
  }

  @Test
  void testEntityLotBase() {
    TestEntityLot lot = new TestEntityLot();
    assertTrue(lot.isEmpty());
    assertEquals(0, lot.size());

    TestEntity e1 = new TestEntity();
    e1.setRegisterDate(new OriginalDateTime("2026-01-02 00:00:00"));
    TestEntity e2 = new TestEntity();
    e2.setRegisterDate(new OriginalDateTime("2026-01-01 00:00:00"));

    lot.add(e1);
    lot.add(e2);

    assertFalse(lot.isEmpty());
    assertEquals(2, lot.size());
    assertEquals(e1, lot.get(0));
    assertEquals(e2, lot.get(1));
    assertNull(lot.get(2));

    lot.sort();
    assertEquals(e2, lot.get(0));

    Iterator<TestEntity> it = lot.iterator();
    assertTrue(it.hasNext());
    assertEquals(e2, it.next());

    String str = lot.toString();
    assertTrue(str.contains("(0)TestEntity"));
    assertTrue(str.contains("(1)TestEntity"));
  }

  /**
   * 主キーが2列のSQLに対し、SELECT-by-PK テンプレートが tenant_id を第3パラメータ（元SQLのプレースホルダ数+1）にバインドすることを確認する.
   *
   * <p>以前は tenant_id を第2パラメータに固定バインドしており、第2主キーが上書きされていた。
   *
   * @throws SQLException
   */
  @Test
  void testExecuteSelectByPkBindsTenantIdAfterAllPlaceholders() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true);
    TestEntity entity = new TestEntity();

    boolean found =
        entity.executeSelectByPk(
            conn,
            "SELECT * FROM T WHERE k1 = ? AND k2 = ?",
            "test-tenant",
            stmt -> {
              stmt.setString(1, "key1");
              stmt.setString(2, "key2");
            },
            rs2 -> {},
            "label");

    assertTrue(found);
    verify(ps).setString(1, "key1");
    verify(ps).setString(2, "key2");
    verify(ps).setString(3, "test-tenant");
    verify(ps, never()).setString(eq(2), eq("test-tenant"));
  }

  /**
   * 主キーが2列のSQLに対し、EXISTS テンプレートが tenant_id を括弧内末尾（第3パラメータ）にバインドすることを確認する.
   *
   * @throws SQLException
   */
  @Test
  void testExecuteExistsBindsTenantIdAfterAllPlaceholders() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true);
    when(rs.getBoolean(1)).thenReturn(true);
    TestEntity entity = new TestEntity();

    boolean exists =
        entity.executeExists(
            conn,
            "SELECT EXISTS (SELECT 1 FROM T WHERE k1 = ? AND k2 = ?)",
            "test-tenant",
            stmt -> {
              stmt.setString(1, "key1");
              stmt.setString(2, "key2");
            },
            "label");

    assertTrue(exists);
    verify(conn)
        .prepareStatement(
            "SELECT EXISTS (SELECT 1 FROM T WHERE k1 = ? AND k2 = ? AND tenant_id = ?)");
    verify(ps).setString(1, "key1");
    verify(ps).setString(2, "key2");
    verify(ps).setString(3, "test-tenant");
    verify(ps, never()).setString(eq(2), eq("test-tenant"));
  }

  /**
   * 主キーが2列のSQLに対し、DELETE-by-PK テンプレートが tenant_id を第3パラメータにバインドすることを確認する.
   *
   * @throws SQLException
   */
  @Test
  void testExecuteDeleteByPkBindsTenantIdAfterAllPlaceholders() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(1);
    TestEntity entity = new TestEntity();

    boolean deleted =
        entity.executeDeleteByPk(
            conn,
            "DELETE FROM T WHERE k1 = ? AND k2 = ?",
            "test-tenant",
            stmt -> {
              stmt.setString(1, "key1");
              stmt.setString(2, "key2");
            },
            "label");

    assertTrue(deleted);
    verify(ps).setString(1, "key1");
    verify(ps).setString(2, "key2");
    verify(ps).setString(3, "test-tenant");
    verify(ps, never()).setString(eq(2), eq("test-tenant"));
  }
}
