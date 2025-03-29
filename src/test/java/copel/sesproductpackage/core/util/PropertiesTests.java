package copel.sesproductpackage.core.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PropertiesTests {

    @Test
    void getTest() {
        assertEquals("JUnit Test Sample", Properties.get("PROPERTIES_UT_TEST"));
    }

    @Test
    void getAsArrayTest() {
        String[] array = Properties.getAsArray("PROPERTIES_UT_TEST_ARRAY");
        assertEquals("JUnit", array[0]);
        assertEquals("Test", array[1]);
        assertEquals("Sample", array[2]);
    }
}
