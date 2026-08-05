package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SES_AI_M_TENANT のテストクラス.
 *
 * <p>JPA ベースの CRUD 操作をモックして検証します。
 *
 * @author Copel Co., Ltd.
 */
class SES_AI_M_TENANTTest {

  private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    entityManager = mock(EntityManager.class);
    SES_AI_M_TENANT.setEntityManager(entityManager);
  }

  @Test
  void testInsertSuccess() throws SQLException {
    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");
    tenant.setTenantName("Test Tenant");
    tenant.setTenantStatusCd("ACTIVE");
    tenant.setRegisterDate(new OriginalDateTime());
    tenant.setRegisterUser("admin");

    int result = tenant.insert(null);
    assertEquals(1, result);
    verify(entityManager, times(1)).persist(tenant);
    verify(entityManager, times(1)).flush();
  }

  @Test
  void testInsertWithoutEntityManager() throws SQLException {
    SES_AI_M_TENANT.setEntityManager(null);
    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");

    assertThrows(SQLException.class, () -> tenant.insert(null));
  }

  @Test
  void testInsertWithPersistenceException() throws SQLException {
    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");

    doThrow(new PersistenceException("Persist failed"))
        .when(entityManager)
        .persist(any(SES_AI_M_TENANT.class));

    assertThrows(SQLException.class, () -> tenant.insert(null));
  }

  @Test
  void testSelectByPkSuccess() throws SQLException {
    SES_AI_M_TENANT found = new SES_AI_M_TENANT("test-tenant");
    found.setTenantId("T1");
    found.setTenantName("Test Tenant");
    found.setTenantStatusCd("ACTIVE");
    found.setRegisterDate(new OriginalDateTime("2026-06-04 10:00:00"));
    found.setRegisterUser("admin");

    when(entityManager.find(SES_AI_M_TENANT.class, "T1")).thenReturn(found);

    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");
    tenant.selectByPk(null);

    assertEquals("T1", tenant.getTenantId());
    assertEquals("Test Tenant", tenant.getTenantName());
    assertEquals("ACTIVE", tenant.getTenantStatusCd());
    assertEquals("admin", tenant.getRegisterUser());
    verify(entityManager, times(1)).find(SES_AI_M_TENANT.class, "T1");
  }

  @Test
  void testSelectByPkNotFound() throws SQLException {
    when(entityManager.find(SES_AI_M_TENANT.class, "T1")).thenReturn(null);

    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");
    tenant.selectByPk(null);

    assertEquals("T1", tenant.getTenantId());
  }

  @Test
  void testSelectByPkWithNullId() throws SQLException {
    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId(null);

    tenant.selectByPk(null);

    verify(entityManager, never()).find(any(), any());
  }

  @Test
  void testSelectByPkWithoutEntityManager() throws SQLException {
    SES_AI_M_TENANT.setEntityManager(null);
    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");

    assertThrows(SQLException.class, () -> tenant.selectByPk(null));
  }

  @Test
  void testSelectByPkWithPersistenceException() throws SQLException {
    when(entityManager.find(SES_AI_M_TENANT.class, "T1"))
        .thenThrow(new PersistenceException("Find failed"));

    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");

    assertThrows(SQLException.class, () -> tenant.selectByPk(null));
  }

  @Test
  void testUpdateByPkSuccess() throws SQLException {
    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");
    tenant.setTenantName("Updated Tenant");
    tenant.setTenantStatusCd("INACTIVE");

    when(entityManager.merge(any(SES_AI_M_TENANT.class))).thenReturn(tenant);

    assertTrue(tenant.updateByPk(null));
    verify(entityManager, times(1)).merge(tenant);
    verify(entityManager, times(1)).flush();
  }

  @Test
  void testUpdateByPkWithNullId() throws SQLException {
    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId(null);

    assertFalse(tenant.updateByPk(null));
    verify(entityManager, never()).merge(any());
  }

  @Test
  void testUpdateByPkWithoutEntityManager() throws SQLException {
    SES_AI_M_TENANT.setEntityManager(null);
    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");

    assertThrows(SQLException.class, () -> tenant.updateByPk(null));
  }

  @Test
  void testUpdateByPkWithPersistenceException() throws SQLException {
    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");

    doThrow(new PersistenceException("Merge failed"))
        .when(entityManager)
        .merge(any(SES_AI_M_TENANT.class));

    assertThrows(SQLException.class, () -> tenant.updateByPk(null));
  }

  @Test
  void testDeleteByPkSuccess() throws SQLException {
    SES_AI_M_TENANT found = new SES_AI_M_TENANT("test-tenant");
    found.setTenantId("T1");

    when(entityManager.find(SES_AI_M_TENANT.class, "T1")).thenReturn(found);

    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");

    assertTrue(tenant.deleteByPk(null));
    verify(entityManager, times(1)).find(SES_AI_M_TENANT.class, "T1");
    verify(entityManager, times(1)).remove(found);
    verify(entityManager, times(1)).flush();
  }

  @Test
  void testDeleteByPkNotFound() throws SQLException {
    when(entityManager.find(SES_AI_M_TENANT.class, "T1")).thenReturn(null);

    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");

    assertFalse(tenant.deleteByPk(null));
  }

  @Test
  void testDeleteByPkWithNullId() throws SQLException {
    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId(null);

    assertFalse(tenant.deleteByPk(null));
    verify(entityManager, never()).find(any(), any());
  }

  @Test
  void testDeleteByPkWithoutEntityManager() throws SQLException {
    SES_AI_M_TENANT.setEntityManager(null);
    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");

    assertThrows(SQLException.class, () -> tenant.deleteByPk(null));
  }

  @Test
  void testDeleteByPkWithPersistenceException() throws SQLException {
    when(entityManager.find(SES_AI_M_TENANT.class, "T1"))
        .thenThrow(new PersistenceException("Find failed"));

    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");

    assertThrows(SQLException.class, () -> tenant.deleteByPk(null));
  }

  @Test
  void testToString() {
    SES_AI_M_TENANT tenant = new SES_AI_M_TENANT("test-tenant");
    tenant.setTenantId("T1");
    tenant.setTenantName("Test Tenant");
    tenant.setTenantStatusCd("ACTIVE");

    assertNotNull(tenant.toString());
    assertTrue(tenant.toString().contains("T1"));
  }

  @Test
  void testEqualsAndHashCode() {
    SES_AI_M_TENANT tenant1 = new SES_AI_M_TENANT("test-tenant");
    tenant1.setTenantId("T1");
    tenant1.setTenantName("Test Tenant");

    SES_AI_M_TENANT tenant2 = new SES_AI_M_TENANT("test-tenant");
    tenant2.setTenantId("T1");
    tenant2.setTenantName("Test Tenant");

    assertEquals(tenant1, tenant2);
    assertEquals(tenant1.hashCode(), tenant2.hashCode());
  }
}
