package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Permission;
import copel.sesproductpackage.core.unit.Plan;
import copel.sesproductpackage.core.unit.Role;
import jakarta.persistence.EntityManager;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SES_AI_WEBAPP_M_USER のテストクラス.
 *
 * <p>JPA ベースの CRUD 操作とビジネスロジックをモックして検証します。
 *
 * @author Copel Co., Ltd.
 */
class SES_AI_WEBAPP_M_USERTest {

  private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    entityManager = mock(EntityManager.class);
    SES_AI_WEBAPP_M_USER.setEntityManager(entityManager);
  }

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

  @Test
  void testGetPermissionsBranches() {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    assertTrue(user.getPermissions().isEmpty());

    user.setRole(Role.システム管理者);
    user.setPlan(null);
    assertFalse(user.getPermissions().isEmpty());

    user.setRole(null);
    user.setPlan(Plan.FREE);
    assertFalse(user.getPermissions().isEmpty());

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
  void testInsertSuccess() throws SQLException {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId("U1");
    user.setUserName("Test User");
    user.setRole(Role.システムユーザー);
    user.setPlan(Plan.PREMIUM);
    user.setRegisterDate(new OriginalDateTime());
    user.setRegisterUser("admin");

    int result = user.insert(null);
    assertEquals(1, result);
    verify(entityManager, times(1)).persist(user);
    verify(entityManager, times(1)).flush();
  }

  @Test
  void testInsertWithoutEntityManager() throws SQLException {
    SES_AI_WEBAPP_M_USER.setEntityManager(null);
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId("U1");

    assertThrows(SQLException.class, () -> user.insert(null));
  }

  @Test
  void testSelectByPkSuccess() throws SQLException {
    SES_AI_WEBAPP_M_USER found = new SES_AI_WEBAPP_M_USER("test-tenant");
    found.setUserId("U1");
    found.setUserName("Test User");
    found.setRole(Role.システムユーザー);
    found.setPlan(Plan.PREMIUM);
    found.setRegisterDate(new OriginalDateTime("2026-06-04 10:00:00"));
    found.setRegisterUser("admin");
    found.setTenantId("test-tenant");

    when(entityManager.find(SES_AI_WEBAPP_M_USER.class, "U1")).thenReturn(found);

    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId("U1");
    user.selectByPk(null);

    assertEquals("U1", user.getUserId());
    assertEquals("Test User", user.getUserName());
    assertEquals(Role.システムユーザー, user.getRole());
  }

  @Test
  void testSelectByPkWithNullId() throws SQLException {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId(null);

    user.selectByPk(null);

    verify(entityManager, never()).find(any(), any());
  }

  @Test
  void testSelectByPkWithoutTenantIdSuccess() throws SQLException {
    SES_AI_WEBAPP_M_USER found = new SES_AI_WEBAPP_M_USER("test-tenant");
    found.setUserId("U1");
    found.setUserName("Test User");
    found.setRole(Role.システムユーザー);
    found.setPlan(Plan.PREMIUM);
    found.setRegisterDate(new OriginalDateTime("2026-06-04 10:00:00"));
    found.setRegisterUser("admin");
    found.setTenantId("test-tenant");

    when(entityManager.find(SES_AI_WEBAPP_M_USER.class, "U1")).thenReturn(found);

    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId("U1");
    user.selectByPkWithoutTenantId(null);

    assertEquals("U1", user.getUserId());
    assertEquals("Test User", user.getUserName());
    assertEquals("test-tenant", user.getTenantId());
  }

  @Test
  void testSelectByPkWithoutTenantIdWithNullId() throws SQLException {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId(null);

    user.selectByPkWithoutTenantId(null);

    verify(entityManager, never()).find(any(), any());
  }

  @Test
  void testUpdateByPkSuccess() throws SQLException {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId("U1");
    user.setUserName("Updated User");
    user.setRole(Role.システム管理者);

    when(entityManager.merge(any(SES_AI_WEBAPP_M_USER.class))).thenReturn(user);

    assertTrue(user.updateByPk(null));
    verify(entityManager, times(1)).merge(user);
    verify(entityManager, times(1)).flush();
  }

  @Test
  void testUpdateByPkWithNullId() throws SQLException {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId(null);

    assertFalse(user.updateByPk(null));
    verify(entityManager, never()).merge(any());
  }

  @Test
  void testUpdateByPkWithoutTenantIdSuccess() throws SQLException {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId("U1");
    user.setUserName("Updated User");
    user.setTenantId("test-tenant");

    when(entityManager.merge(any(SES_AI_WEBAPP_M_USER.class))).thenReturn(user);

    assertTrue(user.updateByPkWithoutTenantId(null));
    verify(entityManager, times(1)).merge(user);
    verify(entityManager, times(1)).flush();
  }

  @Test
  void testUpdateByPkWithoutTenantIdWithNullId() throws SQLException {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId(null);

    assertFalse(user.updateByPkWithoutTenantId(null));
    verify(entityManager, never()).merge(any());
  }

  @Test
  void testDeleteByPkSuccess() throws SQLException {
    SES_AI_WEBAPP_M_USER found = new SES_AI_WEBAPP_M_USER("test-tenant");
    found.setUserId("U1");

    when(entityManager.find(SES_AI_WEBAPP_M_USER.class, "U1")).thenReturn(found);

    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId("U1");

    assertTrue(user.deleteByPk(null));
    verify(entityManager, times(1)).remove(found);
    verify(entityManager, times(1)).flush();
  }

  @Test
  void testDeleteByPkWithNullId() throws SQLException {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId(null);

    assertFalse(user.deleteByPk(null));
    verify(entityManager, never()).remove(any());
  }

  @Test
  void testDeleteByPkWithoutTenantIdFilterSuccess() throws SQLException {
    SES_AI_WEBAPP_M_USER found = new SES_AI_WEBAPP_M_USER("test-tenant");
    found.setUserId("U1");

    when(entityManager.find(SES_AI_WEBAPP_M_USER.class, "U1")).thenReturn(found);

    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId("U1");

    assertTrue(user.deleteByPkWithoutTenantIdFilter(null));
    verify(entityManager, times(1)).remove(found);
    verify(entityManager, times(1)).flush();
  }

  @Test
  void testDeleteByPkWithoutTenantIdFilterWithNullId() throws SQLException {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId(null);

    assertFalse(user.deleteByPkWithoutTenantIdFilter(null));
    verify(entityManager, never()).remove(any());
  }

  @Test
  void testToString() {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER("test-tenant");
    user.setUserId("U1");
    user.setUserName("Test User");

    assertNotNull(user.toString());
    assertTrue(user.toString().contains("U1"));
  }
}
