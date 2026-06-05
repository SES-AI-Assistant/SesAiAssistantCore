package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.database.SES_AI_M_INGEST_ROUTE.ChannelType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SES_AI_M_INGEST_ROUTELotTest {

  @Test
  void testSelectAll() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, true, false);
    when(rs.getString("channel_type")).thenReturn("LINE", "EMAIL");
    when(rs.getString("route_key")).thenReturn("route1", "route2");
    when(rs.getString("tenant_id")).thenReturn("T1", "T1");
    when(rs.getString("register_date")).thenReturn("2026-06-04 10:00:00");
    when(rs.getString("register_user")).thenReturn("admin");

    SES_AI_M_INGEST_ROUTELot lot = new SES_AI_M_INGEST_ROUTELot();
    lot.selectAll(connection, "test-tenant");

    assertEquals(2, lot.size());
  }

  @Test
  void testSelectAllEmpty() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(false);

    SES_AI_M_INGEST_ROUTELot lot = new SES_AI_M_INGEST_ROUTELot();
    lot.selectAll(connection, "test-tenant");

    assertEquals(0, lot.size());
  }

  @Test
  void testGetSelectAllSql() {
    SES_AI_M_INGEST_ROUTELot lot = new SES_AI_M_INGEST_ROUTELot();
    String sql = lot.getSelectAllSql();

    assertNotNull(sql);
    assertTrue(sql.contains("SELECT"));
    assertTrue(sql.contains("channel_type"));
    assertTrue(sql.contains("route_key"));
    assertTrue(sql.contains("tenant_id"));
    assertFalse(sql.contains("WHERE"));
  }

  @Test
  void testGetSelectSql() {
    SES_AI_M_INGEST_ROUTELot lot = new SES_AI_M_INGEST_ROUTELot();
    String sql = lot.getSelectSql();

    assertNotNull(sql);
    assertTrue(sql.contains("SELECT"));
    assertTrue(sql.contains("WHERE"));
    assertTrue(sql.contains("channel_type"));
    assertTrue(sql.contains("route_key"));
    assertTrue(sql.contains("tenant_id"));
  }

  @Test
  void testLotToString() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);
    when(rs.getString("channel_type")).thenReturn("LINE");
    when(rs.getString("route_key")).thenReturn("route1");
    when(rs.getString("tenant_id")).thenReturn("T1");
    when(rs.getString("register_date")).thenReturn("2026-06-04 10:00:00");
    when(rs.getString("register_user")).thenReturn("admin");

    SES_AI_M_INGEST_ROUTELot lot = new SES_AI_M_INGEST_ROUTELot();
    lot.selectAll(connection, "test-tenant");

    assertNotNull(lot.toString());
  }

  @Test
  void testLotAdd() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(false);

    SES_AI_M_INGEST_ROUTELot lot = new SES_AI_M_INGEST_ROUTELot();
    lot.selectAll(connection, "test-tenant");
    assertEquals(0, lot.size());

    SES_AI_M_INGEST_ROUTE route = new SES_AI_M_INGEST_ROUTE("test-tenant");
    route.setChannelType(ChannelType.LINE);
    route.setRouteKey("route1");
    route.setTenantId("T1");
    lot.add(route);

    assertEquals(1, lot.size());
  }

  @Test
  void testLotMultipleAdd() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(false);

    SES_AI_M_INGEST_ROUTELot lot = new SES_AI_M_INGEST_ROUTELot();
    lot.selectAll(connection, "test-tenant");

    SES_AI_M_INGEST_ROUTE route1 = new SES_AI_M_INGEST_ROUTE("test-tenant");
    route1.setChannelType(ChannelType.LINE);
    route1.setRouteKey("route1");
    route1.setTenantId("T1");
    lot.add(route1);

    SES_AI_M_INGEST_ROUTE route2 = new SES_AI_M_INGEST_ROUTE("test-tenant");
    route2.setChannelType(ChannelType.EMAIL);
    route2.setRouteKey("route2");
    route2.setTenantId("T2");
    lot.add(route2);

    assertEquals(2, lot.size());
  }

  @Test
  void testSelectByChannelTypeAndRouteKey() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, true, false);
    // Mock getString with ArgumentMatchers to match specific column names
    doReturn("LINE", "LINE").when(rs).getString("channel_type");
    doReturn("ch123", "ch123").when(rs).getString("route_key");
    doReturn("T1", "T2").when(rs).getString("tenant_id");
    doReturn("2026-06-04 10:00:00", "2026-06-04 10:00:00").when(rs).getString("register_date");
    doReturn("admin", "admin").when(rs).getString("register_user");

    SES_AI_M_INGEST_ROUTELot lot = new SES_AI_M_INGEST_ROUTELot();
    lot.selectByChannelTypeAndRouteKey(connection, "test-tenant", ChannelType.LINE, "ch123");

    assertEquals(2, lot.size());
    assertEquals("T1", lot.get(0).getTenantId());
    assertEquals("T2", lot.get(1).getTenantId());
  }

  @Test
  void testSelectByChannelTypeAndRouteKeyEmpty() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(false);

    SES_AI_M_INGEST_ROUTELot lot = new SES_AI_M_INGEST_ROUTELot();
    lot.selectByChannelTypeAndRouteKey(connection, "test-tenant", ChannelType.LINE, "ch999");

    assertEquals(0, lot.size());
  }

  @Test
  void testSelectByChannelTypeAndRouteKeyWithNullConnection() throws SQLException {
    SES_AI_M_INGEST_ROUTELot lot = new SES_AI_M_INGEST_ROUTELot();
    lot.selectByChannelTypeAndRouteKey(null, "test-tenant", ChannelType.LINE, "ch123");

    assertEquals(0, lot.size());
  }
}
