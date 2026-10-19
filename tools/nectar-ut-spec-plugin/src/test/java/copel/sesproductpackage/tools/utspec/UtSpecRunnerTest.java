package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link UtSpecRunner} の単体テスト。 */
class UtSpecRunnerTest {

  /** 一時ディレクトリ。リポジトリの見立てとして使う。 */
  @TempDir Path tempDir;

  /** 固定の時計。仕様書の日時を毎回同じにするため。 */
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-19T01:00:00Z"), ZoneId.of("Asia/Tokyo"));

  /** JaCoCo の CSV のヘッダー。 */
  private static final String CSV_HEADER =
      "GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED";

  /** 書式が規定どおりのテストクラス ATest。第1階層 f、第2階層 正常系、テスト1件。 */
  private static final String VALID_TEST =
      """
            package pkg;

            import org.junit.jupiter.api.Nested;
            import org.junit.jupiter.api.Test;

            /**
             * {@link A} の単体テスト。
             */
            class ATest {

                @Nested
                class f {

                    @Nested
                    class 正常系 {

                        /**
                         * 入力の件数を集計することを確かめる。
                         * 集計の誤りが仕様書の件数に直結する。
                         *
                         * @given 件数が 3 の入力
                         * @when f を実行する
                         * @then 3 を返す
                         */
                        @Test
                        void 件数が3の場合_3を返す() {
                        }
                    }
                }
            }
            """;

  /** 本体クラス A。第1階層 f を持つ。 */
  private static final String BODY =
      """
            package pkg;

            class A {
                void f() {
                }
            }
            """;

  /**
   * 本体とテストのソースと JaCoCo の CSV を作り、設定を返す。
   *
   * @param testSource テストクラスのソース
   * @return 実行の設定
   * @throws IOException 書き出しに失敗した場合
   */
  private UtSpecRunner.Config project(String testSource) throws IOException {
    write("src/main/java/pkg/A.java", BODY);
    write("src/test/java/pkg/ATest.java", testSource);
    write("target/site/jacoco/jacoco.csv", CSV_HEADER + "\ng,pkg,A,0,5,0,2\n");
    Files.createDirectories(tempDir.resolve("target/surefire-reports"));
    return config();
  }

  /**
   * 現在のプロジェクトの設定を返す。
   *
   * @return 実行の設定
   */
  private UtSpecRunner.Config config() {
    return new UtSpecRunner.Config(
        tempDir,
        tempDir.resolve("src/main/java"),
        tempDir.resolve("src/test/java"),
        tempDir.resolve("target/site/jacoco/jacoco.csv"),
        tempDir.resolve("target/surefire-reports"),
        tempDir.resolve("target/nectar-ut-spec"));
  }

