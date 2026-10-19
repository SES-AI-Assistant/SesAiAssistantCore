package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link TestClassAnalyzer} の単体テスト。 */
class TestClassAnalyzerTest {

  /** 本体クラス A の対応。第1階層の名前は f と コンストラクタ。 */
  private static final Map<String, BodyClass> BODIES =
      Map.of(
          "pkg.A",
          new BodyClass("pkg", "A", "src/main/java/pkg/A.java", 1, List.of("f", "コンストラクタ"), false));

  /** テストクラスのファイルの相対パス。 */
  private static final String TEST_PATH = "src/test/java/pkg/ATest.java";

  /**
   * 書式が規定どおりのテストメソッドを1つ作る。
   *
   * @param name テストメソッドの名前
   * @return テストメソッドのソース
   */
  private static String validTest(String name) {
    return """
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
                """
        .formatted(name);
  }

  /**
   * 階層の宣言を1つ作る。
   *
   * @param name クラス名
   * @param body 中身
   * @return 宣言のソース
   */
  private static String nested(String name, String body) {
    return "@Nested\nclass " + name + " {\n" + body + "\n}\n";
  }

  /**
   * テストクラス ATest のソースを作る。
   *
   * @param link クラス JavaDoc の中身
   * @param members クラスの中身
   * @return テストクラスのソース
   */
  private static String classWith(String link, String members) {
    return "package pkg;\n\nimport org.junit.jupiter.api.Nested;\nimport org.junit.jupiter.api.Test;\n\n"
        + "/**\n * "
        + link
        + " の単体テスト。\n */\nclass ATest {\n"
        + members
        + "\n}\n";
  }

  /**
   * 規定どおりの構造のテストクラスのソースを作る。
   *
   * @param kindName 第2階層の名前
   * @return ソース
   */
  private static String validClass(String kindName) {
    return classWith("{@link A}", nested("f", nested(kindName, validTest("件数が3の場合_3を返す"))));
  }

  /**
   * ソースを解析して、テストクラスを検査する。
   *
   * @param source テストクラスのソース
   * @return 検査結果
   */
  private static TestClassReport analyze(String source) {
    return new TestClassAnalyzer().analyze(StaticJavaParser.parse(source), TEST_PATH, BODIES);
  }

