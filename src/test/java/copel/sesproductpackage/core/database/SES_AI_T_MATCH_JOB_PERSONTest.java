package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.Gender;
import copel.sesproductpackage.core.unit.MatchingStatus;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SES_AI_T_MATCH_JOB_PERSON のテストクラス.
 *
 * @author Copel Co., Ltd.
 */
class SES_AI_T_MATCH_JOB_PERSONTest {

  private SES_AI_T_MATCH_JOB_PERSON entity;

  @BeforeEach
  void setUp() {
    entity = new SES_AI_T_MATCH_JOB_PERSON("test-tenant");
  }

  @Test
  void testConstructorAndBasicFields() {
    assertEquals("test-tenant", entity.getTenantId());
    assertNotNull(entity);
  }

  @Test
  void testSetAndGetAllMatchFields() {
    entity.setMatchingId("M001");
    entity.setUserId("U001");
    entity.setJobId("J001");
    entity.setPersonId("P001");
    entity.setJobContent("Job Content");
    entity.setPersonContent("Person Content");
    entity.setStatus(MatchingStatus.提案中);
    entity.setEvaluationText("Evaluation");
    entity.setScore(85);
    entity.setMustEvaluationText("Must Eval");
    entity.setWantEvaluationText("Want Eval");
    entity.setPlaceEvaluationText("Place Eval");
    entity.setOfficeEvaluationText("Office Eval");
    entity.setOtherEvaluationText("Other Eval");
    entity.setProfit(new Money(100000L));
    entity.setRegisterDate(new OriginalDateTime("2026-01-01 10:00:00"));
    entity.setRegisterUser("admin");

    assertEquals("M001", entity.getMatchingId());
    assertEquals("U001", entity.getUserId());
    assertEquals("J001", entity.getJobId());
    assertEquals("P001", entity.getPersonId());
    assertEquals("Job Content", entity.getJobContent());
    assertEquals("Person Content", entity.getPersonContent());
    assertEquals(MatchingStatus.提案中, entity.getStatus());
    assertEquals("Evaluation", entity.getEvaluationText());
    assertEquals(85, entity.getScore());
    assertEquals("Must Eval", entity.getMustEvaluationText());
    assertEquals("Want Eval", entity.getWantEvaluationText());
    assertEquals("Place Eval", entity.getPlaceEvaluationText());
    assertEquals("Office Eval", entity.getOfficeEvaluationText());
    assertEquals("Other Eval", entity.getOtherEvaluationText());
    assertEquals(100000L, entity.getProfit().toYenValue());
    assertNotNull(entity.getRegisterDate());
    assertEquals("admin", entity.getRegisterUser());
  }

  @Test
  void testSetAndGetJobFields() {
    entity.setJobTitle("Job Title");
    entity.setJobUnitPrice(new Money(800000L));

    assertEquals("Job Title", entity.getJobTitle());
    assertEquals(800000L, entity.getJobUnitPrice().toYenValue());
  }

  @Test
  void testSetAndGetPersonFields() {
    entity.setPersonName("Person Name");
    entity.setPersonAge(35);
    entity.setPersonGender(Gender.Man);
    entity.setPersonUnitPrice(new Money(600000L));

    assertEquals("Person Name", entity.getPersonName());
    assertEquals(35, entity.getPersonAge());
    assertEquals(Gender.Man, entity.getPersonGender());
    assertEquals(600000L, entity.getPersonUnitPrice().toYenValue());
  }

  @Test
  void testHasJobId() {
    entity.setJobId("J001");
    assertTrue(entity.hasJobId());

    entity.setJobId("");
    assertFalse(entity.hasJobId());

    entity.setJobId(null);
    assertFalse(entity.hasJobId());
  }

  @Test
  void testHasPersonId() {
    entity.setPersonId("P001");
    assertTrue(entity.hasPersonId());

    entity.setPersonId("");
    assertFalse(entity.hasPersonId());

    entity.setPersonId(null);
    assertFalse(entity.hasPersonId());
  }

