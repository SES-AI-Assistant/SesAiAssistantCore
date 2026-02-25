package copel.sesproductpackage.core.database.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Vector;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SES_AI_T_EntityBaseTests {

  static class TestVectorEntity extends SES_AI_T_EntityBase {
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
    protected String getRawContent() {
      return "raw content";
    }

    @Override
    protected String getContentSummary() {
      return "summary";
    }

    @Override
    protected String getCheckSql() {
      return "SELECT COUNT(*) FROM test_table;";
    }
  }

  @Test
  void testUniqueCheck_NoResult() throws SQLException {
    TestVectorEntity entity = new TestVectorEntity();
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    assertTrue(entity.uniqueCheck(connection, 0.8));
  }

  @Test
  void testUniqueCheck_Unique() throws SQLException {
    TestVectorEntity entity = new TestVectorEntity();
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getInt(1)).thenReturn(0);

    assertTrue(entity.uniqueCheck(connection, 0.8));
  }

  @Test
  void testUniqueCheck_Duplicate() throws SQLException {
    TestVectorEntity entity = new TestVectorEntity();
    Connection connection = Mockito.mock(Connection.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    ResultSet resultSet = Mockito.mock(ResultSet.class);

    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getInt(1)).thenReturn(1);

    assertFalse(entity.uniqueCheck(connection, 0.8));
  }

  @Test
  void testSES_AI_T_EntityBaseBasic() {
    TestVectorEntity entity = new TestVectorEntity();

    entity.setFromGroup("group1");
    entity.setFromId("id1");
    entity.setFromName("name1");
    Vector v = new Vector(null);
    entity.setVectorData(v);
    OriginalDateTime ttl = new OriginalDateTime();
    entity.setTtl(ttl);
    entity.setDistance(0.5);

    assertEquals("group1", entity.getFromGroup());
    assertEquals("id1", entity.getFromId());
    assertEquals("name1", entity.getFromName());
    assertEquals(v, entity.getVectorData());
    assertEquals(ttl, entity.getTtl());
    assertEquals(0.5, entity.getDistance());
  }

  @Test
  void testCompareTo() {
    TestVectorEntity e1 = new TestVectorEntity();
    TestVectorEntity e2 = new TestVectorEntity();

    e1.setDistance(0.1);
    e2.setDistance(0.2);

    assertTrue(e1.compareTo(e2) < 0);
    assertTrue(e2.compareTo(e1) > 0);
    assertEquals(0, e1.compareTo(e1));
  }
}
