package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link ParameterizedRowExpander} の単体テスト。 */
class ParameterizedRowExpanderTest {

  /**
   * ソースのクラス A に書かれた、最初のメソッドを取り出す。
   *
   * @param source Java のソース
   * @return メソッド
   */
  private static MethodDeclaration methodOf(String source) {
    return StaticJavaParser.parse(source).getClassByName("A").orElseThrow().getMethods().get(0);
  }

  /**
   * メソッドの指定した位置のアノテーションを取り出す。
   *
   * @param method メソッド
   * @param index アノテーションの位置
   * @return アノテーション
   */
  private static AnnotationExpr annotationOf(MethodDeclaration method, int index) {
    return method.getAnnotations().get(index);
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 既定の設定で ParameterizedRowExpander を生成し、行の表示名を作れることを確かめる。 既定の生成で展開できないと、パラメータ付きテストの行が出なくなる。
       *
       * @given 既定のコンストラクタで生成した ParameterizedRowExpander と、値が1つの CsvSource を持つテスト
       * @when labels を実行する
       * @then 値1件分の表示名の一覧を返す
       */
      @Test
      void 値が1件の場合_値1件分の表示名の一覧を返す() {
        MethodDeclaration method =
            methodOf("class A { @ParameterizedTest @CsvSource({\"x\"}) void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().labels(method)).containsExactly("[1] x");
      }
    }
  }

  @Nested
  class labels {

    @Nested
    class 正常系 {

      /**
       * 通常のテストは空の表示名を1件だけ返すことを確かめる。 パラメータ付きでないテストに行番号を付けない。
       *
       * @given パラメータ付きでない通常のテストメソッド
       * @when labels を実行する
       * @then 空文字1件の一覧を返す
       */
      @Test
      void テストがパラメータ付きでない場合_空の表示名を返す() {
        MethodDeclaration method = methodOf("class A { @Test void m() {} }");

        assertThat(new ParameterizedRowExpander().labels(method)).containsExactly("");
      }