  @Test
  void testInsertThrowsUnsupportedOperationException() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> entity.insert(mock(Connection.class)),
        "insert操作はサポートしていません");
  }

  @Test
  void testSelectByPkThrowsUnsupportedOperationException() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> entity.selectByPk(mock(Connection.class)),
        "selectByPk操作はサポートしていません");
  }

  @Test
  void testUpdateByPkThrowsUnsupportedOperationException() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> entity.updateByPk(mock(Connection.class)),
        "updateByPk操作はサポートしていません");
  }

  @Test
  void testDeleteByPkThrowsUnsupportedOperationException() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> entity.deleteByPk(mock(Connection.class)),
        "deleteByPk操作はサポートしていません");
  }

  @Test
  void testEqualsAndHashCode() {
    SES_AI_T_MATCH_JOB_PERSON entity1 = new SES_AI_T_MATCH_JOB_PERSON("test-tenant");
    SES_AI_T_MATCH_JOB_PERSON entity2 = new SES_AI_T_MATCH_JOB_PERSON("test-tenant");

    entity1.setMatchingId("M001");
    entity2.setMatchingId("M001");
    assertEquals(entity1, entity2);
    assertEquals(entity1.hashCode(), entity2.hashCode());

    entity1.setJobTitle("Title1");
    assertNotEquals(entity1, entity2);

    entity2.setJobTitle("Title1");
    assertEquals(entity1, entity2);

    entity1.setPersonName("Name1");
    assertNotEquals(entity1, entity2);

    entity2.setPersonName("Name1");
    assertEquals(entity1, entity2);
  }

  @Test
  void testNotEquals() {
    SES_AI_T_MATCH_JOB_PERSON entity1 = new SES_AI_T_MATCH_JOB_PERSON("test-tenant");
    SES_AI_T_MATCH_JOB_PERSON entity2 = new SES_AI_T_MATCH_JOB_PERSON("test-tenant");

    assertNotEquals(entity1, null);
    assertNotEquals(entity1, new Object());
  }

  @Test
  void testToString() {
    entity.setMatchingId("M001");
    String str = entity.toString();
    assertNotNull(str);
    assertTrue(str.length() > 0);
  }

  @Test
  void testMoneyFieldsWithNullAndEmpty() {
    entity.setProfit(null);
    assertNull(entity.getProfit());

    entity.setJobUnitPrice(Money.empty());
    assertTrue(entity.getJobUnitPrice().isEmpty());

    entity.setPersonUnitPrice(Money.empty());
    assertTrue(entity.getPersonUnitPrice().isEmpty());
  }

  @Test
  void testGenderFieldWithNull() {
    entity.setPersonGender(null);
    assertNull(entity.getPersonGender());

    entity.setPersonGender(Gender.Woman);
    assertEquals(Gender.Woman, entity.getPersonGender());
  }

  @Test
  void testPersonAgeWithNull() {
    entity.setPersonAge(null);
    assertNull(entity.getPersonAge());

    entity.setPersonAge(30);
    assertEquals(30, entity.getPersonAge());
  }

  @Test
  void testScoreWithNull() {
    entity.setScore(null);
    assertNull(entity.getScore());

    entity.setScore(95);
    assertEquals(95, entity.getScore());
  }

  @Test
  void testCanEqual() {
    SES_AI_T_MATCH_JOB_PERSON entity1 = new SES_AI_T_MATCH_JOB_PERSON("test-tenant");
    SES_AI_T_MATCH_JOB_PERSON entity2 = new SES_AI_T_MATCH_JOB_PERSON("test-tenant");

    assertTrue(entity1.canEqual(entity2));
  }

  @Test
  void testAllFieldsWithCompleteData() {
    entity.setMatchingId("M001");
    entity.setUserId("U001");
    entity.setJobId("J001");
    entity.setPersonId("P001");
    entity.setJobContent("Job Content");
    entity.setPersonContent("Person Content");
    entity.setStatus(MatchingStatus.提案中);
    entity.setEvaluationText("Evaluation");
    entity.setScore(85);
    entity.setMustEvaluationText("Must");
    entity.setWantEvaluationText("Want");
    entity.setPlaceEvaluationText("Place");
    entity.setOfficeEvaluationText("Office");
    entity.setOtherEvaluationText("Other");
    entity.setProfit(new Money(100000L));
    entity.setJobTitle("Job Title");
    entity.setJobUnitPrice(new Money(800000L));
    entity.setPersonName("Person Name");
    entity.setPersonAge(35);
    entity.setPersonGender(Gender.Man);
    entity.setPersonUnitPrice(new Money(600000L));
    entity.setRegisterDate(new OriginalDateTime("2026-01-01 10:00:00"));
    entity.setRegisterUser("admin");
    entity.setTenantId("test-tenant");

    assertNotNull(entity.toString());
  }
}
