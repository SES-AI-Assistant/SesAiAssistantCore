package copel.sesproductpackage.core.unit;

import java.math.BigDecimal;
import java.math.RoundingMode;

import copel.sesproductpackage.core.api.gpt.schema.Schema;

/**
 * 金額を表す値オブジェクト. 内部は「円」単位で保持し、画面出力時や比較処理に対応する。
 *
 * @author Copel Co., Ltd.
 */
public class Money implements Comparable<Money> {
  // ================================================
  // 定数
  // ================================================
  /**
   * 単価不問・単価提示依頼（SESにおける「スキル見合い」）を表す仕様値（999万円）.
   *
   * <p>「negotiable」は英語で「交渉可能・応相談・協議可能」を意味する。
   * SES業界において、固定の単価を設けず要員のスキルや経験に応じて単価を相談・交渉して決定する
   * （または案件提案時に単価を提示してもらう）「スキル見合い」案件を表す仕様値である。
   */
  public static final Money NEGOTIABLE_PRICE = new Money(9_990_000L);

  @Schema(
      description = "金額",
      required = true,
      type = "integer",
      itemType = BigDecimal.class,
      example = "800000")
  private BigDecimal value;

  // ================================================
  // コンストラクタ
  // ================================================
  /** 空の Money インスタンス（値が未設定）. */
  private Money() {
    this.value = null;
  }

  /**
   * 円単位で初期化.
   *
   * @param valueInYen 円単位の金額
   */
  public Money(BigDecimal valueInYen) {
    this.value = valueInYen;
  }

  /**
   * 円単位で初期化（long）.
   *
   * @param valueInYen 円単位の金額
   */
  public Money(long valueInYen) {
    this.value = new BigDecimal(valueInYen);
  }

  // ================================================
  // 状態判定
  // ================================================

  /** 値が設定されていないか（抽出失敗時）. */
  public boolean isEmpty() {
    return value == null;
  }

  /** 値が設定されているか. */
  public boolean hasValue() {
    return value != null;
  }

  /**
   * 金額が単価不問・単価提示依頼（SESにおける「スキル見合い」: 999万円）であるかを判定する.
   *
   * <p>「negotiable（交渉可能・応相談）」の名の通り、固定単価ではなく、
   * 提案時に単価の提示や交渉が必要（スキル見合い・応相談・単価不問）な案件であるかを判定する。
   *
   * @return 単価不問（スキル見合い）の場合は true、それ以外は false
   * @author Copel Co., Ltd.
   */
  public boolean isNegotiable() {
    return NEGOTIABLE_PRICE.equals(this);
  }

  // ================================================
  // 出力メソッド
  // ================================================
  public static Money empty() {
    return new Money();
  }

  /** DB保存用：円単位の BigDecimal（NULL可能）. */
  public BigDecimal getValue() {
    return value;
  }

  /**
   * 画面表示用：「100万円」形式.
   *
   * @return 「100万円」形式の文字列、またはnull
   */
  public String toJapaneseFormat() {
    if (isEmpty()) {
      return null;
    }
    BigDecimal manValue = value.divide(new BigDecimal("10000"), 2, RoundingMode.FLOOR);
    return manValue.stripTrailingZeros().toPlainString() + "万円";
  }

  /**
   * シンプル数値形式：「100」（万円単位）.
   *
   * @return 万円単位の数値文字列、またはnull
   */
  public String toManFormat() {
    if (isEmpty()) {
      return null;
    }
    BigDecimal manValue = value.divide(new BigDecimal("10000"), 2, RoundingMode.FLOOR);
    return manValue.stripTrailingZeros().toPlainString();
  }

  /**
   * 円単位の数値：「1000000」.
   *
   * @return 円単位の long 値、empty の場合は 0L
   */
  public long toYenValue() {
    return isEmpty() ? 0L : value.longValue();
  }

  // ================================================
  // 比較処理
  // ================================================

