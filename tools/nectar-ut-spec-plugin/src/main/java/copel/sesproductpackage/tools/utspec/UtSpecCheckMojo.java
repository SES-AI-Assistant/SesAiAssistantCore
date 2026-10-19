package copel.sesproductpackage.tools.utspec;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * UT 仕様の検査と、テスト仕様書 HTML の出力を行う Maven ゴール（nectar-ut-spec:check）。
 *
 * <p>対象リポジトリの {@code mvn verify} で、jacoco:report の後（verify フェーズ）に1回実行する。
 * 違反があればすべてをログに出してビルドを失敗させ、違反がなければ仕様書 HTML を出力する。
 *
 * @author Copel Co., Ltd.
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class UtSpecCheckMojo extends AbstractMojo {

  /** true の場合は検査も仕様書の出力も行わない。 */
  @Parameter(property = "nectar.ut.skip", defaultValue = "false")
  boolean skip;

  /** true の場合は違反があるとビルドを失敗させる。false の場合は警告だけにする。 */
  @Parameter(property = "nectar.ut.failOnViolation", defaultValue = "true")
  boolean failOnViolation;

  /** リポジトリ直下。相対パスの基準。 */
  @Parameter(defaultValue = "${basedir}", readonly = true)
  File basedir;

  /** 本体ソースのディレクトリ。 */
  @Parameter(defaultValue = "${project.build.sourceDirectory}")
  File sourceDirectory;

  /** テストソースのディレクトリ。 */
  @Parameter(defaultValue = "${project.build.testSourceDirectory}")
  File testSourceDirectory;

  /** JaCoCo の CSV レポート（jacoco:report の出力）。 */
  @Parameter(defaultValue = "${project.build.directory}/site/jacoco/jacoco.csv")
  File jacocoCsv;

  /** surefire のレポートのディレクトリ。 */
  @Parameter(defaultValue = "${project.build.directory}/surefire-reports")
  File surefireReportsDirectory;

  /** 仕様書 HTML の出力先ディレクトリ。 */
  @Parameter(defaultValue = "${project.build.directory}/nectar-ut-spec")
  File outputDirectory;

  /**
   * 検査を実行する。違反があればログに出し、failOnViolation が true の場合はビルドを失敗させる。
   *
   * @throws MojoExecutionException 検査に必要なファイルを読めない、または設定が不正な場合
   * @throws MojoFailureException 違反があり、failOnViolation が true の場合
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("UT 仕様の検査をスキップする（nectar.ut.skip=true）");
      return;
    }
    UtSpecRunner.Config config =
        new UtSpecRunner.Config(
            basedir.toPath(),
            sourceDirectory.toPath(),
            testSourceDirectory.toPath(),
            jacocoCsv.toPath(),
            surefireReportsDirectory.toPath(),
            outputDirectory.toPath());
    UtSpecRunner runner =
        new UtSpecRunner(
            new GitContext(System.getenv(), basedir.toPath()), Clock.systemDefaultZone());
    List<Violation> violations;
    try {
      violations = runner.run(config);
    } catch (IOException | IllegalStateException e) {
      throw new MojoExecutionException("UT 仕様の検査を実行できない: " + e.getMessage(), e);
    } catch (UncheckedIOException e) {
      throw new MojoExecutionException(
          "ソースファイルを読み込めない。ファイルの権限と文字コード（UTF-8）を確認する: " + e.getCause().getMessage(), e);
    }
    if (violations.isEmpty()) {
      getLog().info("UT 仕様の検査: 違反なし。仕様書: " + outputDirectory.toPath().resolve("index.html"));
      return;
    }
    for (Violation violation : violations) {
      getLog().error(violation.format());
    }
    String summary = "UT 仕様の違反が " + violations.size() + " 件ある。仕様書は出力していない";
    if (failOnViolation) {
      throw new MojoFailureException(summary);
    }
    getLog().warn(summary + "（failOnViolation=false のため継続）");
  }
}
