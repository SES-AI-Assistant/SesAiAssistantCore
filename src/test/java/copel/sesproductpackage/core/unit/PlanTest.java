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
    assertEquals(Set.of(Permission.values()).size(), perms.size());
    assertTrue(perms.contains(Permission.MANAGE_USERS));
  }
}
