package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link JavaSourceReader} の単体テスト。 */
class JavaSourceReaderTest {

  /** 一時ディレクトリ。Java ファイルを書き出すために使う。 */
  @TempDir Path tempDir;

  /**
   * 文字列を読み込んで、型宣言を1つ解析する。
   *
   * @param source Java のソース
   * @return 先頭のクラス宣言
   */
  private static ClassOrInterfaceDeclaration classOf(String source) {
    return StaticJavaParser.parse(source).getClassByName("A").orElseThrow();
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 既定の設定で JavaSourceReader を生成して、ファイルを解析できることを確かめる。 解析の設定が無いと、すべてのテストソースを読めなくなる。
       *
       * @given 既定のコンストラクタで生成した JavaSourceReader
       * @when 有効な Java ファイルを parse する
       * @then 解析結果を返す
       */
      @Test
      void 既定の設定で生成された場合_有効なファイルの解析結果を返す() throws IOException {
        Path file = tempDir.resolve("A.java");
        Files.writeString(file, "class A {}", StandardCharsets.UTF_8);

        assertThat(new JavaSourceReader().parse(file)).isPresent();
      }
    }
  }

  @Nested
  class parse {

    @Nested
    class 正常系 {

      /**
       * 構文が規定どおりの Java ファイルから Compilation Unit を得られることを確かめる。 検査の対象となるファイルを読み込む。
       *
       * @given クラス A を1つ含む有効な Java ファイル
       * @when parse を実行する
       * @then Compilation Unit を返す
       */
      @Test
      void ファイルが有効な場合_Compilation_Unitを返す() throws IOException {
        Path file = tempDir.resolve("A.java");
        Files.writeString(file, "class A {}", StandardCharsets.UTF_8);

        assertThat(new JavaSourceReader().parse(file).orElseThrow().getTypes()).hasSize(1);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 構文エラーのファイルは空を返すことを確かめる。 解析できないファイルを検査結果で違反として報告する。
       *
       * @given 閉じ括弧の無い Java ファイル
       * @when parse を実行する
       * @then 空の結果を返す
       */
      @Test
      void ファイルが構文エラーの場合_空を返す() throws IOException {
        Path file = tempDir.resolve("A.java");
        Files.writeString(file, "class A {", StandardCharsets.UTF_8);

        assertThat(new JavaSourceReader().parse(file)).isEmpty();
      }

      /**
       * 存在しないファイルを読むと入出力の例外を投げることを確かめる。 読めないファイルを黙って無視しない。
       *
       * @given 存在しないパス
       * @when parse を実行する
       * @then UncheckedIOException を投げる
       */
      @Test
      void ファイルが存在しない場合_UncheckedIOExceptionを投げる() {
        Path missing = tempDir.resolve("None.java");

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> new JavaSourceReader().parse(missing))
            .isInstanceOf(java.io.UncheckedIOException.class);
      }
    }
  }

  @Nested
  class javaFiles {

    @Nested
    class 正常系 {

      /**
       * ディレクトリ内の Java ファイルだけをパスの順に列挙することを確かめる。 検査の対象の順番を毎回同じにし、違反の並びを安定させる。
       *
       * @given Java ファイル B と A、Java 以外の txt ファイルがあるディレクトリ
       * @when javaFiles を実行する
       * @then A.java と B.java の順のリストを返す
       */
      @Test
      void 複数のファイルがある場合_Javaファイルの一覧を返す() throws IOException {
        Files.writeString(tempDir.resolve("B.java"), "class B {}", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("A.java"), "class A {}", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("memo.txt"), "memo", StandardCharsets.UTF_8);

        List<Path> files = new JavaSourceReader().javaFiles(tempDir);

        assertThat(files)
            .extracting(path -> path.getFileName().toString())
            .containsExactly("A.java", "B.java");
      }
    }

    @Nested
    class 境界値 {

      /**
       * ディレクトリが存在しない場合は空のリストを返すことを確かめる。 テストソースが無いリポジトリでも、検査を空として進める。
       *
       * @given 存在しないディレクトリのパス
       * @when javaFiles を実行する
       * @then 空のリストを返す
       */
      @Test
      void ディレクトリが無い場合_空のリストを返す() throws IOException {
        assertThat(new JavaSourceReader().javaFiles(tempDir.resolve("none"))).isEmpty();
      }
    }
  }

  @Nested
  class relativePath {

    @Nested
    class 正常系 {

      /**
       * 基準からの相対パスを / 区切りで返すことを確かめる。 違反の報告と GitHub の行リンクで同じ形式を使う。
       *
       * @given 基準が "/repo"、対象が "/repo/src/A.java"
       * @when relativePath を実行する
       * @then "src/A.java" を返す
       */
      @Test
      void 基準の下にファイルがある場合_スラッシュ区切りの相対パスを返す() {
        String relative =
            new JavaSourceReader().relativePath(Path.of("repo"), Path.of("repo", "src", "A.java"));

        assertThat(relative).isEqualTo("src/A.java");
      }
    }
  }

  @Nested
  class findAnnotation {

    @Nested
    class 正常系 {

      /**
       * 完全修飾名で書かれたアノテーションを単純名で見つけることを確かめる。 import の書き方に関係なく階層の印を見つける。
       *
       * @given 完全修飾名 org.junit.jupiter.api.Nested が付いたクラス A
       * @when 単純名 Nested で findAnnotation を実行する
       * @then アノテーションを返す
       */
      @Test
      void 完全修飾名で付いている場合_単純名で見つけたアノテーションを返す() {
        ClassOrInterfaceDeclaration decl = classOf("@org.junit.jupiter.api.Nested class A {}");

        assertThat(new JavaSourceReader().findAnnotation(decl, "Nested")).isPresent();
      }
    }

    @Nested
    class 境界値 {

      /**
       * アノテーションが無い宣言では空を返すことを確かめる。 階層の印が無いクラスを階層として扱わない。
       *
       * @given アノテーションを持たないクラス A
       * @when 単純名 Nested で findAnnotation を実行する
       * @then 空を返す
       */
      @Test
      void アノテーションが無い場合_空を返す() {
        ClassOrInterfaceDeclaration decl = classOf("class A {}");

        assertThat(new JavaSourceReader().findAnnotation(decl, "Nested")).isEmpty();
      }
    }
  }

  @Nested
  class simpleNameOf {

    @Nested
    class 正常系 {

      /**
       * 完全修飾名のアノテーションから末尾の単純名を取り出すことを確かめる。 名前の比較を単純名で統一する。
       *
       * @given 完全修飾名 org.junit.jupiter.api.Test のアノテーション
       * @when simpleNameOf を実行する
       * @then "Test" を返す
       */
      @Test
      void アノテーションが完全修飾名の場合_末尾の単純名を返す() {
        ClassOrInterfaceDeclaration decl = classOf("@org.junit.jupiter.api.Test class A {}");
        var annotation = decl.getAnnotations().get(0);

        assertThat(new JavaSourceReader().simpleNameOf(annotation)).isEqualTo("Test");
      }
    }
  }
}
