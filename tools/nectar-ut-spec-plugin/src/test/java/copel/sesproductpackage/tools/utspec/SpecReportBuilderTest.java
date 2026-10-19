package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SpecReportBuilder} の単体テスト。 */
class SpecReportBuilderTest {

  /** 実行に使うディレクトリ。git の無い場所として存在しないパスを使う。 */
  private static final Path NO_REPO = Path.of("no-such-dir-for-report-builder");

  /**
   * テストケースを1件作る。
   *
   * @return テストケース
   */
  private static TestCase sampleCase() {
    return new TestCase(
        "pkg.ATest",
        "f",
        "正常系",
        "件数が3の場合_3を返す",
        "",
        0,
        "src/test/java/pkg/ATest.java",
        10,
        "入力を集計することを確かめる。",
        "理由を書く。",
        "件数が3の入力",
        "f を実行する",
        "3を返す",
        TestStatus.UNKNOWN);
  }

  /**
   * テストクラスの検査結果を1件作る。
   *
   * @return 検査結果
   */
  private static TestClassReport sampleReport() {
    return new TestClassReport(
        "pkg.ATest", "pkg.A", "src/test/java/pkg/ATest.java", List.of(), List.of(sampleCase()));
  }

  @Nested
  class build {

    @Nested
    class 正常系 {

      /**
       * surefire の結果を、テストケースごとの実行結果として付けることを確かめる。 仕様書の結果の列に成否を出す。
       *
       * @given クラス pkg.ATest$f$正常系 の成功の結果を持つ実行結果
       * @when build を実行する
       * @then テストケースの結果を成功にし、成功の件数を1件返す
       */
      @Test
      void 成功の結果がある場合_テストケースに成功を付けた値を返す() {
        SurefireResults results =
            new SurefireResults(
                Map.of(
                    "pkg.ATest$f$正常系",
                    List.of(new SurefireResults.Outcome("件数が3の場合_3を返す()", TestStatus.PASSED))));

        SpecReport report =
            new SpecReportBuilder()
                .build(
                    List.of(sampleReport()),
                    results,
                    new JacocoSummary(Set.of(), 0, 0, 0, 0),
                    List.of(),
                    gitContext(),
                    LocalDateTime.of(2026, 10, 19, 10, 0, 0));

        assertThat(report.cases())
            .singleElement()
            .satisfies(testCase -> assertThat(testCase.status()).isEqualTo(TestStatus.PASSED));
        assertThat(report.passedCount()).isEqualTo(1);
        assertThat(report.generatedAt()).isEqualTo("2026-10-19 10:00:00");
      }
    }

    @Nested
    class 境界値 {

      /**
       * surefire の結果が無いテストケースは未実行にすることを確かめる。 テストが実行されていない場合に成功と表示しない。
       *
       * @given 実行結果が空の状態
       * @when build を実行する
       * @then テストケースを未実行にし、各件数を0として返す
       */
      @Test
      void 結果が無い場合_テストケースを未実行にした値を返す() {
        SpecReport report =
            new SpecReportBuilder()
                .build(
                    List.of(sampleReport()),
                    new SurefireResults(Map.of()),
                    new JacocoSummary(Set.of(), 0, 0, 0, 0),
                    List.of(),
                    gitContext(),
                    LocalDateTime.of(2026, 10, 19, 10, 0, 0));

        assertThat(report.cases())
            .singleElement()
            .satisfies(testCase -> assertThat(testCase.status()).isEqualTo(TestStatus.UNKNOWN));
        assertThat(report.failedCount()).isZero();
      }
    }
  }

  /**
   * 環境変数の無い Git の情報を作る。
   *
   * @return Git の情報
   */
  private static GitContext gitContext() {
    return new GitContext(Map.of("GITHUB_SHA", "abc123"), NO_REPO);
  }
}
