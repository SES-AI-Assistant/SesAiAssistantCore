package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.Type;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link ExpectedNameCalculator} の単体テスト。 */
class ExpectedNameCalculatorTest {

  /**
   * ソースから、クラス A の宣言を取り出す。
   *
   * @param source Java のソース
   * @return クラス A の宣言
   */
  private static TypeDeclaration<?> typeOf(String source) {
    JavaParser parser =
        new JavaParser(
            new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17));
    return parser
        .parse(source)
        .getResult()
        .orElseThrow()
        .findFirst(TypeDeclaration.class)
        .orElseThrow();
  }

  /**
   * ソースに書かれた引数の型を、1つの引数として取り出す。
   *
   * @param signature 引数の書き方（例: String... values）
   * @return 引数
   */
  private static Parameter parameterOf(String signature) {
    return StaticJavaParser.parse("class A { void m(" + signature + ") {} }")
        .getClassByName("A")
        .orElseThrow()
        .getMethodsByName("m")
        .get(0)
        .getParameter(0);
  }

  /**
   * ソースに書かれた型を、1つの型として取り出す。
   *
   * @param source 型の書き方（例: String[][]）
   * @return 型
   */
  private static Type typeOfText(String source) {
    return StaticJavaParser.parseType(source);
  }

  @Nested
  class calculate {

    @Nested
    class 正常系 {
      /**
       * 明示したアクセサと構成要素の名前が同じ場合に、名前を1つだけ含むことを確かめる。 同じ名前を二重に期待させない。
       *
       * @given record A がアクセサ x を明示して宣言している
       * @when calculate を実行する
       * @then x と コンストラクタ の2つだけを含むリストを返す
       */
      @Test
      void レコードがアクセサを明示している場合_名前の一覧を返す() {
        TypeDeclaration<?> type = typeOf("record A(int x) { public int x() { return x; } }");

        assertThat(new ExpectedNameCalculator().calculate(type)).containsExactly("x", "コンストラクタ");
      }

      /**
       * record の構成要素の名前を、アクセサの第1階層の名前として含めることを確かめる。 レコードのアクセサも対象メソッドとしてテストできるようにする。
       *
       * @given record A が構成要素 x と y を宣言している
       * @when calculate を実行する
       * @then コンストラクタ、x、y の順のリストを返す
       */
      @Test
      void レコードの構成要素がある場合_構成要素の名前を返す() {
        TypeDeclaration<?> type = typeOf("record A(int x, int y) {}");

        assertThat(new ExpectedNameCalculator().calculate(type))
            .containsExactly("コンストラクタ", "x", "y");
      }

      /**
       * オーバーロードされていないメソッドは名前だけ、オーバーロードされたメソッドは引数型付きの名前にすることを確かめる。 第1階層の名前と対象の宣言を1対1に対応させる。
       *
       * @given メソッド f と g、String 引数の h、int 配列引数の h を持ち、コンストラクタを持たないクラス A
       * @when calculate を実行する
       * @then "f"、"g"、"h_String"、"h_intArray"、"コンストラクタ" の順のリストを返す
       */
      @Test
      void オーバーロードがある場合_型付きの名前の一覧を返す() {
        TypeDeclaration<?> type =
            typeOf(
                "class A { int y; void f() {} void g(int x) {} "
                    + "void h(String s) {} void h(int[] a) {} }");

        assertThat(new ExpectedNameCalculator().calculate(type))
            .containsExactly("f", "g", "h_String", "h_intArray", "コンストラクタ");
      }

      /**
       * 2つのコンストラクタがある場合は、引数型付きの名前にすることを確かめる。 コンストラクタの複数の宣言を区別できるようにする。
       *
       * @given 引数なしと String 引数の2つのコンストラクタを持つクラス A
       * @when calculate を実行する
       * @then "コンストラクタ" と "コンストラクタ_String" のリストを返す
       */
      @Test
      void コンストラクタが2つある場合_型付きの名前の一覧を返す() {
        TypeDeclaration<?> type = typeOf("class A { A() {} A(String s) {} }");

        assertThat(new ExpectedNameCalculator().calculate(type))
            .containsExactly("コンストラクタ", "コンストラクタ_String");
      }

      /**
       * コンストラクタを1つだけ持つ場合は、引数があってもコンストラクタとすることを確かめる。 単一の宣言に型を付けずに、仕様どおりの名前にする。
       *
       * @given 引数を持つコンストラクタを1つだけ持つクラス A
       * @when calculate を実行する
       * @then "コンストラクタ" のリストを返す
       */
      @Test
      void コンストラクタが1つだけある場合_型なしの名前を返す() {
        TypeDeclaration<?> type = typeOf("class A { A(int x) {} }");

        assertThat(new ExpectedNameCalculator().calculate(type)).containsExactly("コンストラクタ");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 引数なしのオーバーロードは、メソッド名だけを名前にすることを確かめる。 引数なしの版をメソッド名で書けるようにする。
       *
       * @given 引数なしの k と、int 引数の k を持つクラス A
       * @when calculate を実行する
       * @then "k" と "k_int" のリストを返す
       */
      @Test
      void 引数なしのオーバーロードがある場合_引数なしはメソッド名だけを返す() {
        TypeDeclaration<?> type = typeOf("class A { void k() {} void k(int x) {} }");

        assertThat(new ExpectedNameCalculator().calculate(type))
            .containsExactly("k", "k_int", "コンストラクタ");
      }
    }
  }

  @Nested
  class overloadName {

    @Nested
    class 正常系 {

      /**
       * 引数の型を_でつないだ名前を返すことを確かめる。 複数の引数を持つオーバーロードを区別する。
       *
       * @given 基本の名前 "m"、引数 int と String
       * @when overloadName を実行する
       * @then "m_int_String" を返す
       */
      @Test
      void 引数が2つある場合_型を下線でつないだ名前を返す() {
        List<Parameter> parameters =
            StaticJavaParser.parse("class A { void m(int a, String b) {} }")
                .getClassByName("A")
                .orElseThrow()
                .getMethodsByName("m")
                .get(0)
                .getParameters();

        assertThat(new ExpectedNameCalculator().overloadName("m", parameters))
            .isEqualTo("m_int_String");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 引数が無い場合は基本の名前だけを返すことを確かめる。 引数なしの版に下線の付いた名前を付けない。
       *
       * @given 基本の名前 "m"、引数が空の一覧
       * @when overloadName を実行する
       * @then "m" を返す
       */
      @Test
      void 引数の一覧が空の場合_基本の名前だけを返す() {
        assertThat(new ExpectedNameCalculator().overloadName("m", List.of())).isEqualTo("m");
      }
    }
  }

  @Nested
  class parameterToken {

    @Nested
    class 正常系 {

      /**
       * 通常の引数は型の名前だけを返すことを確かめる。 引数型付きの名前の断片にそのまま使う。
       *
       * @given String 型の引数
       * @when parameterToken を実行する
       * @then "String" を返す
       */
      @Test
      void 引数が通常の型の場合_型の名前を返す() {
        assertThat(new ExpectedNameCalculator().parameterToken(parameterOf("String s")))
            .isEqualTo("String");
      }

      /**
       * 可変長の引数は末尾に Varargs を付けることを確かめる。 配列の引数と区別できるようにする。
       *
       * @given String の可変長引数
       * @when parameterToken を実行する
       * @then "StringVarargs" を返す
       */
      @Test
      void 引数が可変長の場合_Varargsを付けた値を返す() {
        assertThat(new ExpectedNameCalculator().parameterToken(parameterOf("String... values")))
            .isEqualTo("StringVarargs");
      }
    }
  }

  @Nested
  class typeToken {

    @Nested
    class 正常系 {

      /**
       * プリミティブ型は書かれた名前を返すことを確かめる。 引数型付きの名前で int をそのまま使う。
       *
       * @given 型 int
       * @when typeToken を実行する
       * @then "int" を返す
       */
      @Test
      void 型がプリミティブ型の場合_型名を返す() {
        assertThat(new ExpectedNameCalculator().typeToken(typeOfText("int"))).isEqualTo("int");
      }

      /**
       * ジェネリクスの型引数を除き、単純名だけを返すことを確かめる。 完全修飾名やジェネリクスの書き方に依存しない名前にする。
       *
       * @given 型 java.util.Map の型引数付き参照
       * @when typeToken を実行する
       * @then "Map" を返す
       */
      @Test
      void 型が完全修飾名とジェネリクスを持つ場合_単純名を返す() {
        assertThat(
                new ExpectedNameCalculator()
                    .typeToken(typeOfText("java.util.Map<String, Integer>")))
            .isEqualTo("Map");
      }

      /**
       * 配列は次元ごとに Array を付けることを確かめる。 次元の違うオーバーロードを区別できるようにする。
       *
       * @given 型 String の2次元配列
       * @when typeToken を実行する
       * @then "StringArrayArray" を返す
       */
      @Test
      void 型が二次元配列の場合_次元ごとにArrayを付けた値を返す() {
        assertThat(new ExpectedNameCalculator().typeToken(typeOfText("String[][]")))
            .isEqualTo("StringArrayArray");
      }
    }
  }
}
