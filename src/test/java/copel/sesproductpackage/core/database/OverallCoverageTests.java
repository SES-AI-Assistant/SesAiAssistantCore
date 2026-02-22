package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;

import copel.sesproductpackage.core.util.EntityCoverageHelper;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class OverallCoverageTests {

  @Test
  void testEntityCoverage() {
    List<Class<?>> entities =
        Arrays.asList(
            SES_AI_API_USAGE_HISTORY.class,
            SES_AI_M_COMPANY.class,
            SES_AI_M_GROUP.class,
            SES_AI_M_SENDER.class,
            SES_AI_T_JOB.class,
            SES_AI_T_MATCH.class,
            SES_AI_T_PERSON.class,
            SES_AI_T_SKILLSHEET.class,
            SES_AI_T_WATCH.class,
            SES_AI_WEBAPP_M_USER.class);

    boolean anyFailed = false;
    for (Class<?> clazz : entities) {
      try {
        System.out.println("Verifying coverage for: " + clazz.getName());
        Object entity = clazz.getDeclaredConstructor().newInstance();
        EntityCoverageHelper.verifyEntityCoverage(entity);
      } catch (Throwable e) {
        anyFailed = true;
        System.err.println(
            "Failed to verify coverage for " + clazz.getName() + ": " + e.getMessage());
        e.printStackTrace();
      }
    }
    if (anyFailed) {
      fail("Some entity coverage verifications failed. Check console output.");
    }
  }

  @Test
  void testWatchTargetType() {
    for (SES_AI_T_WATCH.TargetType type : SES_AI_T_WATCH.TargetType.values()) {
      assertNotNull(type.toString());
      assertEquals(type, SES_AI_T_WATCH.TargetType.valueOf(type.name()));
    }
  }

  @Test
  void testApiTypeAndProvider() {
    for (SES_AI_API_USAGE_HISTORY.ApiType type : SES_AI_API_USAGE_HISTORY.ApiType.values()) {
      assertNotNull(type.toString());
    }
    for (SES_AI_API_USAGE_HISTORY.Provider provider : SES_AI_API_USAGE_HISTORY.Provider.values()) {
      assertNotNull(provider.toString());
    }
  }
}
