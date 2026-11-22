package copel.sesproductpackage.core.unit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 案件期間の一覧を表すクラス. 一覧の中で重なる月を1回だけ数える月数（和集合の月数）を求める。
 *
 * <p>同じスキルを複数の案件で使った場合、案件の期間が重なっていても、その月に経験したのは1か月分である。 そのため期間を単純に足さず、重なりを取り除いて数える。
 *
 * @author Copel Co., Ltd.
 */
public class ProjectPeriodList {

  /** 案件期間の一覧。登録の順に並ぶ。 */
  private final List<ProjectPeriod> periods = new ArrayList<>();

  /**
   * 案件期間を一覧に追加します.
   *
   * @param period 追加する案件期間
   */
  public void add(final ProjectPeriod period) {
    this.periods.add(period);
  }

  /**
   * 一覧の案件期間の和集合の月数を返します.
   *
   * <p>開始年月の早い順に並べ、直前までに数えた最後の月より後の部分だけを足す。 期間を1か月ずつ展開して数えるのと同じ結果になるが、期間が長くても件数に比例した計算量で済む。
   *
   * @return 重なる月を1回だけ数えた月数。一覧が空の場合は0
   */
  public int unionMonths() {
    List<ProjectPeriod> sorted = new ArrayList<>(this.periods);
    sorted.sort(Comparator.comparingInt((ProjectPeriod period) -> period.start().monthIndex()));
    int total = 0;
    // 数え終えた範囲の最後の月の通し番号。まだ何も数えていない場合は、どの月よりも前とする。
    int countedEnd = Integer.MIN_VALUE;
    for (ProjectPeriod period : sorted) {
      int startIndex = period.start().monthIndex();
      int endIndex = period.end().monthIndex();
      if (startIndex > countedEnd) {
        // 数え終えた範囲と重ならない（または隣り合う）期間は、全体をそのまま足す
        total += endIndex - startIndex + 1;
        countedEnd = endIndex;
      } else if (endIndex > countedEnd) {
        // 重なる期間は、数え終えた範囲より後の月だけを足す
        total += endIndex - countedEnd;
        countedEnd = endIndex;
      }
    }
    return total;
  }
}
