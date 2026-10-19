package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class RoleTest {

  @Test
  void testGetEnum() {
    assertEquals(Role.システム利用不可, Role.getEnum("00"));
    assertEquals(Role.システムユーザー, Role.getEnum("10"));
    assertEquals(Role.開発, Role.getEnum("20"));
    assertEquals(Role.運用, Role.getEnum("30"));
    assertEquals(Role.システム管理者, Role.getEnum("99"));
    assertNull(Role.getEnum("ZZ"));
  }

  @Test
  void testIsSystemUseAuth() {
    assertFalse(Role.システム利用不可.isSystemUseAuth());
    assertTrue(Role.システムユーザー.isSystemUseAuth());
    assertTrue(Role.開発.isSystemUseAuth());
    assertTrue(Role.運用.isSystemUseAuth());
    assertTrue(Role.システム管理者.isSystemUseAuth());
  }

  @Test
  void testGetSetCode() {
    Role role = Role.システム利用不可;
    assertEquals("00", role.getCode());
    role.setCode("XX");
    assertEquals("XX", role.getCode());
    // Reset back
    role.setCode("00");
  }

  /** 運用ロールは送信者マスタ管理権限（MANAGE_SENDER）と従来のウォッチ管理権限を持つことを確認する. */
  @Test
  void testOperationRoleHasManageSenderAndManageWatch() {
    assertTrue(Role.運用.getPermissions().contains(Permission.MANAGE_SENDER));
    assertTrue(Role.運用.getPermissions().contains(Permission.MANAGE_WATCH));
    assertTrue(Role.運用.getPermissions().contains(Permission.MANAGE_FROM_GROUP));
    assertTrue(Role.運用.getPermissions().contains(Permission.MANAGE_USERS));
    assertEquals(
        EnumSet.of(
            Permission.MANAGE_WATCH,
            Permission.MANAGE_SENDER,
            Permission.MANAGE_FROM_GROUP,
            Permission.MANAGE_USERS),
        Role.運用.getPermissions());
  }

  /** 開発ロールは送信者マスタ管理権限を持たないことを確認する（運用ロールのみ付与）. */
  @Test
  void testDevelopmentRoleHasNoManageSender() {
    assertFalse(Role.開発.getPermissions().contains(Permission.MANAGE_SENDER));
  }

  /** システム管理者は全権限（MANAGE_SENDER を含む）を持つことを確認する. */
  @Test
  void testSystemAdminHasAllPermissions() {
    assertEquals(EnumSet.allOf(Permission.class), Role.システム管理者.getPermissions());
  }
}
