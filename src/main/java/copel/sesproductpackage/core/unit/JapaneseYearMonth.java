package copel.sesproductpackage.core.unit;

import java.time.YearMonth;
import java.time.temporal.ChronoField;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 「YYYY年M月」の形式で表す年月の値オブジェクト.
 *
 * <p>AIが抽出したスキルシートの開始年月・終了年月を、Java で月数を計算できる値にする。 解釈できない文字列は空（{@link
 * Optional#empty()}）として返し、集計から除外するかどうかは呼び出し側が決める。
 *
 * @author Copel Co., Ltd.
 */
public class JapaneseYearMonth implements Comparable<JapaneseYearMonth> {

  /** 「YYYY年M月」の形式を表す正規表現。年は半角数字4桁、月は1から12で、月の先頭の0も許容する。 */
  private static final Pattern PATTERN = Pattern.compile("^([0-9]{4})年(0?[1-9]|1[0-2])月$");

  /** 年月の値。 */
  private final YearMonth yearMonth;

  /**
   * コンストラクタ.
   *
   * @param yearMonth 年月の値
   */
  private JapaneseYearMonth(final YearMonth yearMonth) {
    this.yearMonth = yearMonth;
  }

  /**
   * 「YYYY年M月」の文字列を解釈して年月を作ります.
   *
   * <p>前後の空白は除いてから解釈する。AIの出力は前後に空白が入ることがあるため。 null や形式に合わない文字列（「2023年13月」「令和5年4月」など）は解釈できないため空を返す。
   *
   * @param text 年月の文字列
   * @return 年月。解釈できない場合は空
   */
  public static Optional<JapaneseYearMonth> parse(final String text) {
    if (text == null) {
      return Optional.empty();
    }
    Matcher matcher = PATTERN.matcher(text.strip());
    if (!matcher.matches()) {
      return Optional.empty();
    }
    YearMonth yearMonth =
        YearMonth.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    return Optional.of(new JapaneseYearMonth(yearMonth));
  }

  /**
   * 年月を、0年1月からの通し番号の月として返します.
   *
   * <p>2つの年月の間の月数を、差の計算だけで求めるために使う。年をまたぐ計算で月の桁を間違えないよう、年と月を1つの整数にまとめる。
   *
   * @return 0年1月からの通し番号の月
   */
  public int monthIndex() {
    return (int) this.yearMonth.getLong(ChronoField.PROLEPTIC_MONTH);
  }

  /**
   * 「YYYY年M月」の形式の文字列を返します.
   *
   * <p>月の先頭に0を付けない。要約文の表記を、AIの出力の書き方によらず統一するため。
   *
   * @return 「2024年4月」の形式の文字列
   */
  public String toJapaneseFormat() {
    return this.yearMonth.getYear() + "年" + this.yearMonth.getMonthValue() + "月";
  }

  /**
   * 他の年月と前後を比べます.
   *
   * @param other 比較対象の年月
   * @return 自身が後なら正の値、前なら負の値、同じなら0
   */
  @Override
  public int compareTo(final JapaneseYearMonth other) {
    return this.yearMonth.compareTo(other.yearMonth);
  }

  /**
   * 同じ年月かどうかを判定します.
   *
   * <p>{@link #compareTo(JapaneseYearMonth)} が0を返す組み合わせと一致させる。 同じ年月の値を、等しい値として扱うためである。
   *
   * @param other 比較対象
   * @return 同じ年月の {@link JapaneseYearMonth} ならtrue
   */
  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof JapaneseYearMonth)) {
      return false;
    }
    return this.yearMonth.equals(((JapaneseYearMonth) other).yearMonth);
  }

  /**
   * 年月に対応するハッシュ値を返します.
   *
   * <p>{@link #equals(Object)} で等しい年月は、同じハッシュ値になる。
   *
   * @return ハッシュ値
   */
  @Override
  public int hashCode() {
    return this.yearMonth.hashCode();
  }
}
