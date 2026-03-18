package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class RoleTests {
    @Test
    void testGetEnum() {
        assertEquals(Role.システム管理者, Role.getEnum("99"));
        assertNull(Role.getEnum("unknown"));
        assertNull(Role.getEnum(null));
    }

    @Test
    void testIsSystemUseAuth() {
        assertFalse(Role.システム利用不可.isSystemUseAuth());
        assertTrue(Role.システムユーザー.isSystemUseAuth());
        assertTrue(Role.システム管理者.isSystemUseAuth());
    }

    @Test
    void testGetCode() {
        assertEquals("99", Role.システム管理者.getCode());
    }

    @Test
    void testSetCode() {
        Role role = Role.開発;
        role.setCode("XX");
        assertEquals("XX", role.getCode());
        role.setCode("20"); // restore
    }

    @Test
    void testGetPermissions() {
        assertNotNull(Role.システム管理者.getPermissions());
    }
}
