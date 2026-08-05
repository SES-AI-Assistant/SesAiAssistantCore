package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SES_AI_WEBAPP_M_NOTIFICATIONTest {

  private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    entityManager = mock(EntityManager.class);
    SES_AI_WEBAPP_M_NOTIFICATION.setEntityManager(entityManager);
  }

  @Test
  void testNullScenarios() throws SQLException {
    SES_AI_WEBAPP_M_NOTIFICATION notification = new SES_AI_WEBAPP_M_NOTIFICATION("test-tenant");
    Connection connection = mock(Connection.class);

    notification.setNotificationId(null);
    notification.selectByPk(connection);

    assertFalse(notification.updateByPk(connection));
    notification.setNotificationId(null);
    assertFalse(notification.updateByPk(connection));

    assertFalse(notification.deleteByPk(connection));
    notification.setNotificationId(null);
    assertFalse(notification.deleteByPk(connection));

    assertNotNull(notification.toString());
    assertNotNull(notification.hashCode());
    assertTrue(notification.equals(notification));
    assertFalse(notification.equals(null));
    assertFalse(notification.equals(new Object()));
  }

  @Test
  void testNotification() throws SQLException {
    SES_AI_WEBAPP_M_NOTIFICATION found = new SES_AI_WEBAPP_M_NOTIFICATION("test-tenant");
    found.setNotificationId("N1");
    found.setUserId("U1");
    found.setDeviceType("WEB_PUSH");
    found.setDeviceName("Chrome on Windows");
    found.setPushNotificationEndpoint("https://example.com/push/xyz");
    found.setP256dh("base64_p256dh_key");
    found.setAuth("base64_auth_token");
    found.setEnabled(true);
    found.setRegisterDate(new OriginalDateTime("2026-04-22 00:00:00"));
    found.setRegisterUser("admin");

    when(entityManager.find(SES_AI_WEBAPP_M_NOTIFICATION.class, "N1")).thenReturn(found);

    SES_AI_WEBAPP_M_NOTIFICATION notification = new SES_AI_WEBAPP_M_NOTIFICATION("test-tenant");
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

    assertEquals(1, notification.insert(null));
    verify(entityManager, times(1)).persist(notification);
    verify(entityManager, times(1)).flush();

    assertTrue(notification.updateByPk(null));
    verify(entityManager, times(1)).merge(notification);

    SES_AI_WEBAPP_M_NOTIFICATION target = new SES_AI_WEBAPP_M_NOTIFICATION("test-tenant");
    target.setNotificationId("N1");
    target.selectByPk(null);

    assertEquals("N1", target.getNotificationId());
    assertEquals("U1", target.getUserId());
    assertEquals("WEB_PUSH", target.getDeviceType());
    assertEquals("Chrome on Windows", target.getDeviceName());
    assertEquals("https://example.com/push/xyz", target.getPushNotificationEndpoint());
    assertEquals("base64_p256dh_key", target.getP256dh());
    assertEquals("base64_auth_token", target.getAuth());
    assertTrue(target.getEnabled());

    assertTrue(notification.deleteByPk(null));
    verify(entityManager, times(1)).remove(found);
    assertNotNull(notification.toString());
  }

  @Test
  void testNotificationLot() throws SQLException {
    SES_AI_WEBAPP_M_NOTIFICATION notification = new SES_AI_WEBAPP_M_NOTIFICATION("test-tenant");
    notification.setNotificationId("N1");

    SES_AI_WEBAPP_M_NOTIFICATIONLot lot = new SES_AI_WEBAPP_M_NOTIFICATIONLot();
    lot.add(notification);
    assertNotNull(lot.toString());
  }

  @Test
  void testResultSetBranches() throws SQLException {
    SES_AI_WEBAPP_M_NOTIFICATION notification = new SES_AI_WEBAPP_M_NOTIFICATION("test-tenant");
    notification.setNotificationId("N1");

    when(entityManager.find(SES_AI_WEBAPP_M_NOTIFICATION.class, "N1")).thenReturn(null);
    notification.selectByPk(null);

    SES_AI_WEBAPP_M_NOTIFICATION found = new SES_AI_WEBAPP_M_NOTIFICATION("test-tenant");
    found.setNotificationId("N1");
    found.setRegisterDate(null);
    when(entityManager.find(SES_AI_WEBAPP_M_NOTIFICATION.class, "N1")).thenReturn(found);
    notification.selectByPk(null);
  }
}
