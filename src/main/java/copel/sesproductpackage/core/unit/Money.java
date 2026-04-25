package copel.sesproductpackage.core.unit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 金額を表す値オブジェクト.
 * 内部は「円」単位で保持し、画面出力時や比較処理に対応する。
 *
 * @author Copel Co., Ltd.
 */
public class Money implements Comparable<Money> {

  private BigDecimal valueInYen;

  // ================================================
  // コンストラクタ
  // ================================================

  /** 空の Money インスタンス（値が未設定）. */
  private Money() {
    this.valueInYen = null;
  }

  /**
   * 円単位で初期化.
   *
   * @param valueInYen 円単位の金額
   */
  public Money(BigDecimal valueInYen) {
    this.valueInYen = valueInYen;
  }

  /**
   * 円単位で初期化（long）.
   *
   * @param valueInYen 円単位の金額
   */
  public Money(long valueInYen) {
    this.valueInYen = new BigDecimal(valueInYen);
  }

  // ================================================
  // ファクトリメソッド（抽出処理）
  // ================================================

  /**
   * content_summary から案件の単価を抽出（MAX値を取得）.
   *
   * <p>抽出ルール：
   * <ul>
   *   <li>「■単価：」以降の数字を抽出</li>
   *   <li>範囲がある場合は MAX 値を採用（100-130万 → 130万）</li>
   *   <li>「スキル見合い」「精算確認中」など定量値なしは empty を返す</li>
   *   <li>括弧内のコメントは無視</li>
   *   <li>小数点は切り捨て</li>
   * </ul>
   *
   * @param contentSummary AI生成の要約文
   * @return 抽出成功時は Money、失敗時は empty Money
   */
  public static Money extractJobUnitPrice(String contentSummary) {
    if (contentSummary == null || contentSummary.isEmpty()) {
      return empty();
    }

    String unitPriceSection = extractUnitPriceSection(contentSummary);
    if (unitPriceSection == null) {
      return empty();
    }

    BigDecimal maxPrice = extractMaxPrice(unitPriceSection);
    if (maxPrice == null) {
      return empty();
    }

    return new Money(maxPrice);
  }

  /**
   * content_summary から要員の単価を抽出（MIN値を取得）.
   *
   * <p>抽出ルール：
   * <ul>
   *   <li>「■単価：」以降の数字を抽出</li>
   *   <li>範囲がある場合は MIN 値を採用（100-120万 → 100万）</li>
   *   <li>「スキル見合い」など定量値なしは empty を返す</li>
   *   <li>括弧内のコメントは無視</li>
   *   <li>小数点は切り捨て</li>
   * </ul>
   *
   * @param contentSummary AI生成の要約文
   * @return 抽出成功時は Money、失敗時は empty Money
   */
  public static Money extractPersonUnitPrice(String contentSummary) {
    if (contentSummary == null || contentSummary.isEmpty()) {
      return empty();
    }

    String unitPriceSection = extractUnitPriceSection(contentSummary);
    if (unitPriceSection == null) {
      return empty();
    }

    BigDecimal minPrice = extractMinPrice(unitPriceSection);
    if (minPrice == null) {
      return empty();
    }

    return new Money(minPrice);
  }

  /**
   * 空の Money インスタンスを返す（抽出失敗時）.
   *
   * @return empty な Money
   */
  public static Money empty() {
    return new Money();
  }

  // ================================================
  // 状態判定
  // ================================================

  /** 値が設定されていないか（抽出失敗時）. */
  public boolean isEmpty() {
    return valueInYen == null;
  }

  /** 値が設定されているか. */
  public boolean hasValue() {
    return valueInYen != null;
  }

  // ================================================
  // 出力メソッド
  // ================================================

  /** DB保存用：円単位の BigDecimal（NULL可能）. */
  public BigDecimal getValue() {
    return valueInYen;
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
    BigDecimal manValue =
        valueInYen.divide(new BigDecimal("10000"), 2, RoundingMode.FLOOR);
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
    BigDecimal manValue =
        valueInYen.divide(new BigDecimal("10000"), 2, RoundingMode.FLOOR);
    return manValue.stripTrailingZeros().toPlainString();
  }

  /**
   * 円単位の数値：「1000000」.
   *
   * @return 円単位の long 値、empty の場合は 0L
   */
  public long toYenValue() {
    return isEmpty() ? 0L : valueInYen.longValue();
  }

  // ================================================
  // 比較処理
  // ================================================

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
    return this.valueInYen.compareTo(other.valueInYen);
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
    return this.valueInYen.equals(other.valueInYen);
  }

  @Override
  public int hashCode() {
    return isEmpty() ? 0 : valueInYen.hashCode();
  }

  @Override
  public String toString() {
    return isEmpty() ? "empty" : toJapaneseFormat();
  }

  // ================================================
  // プライベートヘルパーメソッド
  // ================================================

  /**
   * content_summary から「■単価：」で始まる行のみを抽出.
   * 改行まで（次の■が出るまでではなく）を範囲とする。
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
   * 単価セクションから最大値を抽出（案件用）.
   * 括弧内を削除した後、数字をすべて抽出。数字がなければnullを返す。
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
   * 単価セクションから最小値を抽出（要員用）.
   * 括弧内を削除した後、数字をすべて抽出。
   * 最初の範囲がある場合はそのMIN、そうでない場合は最初の数字を採用。
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
}