  /**
   * リポジトリの直下にファイルを書き出す。
   *
   * @param relative 相対パス
   * @param content 内容
   * @throws IOException 書き出しに失敗した場合
   */
  private void write(String relative, String content) throws IOException {
    Path file = tempDir.resolve(relative);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content, StandardCharsets.UTF_8);
  }

  /**
   * Git の情報を、環境変数で固定したものとして返す。
   *
   * @return Git の情報
   */
  private GitContext git() {
    return new GitContext(Map.of("GITHUB_SHA", "abc123"), tempDir);
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 時計と Git の情報を渡して実行器を生成し、検査を実行できることを確かめる。 仕様書の日時を固定して出力できるようにする。
       *
       * @given 固定の時計と Git の情報で生成した実行器と、違反の無いプロジェクト
       * @when run を実行する
       * @then 違反の無い空の一覧を返す
       */
      @Test
      void 時計とGitが設定されている場合_違反の無い一覧を返す() throws IOException {
        project(VALID_TEST);

        assertThat(new UtSpecRunner(git(), CLOCK).run(config())).isEmpty();
      }
    }
  }

  @Nested
  class run {

    @Nested
    class 正常系 {

      /**
       * 違反の無いプロジェクトでは仕様書 HTML を出力することを確かめる。 検査に通ったリポジトリだけ仕様書を作る。
       *
       * @given 違反の無いテストクラスと、surefire の成功の結果
       * @when run を実行する
       * @then 違反の無い一覧を返し、index.html を出力する
       */
      @Test
      void 違反が無い場合_仕様書HTMLの内容を返す() throws IOException {
        project(VALID_TEST);
        write(
            "target/surefire-reports/TEST-pkg.ATest.xml",
            """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <testsuite name="pkg.ATest$f$正常系" tests="1">
                          <testcase name="件数が3の場合_3を返す()" classname="pkg.ATest$f$正常系" time="0.01"/>
                        </testsuite>
                        """);

        List<Violation> violations = new UtSpecRunner(git(), CLOCK).run(config());

        assertThat(violations).isEmpty();
        assertThat(Files.readString(tempDir.resolve("target/nectar-ut-spec/index.html")))
            .contains("件数が3の場合_3を返す")
            .contains("成功");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 違反があるプロジェクトでは仕様書を出力せず、違反の一覧を返すことを確かめる。 違反を直す前の仕様書を残さない。
       *
       * @given テスト名の形式が違うテストクラス
       * @when run を実行する
       * @then 違反を1件以上含む一覧を返し、仕様書を出力しない
       */
      @Test
      void 違反がある場合_違反の一覧を返す() throws IOException {
        project(VALID_TEST.replace("件数が3の場合_3を返す", "件数を確かめる"));

        List<Violation> violations = new UtSpecRunner(git(), CLOCK).run(config());

        assertThat(violations)
            .singleElement()
            .satisfies(
                violation -> {
                  assertThat(violation.rule()).isEqualTo(UtRule.UT08);
                  assertThat(violation.file()).isEqualTo("src/test/java/pkg/ATest.java");
                  assertThat(violation.line()).isPositive();
                  assertThat(violation.message()).contains("件数を確かめる");
                });
        assertThat(tempDir.resolve("target/nectar-ut-spec/index.html")).doesNotExist();
      }

      /**
       * jacoco.csv が無い場合は例外を投げることを確かめる。 1対1 の判定の基準が無いまま検査を進めない。
       *
       * @given jacoco.csv を作っていないプロジェクト
       * @when run を実行する
       * @then IllegalStateException を投げる
       */
      @Test
      void CSVが無い場合_IllegalStateExceptionを投げる() throws IOException {
        write("src/main/java/pkg/A.java", BODY);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> new UtSpecRunner(git(), CLOCK).run(config()))
            .isInstanceOf(IllegalStateException.class);
      }
    }
  }

  @Nested
  class checkOneToOne {

    @Nested
    class 正常系 {

      /**
       * 数えるコードを持つ本体に対応するテストがあれば違反を返さないことを確かめる。 1対1 が守られている状態を通す。
       *
       * @given 本体 pkg.A とテスト pkg.ATest が対応しているプロジェクト
       * @when checkOneToOne を実行する
       * @then 1対1 の違反を返さない
       */
      @Test
      void 対応するテストがある場合_違反の無い一覧を返す() throws IOException {
        project(VALID_TEST);
        UtSpecRunner runner = new UtSpecRunner(git(), CLOCK);
        MainSourceIndex main =
            new MainSourceScanner().scan(tempDir, tempDir.resolve("src/main/java"));
        List<TestClassReport> reports =
            new TestSourceScanner().scan(tempDir, tempDir.resolve("src/test/java"), main.bodies());
        JacocoSummary coverage = new JacocoCsvReader().read(config().jacocoCsv());

        assertThat(runner.checkOneToOne(coverage, main, reports, config().jacocoCsv())).isEmpty();
      }

      /**
       * 本体との対応を持たないテストクラスの結果は、1対1 の判定の対象外にすることを確かめる。 テスト名の形式で違反になったクラスを二重に報告しない。
       *
       * @given 本体の対応が null のテストクラスの結果だけを持つ状態
       * @when checkOneToOne を実行する
       * @then 1対1 の違反を返さない
       */
      @Test
      void 本体の対応が無いテストの場合_UT01の違反だけを返す() throws IOException {
        project(VALID_TEST);
        UtSpecRunner runner = new UtSpecRunner(git(), CLOCK);
        MainSourceIndex main =
            new MainSourceScanner().scan(tempDir, tempDir.resolve("src/main/java"));
        List<TestClassReport> reports =
            List.of(
                new TestClassReport(
                    "", null, "src/test/java/pkg/Sample.java", List.of(), List.of()));
        JacocoSummary coverage = new JacocoCsvReader().read(config().jacocoCsv());

        assertThat(runner.checkOneToOne(coverage, main, reports, config().jacocoCsv()))
            .extracting(Violation::rule)
            .containsExactly(UtRule.UT01);
      }

      /**
       * 除外された本体クラスはテストが無くても違反にしないことを確かめる。 生成コードを1対1 の対象から外す。
       *
       * @given 除外の付いた本体クラス pkg.A で、テストの無いプロジェクト
       * @when checkOneToOne を実行する
       * @then 違反を返さない
       */
      @Test
      void 本体が除外された場合_違反の無い一覧を返す() throws IOException {
        write(
            "src/main/java/pkg/A.java",
            "package pkg;\n@CoverageExcludedGenerated(reason = \"x\")\nclass A {}\n");
        write("target/site/jacoco/jacoco.csv", CSV_HEADER + "\n");
        MainSourceIndex main =
            new MainSourceScanner().scan(tempDir, tempDir.resolve("src/main/java"));
        JacocoSummary coverage = new JacocoCsvReader().read(config().jacocoCsv());

        assertThat(
                new UtSpecRunner(git(), CLOCK)
                    .checkOneToOne(coverage, main, List.of(), config().jacocoCsv()))
            .isEmpty();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 数えるコードを持つ本体にテストが無い場合は UT-01 を返すことを確かめる。 テストの無い本体を残さない。
       *
       * @given 本体 pkg.A が数えるコードを持ち、テストが無いプロジェクト
       * @when checkOneToOne を実行する
       * @then UT-01 の違反を1件返す
       */
      @Test
      void テストが無い場合_UT01の違反を返す() throws IOException {
        write("src/main/java/pkg/A.java", BODY);
        write("target/site/jacoco/jacoco.csv", CSV_HEADER + "\ng,pkg,A,0,5,0,2\n");
        MainSourceIndex main =
            new MainSourceScanner().scan(tempDir, tempDir.resolve("src/main/java"));
        JacocoSummary coverage = new JacocoCsvReader().read(config().jacocoCsv());

        List<Violation> violations =
            new UtSpecRunner(git(), CLOCK)
                .checkOneToOne(coverage, main, new ArrayList<>(), config().jacocoCsv());

        assertThat(violations)
            .singleElement()
            .satisfies(v -> assertThat(v.rule()).isEqualTo(UtRule.UT01));
      }

      /**
       * 本体が見つからない場合は jacoco.csv を違反の場所にすることを確かめる。 ソースが無くても違反を報告できるようにする。
       *
       * @given 本体のソースが無く、数えるコードだけが CSV にある状態
       * @when checkOneToOne を実行する
       * @then 違反の場所を jacoco.csv にした UT-01 の違反を返す
       */
      @Test
      void 本体のソースが無い場合_CSVを場所にした違反を返す() throws IOException {
        write("target/site/jacoco/jacoco.csv", CSV_HEADER + "\ng,pkg,Gone,0,5,0,2\n");
        JacocoSummary coverage = new JacocoCsvReader().read(config().jacocoCsv());
        MainSourceIndex main = new MainSourceIndex(Map.of(), List.of(), List.of());

        List<Violation> violations =
            new UtSpecRunner(git(), CLOCK)
                .checkOneToOne(coverage, main, List.of(), config().jacocoCsv());

        assertThat(violations)
            .singleElement()
            .satisfies(
                v -> {
                  assertThat(v.rule()).isEqualTo(UtRule.UT01);
                  assertThat(v.file()).endsWith("jacoco.csv");
                });
      }

      /**
       * 対応する本体の無いテストクラスは UT-02 を返すことを確かめる。 削除された本体のテストを残さない。
       *
       * @given 本体 pkg.A が無く、テスト pkg.ATest だけがあるプロジェクト
       * @when checkOneToOne を実行する
       * @then UT-02 の違反を1件返す
       */
      @Test
      void 本体が無い場合_UT02の違反を返す() throws IOException {
        write("target/site/jacoco/jacoco.csv", CSV_HEADER + "\n");
        write("src/test/java/pkg/ATest.java", VALID_TEST);
        MainSourceIndex main = new MainSourceIndex(Map.of(), List.of(), List.of());
        List<TestClassReport> reports =
            new TestSourceScanner().scan(tempDir, tempDir.resolve("src/test/java"), main.bodies());
        JacocoSummary coverage = new JacocoCsvReader().read(config().jacocoCsv());

        List<Violation> violations =
            new UtSpecRunner(git(), CLOCK)
                .checkOneToOne(coverage, main, reports, config().jacocoCsv());

        assertThat(violations).extracting(Violation::rule).containsExactly(UtRule.UT02);
      }
    }
  }
}
