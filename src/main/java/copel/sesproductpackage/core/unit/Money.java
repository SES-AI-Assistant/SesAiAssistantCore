package copel.sesproductpackage.core.unit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import copel.sesproductpackage.core.api.gpt.schema.Schema;

/**
 * 金額を表す値オブジェクト. 内部は「円」単位で保持し、画面出力時や比較処理に対応する。
 *
 * @author Copel Co., Ltd.
 */
public class Money implements Comparable<Money> {
  @Schema(
    description = "金額",
    required = true,
    type = "integer",
    itemType = BigDecimal.class,
    example = "800000"
  )
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

  // ================================================
  // 出力メソッド
  // ================================================

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
}
