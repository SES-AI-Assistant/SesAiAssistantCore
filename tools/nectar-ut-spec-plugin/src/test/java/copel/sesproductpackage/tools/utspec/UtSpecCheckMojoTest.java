package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link UtSpecCheckMojo} の単体テスト。 */
class UtSpecCheckMojoTest {

  /** 一時ディレクトリ。リポジトリの見立てとして使う。 */
  @TempDir Path tempDir;

  /** JaCoCo の CSV のヘッダー。 */
  private static final String CSV_HEADER =
      "GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED";

  /**
   * 違反の無い最小のプロジェクトを作る。
   *
   * @param testName テストメソッドの名前
   * @throws IOException 書き出しに失敗した場合
   */
  private void writeValidProject(String testName) throws IOException {
    write("src/main/java/pkg/A.java", "package pkg;\nclass A {\n    void f() {\n    }\n}\n");
    write(
        "src/test/java/pkg/ATest.java",
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
                            void %s() {
                            }
                        }
                    }
                }
                """
            .formatted(testName));
    write("target/site/jacoco/jacoco.csv", CSV_HEADER + "\ng,pkg,A,0,5,0,2\n");
    Files.createDirectories(tempDir.resolve("target/surefire-reports"));
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
   * 基準のパスを指定した Mojo を作る。
   *
   * @return 設定済みの Mojo
   */
  private UtSpecCheckMojo mojo() {
    UtSpecCheckMojo mojo = new UtSpecCheckMojo();
    mojo.skip = false;
    mojo.packaging = "jar";
    mojo.failOnViolation = true;
    mojo.basedir = tempDir.toFile();
    mojo.sourceDirectory = tempDir.resolve("src/main/java").toFile();
    mojo.testSourceDirectory = tempDir.resolve("src/test/java").toFile();
    mojo.jacocoCsv = tempDir.resolve("target/site/jacoco/jacoco.csv").toFile();
    mojo.surefireReportsDirectory = tempDir.resolve("target/surefire-reports").toFile();
    mojo.outputDirectory = tempDir.resolve("target/nectar-ut-spec").toFile();
    return mojo;
  }

  @Nested
  class execute {

    @Nested
    class 正常系 {

      /**
       * 違反の無いプロジェクトでは例外を投げず、仕様書 HTML を出力することを確かめる。 検査に通ったリポジトリの仕様書を作る。
       *
       * @given 違反の無い最小のプロジェクト
       * @when execute を実行する
       * @then 例外を投げず、index.html を出力する
       */
      @Test
      void 違反が無い場合_仕様書HTMLを返す() throws IOException {
        writeValidProject("件数が3の場合_3を返す");

        assertThatCode(() -> mojo().execute()).doesNotThrowAnyException();
        assertThat(tempDir.resolve("target/nectar-ut-spec/index.html")).exists();
      }

      /**
       * skip を true にすると検査を行わず、例外も仕様書も出さないことを確かめる。 一時的に検査を止める手段を提供する。
       *
       * @given skip を true にし、jacoco.csv が無い状態
       * @when execute を実行する
       * @then 例外を投げず、仕様書を出力しない
       */
      @Test
      void skipがtrueの場合_何もしない() throws IOException {
        UtSpecCheckMojo mojo = mojo();
        mojo.skip = true;

        assertThatCode(mojo::execute).doesNotThrowAnyException();
        assertThat(tempDir.resolve("target/nectar-ut-spec/index.html")).doesNotExist();
      }

      /**
       * packaging が pom のプロジェクトでは検査を行わず、例外も仕様書も出さないことを確かめる。 親 pom 自身の build で検査を失敗させない。
       *
       * @given packaging を pom にし、jacoco.csv が無い状態
       * @when execute を実行する
       * @then 例外を投げず、仕様書を出力しない
       */
      @Test
      void packagingがpomの場合_何もしない() throws IOException {
        UtSpecCheckMojo mojo = mojo();
        mojo.packaging = "pom";

        assertThatCode(mojo::execute).doesNotThrowAnyException();
        assertThat(tempDir.resolve("target/nectar-ut-spec/index.html")).doesNotExist();
      }
    }

    @Nested
    class 異常系 {
      /**
       * 本体ソースを読み込めない場合は、原因を示す MojoExecutionException を投げることを確かめる。 読み取りの失敗を内部の例外のまま見せない。
       *
       * @given 読み取り権限を外した本体ソース
       * @when execute を実行する
       * @then MojoExecutionException を投げる
       */
      @Test
      void 読めないソースがある場合_MojoExecutionExceptionを投げる() throws IOException {
        writeValidProject("件数が3の場合_3を返す");
        Path source = tempDir.resolve("src/main/java/pkg/A.java");
        Assumptions.assumeTrue(
            FileSystems.getDefault().supportedFileAttributeViews().contains("posix"),
            "POSIX の権限を扱えない環境ではスキップする");
        Files.setPosixFilePermissions(source, Set.of());
        Assumptions.assumeFalse(Files.isReadable(source), "読み取り権限を外せない環境（root など）ではスキップする");
        try {
          assertThatThrownBy(() -> mojo().execute()).isInstanceOf(MojoExecutionException.class);
        } finally {
          Files.setPosixFilePermissions(source, PosixFilePermissions.fromString("rw-r--r--"));
        }
      }

      /**
       * 違反があり failOnViolation が true の場合は、ビルドを失敗させる例外を投げることを確かめる。 違反を見逃したままビルドを通さない。
       *
       * @given テスト名の形式が違うプロジェクト
       * @when execute を実行する
       * @then MojoFailureException を投げる
       */
      @Test
      void 違反があり失敗にする場合_MojoFailureExceptionを投げる() throws IOException {
        writeValidProject("件数を確かめる");

        assertThatThrownBy(() -> mojo().execute())
            .isInstanceOf(MojoFailureException.class)
            .hasMessageContaining("UT 仕様の違反が");
      }

      /**
       * 違反があっても failOnViolation が false なら、例外を投げずに続けることを確かめる。 移行の途中では警告だけにできるようにする。
       *
       * @given テスト名の形式が違い、failOnViolation が false のプロジェクト
       * @when execute を実行する
       * @then 例外を投げず、仕様書を出力しない
       */
      @Test
      void 違反があり警告にする場合_何もしない() throws IOException {
        writeValidProject("件数を確かめる");
        UtSpecCheckMojo mojo = mojo();
        mojo.failOnViolation = false;

        assertThatCode(mojo::execute).doesNotThrowAnyException();
        assertThat(tempDir.resolve("target/nectar-ut-spec/index.html")).doesNotExist();
      }

      /**
       * jacoco.csv が無い場合は実行を失敗させる例外を投げることを確かめる。 集計が無いまま検査を進めない。
       *
       * @given jacoco.csv を作っていないプロジェクト
       * @when execute を実行する
       * @then MojoExecutionException を投げる
       */
      @Test
      void CSVが無い場合_MojoExecutionExceptionを投げる() throws IOException {
        write("src/main/java/pkg/A.java", "package pkg;\nclass A {}\n");

        assertThatThrownBy(() -> mojo().execute()).isInstanceOf(MojoExecutionException.class);
      }
    }
  }
}
