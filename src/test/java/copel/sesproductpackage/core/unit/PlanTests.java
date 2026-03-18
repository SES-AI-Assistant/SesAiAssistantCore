package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PlanTests {
    @Test
    void testGetEnum() {
        assertEquals(Plan.FREE, Plan.getEnum("00"));
        assertEquals(Plan.PREMIUM, Plan.getEnum("10"));
        assertEquals(Plan.FREE, Plan.getEnum("unknown"));
        assertEquals(Plan.FREE, Plan.getEnum(null));
    }

    @Test
    void testGetCode() {
        assertEquals("00", Plan.FREE.getCode());
    }

    @Test
    void testGetPermissions() {
        assertNotNull(Plan.FREE.getPermissions());
    }
}
