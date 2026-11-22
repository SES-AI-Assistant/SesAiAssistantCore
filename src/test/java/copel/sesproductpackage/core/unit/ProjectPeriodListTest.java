package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link ProjectPeriodList} の単体テスト。 */
class ProjectPeriodListTest {

  @Nested
  class add {

    @Nested
    class 正常系 {

      /**
       * 追加した案件期間が、和集合の月数に反映されることを確かめる。 追加しただけで月数に入らなければ、集計の結果が空になるためである。
       *
       * @given 一覧に 2023年4月から2023年6月の期間を追加する
       * @when unionMonths を実行する
       * @then 3を返す
       */
      @Test
      void 期間を1件追加した場合_その期間の月数を返す() {
        ProjectPeriodList periods = new ProjectPeriodList();
        periods.add(period("2023年4月", "2023年6月"));

        assertThat(periods.unionMonths()).isEqualTo(3);
      }
    }
  }

  @Nested
  class unionMonths {

    @Nested
    class 正常系 {

      /**
       * 重ならない2件の期間の月数を足し合わせることを確かめる。 離れた期間の経験は、それぞれの月数の合計になるためである。
       *
       * @given 2023年1月から2023年12月の期間と、2024年2月から2024年4月の期間
       * @when unionMonths を実行する
       * @then 15を返す
       */
      @Test
      void 重ならない2件の場合_両方の月数の合計を返す() {
        ProjectPeriodList periods = new ProjectPeriodList();
        periods.add(period("2023年1月", "2023年12月"));
        periods.add(period("2024年2月", "2024年4月"));

        assertThat(periods.unionMonths()).isEqualTo(15);
      }

      /**
       * 重なる2件の期間で、重なる月を1回だけ数えることを確かめる。 同じ時期に複数の案件で使ったスキルの経験を、二重に数えないためである。
       *
       * @given 2023年4月から2023年9月の期間と、2023年7月から2023年12月の期間
       * @when unionMonths を実行する
       * @then 9を返す
       */
      @Test
      void 重なる2件の場合_重なる月を1回だけ数えた月数を返す() {
        ProjectPeriodList periods = new ProjectPeriodList();
        periods.add(period("2023年4月", "2023年9月"));
        periods.add(period("2023年7月", "2023年12月"));

        assertThat(periods.unionMonths()).isEqualTo(9);
      }

      /**
       * ある期間が別の期間に含まれる場合に、含まれる側の月数を足さないことを確かめる。 大きい案件の中の小さい案件は、新しい経験を増やさないためである。
       *
       * @given 2023年1月から2023年12月の期間と、その中の2023年3月から2023年5月の期間
       * @when unionMonths を実行する
       * @then 12を返す
       */
      @Test
      void 一方が他方に含まれる場合_含まれる側を足さず12を返す() {
        ProjectPeriodList periods = new ProjectPeriodList();
        periods.add(period("2023年1月", "2023年12月"));
        periods.add(period("2023年3月", "2023年5月"));

        assertThat(periods.unionMonths()).isEqualTo(12);
      }

      /**
       * 期間を登録した順が逆でも、同じ和集合の月数になることを確かめる。 案件の並びは入力の順によって変わるため、結果が順序に左右されないようにするためである。
       *
       * @given 2023年7月から2023年12月の期間と、2023年4月から2023年9月の期間を、この順に登録する
       * @when unionMonths を実行する
       * @then 9を返す
       */
      @Test
      void 登録の順が逆の場合_同じ月数を返す() {
        ProjectPeriodList periods = new ProjectPeriodList();
        periods.add(period("2023年7月", "2023年12月"));
        periods.add(period("2023年4月", "2023年9月"));

        assertThat(periods.unionMonths()).isEqualTo(9);
      }

      /**
       * 同じ期間を2回登録した場合に、1回分の月数になることを確かめる。 同じ案件が2回届いた場合も、経験は1回分として数えるためである。
       *
       * @given 2023年4月から2023年6月の期間を2回登録する
       * @when unionMonths を実行する
       * @then 3を返す
       */
      @Test
      void 同じ期間を2回登録した場合_1回分の月数を返す() {
        ProjectPeriodList periods = new ProjectPeriodList();
        periods.add(period("2023年4月", "2023年6月"));
        periods.add(period("2023年4月", "2023年6月"));

        assertThat(periods.unionMonths()).isEqualTo(3);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 隣り合う2件の期間を、途切れなく連続した月数として数えることを確かめる。 月が途切れていないため、重なりとは違い両方の月を足すためである。
       *
       * @given 2023年1月から2023年3月の期間と、2023年4月から2023年6月の期間
       * @when unionMonths を実行する
       * @then 6を返す
       */
      @Test
      void 隣り合う2件の場合_連続した月数を返す() {
        ProjectPeriodList periods = new ProjectPeriodList();
        periods.add(period("2023年1月", "2023年3月"));
        periods.add(period("2023年4月", "2023年6月"));

        assertThat(periods.unionMonths()).isEqualTo(6);
      }

      /**
       * 期間が1件も無い場合に、月数として0を返すことを確かめる。 経験が無いスキルを、月数0として扱えるようにするためである。
       *
       * @given 一覧に期間を1件も追加しない
       * @when unionMonths を実行する
       * @then 0を返す
       */
      @Test
      void 期間が1件も無い場合_0を返す() {
        assertThat(new ProjectPeriodList().unionMonths()).isZero();
      }

      /**
       * 1か月だけの期間の月数が1になることを確かめる。 開始と終了が同じ月の案件も経験として1か月を残すためである。
       *
       * @given 2023年4月から2023年4月の期間
       * @when unionMonths を実行する
       * @then 1を返す
       */
      @Test
      void 一か月だけの期間の場合_1を返す() {
        ProjectPeriodList periods = new ProjectPeriodList();
        periods.add(period("2023年4月", "2023年4月"));

        assertThat(periods.unionMonths()).isEqualTo(1);
      }
    }
  }

  /**
   * 開始年月と終了年月の文字列から案件期間を作る.
   *
   * @param start 開始年月の文字列
   * @param end 終了年月の文字列
   * @return 案件期間
   */
  private static ProjectPeriod period(final String start, final String end) {
    return ProjectPeriod.of(start, end).orElseThrow();
  }
}
