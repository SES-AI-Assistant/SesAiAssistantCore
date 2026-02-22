package copel.sesproductpackage.core.unit;

/**
 * お金を扱うクラス.
 *
 * @author 鈴木一矢
 */
public class Money {
  /** 金額. */
  private final double amount;

  /** 通貨. */
  private final Currency currency;

  /**
   * コンストラクタ.
   *
   * @param amount 金額
   * @param currency 通貨
   */
  public Money(final double amount, final Currency currency) {
    this.amount = amount;
    this.currency = currency;
  }

  /**
   * 金額を円で取得する.
   *
   * @return 円
   */
  public double getYen() {
    if (this.currency == null) {
      return 0.0;
    }
    return this.amount * this.currency.getExchangeRateToJPY();
  }

  /**
   * 金額をUSドルで取得する.
   *
   * @return USドル
   */
  public double getUsDollar() {
    return this.getYen() / Currency.USD.getExchangeRateToJPY();
  }

  @Override
  public String toString() {
    if (this.currency == null) {
      return Double.toString(this.amount);
    } else {
      return Double.toString(this.amount) + this.currency.name();
    }
  }
}
