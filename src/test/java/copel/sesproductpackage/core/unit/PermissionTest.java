package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class PermissionTest {

  @Test
  void testAllPermissionsHaveName() {
    for (Permission p : Permission.values()) {
      assertNotNull(p.getName());
      assertNotNull(p.name());
    }
  }
}
