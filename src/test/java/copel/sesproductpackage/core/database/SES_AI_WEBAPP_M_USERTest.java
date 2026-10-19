package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Permission;
import copel.sesproductpackage.core.unit.Plan;
import copel.sesproductpackage.core.unit.Role;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SES_AI_WEBAPP_M_USERTest {

  @Test
  void testAuth() {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setRole(Role.システム管理者);
    assertTrue(user.hasSystemUseAuth());
    user.setRole(Role.システムユーザー);
    assertTrue(user.hasSystemUseAuth());
    user.setRole(Role.システム利用不可);
    assertFalse(user.hasSystemUseAuth());
    user.setRole(null);
    assertFalse(user.hasSystemUseAuth());
  }

  /** システムユーザー×プレミアムは管理系権限を持たないことを確認する（ロール・プラン双方から付与されないこと）. */
  @Test
  void testSystemUserPremiumHasNoAdminPermissions() {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setRole(Role.システムユーザー);
    user.setPlan(Plan.PREMIUM);
    assertFalse(user.getPermissions().contains(Permission.MANAGE_MASTER_DATA));
    assertFalse(user.getPermissions().contains(Permission.MANAGE_USERS));
    assertFalse(user.getPermissions().contains(Permission.MANAGE_SENDER));
    assertFalse(user.getPermissions().contains(Permission.MANAGE_FROM_GROUP));
    assertFalse(user.getPermissions().contains(Permission.VIEW_USAGE_HISTORY));
  }

  /** 運用×フリーは送信元グループ・送信者・ユーザー管理の権限を持つことを確認する（ロール由来）. */
  @Test
  void testOperationFreeHasGroupSenderUserManagement() {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setRole(Role.運用);
    user.setPlan(Plan.FREE);
    assertTrue(user.getPermissions().contains(Permission.MANAGE_FROM_GROUP));
    assertTrue(user.getPermissions().contains(Permission.MANAGE_SENDER));
    assertTrue(user.getPermissions().contains(Permission.MANAGE_USERS));
  }

  @Test
  void testGetPermissionsBranches() {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    assertTrue(user.getPermissions().isEmpty());

    user.setRole(Role.システム管理者);
    user.setPlan(null);
    assertFalse(user.getPermissions().isEmpty());

    // role 未設定はプランに関係なく権限なし
    user.setRole(null);
    user.setPlan(Plan.FREE);
    assertTrue(user.getPermissions().isEmpty());

    user.setRole(Role.システムユーザー);
    user.setPlan(Plan.PREMIUM);
    assertFalse(user.getPermissions().isEmpty());
  }

  @Test
  void registerInfoListImportPermissionRequiresPremiumPlanAndGeneralRoleOrHigher() {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setPlan(Plan.PREMIUM);
    user.setRole(Role.システム利用不可);
    assertFalse(user.getPermissions().contains(Permission.REGISTER_INFO_LIST_IMPORT));

    user.setRole(Role.システムユーザー);
    assertTrue(user.getPermissions().contains(Permission.REGISTER_INFO_LIST_IMPORT));

    user.setPlan(Plan.FREE);
    user.setRole(Role.システム管理者);
    assertFalse(user.getPermissions().contains(Permission.REGISTER_INFO_LIST_IMPORT));
  }

  @Test
  void testNullScenarios() throws SQLException {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    Connection connection = mock(Connection.class);

    assertEquals(0, user.insert(null));

    user.selectByPk(null);
    user.setUserId(null);
    user.selectByPk(connection);

    assertFalse(user.updateByPk(null));
    user.setUserId(null);
    assertFalse(user.updateByPk(connection));

    assertFalse(user.deleteByPk(null));
    user.setUserId(null);
    assertFalse(user.deleteByPk(connection));

    user.setRole(null);
    user.setPlan(null);
    user.setTenantId("default");
    user.setRegisterDate(null);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    user.setUserId("U1");
    user.insert(connection);
    user.updateByPk(connection);

    assertNotNull(user.toString());
    assertNotNull(user.hashCode());
    assertTrue(user.equals(user));
    assertFalse(user.equals(null));
    assertFalse(user.equals(new Object()));
  }

  @Test
  void testResultSetBranches() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);

    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId("U1");
    user.setTenantId("default");

    when(rs.next()).thenReturn(false);
    user.selectByPk(connection);

    reset(rs);
    when(rs.next()).thenReturn(true);
    when(rs.getString("role_cd")).thenReturn(null);
    user.selectByPk(connection);
    assertNull(user.getRole());
  }

  @Test
  void testUSER() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(1);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    when(rs.getString("user_id")).thenReturn("U1");
    when(rs.getString("user_name")).thenReturn("Name1");
    when(rs.getString("role_cd")).thenReturn("10");
    when(rs.getString("plan_cd")).thenReturn("P1");
    when(rs.getString("register_date")).thenReturn("2026-01-01 00:00:00");
    when(rs.getString("register_user")).thenReturn("admin");

    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId("U1");
    user.setTenantId("default");
    user.setUserName("Name1");
    user.setRole(Role.システムユーザー);
    user.setRegisterDate(new OriginalDateTime());

    assertEquals(1, user.insert(connection));
    assertTrue(user.updateByPk(connection));

    SES_AI_WEBAPP_M_USER target = new SES_AI_WEBAPP_M_USER("test-tenant");
    target.setUserId("U1");
    target.setTenantId("default");
    target.selectByPk(connection);

    assertEquals("U1", target.getUserId());
    assertEquals("Name1", target.getUserName());
    assertEquals(Role.システムユーザー, target.getRole());

    assertTrue(user.deleteByPk(connection));
    assertNotNull(user.toString());
    assertNotNull(user.getPermissions());
  }

  @Test
  void testUSERLot() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);
    when(rs.getString("role_cd")).thenReturn("99");
    when(rs.getString("plan_cd")).thenReturn("P1");
    when(rs.getString("tenant_id")).thenReturn("test-tenant");

    SES_AI_WEBAPP_M_USERLot lot = new SES_AI_WEBAPP_M_USERLot();
    lot.selectAll(connection, "test-tenant");

    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId("U1");
    user.setTenantId("default");
    lot.add(user);
    assertNotNull(lot.toString());

    when(ps.executeUpdate()).thenReturn(0);
    assertFalse(user.updateByPk(connection));
    assertFalse(user.deleteByPk(connection));
  }

  @Test
  void testSelectByPkWithoutTenantId() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    when(rs.getString("user_id")).thenReturn("U1");
    when(rs.getString("user_name")).thenReturn("Name1");
    when(rs.getString("role_cd")).thenReturn("10");
    when(rs.getString("plan_cd")).thenReturn("P1");
    when(rs.getString("register_date")).thenReturn("2026-01-01 00:00:00");
    when(rs.getString("register_user")).thenReturn("admin");
    when(rs.getString("tenant_id")).thenReturn("test-tenant");

    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId("U1");
    user.selectByPkWithoutTenantId(connection);

    assertEquals("U1", user.getUserId());
    assertEquals("Name1", user.getUserName());
    assertEquals(Role.システムユーザー, user.getRole());
    assertEquals("test-tenant", user.getTenantId());
  }

  @Test
  void testSelectByPkWithoutTenantIdNullHandling() throws SQLException {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    Connection connection = mock(Connection.class);

    user.selectByPkWithoutTenantId(null);

    user.setUserId(null);
    user.selectByPkWithoutTenantId(connection);
  }

  @Test
  void testSelectByPkWithoutTenantIdEmptyResult() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(false);

    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId("U1");
    user.selectByPkWithoutTenantId(connection);

    assertEquals("U1", user.getUserId());
    assertNull(user.getUserName());
  }

  @Test
  void testUSERLotSelectAllWithoutTenantId() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, true, false);
    when(rs.getString("user_id")).thenReturn("U1", "U2");
    when(rs.getString("user_name")).thenReturn("Name1", "Name2");
    when(rs.getString("role_cd")).thenReturn("10", "20");
    when(rs.getString("plan_cd")).thenReturn("P1", "P2");
    when(rs.getString("register_date")).thenReturn("2026-01-01 00:00:00", "2026-01-02 00:00:00");
    when(rs.getString("register_user")).thenReturn("admin", "user");
    when(rs.getString("tenant_id")).thenReturn("tenant-a", "tenant-b");

    SES_AI_WEBAPP_M_USERLot lot = new SES_AI_WEBAPP_M_USERLot();
    lot.selectAllWithoutTenantId(connection);

    assertEquals(2, lot.size());
  }

  @Test
  void testDeleteByPkWithoutTenantIdFilter() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(1);

    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId("U1");

    assertTrue(user.deleteByPkWithoutTenantIdFilter(connection));
  }

  @Test
  void testDeleteByPkWithoutTenantIdFilterNullHandling() throws SQLException {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    Connection connection = mock(Connection.class);

    assertFalse(user.deleteByPkWithoutTenantIdFilter(null));

    user.setUserId(null);
    assertFalse(user.deleteByPkWithoutTenantIdFilter(connection));
  }

  @Test
  void testDeleteByPkWithoutTenantIdFilterFailure() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(0);

    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId("U1");

    assertFalse(user.deleteByPkWithoutTenantIdFilter(connection));
  }

  /** システム利用不可（ロール 00）はプレミアムプランであっても権限を一切持たないことを確認する. */
  @Test
  void testSystemUnusableRolePremiumHasNoPermissions() {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setRole(Role.システム利用不可);
    user.setPlan(Plan.PREMIUM);
    assertTrue(user.getPermissions().isEmpty());
  }

  /** ロール未設定（null）はプレミアムプランであっても権限を一切持たないことを確認する. */
  @Test
  void testNullRolePremiumHasNoPermissions() {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setRole(null);
    user.setPlan(Plan.PREMIUM);
    assertTrue(user.getPermissions().isEmpty());
  }

  /** 直近のSELECTで行が取得できなかった場合は未登録（isRegistered=false）であることを確認する. */
  @Test
  void testIsRegisteredFalseWhenRowNotFound() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(false);

    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    assertFalse(user.isRegistered());
    user.setUserId("U1");
    user.selectByPkWithoutTenantId(connection);
    assertFalse(user.isRegistered());
    assertNull(user.getRole());
  }

  /** 行が取得できた場合は登録済み（isRegistered=true）となり、ロールが null（role_cd 未設定）でも区別できることを確認する. */
  @Test
  void testIsRegisteredTrueWhenRowFoundWithNullRole() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true);
    when(rs.getString("role_cd")).thenReturn(null);

    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId("U1");
    user.selectByPkWithoutTenantId(connection);
    assertTrue(user.isRegistered());
    assertNull(user.getRole());
  }

  /** selectByPk（テナント条件あり）でも登録済みフラグが反映されることを確認する. */
  @Test
  void testIsRegisteredReflectsSelectByPk() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId("U1");
    user.selectByPk(connection);
    assertTrue(user.isRegistered());
    user.selectByPk(connection);
    assertFalse(user.isRegistered());
  }
}
