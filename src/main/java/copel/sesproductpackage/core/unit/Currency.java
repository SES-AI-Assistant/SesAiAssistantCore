package copel.sesproductpackage.core.unit;

/**
 * 通貨を扱う列挙型. 為替レートは2025/4/1現在のため、更新が必要.
 *
 * @author 鈴木一矢
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
  private double exchangeRateToJPY;

  Currency(String fullName, double exchangeRateToJPY) {
    this.fullName = fullName;
    this.exchangeRateToJPY = exchangeRateToJPY;
  }

  public String getFullName() {
    return fullName;
  }

  public double getExchangeRateToJPY() {
    return exchangeRateToJPY;
  }

  public void updateExchangeRate(double newRate) {
    this.exchangeRateToJPY = newRate;
  }
}
