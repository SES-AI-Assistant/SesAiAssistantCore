package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link MainSourceScanner} の単体テスト。 */
class MainSourceScannerTest {

  /** 一時ディレクトリ。リポジトリの見立てとして使う。 */
  @TempDir Path tempDir;

  /**
   * 本体ソースのディレクトリに Java ファイルを書き出す。
   *
   * @param relative src/main/java からの相対パス
   * @param content ファイルの内容
   * @throws IOException 書き出しに失敗した場合
   */
  private void writeMain(String relative, String content) throws IOException {
    Path file = tempDir.resolve("src/main/java").resolve(relative);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content, StandardCharsets.UTF_8);
  }

  /**
   * ソースからクラス A の宣言を取り出す。
   *
   * @param source Java のソース
   * @return クラス A の宣言
   */
  private static ClassOrInterfaceDeclaration classOf(String source) {
    return StaticJavaParser.parse(source).getClassByName("A").orElseThrow();
  }

  @Nested
  class scan {

    @Nested
    class 正常系 {

      /**
       * 除外されたクラスと、通常のクラスを本体の索引に入れることを確かめる。 除外の一覧と本体の一覧を同時に作る。
       *
       * @given 除外アノテーションの付いたクラス A と、通常のクラス B がある本体ソース
       * @when scan を実行する
       * @then 両方を本体の索引に入れ、A の除外理由を一覧に入れて返す
       */
      @Test
      void 除外と通常のクラスがある場合_両方を索引に入れて除外を登録する() throws IOException {
        writeMain(
            "pkg/A.java",
            "package pkg;\n@CoverageExcludedGenerated(reason = \"生成\")\n"
                + "class A { void f() {} }\n");
        writeMain("pkg/B.java", "package pkg;\nclass B { void g() {} }\n");

        MainSourceIndex index =
            new MainSourceScanner().scan(tempDir, tempDir.resolve("src/main/java"));

        assertThat(index.bodies().keySet()).containsExactlyInAnyOrder("pkg.A", "pkg.B");
        assertThat(index.bodies().get("pkg.A").excluded()).isTrue();
        assertThat(index.bodies().get("pkg.B").excluded()).isFalse();
        assertThat(index.exclusions())
            .extracting(MainSourceIndex.ExclusionEntry::reason)
            .containsExactly("生成");
      }

      /**
       * メソッドに付いた除外を、クラス名と メソッド名をつないだ対象として一覧に入れることを確かめる。 除外の対象を仕様書で特定できるようにする。
       *
       * @given メソッド f に除外アノテーションが付いたクラス A
       * @when scan を実行する
       * @then "pkg.A#f" を対象とする除外を一覧に入れて返す
       */
      @Test
      void メソッドに除外がある場合_対象の名前で除外を登録する() throws IOException {
        writeMain(
            "pkg/A.java",
            "package pkg;\nclass A {\n"
                + "@CoverageExcludedGenerated(reason = \"理由\") void f() {}\n}\n");

        MainSourceIndex index =
            new MainSourceScanner().scan(tempDir, tempDir.resolve("src/main/java"));

        assertThat(index.exclusions())
            .extracting(MainSourceIndex.ExclusionEntry::target)
            .containsExactly("pkg.A#f");
      }
    }

    @Nested
    class 異常系 {
      /**
       * 本体ソースが構文エラーの場合は UT-00 の違反を返すことを確かめる。 解析できない本体を黙って対象外にしない。
       *
       * @given 閉じ括弧の無い本体ソース
       * @when scan を実行する
       * @then UT-00 の違反を1件返す
       */
      @Test
      void ファイルが構文エラーの場合_UT00の違反を返す() throws IOException {
        writeMain("pkg/Broken.java", "package pkg;\nclass Broken {\n");

        MainSourceIndex index =
            new MainSourceScanner().scan(tempDir, tempDir.resolve("src/main/java"));

        assertThat(index.violations())
            .singleElement()
            .satisfies(v -> assertThat(v.rule()).isEqualTo(UtRule.UT00));
      }

      /**
       * 単一値の書き方で理由を書いた場合は、reason 属性ではないため UT-18 の違反を返すことを確かめる。 必ず reason 属性で書かせる。
       *
       * @given 除外を単一値の書き方で付けたクラス A
       * @when scan を実行する
       * @then UT-18 の違反を1件返す
       */
      @Test
      void 理由が単一値で書かれた場合_UT18の違反を返す() throws IOException {
        writeMain("pkg/A.java", "package pkg;\n@CoverageExcludedGenerated(\"x\")\nclass A {}\n");

        MainSourceIndex index =
            new MainSourceScanner().scan(tempDir, tempDir.resolve("src/main/java"));

        assertThat(index.violations())
            .singleElement()
            .satisfies(v -> assertThat(v.rule()).isEqualTo(UtRule.UT18));
      }

      /**
       * reason が空白だけの文字列の場合も、理由が無いとみなし UT-18 の違反を返すことを確かめる。 空白だけの理由で除外を通さない。
       *
       * @given reason が空白だけの除外を付けたクラス A
       * @when scan を実行する
       * @then UT-18 の違反を1件返す
       */
      @Test
      void 理由が空白だけの場合_UT18の違反を返す() throws IOException {
        writeMain(
            "pkg/A.java",
            "package pkg;\n@CoverageExcludedGenerated(reason = \"  \")\nclass A {}\n");

        MainSourceIndex index =
            new MainSourceScanner().scan(tempDir, tempDir.resolve("src/main/java"));

        assertThat(index.violations())
            .singleElement()
            .satisfies(v -> assertThat(v.rule()).isEqualTo(UtRule.UT18));
      }

      /**
       * 除外アノテーションに reason を書いていない場合に UT-18 の違反を返すことを確かめる。 除外の理由を仕様書の除外一覧に必ず残す。
       *
       * @given reason を持たない除外を付けたクラス A
       * @when scan を実行する
       * @then UT-18 の違反を1件返す
       */
      @Test
      void 除外の理由が無い場合_UT18の違反を返す() throws IOException {
        writeMain("pkg/A.java", "package pkg;\n@CoverageExcludedGenerated\nclass A {}\n");

        MainSourceIndex index =
            new MainSourceScanner().scan(tempDir, tempDir.resolve("src/main/java"));

        assertThat(index.violations())
            .singleElement()
            .satisfies(v -> assertThat(v.rule()).isEqualTo(UtRule.UT18));
      }

      /**
       * 構文エラーのファイルは本体の索引に入れないことを確かめる。 解析できないファイルを、型の一覧に不正な情報として混ぜない。
       *
       * @given 閉じ括弧の無い Java ファイルだけがある本体ソース
       * @when scan を実行する
       * @then 本体の索引に何も入れずに返す
       */
      @Test
      void 本体ソースが構文エラーの場合_そのファイルを除いた索引を返す() throws IOException {
        writeMain("pkg/Broken.java", "package pkg;\nclass Broken {\n");

        MainSourceIndex index =
            new MainSourceScanner().scan(tempDir, tempDir.resolve("src/main/java"));

        assertThat(index.bodies()).isEmpty();
      }
    }
  }

  @Nested
  class addMemberExclusions {

    @Nested
    class 正常系 {
      /**
       * メソッドの除外に reason が無い場合に UT-18 の違反を登録することを確かめる。 メソッドの除外にも理由を必ず書かせる。
       *
       * @given reason を持たない除外を付けたメソッド f を持つクラス A
       * @when addMemberExclusions を実行する
       * @then UT-18 の違反を1件登録する
       */
      @Test
      void メンバーの理由が無い場合_UT18の違反を登録する() {
        TypeDeclaration<?> type = classOf("class A {\n@CoverageExcludedGenerated void f() {}\n}\n");
        List<Violation> violations = new ArrayList<>();

        new MainSourceScanner()
            .addMemberExclusions("pkg.A", type, new ArrayList<>(), "A.java", violations);

        assertThat(violations)
            .singleElement()
            .satisfies(v -> assertThat(v.rule()).isEqualTo(UtRule.UT18));
      }

      /**
       * メソッドとコンストラクタの除外を、対象の名前で一覧に追加することを確かめる。 コンストラクタの除外を"コンストラクタ"で特定できるようにする。
       *
       * @given メソッド f とコンストラクタに除外が付いたクラス A
       * @when addMemberExclusions を実行する
       * @then "pkg.A#f" と "pkg.A#コンストラクタ" の2件を一覧に登録する
       */
      @Test
      void メソッドとコンストラクタに除外がある場合_除外の対象を登録する() {
        TypeDeclaration<?> type =
            classOf(
                "class A {\n"
                    + "@CoverageExcludedGenerated(reason = \"x\") void f() {}\n"
                    + "@CoverageExcludedGenerated(reason = \"y\") A() {}\n"
                    + "int x;\nvoid g() {}\n}\n");
        List<MainSourceIndex.ExclusionEntry> entries = new ArrayList<>();

        new MainSourceScanner()
            .addMemberExclusions("pkg.A", type, entries, "A.java", new ArrayList<>());

        assertThat(entries)
            .extracting(MainSourceIndex.ExclusionEntry::target)
            .containsExactly("pkg.A#f", "pkg.A#コンストラクタ");
      }
    }
  }

  @Nested
  class exclusionReason {

    @Nested
    class 正常系 {

      /**
       * 除外アノテーションの理由を取り出すことを確かめる。 仕様書の除外一覧に理由を出す。
       *
       * @given reason が "生成コード" の除外アノテーションが付いたクラス A
       * @when exclusionReason を実行する
       * @then "生成コード" を含む値を返す
       */
      @Test
      void 理由が付いている場合_理由を含む値を返す() {
        ClassOrInterfaceDeclaration decl =
            classOf("@CoverageExcludedGenerated(reason = \"生成コード\") class A {}");

        assertThat(new MainSourceScanner().exclusionReason(decl)).contains("生成コード");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 除外アノテーションが無い宣言は除外ではないことを確かめる。 通常のクラスを除外として扱わない。
       *
       * @given 除外アノテーションを持たないクラス A
       * @when exclusionReason を実行する
       * @then 空を返す
       */
      @Test
      void 除外アノテーションが無い場合_空を返す() {
        assertThat(new MainSourceScanner().exclusionReason(classOf("class A {}"))).isEmpty();
      }

      /**
       * 単純名の除外アノテーションで理由が無い場合は空文字を返すことを確かめる。 除外の理由が無くても検出する。
       *
       * @given 理由を書いていない除外アノテーションを付けたクラス A
       * @when exclusionReason を実行する
       * @then 空文字を含む値を返す
       */
      @Test
      void 理由を書いていない場合_空文字を含む値を返す() {
        ClassOrInterfaceDeclaration decl = classOf("@CoverageExcludedGenerated class A {}");

        assertThat(new MainSourceScanner().exclusionReason(decl)).contains("");
      }
    }
  }

  @Nested
  class reasonOf {

    @Nested
    class 正常系 {

      /**
       * reason 属性の文字列を取り出すことを確かめる。 除外の理由を仕様書に出す。
       *
       * @given reason が "外部連携" の通常形式のアノテーション
       * @when reasonOf を実行する
       * @then "外部連携" を返す
       */
      @Test
      void 属性が文字列の場合_文字列を返す() {
        AnnotationExpr annotation =
            classOf("@CoverageExcludedGenerated(reason = \"外部連携\") class A {}")
                .getAnnotations()
                .get(0);

        assertThat(new MainSourceScanner().reasonOf(annotation)).isEqualTo("外部連携");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 単一値の形式のアノテーションは理由を空文字にすることを確かめる。 reason 属性が無い書き方でも検査を止めない。
       *
       * @given 値を書かない単純な形式のアノテーション
       * @when reasonOf を実行する
       * @then 空文字を返す
       */
      @Test
      void 除外が単純な形式の場合_空文字を返す() {
        AnnotationExpr annotation =
            classOf("@CoverageExcludedGenerated class A {}").getAnnotations().get(0);

        assertThat(new MainSourceScanner().reasonOf(annotation)).isEmpty();
      }

      /**
       * reason 属性が文字列以外の値の場合は空文字にすることを確かめる。 定数を指定された場合も、検査を止めずに理由を空として扱う。
       *
       * @given reason に定数名を書いたアノテーション
       * @when reasonOf を実行する
       * @then 空文字を返す
       */
      @Test
      void 属性が定数の場合_空文字を返す() {
        AnnotationExpr annotation =
            classOf("@CoverageExcludedGenerated(reason = REASON) class A {}")
                .getAnnotations()
                .get(0);

        assertThat(new MainSourceScanner().reasonOf(annotation)).isEmpty();
      }

      /**
       * reason 属性が無い通常形式のアノテーションは空文字にすることを確かめる。 属性の名前が違う場合にも理由を空として扱う。
       *
       * @given 属性 note だけを持つ通常形式のアノテーション
       * @when reasonOf を実行する
       * @then 空文字を返す
       */
      @Test
      void 属性がreasonでない場合_空文字を返す() {
        AnnotationExpr annotation =
            classOf("@CoverageExcludedGenerated(note = \"x\") class A {}").getAnnotations().get(0);

        assertThat(new MainSourceScanner().reasonOf(annotation)).isEmpty();
      }
    }
  }

  @Nested
  class lineOf {

    @Nested
    class 正常系 {

      /**
       * 位置の分かるノードは開始行を返すことを確かめる。 違反の報告に行番号を付ける。
       *
       * @given 3行目から始まるクラス A
       * @when lineOf を実行する
       * @then 3 を返す
       */
      @Test
      void 位置が分かる場合_開始行を返す() {
        Node node = classOf("\n\nclass A {}");

        assertThat(new MainSourceScanner().lineOf(node)).isEqualTo(3);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 位置の無いノードは行番号0を返すことを確かめる。 行番号が分からない場合にも例外で止めない。
       *
       * @given 位置情報を持たない新しいクラス宣言
       * @when lineOf を実行する
       * @then 0 を返す
       */
      @Test
      void 位置が無い場合_0を返す() {
        assertThat(new MainSourceScanner().lineOf(new ClassOrInterfaceDeclaration())).isZero();
      }
    }
  }
}
