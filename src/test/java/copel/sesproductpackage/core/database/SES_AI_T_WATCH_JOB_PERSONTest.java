package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;

import copel.sesproductpackage.core.unit.Gender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SES_AI_T_WATCH_JOB_PERSON のテストクラス.
 *
 * @author Copel Co., Ltd.
 */
class SES_AI_T_WATCH_JOB_PERSONTest {

  private SES_AI_T_WATCH_JOB_PERSON entity;
  private static final String TEST_TENANT_ID = "test-tenant-id";

  @BeforeEach
  void setUp() {
    entity = new SES_AI_T_WATCH_JOB_PERSON(TEST_TENANT_ID);
  }

  @Test
  void testConstructor() {
    assertEquals(TEST_TENANT_ID, entity.getTenantId());
  }

  @Test
  void testHasJobInfo_WithJobType() {
    entity.setTargetType(SES_AI_T_WATCH.TargetType.JOB);
    entity.setJobTitle("Software Engineer");

    assertTrue(entity.hasJobInfo());
  }

  @Test
  void testHasJobInfo_WithoutJobTitle() {
    entity.setTargetType(SES_AI_T_WATCH.TargetType.JOB);
    entity.setJobTitle(null);

    assertFalse(entity.hasJobInfo());
  }

  @Test
  void testHasJobInfo_WithPersonType() {
    entity.setTargetType(SES_AI_T_WATCH.TargetType.PERSON);
    entity.setJobTitle("Software Engineer");

    assertFalse(entity.hasJobInfo());
  }

  @Test
  void testHasPersonInfo_WithPersonType() {
    entity.setTargetType(SES_AI_T_WATCH.TargetType.PERSON);
    entity.setPersonName("John Doe");

    assertTrue(entity.hasPersonInfo());
  }

  @Test
  void testHasPersonInfo_WithoutPersonName() {
    entity.setTargetType(SES_AI_T_WATCH.TargetType.PERSON);
    entity.setPersonName(null);

    assertFalse(entity.hasPersonInfo());
  }

  @Test
  void testHasPersonInfo_WithJobType() {
    entity.setTargetType(SES_AI_T_WATCH.TargetType.JOB);
    entity.setPersonName("John Doe");

    assertFalse(entity.hasPersonInfo());
  }

  @Test
  void testSetAndGetAllFields() {
    entity.setUserId("user-123");
    entity.setTargetId("target-456");
    entity.setTargetType(SES_AI_T_WATCH.TargetType.JOB);
    entity.setMemo("Test memo");

    entity.setJobTitle("Project Manager");
    entity.setJobOfficeRequirements(3);
    entity.setJobPlace("Tokyo");
    entity.setJobArea("Kanto");

    entity.setPersonName("Jane Doe");
    entity.setPersonAge(30);
    entity.setPersonGender(Gender.Woman);
    entity.setPersonOfficeAvailability(2);
    entity.setPersonPlace("Osaka");
    entity.setPersonArea("Kansai");

    assertEquals("user-123", entity.getUserId());
    assertEquals("target-456", entity.getTargetId());
    assertEquals(SES_AI_T_WATCH.TargetType.JOB, entity.getTargetType());
    assertEquals("Test memo", entity.getMemo());
    assertEquals("Project Manager", entity.getJobTitle());
    assertEquals(3, entity.getJobOfficeRequirements());
    assertEquals("Tokyo", entity.getJobPlace());
    assertEquals("Kanto", entity.getJobArea());
    assertEquals("Jane Doe", entity.getPersonName());
    assertEquals(30, entity.getPersonAge());
    assertEquals(Gender.Woman, entity.getPersonGender());
    assertEquals(2, entity.getPersonOfficeAvailability());
    assertEquals("Osaka", entity.getPersonPlace());
    assertEquals("Kansai", entity.getPersonArea());
  }

  @Test
  void testInsertThrowsUnsupportedException() {
    assertThrows(
        UnsupportedOperationException.class, () -> entity.insert(null), "insert操作はサポートしていません");
  }

  @Test
  void testSelectByPkThrowsUnsupportedException() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> entity.selectByPk(null),
        "selectByPk操作はサポートしていません");
  }

  @Test
  void testUpdateByPkThrowsUnsupportedException() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> entity.updateByPk(null),
        "updateByPk操作はサポートしていません");
  }

  @Test
  void testDeleteByPkThrowsUnsupportedException() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> entity.deleteByPk(null),
        "deleteByPk操作はサポートしていません");
  }
}
