package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SES_AI_M_TENANTTest {

  @Test
  void testNullScenarios() throws SQLException {
    SES_AI_M_TENANT entity = new SES_AI_M_TENANT("test-tenant");
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(false);

    assertEquals(0, entity.insert(null));
    entity.selectByPk(null);
    entity.selectByPk(connection);
    assertFalse(entity.updateByPk(null));
    assertFalse(entity.updateByPk(connection));
    assertFalse(entity.deleteByPk(null));
    assertFalse(entity.deleteByPk(connection));

    entity.setRegisterDate(null);
    entity.setTenantId("T1");
    when(ps.executeUpdate()).thenReturn(1);
    entity.insert(connection);
    entity.updateByPk(connection);
  }

  @Test
  void testTenantInsert() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(1);

    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");
    tenant.setTenantName("Test Tenant");
    tenant.setTenantStatusCd("ACTIVE");
    tenant.setRegisterDate(new OriginalDateTime());
    tenant.setRegisterUser("admin");

    assertEquals(1, tenant.insert(connection));
    verify(ps, times(1)).setString(1, "T1");
    verify(ps, times(1)).setString(2, "Test Tenant");
    verify(ps, times(1)).setString(3, "ACTIVE");
    verify(ps, times(1)).setString(5, "admin");
  }

  @Test
  void testTenantSelectByPk() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);
    when(rs.getString("tenant_id")).thenReturn("T1");
    when(rs.getString("tenant_name")).thenReturn("Test Tenant");
    when(rs.getString("tenant_status_cd")).thenReturn("ACTIVE");
    when(rs.getString("register_date")).thenReturn("2026-06-04 10:00:00");
    when(rs.getString("register_user")).thenReturn("admin");

    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");
    tenant.selectByPk(connection);

    assertEquals("T1", tenant.getTenantId());
    assertEquals("Test Tenant", tenant.getTenantName());
    assertEquals("ACTIVE", tenant.getTenantStatusCd());
    assertEquals("admin", tenant.getRegisterUser());
  }

  @Test
  void testTenantUpdateByPk() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(1);

    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");
    tenant.setTenantName("Updated Tenant");
    tenant.setTenantStatusCd("INACTIVE");

    assertTrue(tenant.updateByPk(connection));
    verify(ps, times(1)).setString(1, "Updated Tenant");
    verify(ps, times(1)).setString(2, "INACTIVE");
    verify(ps, times(1)).setString(3, "T1");
  }

  @Test
  void testTenantUpdateByPkFail() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(0);

    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");

    assertFalse(tenant.updateByPk(connection));
  }

  @Test
  void testTenantDeleteByPk() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(1);

    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");

    assertTrue(tenant.deleteByPk(connection));
    verify(ps, times(1)).setString(1, "T1");
  }

  @Test
  void testTenantDeleteByPkFail() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(0);

    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");

    assertFalse(tenant.deleteByPk(connection));
  }

  @Test
  void testTenantToString() {
    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");
    tenant.setTenantName("Test Tenant");
    tenant.setTenantStatusCd("ACTIVE");

    assertNotNull(tenant.toString());
    assertTrue(tenant.toString().contains("T1"));
  }

  @Test
  void testTenantEqualsAndHashCode() {
    SES_AI_M_TENANT tenant1 = new SES_AI_M_TENANT("test-tenant");
    tenant1.setTenantId("T1");
    tenant1.setTenantName("Test Tenant");

    SES_AI_M_TENANT tenant2 = new SES_AI_M_TENANT("test-tenant");
    tenant2.setTenantId("T1");
    tenant2.setTenantName("Test Tenant");

    assertEquals(tenant1, tenant2);
    assertEquals(tenant1.hashCode(), tenant2.hashCode());
  }

  @Test
  void testTenantSelectByPkWithNullId() throws SQLException {
    Connection connection = mock(Connection.class);
    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId(null);

    tenant.selectByPk(connection);

    verify(connection, never()).prepareStatement(anyString());
  }
}
