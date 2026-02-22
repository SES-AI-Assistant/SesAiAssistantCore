package copel.sesproductpackage.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MoneyTests {

  private Money yenMoney;
  private Money usdMoney;
  private Money nullCurrencyMoney;

  @BeforeEach
  void setUp() {
    yenMoney = new Money(1000, Currency.JPY);
    usdMoney = new Money(10, Currency.USD);
    nullCurrencyMoney = new Money(500, null);
  }

  @Test
  void testGetYen() {
    assertEquals(1000, yenMoney.getYen(), 0.01);
    assertEquals(1500, usdMoney.getYen(), 0.01); // USD -> JPY の変換を仮定
    assertEquals(0, nullCurrencyMoney.getYen(), 0.01);
  }

  @Test
  void testGetUsDollar() {
    assertEquals(1000 / 150, yenMoney.getUsDollar(), 6.666666666666667); // 仮定: 1 USD = 150 JPY
    assertEquals(10, usdMoney.getUsDollar(), 0.01);
  }

  @Test
  void testToString() {
    assertEquals("1000.0JPY", yenMoney.toString());
    assertEquals("10.0USD", usdMoney.toString());
    assertEquals("500.0", nullCurrencyMoney.toString());
  }
}
