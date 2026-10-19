package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link TestClassReport} の単体テスト。 */
class TestClassReportTest {

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 検査結果の各項目をそのまま保持することを確かめる。 集計と仕様書の出力で同じ値を参照できるようにする。
       *
       * @given 完全修飾名、本体、パス、違反、テストケースが設定されている
       * @when 各項目を取り出す
       * @then 渡した値をそのまま返す
       */
      @Test
      void 全ての項目が設定されている場合_設定した値を返す() {
        TestClassReport report =
            new TestClassReport(
                "pkg.ATest", "pkg.A", "src/test/java/pkg/ATest.java", List.of(), List.of());

        assertThat(report.classFqn()).isEqualTo("pkg.ATest");
        assertThat(report.bodyFqn()).isEqualTo("pkg.A");
        assertThat(report.relativePath()).isEqualTo("src/test/java/pkg/ATest.java");
        assertThat(report.violations()).isEmpty();
        assertThat(report.cases()).isEmpty();
      }
    }
  }
}