  /**
   * ソースの先頭にあるクラス宣言を取り出す。入れ子のクラスより外側の宣言が先に見つかる。
   *
   * @param source Java のソース
   * @return 先頭のクラス宣言
   */
  private static ClassOrInterfaceDeclaration classOf(String source) {
    return StaticJavaParser.parse(source)
        .findFirst(ClassOrInterfaceDeclaration.class)
        .orElseThrow();
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 既定の設定で生成した TestClassAnalyzer が、規定どおりの構造を違反なしと判定することを確かめる。 既定の生成で検査が動かないと、構造の違反を見逃す。
       *
       * @given 既定のコンストラクタで生成した TestClassAnalyzer と、規定どおりの構造のテストクラス
       * @when analyze を実行する
       * @then 違反の無い検査結果を返す
       */
      @Test
      void 既定の設定で生成された場合_違反の無い検査結果を返す() {
        TestClassReport report =
            new TestClassAnalyzer()
                .analyze(StaticJavaParser.parse(validClass("正常系")), TEST_PATH, BODIES);

        assertThat(report.violations()).isEmpty();
      }
    }
  }

  @Nested
  class analyze {

    @Nested
    class 正常系 {

      /**
       * 規定どおりの構造のテストクラスは違反なしで、テストケースを1件持つことを確かめる。 仕様書に出す対象を過不足なく集める。
       *
       * @given 第1階層 f、第2階層 正常系、テストメソッド1件の規定どおりのテストクラス
       * @when analyze を実行する
       * @then 違反の無い検査結果を返し、テストケースを1件含む
       */
      @Test
      void 構造が規定どおりの場合_違反の無い検査結果を返す() {
        TestClassReport report = analyze(validClass("正常系"));

        assertThat(report.violations()).isEmpty();
        assertThat(report.classFqn()).isEqualTo("pkg.ATest");
        assertThat(report.bodyFqn()).isEqualTo("pkg.A");
        assertThat(report.cases()).hasSize(1);
      }

      /**
       * パッケージの無いテストクラスの完全修飾名は単純名になることを確かめる。 デフォルトパッケージのクラスを対応付ける。
       *
       * @given パッケージ宣言の無い ATest で、対象クラス A を参照するテストクラス
       * @when analyze を実行する
       * @then 完全修飾名が ATest の検査結果を返す
       */
      @Test
      void パッケージが無い場合_単純名の検査結果を返す() {
        String source = validClass("正常系").replace("package pkg;\n", "");

        TestClassReport report = analyze(source);

        assertThat(report.classFqn()).isEqualTo("ATest");
      }
    }

    @Nested
    class 異常系 {
      /**
       * 第1階層のクラスに @DisplayName を付けた場合に UT-17 の違反を返すことを確かめる。 階層の名前も日本語のクラス名で書かせる。
       *
       * @given 第1階層 f に @DisplayName を付けた、規定どおりの構造のテストクラス
       * @when analyze を実行する
       * @then UT-17 の違反を含む検査結果を返す
       */
      @Test
      void 階層に表示名がある場合_UT17の違反を返す() {
        String source =
            classWith("{@link A}", nested("f", nested("正常系", validTest("件数が3の場合_3を返す"))))
                .replace("@Nested\nclass f", "@Nested\n@DisplayName(\"x\")\nclass f");

        assertThat(analyze(source).violations()).extracting(Violation::rule).contains(UtRule.UT17);
      }

      /**
       * テストクラス名がファイル名と一致しない場合は UT-03 の違反を返すことを確かめる。 ファイル名と中身の対応を崩さない。
       *
       * @given ファイルが ATest.java で、クラス名が BTest のテストクラス
       * @when analyze を実行する
       * @then UT-03 の違反を含む検査結果を返す
       */
      @Test
      void クラス名がファイル名と異なる場合_UT03の違反を返す() {
        String source = validClass("正常系").replace("class ATest", "class BTest");

        assertThat(analyze(source).violations()).extracting(Violation::rule).contains(UtRule.UT03);
      }

      /**
       * 本体名 + Test の形式でない名前は UT-03 の違反を返し、対応する本体を持たないことを確かめる。 テストクラスは必ず本体の名前から作る。
       *
       * @given クラス名が Sample のテストクラス
       * @when analyze を実行する
       * @then UT-03 の違反を含み、対応する本体クラスを空にする検査結果を返す
       */
      @Test
      void テストの接尾辞が無い場合_UT03の違反と本体なしを返す() {
        String source =
            validClass("正常系").replace("class ATest", "class ATest2").replace("ATest2", "Sample");

        TestClassReport report =
            new TestClassAnalyzer()
                .analyze(StaticJavaParser.parse(source), "src/test/java/pkg/Sample.java", BODIES);

        assertThat(report.violations()).extracting(Violation::rule).contains(UtRule.UT03);
        assertThat(report.bodyFqn()).isNull();
      }

      /**
       * クラス以外の型だけを書いたファイルは UT-03 の違反を返すことを確かめる。 テストは通常のクラスとして書かせる。
       *
       * @given enum の宣言だけを持つテストファイル
       * @when analyze を実行する
       * @then UT-03 の違反を1件含む検査結果を返す
       */
      @Test
      void ファイルがクラス以外の型だけの場合_UT03の違反を返す() {
        String source = "package pkg;\nenum ATest { X }\n";

        assertThat(analyze(source).violations()).extracting(Violation::rule).contains(UtRule.UT03);
      }

      /**
       * 1ファイルに2つのクラスがある場合は UT-03 の違反を返すことを確かめる。 1ファイル1クラスの規則を守らせる。
       *
       * @given クラス ATest と補助のクラス Other を含むファイル
       * @when analyze を実行する
       * @then UT-03 の違反を1件含み、テストクラス名を空にする検査結果を返す
       */
      @Test
      void クラスが2つの場合_UT03の違反を返す() {
        String source = validClass("正常系") + "\nclass Other {}\n";

        TestClassReport report = analyze(source);

        assertThat(report.violations())
            .singleElement()
            .satisfies(v -> assertThat(v.rule()).isEqualTo(UtRule.UT03));
        assertThat(report.classFqn()).isEmpty();
      }

      /**
       * テストクラスの JavaDoc に対象クラスの参照が無い場合は UT-04 の違反を返すことを確かめる。 テストがどの本体を検証するかを明示させる。
       *
       * @given JavaDoc に対象クラスの参照が無いテストクラス
       * @when analyze を実行する
       * @then UT-04 の違反を含む検査結果を返す
       */
      @Test
      void 参照が無い場合_UT04の違反を返す() {
        String source = validClass("正常系").replace("{@link A}", "対象");

        assertThat(analyze(source).violations()).extracting(Violation::rule).contains(UtRule.UT04);
      }

      /**
       * 対象の本体クラスが無い場合は第1階層の名前が本体に無いとして UT-05 の違反を返すことを確かめる。 存在しない対象を指すテストを残さない。
       *
       * @given 本体クラスの対応が空の状態で、第1階層 f のテストクラス
       * @when analyze を実行する
       * @then UT-05 の違反を含む検査結果を返す
       */
      @Test
      void 本体が無い場合_UT05の違反を返す() {
        TestClassReport report =
            new TestClassAnalyzer()
                .analyze(StaticJavaParser.parse(validClass("正常系")), TEST_PATH, Map.of());

        assertThat(report.violations()).extracting(Violation::rule).contains(UtRule.UT05);
      }

      /**
       * 第1階層の名前が本体の対象メソッドに無い場合は UT-05 の違反を返すことを確かめる。 存在しないメソッドを指すテストを防ぐ。
       *
       * @given 第1階層が g で、本体に g が無いテストクラス
       * @when analyze を実行する
       * @then UT-05 の違反を含む検査結果を返す
       */
      @Test
      void 第1階層の名前が本体に無い場合_UT05の違反を返す() {
        String source =
            classWith("{@link A}", nested("g", nested("正常系", validTest("件数が3の場合_3を返す"))));

        assertThat(analyze(source).violations()).extracting(Violation::rule).contains(UtRule.UT05);
      }

      /**
       * テストクラス直下の変数は検査の対象外として違反を返さないことを確かめる。 テストに関係しないメンバーを制限しない。
       *
       * @given テストクラス直下に変数を置いた規定どおりのテストクラス
       * @when analyze を実行する
       * @then 違反の無い検査結果を返す
       */
      @Test
      void 最上位に変数がある場合_違反の無い検査結果を返す() {
        String source =
            classWith(
                "{@link A}", "int x;\n" + nested("f", nested("正常系", validTest("件数が3の場合_3を返す"))));

        assertThat(analyze(source).violations()).isEmpty();
      }

      /**
       * 第1階層の中の変数は検査の対象外として違反を返さないことを確かめる。 テストに関係しない変数を制限しない。
       *
       * @given 第1階層 f の中に変数を置いた規定どおりのテストクラス
       * @when analyze を実行する
       * @then 違反の無い検査結果を返す
       */
      @Test
      void 第一階層に変数がある場合_違反の無い検査結果を返す() {
        String source =
            classWith(
                "{@link A}", nested("f", "int x;\n" + nested("正常系", validTest("件数が3の場合_3を返す"))));

        assertThat(analyze(source).violations()).isEmpty();
      }

      /**
       * 第1階層の中の階層ではない補助クラスは違反を返さないことを確かめる。 テストの共通の補助クラスを第1階層の中に置けるようにする。
       *
       * @given 第1階層 f の中に @Nested のない補助クラスを置いた規定どおりのテストクラス
       * @when analyze を実行する
       * @then 違反の無い検査結果を返す
       */
      @Test
      void 第一階層に補助クラスがある場合_違反の無い検査結果を返す() {
        String source =
            classWith(
                "{@link A}",
                nested("f", "class Helper {\n}\n" + nested("正常系", validTest("件数が3の場合_3を返す"))));

        assertThat(analyze(source).violations()).isEmpty();
      }

      /**
       * 第2階層の中の変数は検査の対象外として違反を返さないことを確かめる。 テストに関係しない変数を制限しない。
       *
       * @given 第2階層 正常系 の中に変数を置いた規定どおりのテストクラス
       * @when analyze を実行する
       * @then 違反の無い検査結果を返す
       */
      @Test
      void 第二階層に変数がある場合_違反の無い検査結果を返す() {
        String source =
            classWith(
                "{@link A}", nested("f", nested("正常系", "int x;\n" + validTest("件数が3の場合_3を返す"))));

        assertThat(analyze(source).violations()).isEmpty();
      }

      /**
       * 規定外の種類の名前は UT-06 の違反を返すことを確かめる。 第2階層は5種類だけに限る。
       *
       * @given 第2階層が その他 のテストクラス
       * @when analyze を実行する
       * @then UT-06 の違反を含む検査結果を返す
       */
      @Test
      void 第2階層が規定外の種類の場合_UT06の違反を返す() {
        assertThat(analyze(validClass("その他")).violations())
            .extracting(Violation::rule)
            .contains(UtRule.UT06);
      }

      /**
       * 種類の順番が規定と違う場合は UT-06 の違反を返すことを確かめる。 仕様書の順番を毎回同じにする。
       *
       * @given 第2階層が 異常系 の後に 正常系 を書いたテストクラス
       * @when analyze を実行する
       * @then UT-06 の違反を含む検査結果を返す
       */
      @Test
      void 順番が違う場合_UT06の違反を返す() {
        String source =
            classWith(
                "{@link A}",
                nested(
                    "f",
                    nested("異常系", validTest("件数が3の場合_3を返す"))
                        + nested("正常系", validTest("件数が4の場合_4を返す"))));

        assertThat(analyze(source).violations()).extracting(Violation::rule).contains(UtRule.UT06);
      }

      /**
       * 第1階層に直接テストメソッドを置いた場合は UT-07 の違反を返すことを確かめる。 種類の階層を飛ばして置かせない。
       *
       * @given 第1階層 f の直下にテストメソッドを置いたテストクラス
       * @when analyze を実行する
       * @then UT-07 の違反を含む検査結果を返す
       */
      @Test
      void 第一階層にテストがある場合_UT07の違反を返す() {
        String source = classWith("{@link A}", nested("f", validTest("件数が3の場合_3を返す")));

        assertThat(analyze(source).violations()).extracting(Violation::rule).contains(UtRule.UT07);
      }

      /**
       * テストクラスの直下にテストメソッドを置いた場合は UT-07 の違反を返すことを確かめる。 テストメソッドを必ず第2階層に置かせる。
       *
       * @given テストクラスの直下にテストメソッドを置いたテストクラス
       * @when analyze を実行する
       * @then UT-07 の違反を含む検査結果を返す
       */
      @Test
      void 最上位にテストがある場合_UT07の違反を返す() {
        String source = classWith("{@link A}", validTest("件数が3の場合_3を返す"));

        assertThat(analyze(source).violations()).extracting(Violation::rule).contains(UtRule.UT07);
      }

      /**
       * 第2階層の中に @Nested を置いた場合は UT-07 の違反を返すことを確かめる。 階層を3段以上にしない。
       *
       * @given 第2階層の中に @Nested のクラスを置いたテストクラス
       * @when analyze を実行する
       * @then UT-07 の違反を含む検査結果を返す
       */
      @Test
      void 第二階層の中に階層がある場合_UT07の違反を返す() {
        String source =
            classWith(
                "{@link A}", nested("f", nested("正常系", nested("深い", validTest("件数が3の場合_3を返す")))));

        assertThat(analyze(source).violations()).extracting(Violation::rule).contains(UtRule.UT07);
      }

      /**
       * 未対応のテストアノテーションは UT-07 の違反を返すことを確かめる。 仕様で使うのは Test と ParameterizedTest だけにする。
       *
       * @given RepeatedTest を付けたテストメソッド
       * @when analyze を実行する
       * @then UT-07 の違反を含む検査結果を返す
       */
      @Test
      void テストが未対応のアノテーションの場合_UT07の違反を返す() {
        String source =
            classWith(
                "{@link A}",
                nested(
                    "f",
                    nested("正常系", validTest("件数が3の場合_3を返す").replace("@Test", "@RepeatedTest(2)"))));

        TestClassReport report = analyze(source);

        assertThat(report.violations()).extracting(Violation::rule).contains(UtRule.UT07);
        assertThat(report.cases()).isEmpty();
      }

      /**
       * 階層の外にあるテストクラスの補助クラスにテストを置いた場合は UT-07 の違反を返すことを確かめる。 JUnit が実行しないテストを残さない。
       *
       * @given @Nested の無いクラスの中にテストメソッドを置いたテストクラス
       * @when analyze を実行する
       * @then UT-07 の違反を含む検査結果を返す
       */
      @Test
      void 階層の外に補助クラスがある場合_UT07の違反を返す() {
        String source =
            classWith("{@link A}", "class Helper {\n" + validTest("件数が3の場合_3を返す") + "}\n");

        assertThat(analyze(source).violations()).extracting(Violation::rule).contains(UtRule.UT07);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 補助クラスの中にテストが無い場合は違反を返さないことを確かめる。 テストの共通の補助クラスを置けるようにする。
       *
       * @given テストを持たない補助クラスを置いた規定どおりのテストクラス
       * @when analyze を実行する
       * @then 違反の無い検査結果を返す
       */
      @Test
      void 補助クラスにテストが無い場合_違反の無い検査結果を返す() {
        String source =
            classWith(
                "{@link A}",
                "class Helper {\n}\n" + nested("f", nested("正常系", validTest("件数が3の場合_3を返す"))));

        assertThat(analyze(source).violations()).isEmpty();
      }
    }
  }

  @Nested
  class checkClassLink {

    @Nested
    class 正常系 {

      /**
       * 対象クラスへの単純名の参照が解決できる場合は違反を登録しないことを確かめる。 同じパッケージの対象クラスを参照するテストを通す。
       *
       * @given JavaDoc に {@link A} があり、import が無いテストクラス ATest
       * @when checkClassLink を実行する
       * @then 違反の一覧を変えない
       */
      @Test
      void 同じパッケージの参照がある場合_何もしない() {
        String source = "package pkg;\n/**\n * {@link A} の確認。\n */\nclass ATest {}\n";
        List<Violation> violations = new ArrayList<>();

        new TestClassAnalyzer()
            .checkClassLink(
                classOf(source), StaticJavaParser.parse(source), TEST_PATH, "pkg", "A", violations);

        assertThat(violations).isEmpty();
      }

      /**
       * 本体の名前が無い場合は検査せず、違反を登録しないことを確かめる。 本体の対応が無いテストは UT-02 で別に報告する。
       *
       * @given JavaDoc の無いテストクラス ATest と、本体の名前が null
       * @when checkClassLink を実行する
       * @then 違反の一覧を変えない
       */
      @Test
      void 本体の名前がnullの場合_何もしない() {
        String source = "class ATest {}";
        List<Violation> violations = new ArrayList<>();

        new TestClassAnalyzer()
            .checkClassLink(
                classOf(source), StaticJavaParser.parse(source), TEST_PATH, "", null, violations);

        assertThat(violations).isEmpty();
      }
    }

    @Nested
    class 異常系 {

      /**
       * JavaDoc が無いテストクラスは UT-04 を登録することを確かめる。 対象クラスを示す参照を必ず書かせる。
       *
       * @given JavaDoc の無いテストクラス ATest
       * @when checkClassLink を実行する
       * @then UT-04 の違反を1件登録する
       */
      @Test
      void 参照のJavaDocが無い場合_UT04の違反を登録する() {
        String source = "class ATest {}";
        List<Violation> violations = new ArrayList<>();

        new TestClassAnalyzer()
            .checkClassLink(
                classOf(source), StaticJavaParser.parse(source), TEST_PATH, "", "A", violations);

        assertThat(violations)
            .singleElement()
            .satisfies(v -> assertThat(v.rule()).isEqualTo(UtRule.UT04));
      }

      /**
       * 別のパッケージの同名クラスを完全修飾名で参照する場合は UT-04 を登録することを確かめる。 対象とは別のクラスを検証するテストを通さない。
       *
       * @given 完全修飾名 other.A を参照し、対象は pkg.A のテストクラス ATest
       * @when checkClassLink を実行する
       * @then UT-04 の違反を1件登録する
       */
      @Test
      void 別パッケージの完全修飾名を参照する場合_UT04の違反を登録する() {
        String source = "package pkg;\n/**\n * {@link other.A} の確認。\n */\nclass ATest {}\n";
        List<Violation> violations = new ArrayList<>();

        new TestClassAnalyzer()
            .checkClassLink(
                classOf(source), StaticJavaParser.parse(source), TEST_PATH, "pkg", "A", violations);

        assertThat(violations)
            .singleElement()
            .satisfies(v -> assertThat(v.rule()).isEqualTo(UtRule.UT04));
      }
    }
  }

  @Nested
  class checkDisplayName {

    @Nested
    class 正常系 {

      /**
       * 表示名のアノテーションが無いクラスは違反を登録しないことを確かめる。 通常のテストクラスを違反にしない。
       *
       * @given @DisplayName を付けていないクラス A
       * @when checkDisplayName を実行する
       * @then 違反の一覧を変えない
       */
      @Test
      void 表示名が無い場合_何もしない() {
        List<Violation> violations = new ArrayList<>();

        new TestClassAnalyzer().checkDisplayName(classOf("class A {}"), TEST_PATH, violations);

        assertThat(violations).isEmpty();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 表示名のアノテーションを付けたクラスが UT-17 の違反になることを確かめる。 テストクラスと階層の名前を日本語のクラス名で書かせる。
       *
       * @given DisplayName の付いたクラス A
       * @when checkDisplayName を実行する
       * @then UT-17 の違反を1件登録する
       */
      @Test
      void 表示名が付いている場合_UT17の違反を登録する() {
        List<Violation> violations = new ArrayList<>();

        new TestClassAnalyzer()
            .checkDisplayName(classOf("@DisplayName(\"x\") class A {}"), TEST_PATH, violations);

        assertThat(violations)
            .singleElement()
            .satisfies(v -> assertThat(v.rule()).isEqualTo(UtRule.UT17));
      }
    }
  }

  @Nested
  class checkLevel1 {

    @Nested
    class 正常系 {

      /**
       * 本体に存在する第1階層で、種類の違反が無ければ違反を登録しないことを確かめる。 規定どおりの第1階層を通す。
       *
       * @given 第1階層 f で、正常系のテストを1件持つクラス宣言
       * @when checkLevel1 を実行する
       * @then 違反を登録せず、テストケースを1件登録する
       */
      @Test
      void 本体にある名前の場合_違反を登録せずテストケースを登録する() {
        ClassOrInterfaceDeclaration level1 =
            classOf("class f { @Nested class 正常系 { @Test void 件数が3の場合_3を返す() {} } }");
        List<Violation> violations = new ArrayList<>();
        List<TestCase> cases = new ArrayList<>();

        new TestClassAnalyzer()
            .checkLevel1(level1, BODIES.get("pkg.A"), "pkg.ATest", TEST_PATH, violations, cases);

        assertThat(cases).hasSize(1);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 第2階層の無い第1階層は違反を登録しないことを確かめる。 種類をまだ書いていない対象も検査の対象にする。
       *
       * @given 第1階層 f で、第2階層を持たない クラス宣言
       * @when checkLevel1 を実行する
       * @then 違反を登録しない
       */
      @Test
      void 第二階層が無い場合_何もしない() {
        ClassOrInterfaceDeclaration level1 = classOf("class f {}");
        List<Violation> violations = new ArrayList<>();

        new TestClassAnalyzer()
            .checkLevel1(
                level1, BODIES.get("pkg.A"), "pkg.ATest", TEST_PATH, violations, new ArrayList<>());

        assertThat(violations).isEmpty();
      }
    }
  }

  @Nested
  class checkLevel2 {

    @Nested
    class 正常系 {

      /**
       * 規定の種類の名前は違反を登録せず、その順番を返すことを確かめる。 次の兄弟の順番の比較に使う。
       *
       * @given 第2階層 境界値 のクラス宣言で、直前の順番が 異常系 の 1
       * @when checkLevel2 を実行する
       * @then 違反を登録せず、順番の 2 を返す
       */
      @Test
      void 第2階層が規定の種類の場合_違反を登録せず順番を返す() {
        ClassOrInterfaceDeclaration kind = classOf("class 境界値 { }");
        List<Violation> violations = new ArrayList<>();

        int index =
            new TestClassAnalyzer()
                .checkLevel2(kind, "f", "pkg.ATest", TEST_PATH, 1, violations, new ArrayList<>());

        assertThat(index).isEqualTo(2);
        assertThat(violations).isEmpty();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 規定外の種類の名前は直前の順番のまま返すことを確かめる。 規定外の名前が後続の順番の判定を壊さない。
       *
       * @given 第2階層 その他 のクラス宣言で、直前の順番が 1
       * @when checkLevel2 を実行する
       * @then UT-06 を登録し、順番の 1 を返す
       */
      @Test
      void 第2階層が規定外の種類の場合_直前の順番を返す() {
        ClassOrInterfaceDeclaration kind = classOf("class その他 { }");
        List<Violation> violations = new ArrayList<>();

        int index =
            new TestClassAnalyzer()
                .checkLevel2(kind, "f", "pkg.ATest", TEST_PATH, 1, violations, new ArrayList<>());

        assertThat(index).isEqualTo(1);
        assertThat(violations)
            .singleElement()
            .satisfies(v -> assertThat(v.rule()).isEqualTo(UtRule.UT06));
      }

      /**
       * 直前より前の順番の種類は UT-06 を登録することを確かめる。 規定の順番を守らせる。
       *
       * @given 第2階層 正常系 のクラス宣言で、直前の順番が 2
       * @when checkLevel2 を実行する
       * @then UT-06 を登録し、順番の 2 を返す
       */
      @Test
      void 順番が前に戻る場合_UT06を登録し直前の順番を返す() {
        ClassOrInterfaceDeclaration kind = classOf("class 正常系 { }");
        List<Violation> violations = new ArrayList<>();

        int index =
            new TestClassAnalyzer()
                .checkLevel2(kind, "f", "pkg.ATest", TEST_PATH, 2, violations, new ArrayList<>());

        assertThat(index).isEqualTo(2);
        assertThat(violations)
            .singleElement()
            .satisfies(v -> assertThat(v.rule()).isEqualTo(UtRule.UT06));
      }
    }
  }

  @Nested
  class checkForbiddenNesting {

    @Nested
    class 正常系 {

      /**
       * テストも @Nested も無いクラスは違反を登録しないことを確かめる。 補助クラスを自由に置けるようにする。
       *
       * @given テストも @Nested も持たないクラス宣言
       * @when checkForbiddenNesting を実行する
       * @then 違反を登録しない
       */
      @Test
      void クラスに階層もテストも無い場合_何もしない() {
        List<Violation> violations = new ArrayList<>();

        new TestClassAnalyzer().checkForbiddenNesting(classOf("class A {}"), TEST_PATH, violations);

        assertThat(violations).isEmpty();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 階層の外の @Nested を UT-07 として登録することを確かめる。 JUnit に実行されない階層を残さない。
       *
       * @given @Nested を付けたクラス宣言
       * @when checkForbiddenNesting を実行する
       * @then UT-07 を登録する
       */
      @Test
      void 階層の印がある場合_UT07を登録する() {
        List<Violation> violations = new ArrayList<>();

        new TestClassAnalyzer()
            .checkForbiddenNesting(classOf("@Nested class A {}"), TEST_PATH, violations);

        assertThat(violations)
            .singleElement()
            .satisfies(v -> assertThat(v.rule()).isEqualTo(UtRule.UT07));
      }

      /**
       * 変数と子クラスがあっても、子クラスのテストを UT-07 として登録することを確かめる。 変数の存在が子クラスの検査を妨げない。
       *
       * @given 変数と、テストメソッドを持つ子クラスを含むクラス宣言
       * @when checkForbiddenNesting を実行する
       * @then UT-07 を登録する
       */
      @Test
      void 変数と子クラスがある場合_UT07を登録する() {
        List<Violation> violations = new ArrayList<>();

        new TestClassAnalyzer()
            .checkForbiddenNesting(
                classOf("class A { int x; class B { @Test void 件数が3の場合_3を返す() {} } }"),
                TEST_PATH,
                violations);

        assertThat(violations)
            .singleElement()
            .satisfies(v -> assertThat(v.rule()).isEqualTo(UtRule.UT07));
      }

      /**
       * 階層の外にある子クラスのテストも UT-07 として登録することを確かめる。 子の中のテストを見逃さない。
       *
       * @given テストメソッドを持つ子クラスを含むクラス宣言
       * @when checkForbiddenNesting を実行する
       * @then UT-07 を登録する
       */
      @Test
      void 子クラスにテストがある場合_UT07を登録する() {
        List<Violation> violations = new ArrayList<>();

        new TestClassAnalyzer()
            .checkForbiddenNesting(
                classOf("class A { class B { @Test void 件数が3の場合_3を返す() {} } }"),
                TEST_PATH,
                violations);

        assertThat(violations)
            .singleElement()
            .satisfies(v -> assertThat(v.rule()).isEqualTo(UtRule.UT07));
      }
    }
  }

  @Nested
  class resolvesToBody {

    @Nested
    class 正常系 {
      /**
       * 対象クラスと同じ完全修飾名を import している場合は、単純名の参照も解決できることを確かめる。 テストが対象クラスを明示的に import して書いても検査を通す。
       *
       * @given 対象クラス pkg.A を import しているテストソース
       * @when resolvesToBody を実行する
       * @then true を返す
       */
      @Test
      void 対象クラスを明示的にimportしている場合_trueを返す() {
        String source = "package pkg;\nimport pkg.A;\nclass ATest {}\n";

        assertThat(
                new TestClassAnalyzer()
                    .resolvesToBody("対象 {@link A}", StaticJavaParser.parse(source), "pkg", "A"))
            .isTrue();
      }

      /**
       * 同じパッケージの単純名の参照が、import が無い場合に解決できることを確かめる。 テストと対象が同じパッケージにある場合の標準の書き方を通す。
       *
       * @given 参照 {@link A} を含む文章と、import が無いテストソース
       * @when resolvesToBody を実行する
       * @then true を返す
       */
      @Test
      void 単純名の参照でimportが無い場合_trueを返す() {
        String source = "package pkg;\nclass ATest {}\n";

        assertThat(
                new TestClassAnalyzer()
                    .resolvesToBody("対象 {@link A} です", StaticJavaParser.parse(source), "pkg", "A"))
            .isTrue();
      }

      /**
       * 完全修飾名の参照が対象クラスと一致する場合に解決できることを確かめる。 参照の書き方を変えても対象を示せるようにする。
       *
       * @given 参照 {@link pkg.A} を含む文章
       * @when resolvesToBody を実行する
       * @then true を返す
       */
      @Test
      void 完全修飾名の参照が一致する場合_trueを返す() {
        String source = "package pkg;\nclass ATest {}\n";

        assertThat(
                new TestClassAnalyzer()
                    .resolvesToBody("対象 {@link pkg.A}", StaticJavaParser.parse(source), "pkg", "A"))
            .isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 別のクラスだけを参照する文章は対象に解決できないことを確かめる。 別の本体を対象にしたテストを通さない。
       *
       * @given 参照 {@link B} だけを含む文章
       * @when resolvesToBody を実行する
       * @then false を返す
       */
      @Test
      void 参照が別のクラスだけの場合_falseを返す() {
        String source = "package pkg;\nclass ATest {}\n";

        assertThat(
                new TestClassAnalyzer()
                    .resolvesToBody("対象 {@link B}", StaticJavaParser.parse(source), "pkg", "A"))
            .isFalse();
      }

      /**
       * 同じ単純名の別クラスを import している場合は、単純名の参照を解決できないことを確かめる。 import で別のクラスに解決される参照を、対象の検証とみなさない。
       *
       * @given import other.A を持ち、参照 {@link A} を含む文章
       * @when resolvesToBody を実行する
       * @then false を返す
       */
      @Test
      void 同名の別クラスをimportしている場合_falseを返す() {
        String source = "package pkg;\nimport other.A;\nclass ATest {}\n";

        assertThat(
                new TestClassAnalyzer()
                    .resolvesToBody("対象 {@link A}", StaticJavaParser.parse(source), "pkg", "A"))
            .isFalse();
      }
    }
  }

  @Nested
  class isNested {

    @Nested
    class 正常系 {

      /**
       * 階層の印を持つ宣言を階層として判定することを確かめる。 階層の対象を見分ける。
       *
       * @given @Nested を付けたクラス宣言
       * @when isNested を実行する
       * @then true を返す
       */
      @Test
      void 階層の印がある場合_trueを返す() {
        assertThat(new TestClassAnalyzer().isNested(classOf("@Nested class A {}"))).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 階層の印を持たない宣言は階層として判定しないことを確かめる。 補助クラスを階層に含めない。
       *
       * @given 印の無いクラス宣言
       * @when isNested を実行する
       * @then false を返す
       */
      @Test
      void 階層の印がない場合_falseを返す() {
        assertThat(new TestClassAnalyzer().isNested(classOf("class A {}"))).isFalse();
      }
    }
  }

  @Nested
  class isTestMethod {

    @Nested
    class 正常系 {

      /**
       * テストのアノテーションを持つメソッドをテストとして判定することを確かめる。 テストメソッドを見落とさない。
       *
       * @given @Test を付けたメソッド
       * @when isTestMethod を実行する
       * @then true を返す
       */
      @Test
      void テストの印がある場合_trueを返す() {
        BodyDeclaration<?> member = classOf("class A { @Test void m() {} }").getMembers().get(0);

        assertThat(new TestClassAnalyzer().isTestMethod(member)).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * テストの印の無いメソッドは判定しないことを確かめる。 通常のメソッドをテストとして扱わない。
       *
       * @given 印の無いメソッド
       * @when isTestMethod を実行する
       * @then false を返す
       */
      @Test
      void テストの印がない場合_falseを返す() {
        BodyDeclaration<?> member = classOf("class A { void m() {} }").getMembers().get(0);

        assertThat(new TestClassAnalyzer().isTestMethod(member)).isFalse();
      }

      /**
       * メソッド以外の宣言は判定しないことを確かめる。 フィールドを誤ってテストとして扱わない。
       *
       * @given フィールドの宣言
       * @when isTestMethod を実行する
       * @then false を返す
       */
      @Test
      void 宣言がメソッド以外の場合_falseを返す() {
        BodyDeclaration<?> member = classOf("class A { int x; }").getMembers().get(0);

        assertThat(new TestClassAnalyzer().isTestMethod(member)).isFalse();
      }
    }
  }

  @Nested
  class hasUnsupportedTestAnnotation {

    @Nested
    class 正常系 {

      /**
       * 未対応のテストアノテーションを持つメソッドを判定することを確かめる。 仕様で使わない種類を検出する。
       *
       * @given TestFactory を付けたメソッド
       * @when hasUnsupportedTestAnnotation を実行する
       * @then true を返す
       */
      @Test
      void 未対応の印がある場合_trueを返す() {
        MethodDeclaration method =
            classOf("class A { @TestFactory void m() {} }").getMethods().get(0);

        assertThat(new TestClassAnalyzer().hasUnsupportedTestAnnotation(method)).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 対応済みのテストアノテーションだけなら未対応ではないことを確かめる。 通常のテストを違反にしない。
       *
       * @given Test を付けたメソッド
       * @when hasUnsupportedTestAnnotation を実行する
       * @then false を返す
       */
      @Test
      void 印が対応済みだけの場合_falseを返す() {
        MethodDeclaration method = classOf("class A { @Test void m() {} }").getMethods().get(0);

        assertThat(new TestClassAnalyzer().hasUnsupportedTestAnnotation(method)).isFalse();
      }
    }
  }

  @Nested
  class qualify {

    @Nested
    class 正常系 {

      /**
       * パッケージがある場合は、パッケージとクラス名をドットでつなぐことを確かめる。 完全修飾名で本体と対応させる。
       *
       * @given パッケージが "pkg"、クラス名が "A"
       * @when qualify を実行する
       * @then "pkg.A" を返す
       */
      @Test
      void パッケージがある場合_ドットでつないだ名前を返す() {
        assertThat(new TestClassAnalyzer().qualify("pkg", "A")).isEqualTo("pkg.A");
      }
    }

    @Nested
    class 境界値 {

      /**
       * パッケージが空の場合は、クラス名だけを返すことを確かめる。 デフォルトパッケージで先頭に区切りを付けない。
       *
       * @given パッケージが空、クラス名が "A"
       * @when qualify を実行する
       * @then "A" を返す
       */
      @Test
      void パッケージが空の場合_クラス名だけを返す() {
        assertThat(new TestClassAnalyzer().qualify("", "A")).isEqualTo("A");
      }
    }
  }

  @Nested
  class fileBaseName {

    @Nested
    class 正常系 {

      /**
       * ディレクトリを除いた拡張子の無いファイル名を返すことを確かめる。 ファイル名とクラス名の一致を確かめる。
       *
       * @given 相対パスが "src/test/java/pkg/ATest.java"
       * @when fileBaseName を実行する
       * @then "ATest" を返す
       */
      @Test
      void パスが拡張子を持つ場合_拡張子を除いたファイル名を返す() {
        assertThat(new TestClassAnalyzer().fileBaseName("src/test/java/pkg/ATest.java"))
            .isEqualTo("ATest");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 拡張子の無いパスはそのままのファイル名を返すことを確かめる。 想定外の入力でも例外にしない。
       *
       * @given 相対パスが "ATest"
       * @when fileBaseName を実行する
       * @then "ATest" を返す
       */
      @Test
      void パスが拡張子を持たない場合_そのままのファイル名を返す() {
        assertThat(new TestClassAnalyzer().fileBaseName("ATest")).isEqualTo("ATest");
      }
    }
  }

  @Nested
  class lineOf {

    @Nested
    class 正常系 {

      /**
       * 宣言の開始行を返すことを確かめる。 違反の報告に行番号を付ける。
       *
       * @given 2行目から始まるクラス宣言
       * @when lineOf を実行する
       * @then 2 を返す
       */
      @Test
      void 位置が分かる場合_開始行を返す() {
        assertThat(new TestClassAnalyzer().lineOf(classOf("\nclass A {}"))).isEqualTo(2);
      }
    }
  }
}
