package copel.sesproductpackage.core.unit;

/**
 * 通貨を扱う列挙型. 為替レートは2025/4/1現在のため、更新が必要.
 *
 * @author Copel Co., Ltd.
 */
public enum Currency {
  JPY("Japanese Yen", 1.0),
  USD("US Dollar", 150.0),
  EUR("Euro", 160.0),
  GBP("British Pound", 180.0),
  AUD("Australian Dollar", 100.0);

  /** 名前. */
  private final String fullName;

  /** 円との為替レート. */
  private double exchangeRateToJpy;

  Currency(String fullName, double exchangeRateToJpy) {
    this.fullName = fullName;
    this.exchangeRateToJpy = exchangeRateToJpy;
  }

  public String getFullName() {
    return fullName;
  }

  public double getExchangeRateToJpy() {
    return exchangeRateToJpy;
  }

  public void updateExchangeRate(double newRate) {
    this.exchangeRateToJpy = newRate;
  }
}
