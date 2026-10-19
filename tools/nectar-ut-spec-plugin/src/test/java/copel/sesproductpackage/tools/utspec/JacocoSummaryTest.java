package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link JacocoSummary} の単体テスト。 */
class JacocoSummaryTest {

  @Nested
  class instructionPercent {

    @Nested
    class 正常系 {

      /**
       * 命令のカバレッジ率を、カバー数と未カバー数から百分率で求めることを確かめる。 仕様書のサマリに命令のカバレッジを出す。
       *
       * @given 命令のカバー数が 3、未カバー数が 1 の集計結果
       * @when instructionPercent を実行する
       * @then 75.0 を返す
       */
      @Test
      void カバー数が3で未カバーが1の場合_75を返す() {
        JacocoSummary summary = new JacocoSummary(Set.of(), 3, 1, 0, 0);

        assertThat(summary.instructionPercent()).isEqualTo(75.0);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 対象の命令が0件の場合はカバレッジを100.0にすることを確かめる。 対象が無いクラスだけの集計で、ゼロ除算を起こさない。
       *
       * @given 命令のカバー数と未カバー数が共に 0 の集計結果
       * @when instructionPercent を実行する
       * @then 100.0 を返す
       */
      @Test
      void 対象の命令が0件の場合_100を返す() {
        JacocoSummary summary = new JacocoSummary(Set.of(), 0, 0, 0, 0);

        assertThat(summary.instructionPercent()).isEqualTo(100.0);
      }
    }
  }

  @Nested
  class branchPercent {

    @Nested
    class 正常系 {

      /**
       * 分岐のカバレッジ率を、カバー数と未カバー数から百分率で求めることを確かめる。 仕様書のサマリに分岐のカバレッジを出す。
       *
       * @given 分岐のカバー数が 1、未カバー数が 1 の集計結果
       * @when branchPercent を実行する
       * @then 50.0 を返す
       */
      @Test
      void カバー数と未カバーが1ずつの場合_50を返す() {
        JacocoSummary summary = new JacocoSummary(Set.of(), 0, 0, 1, 1);

        assertThat(summary.branchPercent()).isEqualTo(50.0);
      }
    }
  }

  @Nested
  class rate {

    @Nested
    class 正常系 {

      /**
       * 指定したカバー数と未カバー数からカバレッジ率を求めることを確かめる。 命令と分岐の両方で同じ計算を使うため、計算を1か所にまとめる。
       *
       * @given カバー数が 1、未カバー数が 3 の値
       * @when rate を実行する
       * @then 25.0 を返す
       */
      @Test
      void カバー数が1で未カバーが3の場合_25を返す() {
        JacocoSummary summary = new JacocoSummary(Set.of(), 0, 0, 0, 0);

        assertThat(summary.rate(1, 3)).isEqualTo(25.0);
      }
    }
  }
}
