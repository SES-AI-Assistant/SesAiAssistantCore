package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class PlanTest {

  @Test
  void testGetEnum() {
    assertEquals(Plan.FREE, Plan.getEnum("00"));
    assertEquals(Plan.PREMIUM, Plan.getEnum("10"));
    assertEquals(Plan.FREE, Plan.getEnum("unknown"));
  }

  @Test
  void testFreePlan() {
    assertEquals("00", Plan.FREE.getCode());
    assertEquals("フリープラン", Plan.FREE.getName());
    Set<Permission> perms = Plan.FREE.getPermissions();
    assertNotNull(perms);
    assertTrue(perms.contains(Permission.VIEW_MATCHING_LIST));
    assertFalse(perms.contains(Permission.MANAGE_USERS));
  }

  @Test
  void testPremiumPlan() {
    assertEquals("10", Plan.PREMIUM.getCode());
    assertEquals("プレミアムプラン", Plan.PREMIUM.getName());
    Set<Permission> perms = Plan.PREMIUM.getPermissions();
    assertNotNull(perms);
    assertEquals(
        Set.of(
            Permission.VIEW_MATCHING_LIST,
            Permission.VIEW_JOB_LIST,
            Permission.DOWNLOAD_SKILLSHEET,
            Permission.EXEC_AI_MATCHING,
            Permission.REGISTER_INFO_LIST_IMPORT,
            Permission.MANAGE_WATCH),
        perms);
  }

  /** プレミアムプランは管理系権限（ユーザー・マスタ・送信者・送信元グループ・利用履歴）を持たないことを確認する. */
  @Test
  void testPremiumPlanHasNoAdminPermissions() {
    Set<Permission> perms = Plan.PREMIUM.getPermissions();
    assertFalse(perms.contains(Permission.MANAGE_USERS));
    assertFalse(perms.contains(Permission.MANAGE_MASTER_DATA));
    assertFalse(perms.contains(Permission.MANAGE_SENDER));
    assertFalse(perms.contains(Permission.MANAGE_FROM_GROUP));
    assertFalse(perms.contains(Permission.VIEW_USAGE_HISTORY));
  }
}
