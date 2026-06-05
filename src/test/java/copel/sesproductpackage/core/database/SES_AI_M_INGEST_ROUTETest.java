package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SES_AI_M_INGEST_ROUTETest {

  @Test
  void testNullScenarios() throws SQLException {
    SES_AI_M_INGEST_ROUTE entity = new SES_AI_M_INGEST_ROUTE();
    Connection connection = mock(Connection.class);

    assertEquals(0, entity.insert(null));
    entity.selectByPk(null);
    entity.selectByPk(connection);
    assertFalse(entity.updateByPk(null));
    assertFalse(entity.updateByPk(connection));
    assertFalse(entity.deleteByPk(null));
    assertFalse(entity.deleteByPk(connection));

    entity.setRegisterDate(null);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    entity.setChannelType("LINE");
    entity.setRouteKey("route1");
    entity.setTenantId("T1");
    entity.insert(connection);
    entity.updateByPk(connection);
  }

  @Test
  void testIngestRouteInsert() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(1);

    SES_AI_M_INGEST_ROUTE route = new SES_AI_M_INGEST_ROUTE();
    route.setChannelType("LINE");
    route.setRouteKey("route1");
    route.setTenantId("T1");
    route.setRegisterDate(new OriginalDateTime());
    route.setRegisterUser("admin");

    assertEquals(1, route.insert(connection));
    verify(ps, times(1)).setString(1, "LINE");
    verify(ps, times(1)).setString(2, "route1");
    verify(ps, times(1)).setString(3, "T1");
    verify(ps, times(1)).setString(5, "admin");
  }

  @Test
  void testIngestRouteSelectByPk() throws SQLException {
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

    SES_AI_M_INGEST_ROUTE route = new SES_AI_M_INGEST_ROUTE();
    route.setChannelType("LINE");
    route.setRouteKey("route1");
    route.setTenantId("T1");
    route.selectByPk(connection);

    assertEquals("LINE", route.getChannelType());
    assertEquals("route1", route.getRouteKey());
    assertEquals("T1", route.getTenantId());
    assertEquals("admin", route.getRegisterUser());
  }

  @Test
  void testIngestRouteSelectByPkWithNullKeys() throws SQLException {
    Connection connection = mock(Connection.class);

    SES_AI_M_INGEST_ROUTE route = new SES_AI_M_INGEST_ROUTE();
    route.selectByPk(connection);

    verify(connection, never()).prepareStatement(anyString());
  }

  @Test
  void testIngestRouteSelectByPkWithPartialKeys() throws SQLException {
    Connection connection = mock(Connection.class);

    SES_AI_M_INGEST_ROUTE route = new SES_AI_M_INGEST_ROUTE();
    route.setChannelType("LINE");
    route.selectByPk(connection);

    verify(connection, never()).prepareStatement(anyString());
  }

  @Test
  void testIngestRouteUpdateByPk() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(1);

    SES_AI_M_INGEST_ROUTE route = new SES_AI_M_INGEST_ROUTE();
    route.setChannelType("LINE");
    route.setRouteKey("route1");
    route.setTenantId("T1");
    route.setRegisterUser("updater");

    assertTrue(route.updateByPk(connection));
    verify(ps, times(1)).setString(1, "updater");
    verify(ps, times(1)).setString(2, "LINE");
    verify(ps, times(1)).setString(3, "route1");
    verify(ps, times(1)).setString(4, "T1");
  }

  @Test
  void testIngestRouteUpdateByPkFail() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(0);

    SES_AI_M_INGEST_ROUTE route = new SES_AI_M_INGEST_ROUTE();
    route.setChannelType("LINE");
    route.setRouteKey("route1");
    route.setTenantId("T1");

    assertFalse(route.updateByPk(connection));
  }

  @Test
  void testIngestRouteDeleteByPk() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(1);

    SES_AI_M_INGEST_ROUTE route = new SES_AI_M_INGEST_ROUTE();
    route.setChannelType("LINE");
    route.setRouteKey("route1");
    route.setTenantId("T1");

    assertTrue(route.deleteByPk(connection));
    verify(ps, times(1)).setString(1, "LINE");
    verify(ps, times(1)).setString(2, "route1");
    verify(ps, times(1)).setString(3, "T1");
  }

  @Test
  void testIngestRouteDeleteByPkFail() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(0);

    SES_AI_M_INGEST_ROUTE route = new SES_AI_M_INGEST_ROUTE();
    route.setChannelType("LINE");
    route.setRouteKey("route1");
    route.setTenantId("T1");

    assertFalse(route.deleteByPk(connection));
  }

  @Test
  void testIngestRouteToString() {
    SES_AI_M_INGEST_ROUTE route = new SES_AI_M_INGEST_ROUTE();
    route.setChannelType("LINE");
    route.setRouteKey("route1");
    route.setTenantId("T1");

    assertNotNull(route.toString());
    assertTrue(route.toString().contains("LINE"));
  }

  @Test
  void testIngestRouteEqualsAndHashCode() {
    SES_AI_M_INGEST_ROUTE route1 = new SES_AI_M_INGEST_ROUTE();
    route1.setChannelType("LINE");
    route1.setRouteKey("route1");
    route1.setTenantId("T1");

    SES_AI_M_INGEST_ROUTE route2 = new SES_AI_M_INGEST_ROUTE();
    route2.setChannelType("LINE");
    route2.setRouteKey("route1");
    route2.setTenantId("T1");

    assertEquals(route1, route2);
    assertEquals(route1.hashCode(), route2.hashCode());
  }

  @Test
  void testIngestRouteInsertWithNullConnection() throws SQLException {
    SES_AI_M_INGEST_ROUTE route = new SES_AI_M_INGEST_ROUTE();
    route.setChannelType("LINE");
    route.setRouteKey("route1");
    route.setTenantId("T1");

    assertEquals(0, route.insert(null));
  }
}
