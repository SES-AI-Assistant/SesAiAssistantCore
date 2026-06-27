package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SES_AI_WEBAPP_M_NOTIFICATIONLotTest {

  @Test
  void testLot() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    when(rs.getString("notification_id")).thenReturn("N1");
    when(rs.getString("user_id")).thenReturn("U1");
    when(rs.getString("device_type")).thenReturn("WEB_PUSH");
    when(rs.getString("device_name")).thenReturn("test-device");
    when(rs.getString("push_notification_endpoint")).thenReturn("https://example.com");
    when(rs.getString("p256dh")).thenReturn("p256dh-key");
    when(rs.getString("auth")).thenReturn("auth-key");
    when(rs.getBoolean("enabled")).thenReturn(true);
    when(rs.getBoolean("notify_all_match")).thenReturn(false);
    when(rs.getString("register_date")).thenReturn("2026-06-27 12:00:00");
    when(rs.getString("register_user")).thenReturn("admin");
    when(rs.getString("tenant_id")).thenReturn("test-tenant");

    SES_AI_WEBAPP_M_NOTIFICATIONLot lot = new SES_AI_WEBAPP_M_NOTIFICATIONLot();
    lot.selectAll(connection, "test-tenant");

    assertEquals(1, lot.size());
    SES_AI_WEBAPP_M_NOTIFICATION first = lot.get(0);
    assertEquals("N1", first.getNotificationId());
    assertEquals("U1", first.getUserId());
    assertEquals("test-tenant", first.getTenantId());

    assertNotNull(lot.toString());
  }

  @Test
  void testSelectByUserId() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    when(rs.getString("notification_id")).thenReturn("N1");
    when(rs.getString("user_id")).thenReturn("U1");
    when(rs.getString("device_type")).thenReturn("WEB_PUSH");
    when(rs.getString("device_name")).thenReturn("test-device");
    when(rs.getString("push_notification_endpoint")).thenReturn("https://example.com");
    when(rs.getString("p256dh")).thenReturn("p256dh-key");
    when(rs.getString("auth")).thenReturn("auth-key");
    when(rs.getBoolean("enabled")).thenReturn(true);
    when(rs.getBoolean("notify_all_match")).thenReturn(true);
    when(rs.getString("register_date")).thenReturn("2026-06-27 12:00:00");
    when(rs.getString("register_user")).thenReturn("admin");
    when(rs.getString("tenant_id")).thenReturn("test-tenant");

    SES_AI_WEBAPP_M_NOTIFICATIONLot lot = new SES_AI_WEBAPP_M_NOTIFICATIONLot();
    lot.selectByUserId(connection, "test-tenant", "U1");

    assertEquals(1, lot.size());
    assertEquals("N1", lot.get(0).getNotificationId());
    assertEquals("test-tenant", lot.get(0).getTenantId());
    assertTrue(lot.get(0).getNotifyAllMatch());
  }

  @Test
  void testSelectByUserIdWithNullConnection() throws SQLException {
    SES_AI_WEBAPP_M_NOTIFICATIONLot lot = new SES_AI_WEBAPP_M_NOTIFICATIONLot();
    lot.selectByUserId(null, "test-tenant", "U1");
    assertEquals(0, lot.size());
  }

  @Test
  void testSelectByUserIdWithNullUserId() throws SQLException {
    Connection connection = mock(Connection.class);
    SES_AI_WEBAPP_M_NOTIFICATIONLot lot = new SES_AI_WEBAPP_M_NOTIFICATIONLot();
    lot.selectByUserId(connection, "test-tenant", null);
    assertEquals(0, lot.size());
  }
}
