package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link TestMethodAnalyzer} の単体テスト。 */
class TestMethodAnalyzerTest {

  /** 書式が規定どおりのテストメソッドのソース。 */
  private static final String VALID_METHOD =
      """
            class A {
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
            """;

  /**
   * ソースのクラス A に書かれた、最初のメソッドを取り出す。
   *
   * @param source Java のソース
   * @return メソッド
   */
  private static MethodDeclaration methodOf(String source) {
    return StaticJavaParser.parse(source).getClassByName("A").orElseThrow().getMethods().get(0);
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 既定の設定で TestMethodAnalyzer を生成し、書式が規定どおりのテストを解析できることを確かめる。
       * 既定の生成で検査が動かないと、どのテストも検査されないまま通ってしまう。
       *
       * @given 既定のコンストラクタで生成した TestMethodAnalyzer と、書式が規定どおりのテストメソッド
       * @when analyze を実行する
       * @then 違反の無いテストケースを1件返す
       */
      @Test
      void 既定の設定で生成された場合_違反の無いテストケースを返す() {
        TestMethodAnalyzer.MethodAnalysis analysis =
            new TestMethodAnalyzer()
                .analyze(methodOf(VALID_METHOD), "A.java", "pkg.ATest", "f", "正常系");

        assertThat(analysis.violations()).isEmpty();
        assertThat(analysis.cases()).hasSize(1);
      }
    }
  }

  @Nested
  class analyze {

    @Nested
    class 正常系 {

      /**
       * 書式が規定どおりのテストメソッドから、仕様書に出す項目を取り出すことを確かめる。 仕様書の何を確かめるか、理由、前提、操作、期待結果の列に出す。
       *
       * @given 書式が規定どおりのテストメソッド
       * @when analyze を実行する
       * @then 違反を返さず、本文とタグの内容を持つテストケースを1件返す
       */
      @Test
      void 書式が規定どおりの場合_項目を持つテストケースを返す() {
        TestMethodAnalyzer.MethodAnalysis analysis =
            new TestMethodAnalyzer()
                .analyze(methodOf(VALID_METHOD), "A.java", "pkg.ATest", "f", "正常系");

        assertThat(analysis.violations()).isEmpty();
        assertThat(analysis.cases())
            .singleElement()
            .satisfies(
                testCase -> {
                  assertThat(testCase.what()).isEqualTo("入力の件数を集計することを確かめる。");
                  assertThat(testCase.given()).isEqualTo("件数が 3 の入力");
                  assertThat(testCase.then()).isEqualTo("3 を返す");
                  assertThat(testCase.line()).isPositive();
                });
      }

      /**
       * パラメータ付きのテストは、組の数だけテストケースを作ることを確かめる。 組ごとに仕様書の行を分ける。
       *
       * @given CsvSource で値を2件持つパラメータ付きテストメソッド
       * @when analyze を実行する
       * @then テストケースを2件返す
       */
      @Test
      void パラメータが2組の場合_テストケースを返す() {
        String source =
            """
                        class A {
                            /**
                             * 入力の件数を集計することを確かめる。
                             * 集計の誤りが仕様書の件数に直結する。
                             *
                             * @given 件数の入力
                             * @when f を実行する
                             * @then 件数を返す
                             */
                            @ParameterizedTest
                            @CsvSource({"1", "2"})
                            void 件数がNの場合_件数を返す(int n) {
                            }
                        }
                        """;

        TestMethodAnalyzer.MethodAnalysis analysis =
            new TestMethodAnalyzer().analyze(methodOf(source), "A.java", "pkg.ATest", "f", "正常系");

        assertThat(analysis.cases()).hasSize(2);
        assertThat(analysis.cases().get(1).rowIndex()).isEqualTo(1);
      }
    }

    @Nested
    class 異常系 {

      /**
       * JavaDoc の無いテストメソッドは UT-10 の違反を返すことを確かめる。 仕様書に説明の無いテストを残さない。
       *
       * @given JavaDoc が無く、名前が条件と期待結果の形式のテストメソッド
       * @when analyze を実行する
       * @then UT-10 の違反を1件含む結果を返す
       */
      @Test
      void JavaDocが書かれていない場合_UT10の違反を返す() {
        String source = "class A { @Test void 件数が3の場合_3を返す() {} }";

        TestMethodAnalyzer.MethodAnalysis analysis =
            new TestMethodAnalyzer().analyze(methodOf(source), "A.java", "pkg.ATest", "f", "正常系");

        assertThat(analysis.violations()).extracting(Violation::rule).containsExactly(UtRule.UT10);
      }

      /**
       * テスト名の形式が違う場合は UT-08 の違反を返すことを確かめる。 テスト名の書き方を揃える。
       *
       * @given テスト名に区切りの語が無いテストメソッド
       * @when analyze を実行する
       * @then UT-08 の違反を含む結果を返す
       */
      @Test
      void テスト名の形式が違う場合_UT08の違反を返す() {
        String source = VALID_METHOD.replace("件数が3の場合_3を返す", "件数を確かめる");

        TestMethodAnalyzer.MethodAnalysis analysis =
            new TestMethodAnalyzer().analyze(methodOf(source), "A.java", "pkg.ATest", "f", "正常系");

        assertThat(analysis.violations()).extracting(Violation::rule).contains(UtRule.UT08);
      }

      /**
       * DisplayName を使ったテストは UT-17 の違反を返すことを確かめる。 テストのメソッド名を日本語で書く規則を守らせる。
       *
       * @given @DisplayName を付けたテストメソッド
       * @when analyze を実行する
       * @then UT-17 の違反を含む結果を返す
       */
      @Test
      void 表示名を付けた場合_UT17の違反を返す() {
        String source = VALID_METHOD.replace("@Test", "@Test @DisplayName(\"x\")");

        TestMethodAnalyzer.MethodAnalysis analysis =
            new TestMethodAnalyzer().analyze(methodOf(source), "A.java", "pkg.ATest", "f", "正常系");

        assertThat(analysis.violations()).extracting(Violation::rule).contains(UtRule.UT17);
      }
    }
  }

  @Nested
  class javadocOf {

    @Nested
    class 正常系 {

      /**
       * JavaDoc を分解し、書式の違反を検査して返すことを確かめる。 本文とタグの内容を仕様書に出す。
       *
       * @given 書式が規定どおりのテストメソッド
       * @when javadocOf を実行する
       * @then 本文とタグの内容を持つ JavaDoc を返し、違反を追加しない
       */
      @Test
      void 書式が規定どおりの場合_本文とタグを持つ値を返す() {
        List<Violation> violations = new ArrayList<>();

        JavadocSpec spec =
            new TestMethodAnalyzer().javadocOf(methodOf(VALID_METHOD), "A.java", 9, violations);

        assertThat(spec.tag("when")).isEqualTo("f を実行する");
        assertThat(violations).isEmpty();
      }
    }

    @Nested
    class 異常系 {

      /**
       * JavaDoc が無い場合は UT-10 を登録し、空の分解結果を返すことを確かめる。 説明の無いテストを仕様書の空欄として残す。
       *
       * @given JavaDoc の無いテストメソッド
       * @when javadocOf を実行する
       * @then 空の分解結果を返し、UT-10 の違反を登録する
       */
      @Test
      void JavaDocが書かれていない場合_空の分解結果を返し違反を登録する() {
        List<Violation> violations = new ArrayList<>();

        JavadocSpec spec =
            new TestMethodAnalyzer()
                .javadocOf(methodOf("class A { void m() {} }"), "A.java", 1, violations);

        assertThat(spec.sentences()).isEmpty();
        assertThat(violations)
            .singleElement()
            .satisfies(v -> assertThat(v.rule()).isEqualTo(UtRule.UT10));
      }
    }
  }
}
