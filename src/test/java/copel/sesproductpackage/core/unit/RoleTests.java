package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RoleTests {

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
        // 戻しておく
        role.setCode("00");
    }
}