  /**
   * 自身が指定された金額より大きいかを判定する.
   * 自身または対象の金額が未設定（empty）または null の場合は安全に false を返す.
   *
   * @param other 比較対象の金額
   * @return 自身が other より大きい場合は true、それ以外は false
   * @author Copel Co., Ltd.
   */
  public boolean isGreaterThan(Money other) {
    if (this.isEmpty() || other == null || other.isEmpty()) {
      return false;
    }
    return this.value.compareTo(other.value) > 0;
  }

  /**
   * 自身が指定された金額以上であるかを判定する.
   * 自身または対象の金額が未設定（empty）または null の場合は安全に false を返す.
   *
   * @param other 比較対象の金額
   * @return 自身が other 以上の場合は true、それ以外は false
   * @author Copel Co., Ltd.
   */
  public boolean isGreaterThanOrEqualTo(Money other) {
    if (this.isEmpty() || other == null || other.isEmpty()) {
      return false;
    }
    return this.value.compareTo(other.value) >= 0;
  }

  /**
   * 自身が指定された金額より小さいかを判定する.
   * 自身または対象の金額が未設定（empty）または null の場合は安全に false を返す.
   *
   * @param other 比較対象の金額
   * @return 自身が other より小さい場合は true、それ以外は false
   * @author Copel Co., Ltd.
   */
  public boolean isLessThan(Money other) {
    if (this.isEmpty() || other == null || other.isEmpty()) {
      return false;
    }
    return this.value.compareTo(other.value) < 0;
  }

  /**
   * 自身が指定された金額以下であるかを判定する.
   * 自身または対象の金額が未設定（empty）または null の場合は安全に false を返す.
   *
   * @param other 比較対象の金額
   * @return 自身が other 以下の場合は true、それ以外は false
   * @author Copel Co., Ltd.
   */
  public boolean isLessThanOrEqualTo(Money other) {
    if (this.isEmpty() || other == null || other.isEmpty()) {
      return false;
    }
    return this.value.compareTo(other.value) <= 0;
  }

  @Override
  public int compareTo(Money other) {
    if (this.isEmpty() && other.isEmpty()) {
      return 0;
    }
    if (this.isEmpty()) {
      return -1;
    }
    if (other.isEmpty()) {
      return 1;
    }
    return this.value.compareTo(other.value);
  }

  /** 金額が等しいか. */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Money)) {
      return false;
    }
    Money other = (Money) obj;
    if (this.isEmpty() && other.isEmpty()) {
      return true;
    }
    if (this.isEmpty() || other.isEmpty()) {
      return false;
    }
    return this.value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return isEmpty() ? 0 : value.hashCode();
  }

  @Override
  public String toString() {
    return isEmpty() ? "empty" : toJapaneseFormat();
  }

  /**
   * この金額を指定の数値で割った結果を返す.
   *
   * 金額を n 分の1 にする際に使用。例えば、万円単位への換算は divide(10000.0) で実現。
   * 0で割った場合は ArithmeticException をスロー。
   *
   * @param divisor 除数
   * @return 割った結果の金額
   * @throws ArithmeticException divisor が 0 の場合
   */
  public Money divide(double divisor) {
    if (divisor == 0) {
      throw new ArithmeticException("0で割ることはできません");
    }
    if (this.isEmpty()) {
      return Money.empty();
    }
    BigDecimal result = this.value.divide(
        new BigDecimal(divisor),
        this.value.scale(),
        RoundingMode.HALF_UP
    );
    return new Money(result);
  }

  /**
   * この金額に指定の数値を掛けた結果を返す.
   *
   * 金額を n 倍する際に使用。
   * 例えば、金額を 1.5 倍する場合は multiply(1.5) で実現。
   *
   * <p>値が未設定（empty）の場合は empty を返す。
   *
   * @param multiplier 乗数
   * @return 掛けた結果の金額
   */
  public Money multiply(double multiplier) {
    if (this.isEmpty()) {
      return Money.empty();
    }

    BigDecimal result = this.value.multiply(
        BigDecimal.valueOf(multiplier)
    );

    return new Money(result);
  }
}
