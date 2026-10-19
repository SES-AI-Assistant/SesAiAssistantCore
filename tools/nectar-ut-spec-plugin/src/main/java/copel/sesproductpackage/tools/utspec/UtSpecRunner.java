package copel.sesproductpackage.tools.utspec;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * UT 仕様の検査と仕様書の出力を、1回の実行で行う。
 *
 * <p>処理の順番は次のとおり。
 *
 * <ol>
 *   <li>JaCoCo の CSV から数えるコードを持つ本体クラスを求める
 *   <li>本体ソースとテストソースを読み、テストクラスごとに構造と書式を検査する
 *   <li>本体クラスとテストクラスの1対1を検査する（UT-01、UT-02）
 *   <li>違反が無ければ、surefire の結果と結合して仕様書 HTML を出力する
 * </ol>
 *
 * <p>違反がある場合は仕様書を出力せず、違反の一覧だけを返す。
 *
 * @author Copel Co., Ltd.
 */
public class UtSpecRunner {

  /** 実行に必要なパスの設定。 */
  public record Config(
      Path basedir,
      Path mainSourceDir,
      Path testSourceDir,
      Path jacocoCsv,
      Path surefireReportsDir,
      Path outputDirectory) {}

  /** 仕様書の出力ファイル名。 */
  private static final String REPORT_FILE_NAME = "index.html";

  /** GitHub の情報を求める部品。 */
  private final GitContext git;

  /** 仕様書の日時に使う時計。テストで固定できるように外から渡す。 */
  private final Clock clock;

  /** JaCoCo の CSV を読む部品。 */
  private final JacocoCsvReader jacocoReader;

  /** 本体ソースを走査する部品。 */
  private final MainSourceScanner mainScanner;

  /** テストソースを走査する部品。 */
  private final TestSourceScanner testScanner;

  /** surefire のレポートを読む部品。 */
  private final SurefireResultReader surefireReader;

  /** 仕様書の内容を組み立てる部品。 */
  private final SpecReportBuilder reportBuilder;

  /** 仕様書の HTML を出力する部品。 */
  private final SpecHtmlWriter htmlWriter;

  /**
   * Git の情報と時計を指定して実行器を生成する。
   *
   * @param git リポジトリの情報
   * @param clock 仕様書の日時に使う時計
   */
  public UtSpecRunner(GitContext git, Clock clock) {
    this.git = git;
    this.clock = clock;
    this.jacocoReader = new JacocoCsvReader();
    this.mainScanner = new MainSourceScanner();
    this.testScanner = new TestSourceScanner();
    this.surefireReader = new SurefireResultReader();
    this.reportBuilder = new SpecReportBuilder();
    this.htmlWriter = new SpecHtmlWriter();
  }

  /**
   * 検査を行い、違反があればその一覧を、無ければ仕様書 HTML を出力して空の一覧を返す。
   *
   * @param config パスの設定
   * @return 検出した違反の一覧（昇順）
   * @throws IOException ファイルを読み書きできない場合
   * @throws IllegalStateException jacoco.csv が無い場合
   */
  public List<Violation> run(Config config) throws IOException {
    JacocoSummary coverage = jacocoReader.read(config.jacocoCsv());
    MainSourceIndex main = mainScanner.scan(config.basedir(), config.mainSourceDir());
    List<TestClassReport> reports =
        testScanner.scan(config.basedir(), config.testSourceDir(), main.bodies());

    List<Violation> violations = new ArrayList<>();
    for (TestClassReport report : reports) {
      violations.addAll(report.violations());
    }
    violations.addAll(main.violations());
    violations.addAll(checkOneToOne(coverage, main, reports, config.jacocoCsv()));
    violations.sort(Comparator.comparing(Violation::file).thenComparingInt(Violation::line));

    if (violations.isEmpty()) {
      SurefireResults results = surefireReader.read(config.surefireReportsDir());
      SpecReport report =
          reportBuilder.build(
              reports, results, coverage, main.exclusions(), git, LocalDateTime.now(clock));
      htmlWriter.write(
          config.outputDirectory().resolve(REPORT_FILE_NAME), htmlWriter.render(report));
    }
    return violations;
  }

  /**
   * 数えるコードを持つ本体クラスと、テストクラスの1対1を検査する。
   *
   * <p>除外された本体クラス（{@code @CoverageExcludedGenerated}）はテストが無くても違反にしない。
   * ただし、テストクラスの対象が本体に無い場合は、除外されていても違反にする。
   *
   * @param coverage JaCoCo の集計結果
   * @param main 本体ソースの索引
   * @param reports テストクラスごとの検査結果
   * @param jacocoCsv JaCoCo の CSV のパス（本体のソースが見つからない場合の報告先）
   * @return 1対1 の違反の一覧
   */
  public List<Violation> checkOneToOne(
      JacocoSummary coverage, MainSourceIndex main, List<TestClassReport> reports, Path jacocoCsv) {
    Set<String> knownBodies = new TreeSet<>(coverage.countedClasses());
    main.bodies().values().stream()
        .filter(BodyClass::excluded)
        .forEach(body -> knownBodies.add(body.fqn()));
    Set<String> testedBodies =
        reports.stream()
            .map(TestClassReport::bodyFqn)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    List<Violation> violations = new ArrayList<>();
    for (TestClassReport report : reports) {
      if (report.bodyFqn() != null && !knownBodies.contains(report.bodyFqn())) {
        violations.add(
            new Violation(
                report.relativePath(),
                1,
                UtRule.UT02,
                "対応する本体クラス「" + report.bodyFqn() + "」が数えるコードを持つクラスに存在しない"));
      }
    }
    for (String counted : coverage.countedClasses()) {
      if (!testedBodies.contains(counted)) {
        BodyClass body = main.bodies().get(counted);
        String file = body == null ? jacocoCsv.toString() : body.relativePath();
        int line = body == null ? 0 : body.line();
        violations.add(
            new Violation(
                file,
                line,
                UtRule.UT01,
                "本体クラス「" + counted + "」に対応するテストクラス「" + counted + "Test」がない"));
      }
    }
    return violations;
  }
}
