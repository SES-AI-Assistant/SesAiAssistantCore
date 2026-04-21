package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SES_AI_WEBAPP_M_NOTIFICATIONTest {

  @Test
  void testNullScenarios() throws SQLException {
    SES_AI_WEBAPP_M_NOTIFICATION notification = new SES_AI_WEBAPP_M_NOTIFICATION();
    Connection connection = mock(Connection.class);

    assertEquals(0, notification.insert(null));

    notification.selectByPk(null);
    notification.setNotificationId(null);
    notification.selectByPk(connection);

    assertFalse(notification.updateByPk(null));
    notification.setNotificationId(null);
    assertFalse(notification.updateByPk(connection));

    assertFalse(notification.deleteByPk(null));
    notification.setNotificationId(null);
    assertFalse(notification.deleteByPk(connection));

    notification.setRegisterDate(null);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    notification.setNotificationId("N1");
    notification.insert(connection);
    notification.updateByPk(connection);

    assertNotNull(notification.toString());
    assertNotNull(notification.hashCode());
    assertTrue(notification.equals(notification));
    assertFalse(notification.equals(null));
    assertFalse(notification.equals(new Object()));
  }

  @Test
  void testResultSetBranches() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);

    SES_AI_WEBAPP_M_NOTIFICATION notification = new SES_AI_WEBAPP_M_NOTIFICATION();
    notification.setNotificationId("N1");

    when(rs.next()).thenReturn(false);
    notification.selectByPk(connection);

    reset(rs);
    when(rs.next()).thenReturn(true);
    when(rs.getString("register_date")).thenReturn(null);
    notification.selectByPk(connection);
  }

  @Test
  void testNotification() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(1);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    when(rs.getString("notification_id")).thenReturn("N1");
    when(rs.getString("user_id")).thenReturn("U1");
    when(rs.getString("device_type")).thenReturn("WEB_PUSH");
    when(rs.getString("device_name")).thenReturn("Chrome on Windows");
    when(rs.getString("push_notification_endpoint")).thenReturn("https://example.com/push/xyz");
    when(rs.getString("p256dh")).thenReturn("base64_p256dh_key");
    when(rs.getString("auth")).thenReturn("base64_auth_token");
    when(rs.getBoolean("enabled")).thenReturn(true);
    when(rs.getString("register_date")).thenReturn("2026-04-22 00:00:00");
    when(rs.getString("register_user")).thenReturn("admin");

    SES_AI_WEBAPP_M_NOTIFICATION notification = new SES_AI_WEBAPP_M_NOTIFICATION();
    notification.setNotificationId("N1");
    notification.setUserId("U1");
    notification.setDeviceType("WEB_PUSH");
    notification.setDeviceName("Chrome on Windows");
    notification.setPushNotificationEndpoint("https://example.com/push/xyz");
    notification.setP256dh("base64_p256dh_key");
    notification.setAuth("base64_auth_token");
    notification.setEnabled(true);
    notification.setRegisterDate(new OriginalDateTime());
    notification.setRegisterUser("admin");

    assertEquals(1, notification.insert(connection));
    assertTrue(notification.updateByPk(connection));

    SES_AI_WEBAPP_M_NOTIFICATION target = new SES_AI_WEBAPP_M_NOTIFICATION();
    target.setNotificationId("N1");
    target.selectByPk(connection);

    assertEquals("N1", target.getNotificationId());
    assertEquals("U1", target.getUserId());
    assertEquals("WEB_PUSH", target.getDeviceType());
    assertEquals("Chrome on Windows", target.getDeviceName());
    assertEquals("https://example.com/push/xyz", target.getPushNotificationEndpoint());
    assertEquals("base64_p256dh_key", target.getP256dh());
    assertEquals("base64_auth_token", target.getAuth());
    assertTrue(target.getEnabled());

    assertTrue(notification.deleteByPk(connection));
    assertNotNull(notification.toString());
  }

  @Test
  void testNotificationLot() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    SES_AI_WEBAPP_M_NOTIFICATIONLot lot = new SES_AI_WEBAPP_M_NOTIFICATIONLot();
    lot.selectAll(connection);

    SES_AI_WEBAPP_M_NOTIFICATION notification = new SES_AI_WEBAPP_M_NOTIFICATION();
    notification.setNotificationId("N1");
    lot.add(notification);
    assertNotNull(lot.toString());

    when(ps.executeUpdate()).thenReturn(0);
    assertFalse(notification.updateByPk(connection));
    assertFalse(notification.deleteByPk(connection));
  }
}
