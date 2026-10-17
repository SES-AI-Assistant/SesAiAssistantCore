package copel.sesproductpackage.core.unit;

/**
 * 経験期間を表す値オブジェクト. 内部は「月数」で保持し、「N年」「Nヶ月」「N年Nヶ月」形式で出力する。
 *
 * <p>AIに期間を文字列で出力させると、記載がない場合に「unknown」等の不明値が混入するため、 月数（整数）で受け取り本クラスで表示形式へ変換する。
 *
 * @author Copel Co., Ltd.
 */
public class ExperienceDuration {
  /** 1年あたりの月数. */
  private static final int MONTHS_PER_YEAR = 12;

  /** 経験月数. 未設定の場合はnull. */
  private final Integer months;

  /**
   * コンストラクタ.
   *
   * @param months 経験月数（null・0以下は期間未設定として扱う）
   */
  public ExperienceDuration(final Integer months) {
    this.months = months;
  }

  /**
   * 期間が未設定かどうかを判定します.
   *
   * @return 月数がnullまたは0以下の場合はtrue
   */
  public boolean isEmpty() {
    return this.months == null || this.months <= 0;
  }

  /**
   * 「N年」「Nヶ月」「N年Nヶ月」形式の文字列に変換します.
   *
   * @return 期間の日本語表記。未設定の場合は空文字
   */
  public String toJapaneseFormat() {
    if (isEmpty()) {
      return "";
    }
    int years = this.months / MONTHS_PER_YEAR;
    int restMonths = this.months % MONTHS_PER_YEAR;
    StringBuilder sb = new StringBuilder();
    if (years > 0) {
      sb.append(years).append("年");
    }
    if (restMonths > 0) {
      sb.append(restMonths).append("ヶ月");
    }
    return sb.toString();
  }

  /**
   * 「スキル: 期間」の「: 期間」部分を生成します.
   *
   * @param separator スキル名と期間の区切り文字
   * @return 区切り文字付きの期間表記。未設定の場合は空文字
   */
  public String toSuffix(final String separator) {
    return isEmpty() ? "" : separator + toJapaneseFormat();
  }
}
