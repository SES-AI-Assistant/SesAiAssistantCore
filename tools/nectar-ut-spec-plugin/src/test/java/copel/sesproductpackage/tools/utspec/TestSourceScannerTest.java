package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link TestSourceScanner} の単体テスト。 */
class TestSourceScannerTest {

  /** 一時ディレクトリ。リポジトリの見立てとして使う。 */
  @TempDir Path tempDir;

  /**
   * テストソースのディレクトリに Java ファイルを書き出す。
   *
   * @param relative src/test/java からの相対パス
   * @param content ファイルの内容
   * @throws IOException 書き出しに失敗した場合
   */
  private void writeTest(String relative, String content) throws IOException {
    Path file = tempDir.resolve("src/test/java").resolve(relative);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content, StandardCharsets.UTF_8);
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 既定の設定で TestSourceScanner を生成し、テストソースを検査できることを確かめる。 既定の生成で走査できないと、テスト全体の検査が止まる。
       *
       * @given 既定のコンストラクタで生成した TestSourceScanner と、構文エラーの無いテストファイル
       * @when scan を実行する
       * @then テストクラスの検査結果を1件返す
       */
      @Test
      void 既定の設定で生成された場合_検査結果を返す() throws IOException {
        writeTest("pkg/ATest.java", "package pkg;\nclass ATest {}\n");

        List<TestClassReport> reports =
            new TestSourceScanner().scan(tempDir, tempDir.resolve("src/test/java"), Map.of());

        assertThat(reports).hasSize(1);
      }
    }
  }

  @Nested
  class scan {

    @Nested
    class 正常系 {

      /**
       * 構文エラーでないテストファイルは、テストクラスとして検査することを確かめる。 検査の対象を1ファイルずつ漏れなく扱う。
       *
       * @given テストファイル ATest.java が1つあるテストソース
       * @when scan を実行する
       * @then ATest のテストクラスの検査結果を1件返す
       */
      @Test
      void 有効なファイルがある場合_テストクラスの検査結果を返す() throws IOException {
        writeTest("pkg/ATest.java", "package pkg;\nclass ATest {}\n");

        List<TestClassReport> reports =
            new TestSourceScanner().scan(tempDir, tempDir.resolve("src/test/java"), Map.of());

        assertThat(reports)
            .singleElement()
            .satisfies(
                report -> {
                  assertThat(report.classFqn()).isEqualTo("pkg.ATest");
                  assertThat(report.relativePath()).isEqualTo("src/test/java/pkg/ATest.java");
                });
      }
    }

    @Nested
    class 異常系 {

      /**
       * 構文エラーのテストファイルは UT-00 の違反として報告することを確かめる。 解析できないテストを黙って通さない。
       *
       * @given 閉じ括弧の無いテストファイル
       * @when scan を実行する
       * @then UT-00 の違反を含み、テストケースを持たない検査結果を返す
       */
      @Test
      void ファイルが構文エラーの場合_UT00の違反を返す() throws IOException {
        writeTest("pkg/ATest.java", "package pkg;\nclass ATest {\n");

        List<TestClassReport> reports =
            new TestSourceScanner().scan(tempDir, tempDir.resolve("src/test/java"), Map.of());

        assertThat(reports)
            .singleElement()
            .satisfies(
                report -> {
                  assertThat(report.violations())
                      .singleElement()
                      .satisfies(v -> assertThat(v.rule()).isEqualTo(UtRule.UT00));
                  assertThat(report.cases()).isEmpty();
                });
      }
    }

    @Nested
    class 境界値 {

      /**
       * テストディレクトリが無い場合は空の一覧を返すことを確かめる。 テストの無いリポジトリでも検査を空として進める。
       *
       * @given 存在しないテストディレクトリ
       * @when scan を実行する
       * @then 空の一覧を返す
       */
      @Test
      void ディレクトリが無い場合_空の一覧を返す() throws IOException {
        assertThat(new TestSourceScanner().scan(tempDir, tempDir.resolve("none"), Map.of()))
            .isEmpty();
      }
    }
  }
}
