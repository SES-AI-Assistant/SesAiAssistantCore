package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import jakarta.persistence.EntityManager;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SES_AI_M_GROUP のテストクラス.
 *
 * <p>JPA ベースの CRUD 操作をモックして検証します。
 *
 * @author Copel Co., Ltd.
 */
class SES_AI_M_GROUPTest {

  private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    entityManager = mock(EntityManager.class);
    SES_AI_M_GROUP.setEntityManager(entityManager);
  }

  @Test
  void testInsertSuccess() throws SQLException {
    SES_AI_M_GROUP group = new SES_AI_M_GROUP("test-tenant");
    group.setFromGroup("G1");
    group.setGroupName("Test Group");
    group.setRegisterDate(new OriginalDateTime());
    group.setRegisterUser("admin");

    int result = group.insert(null);
    assertEquals(1, result);
    verify(entityManager, times(1)).persist(group);
    verify(entityManager, times(1)).flush();
  }

  @Test
  void testInsertWithoutEntityManager() throws SQLException {
    SES_AI_M_GROUP.setEntityManager(null);
    SES_AI_M_GROUP group = new SES_AI_M_GROUP("test-tenant");
    group.setFromGroup("G1");

    assertThrows(SQLException.class, () -> group.insert(null));
  }

  @Test
  void testSelectByPkSuccess() throws SQLException {
    SES_AI_M_GROUP found = new SES_AI_M_GROUP("test-tenant");
    found.setFromGroup("G1");
    found.setGroupName("Test Group");
    found.setRegisterDate(new OriginalDateTime("2026-06-04 10:00:00"));
    found.setRegisterUser("admin");

    when(entityManager.find(SES_AI_M_GROUP.class, "G1")).thenReturn(found);

    SES_AI_M_GROUP group = new SES_AI_M_GROUP("test-tenant");
    group.setFromGroup("G1");
    group.selectByPk(null);

    assertEquals("G1", group.getFromGroup());
    assertEquals("Test Group", group.getGroupName());
    assertEquals("admin", group.getRegisterUser());
  }

  @Test
  void testSelectByPkWithNullId() throws SQLException {
    SES_AI_M_GROUP group = new SES_AI_M_GROUP("test-tenant");
    group.setFromGroup(null);

    group.selectByPk(null);

    verify(entityManager, never()).find(any(), any());
  }

  @Test
  void testUpdateByPkSuccess() throws SQLException {
    SES_AI_M_GROUP group = new SES_AI_M_GROUP("test-tenant");
    group.setFromGroup("G1");
    group.setGroupName("Updated Group");

    when(entityManager.merge(any(SES_AI_M_GROUP.class))).thenReturn(group);

    assertTrue(group.updateByPk(null));
    verify(entityManager, times(1)).merge(group);
    verify(entityManager, times(1)).flush();
  }

  @Test
  void testUpdateByPkWithNullId() throws SQLException {
    SES_AI_M_GROUP group = new SES_AI_M_GROUP("test-tenant");
    group.setFromGroup(null);

    assertFalse(group.updateByPk(null));
    verify(entityManager, never()).merge(any());
  }

  @Test
  void testDeleteByPkSuccess() throws SQLException {
    SES_AI_M_GROUP found = new SES_AI_M_GROUP("test-tenant");
    found.setFromGroup("G1");

    when(entityManager.find(SES_AI_M_GROUP.class, "G1")).thenReturn(found);

    SES_AI_M_GROUP group = new SES_AI_M_GROUP("test-tenant");
    group.setFromGroup("G1");

    assertTrue(group.deleteByPk(null));
    verify(entityManager, times(1)).remove(found);
    verify(entityManager, times(1)).flush();
  }

  @Test
  void testDeleteByPkWithNullId() throws SQLException {
    SES_AI_M_GROUP group = new SES_AI_M_GROUP("test-tenant");
    group.setFromGroup(null);

    assertFalse(group.deleteByPk(null));
    verify(entityManager, never()).remove(any());
  }

  @Test
  void testToString() {
    SES_AI_M_GROUP group = new SES_AI_M_GROUP("test-tenant");
    group.setFromGroup("G1");
    group.setGroupName("Test Group");

    assertNotNull(group.toString());
    assertTrue(group.toString().contains("G1"));
  }
}
