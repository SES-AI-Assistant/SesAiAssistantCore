package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EntityLombokTests {

  @Test
  void testSES_AI_T_JOB() {
    SES_AI_T_JOB e1 = new SES_AI_T_JOB();
    SES_AI_T_JOB e2 = new SES_AI_T_JOB();
    assertEquals(e1, e2);
    assertEquals(e1.hashCode(), e2.hashCode());
    assertNotNull(e1.toString());
    assertTrue(e1.canEqual(e2));

    e1.setJobId("J1");
    assertNotEquals(e1, e2);
    e2.setJobId("J1");
    assertEquals(e1, e2);

    e1.setRawContent("C1");
    assertNotEquals(e1, e2);
    e2.setRawContent("C1");
    assertEquals(e1, e2);

    e1.setContentSummary("S1");
    assertNotEquals(e1, e2);
    e2.setContentSummary("S1");
    assertEquals(e1, e2);

    e1.setFromGroup("G1");

    // Test distance and other inherited fields
    e1.setDistance(0.5);
    assertNotEquals(e1.getDistance(), 0.0);
  }

  @Test
  void testSES_AI_T_PERSON() {
    SES_AI_T_PERSON e1 = new SES_AI_T_PERSON();
    SES_AI_T_PERSON e2 = new SES_AI_T_PERSON();
    assertEquals(e1, e2);
    e1.setPersonId("P1");
    assertNotEquals(e1, e2);
    e2.setPersonId("P1");
    assertEquals(e1, e2);

    e1.setRawContent("C1");
    assertNotEquals(e1, e2);
    e2.setRawContent("C1");
    assertEquals(e1, e2);

    assertNotNull(e1.toString());
  }

  @Test
  void testSES_AI_T_SKILLSHEET() {
    SES_AI_T_SKILLSHEET e1 = new SES_AI_T_SKILLSHEET();
    SES_AI_T_SKILLSHEET e2 = new SES_AI_T_SKILLSHEET();
    assertNotNull(e1.toString());
    e1.setFileId("F1");
    assertEquals("F1", e1.getFileId());
    e1.setFileName("N1");
    assertEquals("N1", e1.getFileName());
    e1.setFileContent("C1");
    assertEquals("C1", e1.getFileContent());
    e1.setFileContentSummary("S1");
    assertEquals("S1", e1.getFileContentSummary());
  }

  @Test
  void testSES_AI_WEBAPP_M_USER() {
    SES_AI_WEBAPP_M_USER e1 = new SES_AI_WEBAPP_M_USER();
    SES_AI_WEBAPP_M_USER e2 = new SES_AI_WEBAPP_M_USER();
    assertEquals(e1, e2);
    e1.setUserId("U1");
    assertNotEquals(e1, e2);
    e2.setUserId("U1");
    assertEquals(e1, e2);
    assertNotNull(e1.toString());
  }

  @Test
  void testSES_AI_API_USAGE_HISTORY() {
    SES_AI_API_USAGE_HISTORY e1 = new SES_AI_API_USAGE_HISTORY();
    SES_AI_API_USAGE_HISTORY e2 = new SES_AI_API_USAGE_HISTORY();
    assertEquals(e1, e2);
    e1.setUserId("U1");
    assertNotEquals(e1, e2);
    e2.setUserId("U1");
    assertEquals(e1, e2);
  }

  @Test
  void testSES_AI_T_WATCH() {
    SES_AI_T_WATCH e1 = new SES_AI_T_WATCH();
    SES_AI_T_WATCH e2 = new SES_AI_T_WATCH();
    assertEquals(e1, e2);
    e1.setUserId("U1");
    assertNotEquals(e1, e2);
    e2.setUserId("U1");
    assertEquals(e1, e2);

    e1.setTargetId("T1");
    assertNotEquals(e1, e2);
    e2.setTargetId("T1");
    assertEquals(e1, e2);
  }
}
