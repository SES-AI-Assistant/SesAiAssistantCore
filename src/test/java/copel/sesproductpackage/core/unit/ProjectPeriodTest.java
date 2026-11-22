package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link ProjectPeriod} の単体テスト。 */
class ProjectPeriodTest {

  @Nested
  class of {

    @Nested
    class 正常系 {

      /**
       * 開始と終了が有効な年月の場合に、案件期間を作ることを確かめる。 案件の期間を月数に変換して集計するための前提である。
       *
       * @given 開始年月が "2023年4月"、終了年月が "2023年6月"
       * @when of を実行する
       * @then 3か月の案件期間を返す
       */
      @Test
      void 開始と終了が有効な年月の場合_3か月の案件期間を返す() {
        assertThat(ProjectPeriod.of("2023年4月", "2023年6月").map(ProjectPeriod::months)).contains(3);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 開始年月が null の場合は案件期間を作らないことを確かめる。 開始が無い案件は期間が決まらず、月数を計算できないため集計から除外する。
       *
       * @given 開始年月が null、終了年月が "2023年6月"
       * @when of を実行する
       * @then 空を返す
       */
      @Test
      void 開始年月がnullの場合_空を返す() {
        assertThat(ProjectPeriod.of(null, "2023年6月")).isEmpty();
      }

      /**
       * 終了年月が解釈できない場合は案件期間を作らないことを確かめる。 解釈できない終了年月で月数を計算すると、誤った合計になるためである。
       *
       * @given 開始年月が "2023年4月"、終了年月が "2023年13月"
       * @when of を実行する
       * @then 空を返す
       */
      @Test
      void 終了年月が解釈できない場合_空を返す() {
        assertThat(ProjectPeriod.of("2023年4月", "2023年13月")).isEmpty();
      }

      /**
       * 終了年月が開始年月より前の場合は案件期間を作らないことを確かめる。 負の月数が合計に混ざると、経験月数が実際より少なく出るためである。
       *
       * @given 開始年月が "2023年9月"、終了年月が "2023年4月"
       * @when of を実行する
       * @then 空を返す
       */
      @Test
      void 終了年月が開始年月より前の場合_空を返す() {
        assertThat(ProjectPeriod.of("2023年9月", "2023年4月")).isEmpty();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 開始と終了が同じ年月の場合に、1か月の案件期間を作ることを確かめる。 開始と終了の両方を含めて数えるため、1か月の案件も経験として残す。
       *
       * @given 開始年月が "2023年4月"、終了年月が "2023年4月"
       * @when of を実行する
       * @then 1か月の案件期間を返す
       */
      @Test
      void 開始と終了が同じ年月の場合_1か月の案件期間を返す() {
        assertThat(ProjectPeriod.of("2023年4月", "2023年4月").map(ProjectPeriod::months)).contains(1);
      }

      /**
       * 終了年月が無く開始年月だけが有効な場合は、案件期間を作らないことを確かめる。 終了が無い案件を開始だけで集計すると、期間を勝手に決めることになるためである。
       *
       * @given 開始年月が "2023年4月"、終了年月が null
       * @when of を実行する
       * @then 空を返す
       */
      @Test
      void 終了年月がnullで開始年月が有効な場合_空を返す() {
        assertThat(ProjectPeriod.of("2023年4月", null)).isEmpty();
      }

      /**
       * 年をまたぐ案件の期間が、開始と終了を含めて12か月になることを確かめる。 年の境目で月数が1つ減らないようにするためである。
       *
       * @given 開始年月が "2023年4月"、終了年月が "2024年3月"
       * @when of を実行する
       * @then 12か月の案件期間を返す
       */
      @Test
      void 年をまたぐ期間の場合_12か月の案件期間を返す() {
        assertThat(ProjectPeriod.of("2023年4月", "2024年3月").map(ProjectPeriod::months)).contains(12);
      }
    }
  }

  @Nested
  class months {

    @Nested
    class 正常系 {

      /**
       * 未来の年月を含む案件期間を、記載どおりに月数へ数えることを確かめる。 終了年月が基準日より未来でも補正や除外をしない仕様のためである。
       *
       * @given 開始年月が "2099年10月"、終了年月が "2099年12月"
       * @when months を実行する
       * @then 3を返す
       */
      @Test
      void 終了年月が未来の場合_記載どおりの月数を返す() {
        assertThat(ProjectPeriod.of("2099年10月", "2099年12月").orElseThrow().months()).isEqualTo(3);
      }
    }
  }
}
