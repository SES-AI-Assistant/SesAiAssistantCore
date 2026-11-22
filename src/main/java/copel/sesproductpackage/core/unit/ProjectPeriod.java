package copel.sesproductpackage.core.unit;

import java.util.Optional;

/**
 * 案件の期間（開始年月から終了年月まで）を表す値オブジェクト.
 *
 * <p>開始年月と終了年月の両方を含めて月数を数える。2023年4月から2023年6月までは3か月である。
 *
 * @author Copel Co., Ltd.
 */
public final class ProjectPeriod {

  /** 開始年月。 */
  private final JapaneseYearMonth start;

  /** 終了年月。 */
  private final JapaneseYearMonth end;

  /**
   * コンストラクタ.
   *
   * @param start 開始年月
   * @param end 終了年月
   */
  private ProjectPeriod(final JapaneseYearMonth start, final JapaneseYearMonth end) {
    this.start = start;
    this.end = end;
  }

  /**
   * 開始年月と終了年月の文字列から案件期間を作ります.
   *
   * <p>どちらかが「YYYY年M月」として解釈できない場合、終了年月が開始年月より前の場合は、期間として扱えないため空を返す。
   * 期間が無効な案件を集計すると、負の月数や誤った合計になるため除外する。
   *
   * @param startText 開始年月の文字列
   * @param endText 終了年月の文字列
   * @return 案件期間。無効な場合は空
   */
  public static Optional<ProjectPeriod> of(final String startText, final String endText) {
    Optional<JapaneseYearMonth> start = JapaneseYearMonth.parse(startText);
    Optional<JapaneseYearMonth> end = JapaneseYearMonth.parse(endText);
    if (start.isEmpty() || end.isEmpty() || start.get().compareTo(end.get()) > 0) {
      return Optional.empty();
    }
    return Optional.of(new ProjectPeriod(start.get(), end.get()));
  }

  /**
   * 開始年月から終了年月までの月数を返します.
   *
   * <p>開始年月と終了年月の両方を含める。開始と終了が同じ年月の場合は1か月とする。
   *
   * @return 月数（1以上）
   */
  public int months() {
    return this.end.monthIndex() - this.start.monthIndex() + 1;
  }

  /**
   * 開始年月を返します.
   *
   * @return 開始年月
   */
  public JapaneseYearMonth start() {
    return this.start;
  }

  /**
   * 終了年月を返します.
   *
   * @return 終了年月
   */
  public JapaneseYearMonth end() {
    return this.end;
  }
}
