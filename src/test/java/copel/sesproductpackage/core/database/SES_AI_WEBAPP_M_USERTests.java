package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Plan;
import copel.sesproductpackage.core.unit.Role;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SES_AI_WEBAPP_M_USERTests {

  @Test
  void testAuth() {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER();
    user.setRole(Role.システム管理者);
    assertTrue(user.hasSystemUseAuth());
    user.setRole(Role.システムユーザー);
    assertTrue(user.hasSystemUseAuth());
  }

  @Test
  void testNullScenarios() throws SQLException {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER();
    Connection connection = mock(Connection.class);

    // insert null connection
    assertEquals(0, user.insert(null));

    // selectByPk null connection or userId
    user.selectByPk(null);
    user.setUserId(null);
    user.selectByPk(connection);

    // updateByPk null connection or userId
    assertFalse(user.updateByPk(null));
    user.setUserId(null);
    assertFalse(user.updateByPk(connection));

    // deleteByPk null connection or userId
    assertFalse(user.deleteByPk(null));
    user.setUserId(null);
    assertFalse(user.deleteByPk(connection));

    user.setRole(null);
    user.setRegisterDate(null);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    user.setUserId("U1");
    user.insert(connection);
    user.updateByPk(connection);

    // Covering Lombok equals/hashCode/toString
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

    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER();
    user.setUserId("U1");

    // resultSet.next() is false
    when(rs.next()).thenReturn(false);
    user.selectByPk(connection);

    // role is null in DB
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
    when(rs.getString("company_id")).thenReturn("C1");
    when(rs.getString("role_cd")).thenReturn("10");
    when(rs.getString("register_date")).thenReturn("2026-01-01 00:00:00");
    when(rs.getString("register_user")).thenReturn("admin");

    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER();
    user.setUserId("U1");
    user.setUserName("Name1");
    user.setCompanyId("C1");
    user.setRole(Role.システムユーザー);
    user.setRegisterDate(new OriginalDateTime());

    assertEquals(1, user.insert(connection));
    assertTrue(user.updateByPk(connection));

    SES_AI_WEBAPP_M_USER target = new SES_AI_WEBAPP_M_USER();
    target.setUserId("U1");
    target.selectByPk(connection);

    assertEquals("U1", target.getUserId());
    assertEquals("Name1", target.getUserName());
    assertEquals("C1", target.getCompanyId());
    assertEquals(Role.システムユーザー, target.getRole());

    assertTrue(user.deleteByPk(connection));
    assertNotNull(user.toString());
  }

  @Test
  void testGetPermissions() {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER();
    assertTrue(user.getPermissions().isEmpty());

    user.setRole(Role.システム管理者);
    user.setPlan(Plan.PREMIUM);
    assertFalse(user.getPermissions().isEmpty());
    
    // role only
    user.setPlan(null);
    assertFalse(user.getPermissions().isEmpty());

    // plan only
    user.setRole(null);
    user.setPlan(Plan.FREE);
    assertFalse(user.getPermissions().isEmpty());
  }

  @Test
  void testHasSystemUseAuth_NullRole() {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER();
    user.setRole(null);
    assertFalse(user.hasSystemUseAuth());
  }

  @Test
  void testLombokMethods() {
    SES_AI_WEBAPP_M_USER user1 = new SES_AI_WEBAPP_M_USER();
    user1.setUserId("u1");
    user1.setUserName("n1");
    user1.setCompanyId("c1");
    user1.setRole(Role.システム管理者);
    user1.setPlan(Plan.PREMIUM);
    user1.setRegisterUser("admin");
    user1.setRegisterDate(new OriginalDateTime());

    SES_AI_WEBAPP_M_USER user2 = new SES_AI_WEBAPP_M_USER();
    user2.setUserId("u1");
    user2.setUserName("n1");
    user2.setCompanyId("c1");
    user2.setRole(Role.システム管理者);
    user2.setPlan(Plan.PREMIUM);
    user2.setRegisterUser("admin");
    user2.setRegisterDate(user1.getRegisterDate());

    // Basic Lombok checks
    assertTrue(user1.equals(user2));
    assertEquals(user1.hashCode(), user2.hashCode());
    assertNotNull(user1.toString());
    assertTrue(user1.canEqual(user2));

    // Field-by-field equals coverage
    user2.setUserId("u2");
    assertNotEquals(user1, user2);
    user2.setUserId("u1");

    user2.setUserName("n2");
    assertNotEquals(user1, user2);
    user2.setUserName("n1");

    user2.setCompanyId("c2");
    assertNotEquals(user1, user2);
    user2.setCompanyId("c1");
    
    user1.setRole(null);
    assertNotEquals(user1, user2);
    user1.setRole(Role.システム管理者);
    
    // Superclass field equality (EntityBase)
    user2.setRegisterUser("other");
    assertNotEquals(user1, user2);
    user2.setRegisterUser("admin");
    
    // canEqual and equals branches
    assertNotEquals(user1, null);
    assertNotEquals(user1, new Object());
    assertFalse(user1.canEqual(new Object()));
    
    SES_AI_WEBAPP_M_USER userSub = new SES_AI_WEBAPP_M_USER() {
      @Override
      public boolean canEqual(Object other) {
        return false;
      }
    };
    assertFalse(user1.equals(userSub));
  }

  @Test
  void testInsertUpdateWithRoleAndPlan() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeUpdate()).thenReturn(1);

    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER();
    user.setUserId("U1");
    user.setRole(Role.システム管理者);
    user.setPlan(Plan.FREE);
    user.setRegisterDate(new OriginalDateTime());

    assertEquals(1, user.insert(connection));
    assertTrue(user.updateByPk(connection));
  }

  @Test
  void testSelectByPkAllFields() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true);

    when(rs.getString("user_id")).thenReturn("U1");
    when(rs.getString("user_name")).thenReturn("N1");
    when(rs.getString("company_id")).thenReturn("C1");
    when(rs.getString("role_cd")).thenReturn("99");
    when(rs.getString("plan_cd")).thenReturn("10");
    when(rs.getString("register_date")).thenReturn("2026-01-01");
    when(rs.getString("register_user")).thenReturn("admin");

    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER();
    user.setUserId("U1");
    user.selectByPk(connection);

    assertEquals("U1", user.getUserId());
    assertEquals("N1", user.getUserName());
    assertEquals("C1", user.getCompanyId());
    assertEquals(Role.システム管理者, user.getRole());
    assertEquals(Plan.PREMIUM, user.getPlan());
    assertEquals("admin", user.getRegisterUser());
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

    SES_AI_WEBAPP_M_USERLot lot = new SES_AI_WEBAPP_M_USERLot();
    lot.selectAll(connection);

    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER();
    user.setUserId("U1");
    lot.add(user);
    assertNotNull(lot.toString());

    // Fails for update/delete
    when(ps.executeUpdate()).thenReturn(0);
    assertFalse(user.updateByPk(connection));
    assertFalse(user.deleteByPk(connection));
  }
}
