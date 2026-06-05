package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SES_AI_M_TENANTLotTest {

  @Test
  void testSelectAll() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, true, false);
    when(rs.getString("tenant_id")).thenReturn("T1", "T2");
    when(rs.getString("tenant_name")).thenReturn("Tenant 1", "Tenant 2");
    when(rs.getString("tenant_status_cd")).thenReturn("ACTIVE", "INACTIVE");
    when(rs.getString("register_date")).thenReturn("2026-06-04 10:00:00");
    when(rs.getString("register_user")).thenReturn("admin");

    SES_AI_M_TENANTLot lot = new SES_AI_M_TENANTLot();
    lot.selectAll(connection);

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

    SES_AI_M_TENANTLot lot = new SES_AI_M_TENANTLot();
    lot.selectAll(connection);

    assertEquals(0, lot.size());
  }

  @Test
  void testGetSelectAllSql() {
    SES_AI_M_TENANTLot lot = new SES_AI_M_TENANTLot();
    String sql = lot.getSelectAllSql();

    assertNotNull(sql);
    assertTrue(sql.contains("SELECT"));
    assertTrue(sql.contains("tenant_id"));
    assertTrue(sql.contains("tenant_status_cd"));
    assertFalse(sql.contains("WHERE"));
  }

  @Test
  void testGetSelectSql() {
    SES_AI_M_TENANTLot lot = new SES_AI_M_TENANTLot();
    String sql = lot.getSelectSql();

    assertNotNull(sql);
    assertTrue(sql.contains("SELECT"));
    assertTrue(sql.contains("WHERE"));
    assertTrue(sql.contains("tenant_id"));
    assertTrue(sql.contains("tenant_status_cd"));
  }

  @Test
  void testLotToString() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);
    when(rs.getString(anyString())).thenReturn("T1");

    SES_AI_M_TENANTLot lot = new SES_AI_M_TENANTLot();
    lot.selectAll(connection);

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

    SES_AI_M_TENANTLot lot = new SES_AI_M_TENANTLot();
    lot.selectAll(connection);
    assertEquals(0, lot.size());

    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT();
    tenant.setTenantId("T1");
    tenant.setTenantName("Test Tenant");
    lot.add(tenant);

    assertEquals(1, lot.size());
  }
}
