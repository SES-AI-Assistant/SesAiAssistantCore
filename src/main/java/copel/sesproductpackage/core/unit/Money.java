package copel.sesproductpackage.core.unit;

import copel.sesproductpackage.core.api.gpt.schema.Schema;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  /** 万円から円への換算倍率（10,000）. */
  private static final BigDecimal MAN_UNIT = new BigDecimal("10000");

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

  /**
   * 万円単位の数値から、SESの業務単位（円単位、スキル見合い、時給・日給換算）に適合した Money インスタンスを生成します.
   * 入力値はSESの相場・仕様に基づいて自動修復され、安全な Money オブジェクトとして返されます.
   *
   * @param manValue 万円単位の値（null可）
   * @return Money インスタンス（nullの場合は empty）
   * @author Copel Co., Ltd.
   */
  public static Money toSesUnitFromMan(BigDecimal manValue) {
    if (manValue == null) {
      return Money.empty();
    }
    if (manValue.compareTo(BigDecimal.ZERO) <= 0) {
      return new Money(manValue.setScale(0, RoundingMode.HALF_UP));
    }
    // 1. スキル見合い仕様値（999 または 9,990,000）→ 定義済みオブジェクトを返却
    if (manValue.compareTo(new BigDecimal("999")) == 0
        || manValue.compareTo(NEGOTIABLE_PRICE.getValue()) == 0) {
      return NEGOTIABLE_PRICE;
    }
    // 2. 正常な万円範囲（10以上 500以下、小数の65.5なども含む）→ 1万倍して円単位
    if (manValue.compareTo(new BigDecimal("10")) >= 0
        && manValue.compareTo(new BigDecimal("500")) <= 0) {
      return new Money(manValue.multiply(MAN_UNIT).setScale(0, RoundingMode.HALF_UP));
    }
    // 3. 【時給の漏れ検知】1,000以上 8,000以下（160h換算）
    if (manValue.compareTo(new BigDecimal("1000")) >= 0
        && manValue.compareTo(new BigDecimal("8000")) <= 0) {
      return new Money(manValue.multiply(new BigDecimal("160")).setScale(0, RoundingMode.HALF_UP));
    }
    // 4. 【日給の漏れ検知】10,000以上 50,000以下（20日換算）
    if (manValue.compareTo(new BigDecimal("10000")) >= 0
        && manValue.compareTo(new BigDecimal("50000")) <= 0) {
      return new Money(manValue.multiply(new BigDecimal("20")).setScale(0, RoundingMode.HALF_UP));
    }
    // 5. 【ゼロ1個過剰の救済】1,000万以上
    // ※SES業界の相場上、月額1,000万円以上の正規案件・要員は実質的に存在しないため、LLMの1桁過剰出力（1850万円など）を安全に救済する仕様
    if (manValue.compareTo(new BigDecimal("10000000")) >= 0) {
      return new Money(manValue.divide(BigDecimal.TEN, 0, RoundingMode.HALF_UP));
    }
    // 6. 通常の円範囲（100,000以上 5,000,000以下）→ そのまま円として採用
    if (manValue.compareTo(new BigDecimal("100000")) >= 0
        && manValue.compareTo(new BigDecimal("5000000")) <= 0) {
      return new Money(manValue.setScale(0, RoundingMode.HALF_UP));
    }
    // 7. その他の範囲（500超〜1000未満など）→ 万円として1万倍
    if (manValue.compareTo(new BigDecimal("1000")) < 0) {
      return new Money(manValue.multiply(MAN_UNIT).setScale(0, RoundingMode.HALF_UP));
    }
    return new Money(manValue.setScale(0, RoundingMode.HALF_UP));
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

  // ================================================
  // プライベートヘルパーメソッド
  // ================================================

  /**
   * content_summary から「■単価：」で始まる行のみを抽出. 改行まで（次の■が出るまでではなく）を範囲とする。
   *
   * @param contentSummary AI生成の要約文
   * @return 「■単価：」以降の行の内容、見つからない場合は null
   */
  private static String extractUnitPriceSection(String contentSummary) {
    Pattern pattern = Pattern.compile("■単価[：:]([^\n]*)", Pattern.MULTILINE);
    Matcher matcher = pattern.matcher(contentSummary);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    return null;
  }

  /**
   * 単価セクションから最大値を抽出（案件用）. 括弧内を削除した後、数字をすべて抽出。数字がなければnullを返す。
   *
   * @param unitPriceSection 「■単価：」以降の行のテキスト
   * @return 円単位の最大値、数字が抽出できない場合は null
   */
  private static BigDecimal extractMaxPrice(String unitPriceSection) {
    // 括弧内のコメント（スキル見合い、精算確認中など）を削除
    String cleaned = unitPriceSection.replaceAll("（[^）]*）", "").replaceAll("\\([^)]*\\)", "");

    // 数字を抽出（「万」「円」「?」などの区切り文字で複数の値がある場合）
    // パターン: 123, 123.45, 123万, 123円, 1,234,567
    Pattern numberPattern = Pattern.compile("([0-9,]+(?:\\.[0-9]+)?)");
    Matcher matcher = numberPattern.matcher(cleaned);

    BigDecimal maxPrice = null;
    while (matcher.find()) {
      String numStr = matcher.group(1).replace(",", "");
      BigDecimal num = parsePrice(numStr, cleaned);
      if (num != null) {
        if (maxPrice == null || num.compareTo(maxPrice) > 0) {
          maxPrice = num;
        }
      }
    }

    // 数字が抽出できなければnullを返す
    return maxPrice;
  }

  /**
   * 単価セクションから最小値を抽出（要員用）. 括弧内を削除した後、数字をすべて抽出。 最初の範囲がある場合はそのMIN、そうでない場合は最初の数字を採用。
   *
   * @param unitPriceSection 「■単価：」以降の行のテキスト
   * @return 円単位の最小値、数字が抽出できない場合は null
   */
  private static BigDecimal extractMinPrice(String unitPriceSection) {
    // 括弧内のコメント（スキル見合い、精算確認中など）を削除
    String cleaned = unitPriceSection.replaceAll("（[^）]*）", "").replaceAll("\\([^)]*\\)", "");

    // 最初の範囲（複数条件がある場合は最初だけ）を抽出
    Pattern rangePattern = Pattern.compile("([0-9,]+(?:\\.[0-9]+)?)[~～?-]([0-9,]+(?:\\.[0-9]+)?)");
    Matcher matcher = rangePattern.matcher(cleaned);

    if (matcher.find()) {
      // 範囲がある場合は最小値を採用
      String minStr = matcher.group(1).replace(",", "");
      BigDecimal minPrice = parsePrice(minStr, cleaned);
      if (minPrice != null) {
        return minPrice;
      }
    }

    // 単一値の場合（最初の数字を採用）
    Pattern singlePattern = Pattern.compile("([0-9,]+(?:\\.[0-9]+)?)");
    matcher = singlePattern.matcher(cleaned);
    if (matcher.find()) {
      String numStr = matcher.group(1).replace(",", "");
      return parsePrice(numStr, cleaned);
    }

    // 数字が抽出できなければnullを返す
    return null;
  }

  /**
   * 数値文字列を元のテキストから単位を判定して円単位に変換.
   *
   * @param numStr 数値文字列（カンマなし）
   * @param originalText 元のテキスト（単位判定用）
   * @return 円単位の値、判定不可の場合は null
   */
  private static BigDecimal parsePrice(String numStr, String originalText) {
    try {
      BigDecimal num = new BigDecimal(numStr);

      // 小数点は切り捨て
      if (num.scale() > 0) {
        num = num.setScale(0, RoundingMode.FLOOR);
      }

      // 単位を判定
      // 「万円」「万」の場合は 10000 倍
      if (originalText.contains("万円") || originalText.contains("万")) {
        return num.multiply(new BigDecimal("10000"));
      }

      // 「円」の場合はそのまま
      if (originalText.contains("円")) {
        return num;
      }

      // 単位が明記されていない場合は「万」と仮定（より安全）
      // ただし 1000000 以上の値は「円」と判定
      if (num.compareTo(new BigDecimal("100000")) >= 0) {
        return num; // そのまま円
      }

      return num.multiply(new BigDecimal("10000")); // 万円と仮定
    } catch (NumberFormatException e) {
      return null;
    }
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
}
