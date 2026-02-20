package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CurrencyTests {

    @Test
    void testGetFullName() {
        assertEquals("Japanese Yen", Currency.JPY.getFullName());
        assertEquals("US Dollar", Currency.USD.getFullName());
        assertEquals("Euro", Currency.EUR.getFullName());
        assertEquals("British Pound", Currency.GBP.getFullName());
        assertEquals("Australian Dollar", Currency.AUD.getFullName());
    }

    @Test
    void testGetExchangeRateToJPY() {
        assertEquals(1.0, Currency.JPY.getExchangeRateToJPY());
        assertEquals(150.0, Currency.USD.getExchangeRateToJPY());
    }

    @Test
    void testUpdateExchangeRate() {
        Currency.USD.updateExchangeRate(155.0);
        assertEquals(155.0, Currency.USD.getExchangeRateToJPY());
        
        // 他のテストに影響を与えないよう戻しておく（実際にはstaticなので影響する可能性があるが、JUnit内では許容）
        Currency.USD.updateExchangeRate(150.0);
    }
    
    @Test
    void testEnumMethods() {
        assertEquals(Currency.JPY, Currency.valueOf("JPY"));
        assertTrue(Currency.values().length > 0);
    }
}
