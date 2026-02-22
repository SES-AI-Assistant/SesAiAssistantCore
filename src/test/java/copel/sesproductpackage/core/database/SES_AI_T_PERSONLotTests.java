package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.LogicalOperators;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Vector;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SES_AI_T_PERSONLotTests {

  @Test
  void testRetrieve() throws Exception {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);
    when(rs.getString("person_id")).thenReturn("P1");
    when(rs.getDouble("distance")).thenReturn(0.5);

    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();

    Vector vector = new Vector(null);
    java.lang.reflect.Field valueField = Vector.class.getDeclaredField("value");
    valueField.setAccessible(true);
    valueField.set(vector, new float[] {0.1f});

    lot.retrieve(connection, vector, 10);

    assertEquals(1, lot.size());
    assertEquals("P1", lot.get(0).getPersonId());
  }

  @Test
  void testSearchByRawContent() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    lot.searchByRawContent(connection, "query");

    assertEquals(1, lot.size());
  }

  @Test
  void testSearchByRawContentMulti() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    LogicalOperators op = new LogicalOperators(LogicalOperators.論理演算子.AND, "val");
    lot.searchByRawContent(connection, "first", List.of(op));

    assertEquals(1, lot.size());
  }

  @Test
  void testSelectByAndQuery() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    lot.selectByAndQuery(connection, Map.of("col", "val"));

    assertEquals(1, lot.size());
  }

  @Test
  void testSelectByOrQuery() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    lot.selectByOrQuery(connection, Map.of("col", "val"));

    assertEquals(1, lot.size());
  }

  @Test
  void testSelectByRegisterDateAfter() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    lot.selectByRegisterDateAfter(connection, new OriginalDateTime());

    assertEquals(1, lot.size());
  }

  @Test
  void testGetEntityByPk() {
    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    SES_AI_T_PERSON p1 = new SES_AI_T_PERSON();
    p1.setPersonId("P1");
    lot.add(p1);

    assertEquals(p1, lot.getEntityByPk("P1"));
    assertNull(lot.getEntityByPk("P2"));
    assertNull(lot.getEntityByPk(null));
  }

  @Test
  void testIsExistByFileId() {
    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    SES_AI_T_PERSON p1 = new SES_AI_T_PERSON();
    p1.setFileId("F1");
    lot.add(p1);

    assertTrue(lot.isExistByFileId("F1"));
    assertFalse(lot.isExistByFileId("F2"));
    assertFalse(lot.isExistByFileId(null));
  }

  @Test
  void testTo要員選出用文章() {
    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    SES_AI_T_PERSON p1 = new SES_AI_T_PERSON();
    p1.setPersonId("P1");
    p1.setRawContent("C1");
    p1.setFileSummary("S1");
    lot.add(p1);

    String result = lot.to要員選出用文章();
    assertTrue(result.contains("1人目："));
  }

  @Test
  void testSelectAll() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    lot.selectAll(connection);
    assertEquals(1, lot.size());
  }
}
