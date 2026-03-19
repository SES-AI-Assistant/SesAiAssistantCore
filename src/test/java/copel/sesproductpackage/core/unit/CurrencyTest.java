package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CurrencyTest {

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

    // Reset back to original state
    Currency.USD.updateExchangeRate(150.0);
  }

  @Test
  void testEnumMethods() {
    assertEquals(Currency.JPY, Currency.valueOf("JPY"));
    assertTrue(Currency.values().length > 0);
  }
}
