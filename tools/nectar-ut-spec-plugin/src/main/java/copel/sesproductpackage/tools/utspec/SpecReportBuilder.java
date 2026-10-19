package copel.sesproductpackage.tools.utspec;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * テストクラスの検査結果と surefire・JaCoCo の結果を結合し、仕様書の内容（{@link SpecReport}）を作る。
 *
 * <p>テストケースごとに実行結果を探して付ける。surefire の結果が無いテストは「未実行」になる。
 *
 * @author Copel Co., Ltd.
 */
public class SpecReportBuilder {

  /** 仕様書に出す日時の書式。 */
  private static final DateTimeFormatter TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /**
   * 仕様書の内容を組み立てる。
   *
   * @param reports テストクラスごとの検査結果
   * @param results surefire の実行結果
   * @param coverage JaCoCo の集計結果
   * @param exclusions 除外の一覧
   * @param git リポジトリの情報
   * @param runAt 出力の日時
   * @return 仕様書の内容
   */
  public SpecReport build(
      List<TestClassReport> reports,
      SurefireResults results,
      JacocoSummary coverage,
      List<MainSourceIndex.ExclusionEntry> exclusions,
      GitContext git,
      LocalDateTime runAt) {
    String prefix = git.pathPrefix();
    List<TestCase> cases = new ArrayList<>();
    for (TestClassReport report : reports) {
      for (TestCase testCase : report.cases()) {
        TestStatus status =
            results.statusOf(
                testCase.surefireClassName(), testCase.methodName(), testCase.rowIndex());
        cases.add(testCase.withStatus(status).withFile(prefix + testCase.file()));
      }
    }
    return new SpecReport(
        git.repositoryName(),
        git.commitHash(),
        git.repositoryUrl(),
        runAt.format(TIME_FORMAT),
        results.count(TestStatus.PASSED),
        results.count(TestStatus.FAILED),
        results.count(TestStatus.SKIPPED),
        coverage,
        exclusions,
        cases);
  }
}
