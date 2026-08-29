package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import jakarta.persistence.EntityManager;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SES_AI_M_SENDER のテストクラス.
 *
 * <p>JPA ベースの CRUD 操作をモックして検証します。
 *
 * @author Copel Co., Ltd.
 */
class SES_AI_M_SENDERTest {

  private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    entityManager = mock(EntityManager.class);
    SES_AI_M_SENDER.setEntityManager(entityManager);
  }

  @Test
  void testInsertSuccess() throws SQLException {
    SES_AI_M_SENDER sender = new SES_AI_M_SENDER("test-tenant");
    sender.setFromId("S1");
    sender.setFromName("Test Sender");
    sender.setRegisterDate(new OriginalDateTime());
    sender.setRegisterUser("admin");

    int result = sender.insert(null);
    assertEquals(1, result);
    verify(entityManager, times(1)).persist(sender);
    verify(entityManager, times(1)).flush();
  }

  @Test
  void testInsertWithoutEntityManager() throws SQLException {
    SES_AI_M_SENDER.setEntityManager(null);
    SES_AI_M_SENDER sender = new SES_AI_M_SENDER("test-tenant");
    sender.setFromId("S1");

    assertThrows(SQLException.class, () -> sender.insert(null));
  }

  @Test
  void testSelectByPkSuccess() throws SQLException {
    SES_AI_M_SENDER found = new SES_AI_M_SENDER("test-tenant");
    found.setFromId("S1");
    found.setFromName("Test Sender");
    found.setRegisterDate(new OriginalDateTime("2026-06-04 10:00:00"));
    found.setRegisterUser("admin");

    when(entityManager.find(SES_AI_M_SENDER.class, "S1")).thenReturn(found);

    SES_AI_M_SENDER sender = new SES_AI_M_SENDER("test-tenant");
    sender.setFromId("S1");
    sender.selectByPk(null);

    assertEquals("S1", sender.getFromId());
    assertEquals("Test Sender", sender.getFromName());
    assertEquals("admin", sender.getRegisterUser());
  }

  @Test
  void testSelectByPkWithNullId() throws SQLException {
    SES_AI_M_SENDER sender = new SES_AI_M_SENDER("test-tenant");
    sender.setFromId(null);

    sender.selectByPk(null);

    verify(entityManager, never()).find(any(), any());
  }

  @Test
  void testUpdateByPkSuccess() throws SQLException {
    SES_AI_M_SENDER sender = new SES_AI_M_SENDER("test-tenant");
    sender.setFromId("S1");
    sender.setFromName("Updated Sender");

    when(entityManager.merge(any(SES_AI_M_SENDER.class))).thenReturn(sender);

    assertTrue(sender.updateByPk(null));
    verify(entityManager, times(1)).merge(sender);
    verify(entityManager, times(1)).flush();
  }

  @Test
  void testUpdateByPkWithNullId() throws SQLException {
    SES_AI_M_SENDER sender = new SES_AI_M_SENDER("test-tenant");
    sender.setFromId(null);

    assertFalse(sender.updateByPk(null));
    verify(entityManager, never()).merge(any());
  }

  @Test
  void testDeleteByPkSuccess() throws SQLException {
    SES_AI_M_SENDER found = new SES_AI_M_SENDER("test-tenant");
    found.setFromId("S1");

    when(entityManager.find(SES_AI_M_SENDER.class, "S1")).thenReturn(found);

    SES_AI_M_SENDER sender = new SES_AI_M_SENDER("test-tenant");
    sender.setFromId("S1");

    assertTrue(sender.deleteByPk(null));
    verify(entityManager, times(1)).remove(found);
    verify(entityManager, times(1)).flush();
  }

  @Test
  void testDeleteByPkWithNullId() throws SQLException {
    SES_AI_M_SENDER sender = new SES_AI_M_SENDER("test-tenant");
    sender.setFromId(null);

    assertFalse(sender.deleteByPk(null));
    verify(entityManager, never()).remove(any());
  }

  @Test
  void testIsExistTrue() throws SQLException {
    SES_AI_M_SENDER found = new SES_AI_M_SENDER("test-tenant");
    found.setFromId("S1");

    when(entityManager.find(SES_AI_M_SENDER.class, "S1")).thenReturn(found);

    SES_AI_M_SENDER sender = new SES_AI_M_SENDER("test-tenant");
    sender.setFromId("S1");

    assertTrue(sender.isExist(null));
  }

  @Test
  void testIsExistFalse() throws SQLException {
    when(entityManager.find(SES_AI_M_SENDER.class, "S1")).thenReturn(null);

    SES_AI_M_SENDER sender = new SES_AI_M_SENDER("test-tenant");
    sender.setFromId("S1");

    assertFalse(sender.isExist(null));
  }

  @Test
  void testIsExistWithNullId() throws SQLException {
    SES_AI_M_SENDER sender = new SES_AI_M_SENDER("test-tenant");
    sender.setFromId(null);

    assertFalse(sender.isExist(null));
    verify(entityManager, never()).find(any(), any());
  }

  @Test
  void testToString() {
    SES_AI_M_SENDER sender = new SES_AI_M_SENDER("test-tenant");
    sender.setFromId("S1");
    sender.setFromName("Test Sender");

    assertNotNull(sender.toString());
    assertTrue(sender.toString().contains("S1"));
  }
}