      /**
       * CsvSource の値ごとに番号と値をつないだ表示名を作ることを確かめる。 パラメータの組ごとに仕様書の行を分ける。
       *
       * @given 値 "a" と "b" を持つ CsvSource のパラメータ付きテスト
       * @when labels を実行する
       * @then 1番目の値 a と 2番目の値 b の表示名の一覧を返す
       */
      @Test
      void 値が2件の場合_組ごとの表示名を返す() {
        MethodDeclaration method =
            methodOf(
                "class A { @ParameterizedTest @CsvSource({\"a\", \"b\"}) void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().labels(method)).containsExactly("[1] a", "[2] b");
      }

      /**
       * name 属性の書式で、番号と値を置き換えた表示名を作ることを確かめる。 利用者が指定した表示名の形式を仕様書に反映する。
       *
       * @given name 属性が "{index} 件目 {arguments}" で、値が "x" の ValueSource のテスト
       * @when labels を実行する
       * @then "1 件目 x" の一覧を返す
       */
      @Test
      void 名前の属性がある場合_書式で置き換えた表示名を返す() {
        MethodDeclaration method =
            methodOf(
                "class A { @ParameterizedTest(name = \"{index} 件目 {arguments}\") "
                    + "@ValueSource(strings = {\"x\"}) void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().labels(method)).containsExactly("1 件目 x");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 展開できないソースのテストは1件の表示名として未展開と示すことを確かめる。 対応していないソースを仕様書から落とさない。
       *
       * @given MethodSource を持つパラメータ付きテスト
       * @when labels を実行する
       * @then 未展開のソース名を含む1件の一覧を返す
       */
      @Test
      void ソースが未対応の場合_未展開の表示名を返す() {
        MethodDeclaration method =
            methodOf("class A { @ParameterizedTest @MethodSource(\"data\") void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().labels(method))
            .containsExactly("(未展開のソース: MethodSource)");
      }

      /**
       * ソースが1つも無いパラメータ付きテストは未展開として1件返すことを確かめる。 値が無いのに仕様書の行を消さない。
       *
       * @given ParameterizedTest だけを持つテスト
       * @when labels を実行する
       * @then 未展開の表示名1件を返す
       */
      @Test
      void ソースが無い場合_未展開の表示名を返す() {
        MethodDeclaration method = methodOf("class A { @ParameterizedTest void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().labels(method)).containsExactly("(未展開のソース)");
      }
    }
  }

  @Nested
  class valuesOf {

    @Nested
    class 正常系 {
      /**
       * CsvSource の delimiter 属性は値として扱わず、value 属性の値だけを取り出すことを確かめる。 区切り文字の指定を値として表示名に混ぜない。
       *
       * @given delimiter と value を持つ CsvSource
       * @when valuesOf を実行する
       * @then value の値 a;b の1件を返す
       */
      @Test
      void 区切り文字の属性がある場合_value属性の値だけを返す() {
        MethodDeclaration method =
            methodOf(
                "class A { @ParameterizedTest @CsvSource(delimiter = ';', value = {\"a;b\"}) void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().valuesOf(annotationOf(method, 1)))
            .containsExactly("a;b");
      }

      /**
       * 配列の値を要素ごとに文字列にして返すことを確かめる。 値の1組ごとに表示名を作る。
       *
       * @given 値 "a" と 1 を持つ配列形式の CsvSource
       * @when valuesOf を実行する
       * @then "a" と "1" の一覧を返す
       */
      @Test
      void 配列の値がある場合_要素ごとの文字列を返す() {
        MethodDeclaration method =
            methodOf(
                "class A { @ParameterizedTest @CsvSource({\"a\", \"1\"}) void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().valuesOf(annotationOf(method, 1)))
            .containsExactly("a", "1");
      }

      /**
       * 単一値の書き方の値を、1件の一覧として取り出すことを確かめる。 配列でない書き方のソースも展開する。
       *
       * @given 単一値の文字列 "a" を書いた CsvSource
       * @when valuesOf を実行する
       * @then "a" の1件を返す
       */
      @Test
      void 値が単一の書き方の場合_値を返す() {
        MethodDeclaration method =
            methodOf("class A { @ParameterizedTest @CsvSource(\"a\") void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().valuesOf(annotationOf(method, 1)))
            .containsExactly("a");
      }

      /**
       * 属性付きの書き方の値を取り出すことを確かめる。 value 属性で書かれたソースも同じく展開する。
       *
       * @given value 属性で値 "x" を書いた CsvSource
       * @when valuesOf を実行する
       * @then "x" の1件を返す
       */
      @Test
      void 値が属性付きの書き方の場合_値を返す() {
        MethodDeclaration method =
            methodOf(
                "class A { @ParameterizedTest @CsvSource(value = {\"x\"}) void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().valuesOf(annotationOf(method, 1)))
            .containsExactly("x");
      }
    }

    @Nested
    class 境界値 {
      /**
       * ValueSource の ints 属性の値を、組ごとの文字列として取り出すことを確かめる。 型ごとの属性のソースも仕様書に展開する。
       *
       * @given ints に 1 と 2 を書いた ValueSource
       * @when valuesOf を実行する
       * @then 1 と 2 の一覧を返す
       */
      @Test
      void ints属性を持つValueSourceの場合_値の文字列の一覧を返す() {
        MethodDeclaration method =
            methodOf("class A { @ParameterizedTest @ValueSource(ints = {1, 2}) void m(int v) {} }");

        assertThat(new ParameterizedRowExpander().valuesOf(annotationOf(method, 1)))
            .containsExactly("1", "2");
      }

      /**
       * value 以外の属性だけを持つ CsvSource からは値を取り出さないことを確かめる。 区切り文字だけを書いた指定を値の一覧にしない。
       *
       * @given delimiter だけを持つ CsvSource
       * @when valuesOf を実行する
       * @then 空の一覧を返す
       */
      @Test
      void 値の属性が無い場合_空の一覧を返す() {
        MethodDeclaration method =
            methodOf(
                "class A { @ParameterizedTest @CsvSource(delimiter = ';') void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().valuesOf(annotationOf(method, 1))).isEmpty();
      }

      /**
       * 値を持たないアノテーションは空の一覧を返すことを確かめる。 空のソースで例外を投げない。
       *
       * @given 値を書かない マーカー形式の ValueSource
       * @when valuesOf を実行する
       * @then 空の一覧を返す
       */
      @Test
      void 値を書かない場合_空の一覧を返す() {
        MethodDeclaration method =
            methodOf("class A { @ParameterizedTest @ValueSource void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().valuesOf(annotationOf(method, 1))).isEmpty();
      }
    }
  }

  @Nested
  class expressionOf {

    @Nested
    class 正常系 {

      /**
       * 単一値の形式の式を取り出すことを確かめる。 括弧の中の1つの値を表示名にする。
       *
       * @given 単一値の形式の CsvSource
       * @when expressionOf を実行する
       * @then 式を返す
       */
      @Test
      void 式が単一値の形式の場合_式を返す() {
        MethodDeclaration method =
            methodOf("class A { @ParameterizedTest @CsvSource(\"a\") void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().expressionOf(annotationOf(method, 1)))
            .isPresent();
      }

      /**
       * 属性付きの形式の式を取り出すことを確かめる。 属性の名前に関係なく値を取り出す。
       *
       * @given 属性 strings を持つ ValueSource
       * @when expressionOf を実行する
       * @then 式を返す
       */
      @Test
      void 式が属性付きの形式の場合_式を返す() {
        MethodDeclaration method =
            methodOf(
                "class A { @ParameterizedTest @ValueSource(strings = {\"a\"}) void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().expressionOf(annotationOf(method, 1)))
            .isPresent();
      }
    }

    @Nested
    class 境界値 {
      /**
       * ValueSource の単一値の書き方は値として扱わず、空を返すことを確かめる。 ValueSource は型ごとの属性で値を書くため、単一値を値として読むと誤った表示名になる。
       *
       * @given 単一値 "a" を書いた ValueSource
       * @when expressionOf を実行する
       * @then 空を返す
       */
      @Test
      void ValueSourceが単一値の書き方の場合_空を返す() {
        MethodDeclaration method =
            methodOf("class A { @ParameterizedTest @ValueSource({\"a\"}) void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().expressionOf(annotationOf(method, 1))).isEmpty();
      }

      /**
       * 値を書かないマーカー形式は空を返すことを確かめる。 値が無いソースを展開の対象にしない。
       *
       * @given 値を書かない ValueSource
       * @when expressionOf を実行する
       * @then 空を返す
       */
      @Test
      void 式がマーカー形式の場合_空を返す() {
        MethodDeclaration method =
            methodOf("class A { @ParameterizedTest @ValueSource void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().expressionOf(annotationOf(method, 1))).isEmpty();
      }
    }
  }

  @Nested
  class label {

    @Nested
    class 正常系 {

      /**
       * 書式が空の場合は番号と値をつないだ既定の表示名を返すことを確かめる。 既定の表示名の形式を統一する。
       *
       * @given 書式が空、番号が 2、値が "b"
       * @when label を実行する
       * @then 番号2と値 b をつないだ表示名を返す
       */
      @Test
      void 書式が空の場合_既定の表示名を返す() {
        assertThat(new ParameterizedRowExpander().label("", 2, "b")).isEqualTo("[2] b");
      }

      /**
       * 書式の置き換え文字列を番号と値で置き換えることを確かめる。 利用者が指定した表示名の書式を反映する。
       *
       * @given 書式が "{index}:{arguments}"、番号が 1、値が "x"
       * @when label を実行する
       * @then "1:x" を返す
       */
      @Test
      void 書式がある場合_置き換えた表示名を返す() {
        assertThat(new ParameterizedRowExpander().label("{index}:{arguments}", 1, "x"))
            .isEqualTo("1:x");
      }
    }
  }

  @Nested
  class nameAttributeOf {

    @Nested
    class 正常系 {

      /**
       * name 属性の文字列を取り出すことを確かめる。 表示名の書式として使う。
       *
       * @given name 属性が "{index} 件目" の ParameterizedTest
       * @when nameAttributeOf を実行する
       * @then "{index} 件目" を返す
       */
      @Test
      void 名前の属性がある場合_文字列を返す() {
        MethodDeclaration method =
            methodOf("class A { @ParameterizedTest(name = \"{index} 件目\") void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().nameAttributeOf(annotationOf(method, 0)))
            .isEqualTo("{index} 件目");
      }
    }

    @Nested
    class 境界値 {

      /**
       * name 以外の属性だけを持つ場合は空文字を返すことを確かめる。 別の属性の値を表示名の書式として使わない。
       *
       * @given 属性 allowZeroInvocations だけを持つ ParameterizedTest
       * @when nameAttributeOf を実行する
       * @then 空文字を返す
       */
      @Test
      void 属性がname以外だけの場合_空文字を返す() {
        MethodDeclaration method =
            methodOf(
                "class A { @ParameterizedTest(allowZeroInvocations = true) void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().nameAttributeOf(annotationOf(method, 0)))
            .isEmpty();
      }

      /**
       * name 属性が無い場合は空文字を返すことを確かめる。 既定の表示名の形式を使う。
       *
       * @given 属性を書かない ParameterizedTest
       * @when nameAttributeOf を実行する
       * @then 空文字を返す
       */
      @Test
      void 属性を書かない場合_空文字を返す() {
        MethodDeclaration method = methodOf("class A { @ParameterizedTest void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().nameAttributeOf(annotationOf(method, 0)))
            .isEmpty();
      }

      /**
       * マーカー形式のアノテーションは空文字を返すことを確かめる。 属性を持たない書き方でも例外にしない。
       *
       * @given マーカー形式の ParameterizedTest
       * @when nameAttributeOf を実行する
       * @then 空文字を返す
       */
      @Test
      void 属性がマーカー形式の場合_空文字を返す() {
        MethodDeclaration method = methodOf("class A { @ParameterizedTest void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().nameAttributeOf(annotationOf(method, 0)))
            .isEmpty();
      }

      /**
       * name 属性が文字列以外の場合は空文字を返すことを確かめる。 定数で書かれた書式を表示名に使えないため、空として扱う。
       *
       * @given name 属性に定数名を書いた ParameterizedTest
       * @when nameAttributeOf を実行する
       * @then 空文字を返す
       */
      @Test
      void 属性が定数の場合_空文字を返す() {
        MethodDeclaration method =
            methodOf("class A { @ParameterizedTest(name = PATTERN) void m(String v) {} }");

        assertThat(new ParameterizedRowExpander().nameAttributeOf(annotationOf(method, 0)))
            .isEmpty();
      }
    }
  }

  @Nested
  class textOf {

    @Nested
    class 正常系 {

      /**
       * 文字列リテラルは引用符を外した文字列を返すことを確かめる。 表示名に引用符を入れない。
       *
       * @given 文字列リテラル "abc" の式
       * @when textOf を実行する
       * @then "abc" を返す
       */
      @Test
      void 式が文字列リテラルの場合_引用符を外した文字列を返す() {
        Expression expression = StaticJavaParser.parseExpression("\"abc\"");

        assertThat(new ParameterizedRowExpander().textOf(expression)).isEqualTo("abc");
      }

      /**
       * 数値リテラルはソースの書き方のままの文字列を返すことを確かめる。 数値の値を表示名にそのまま出す。
       *
       * @given 整数リテラル 1 の式
       * @when textOf を実行する
       * @then "1" を返す
       */
      @Test
      void 式が数値リテラルの場合_書き方のままの文字列を返す() {
        Expression expression = StaticJavaParser.parseExpression("1");

        assertThat(new ParameterizedRowExpander().textOf(expression)).isEqualTo("1");
      }
    }
  }
}
