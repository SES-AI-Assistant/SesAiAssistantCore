package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SpecReport} の単体テスト。 */
class SpecReportTest {

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 仕様書に出す各項目をそのまま保持することを確かめる。 HTML の出力で値を取り違えない。
       *
       * @given リポジトリ名、件数、カバレッジが設定されている
       * @when 件数とリポジトリ名を取り出す
       * @then 渡した値をそのまま返す
       */
      @Test
      void 項目が設定されている場合_設定した値を返す() {
        SpecReport report =
            new SpecReport(
                "repo",
                "abc",
                null,
                "2026-10-19 10:00:00",
                1,
                2,
                3,
                new JacocoSummary(Set.of(), 0, 0, 0, 0),
                List.of(),
                List.of());

        assertThat(report.repositoryName()).isEqualTo("repo");
        assertThat(report.failedCount()).isEqualTo(2);
        assertThat(report.skippedCount()).isEqualTo(3);
      }
    }
  }
}
