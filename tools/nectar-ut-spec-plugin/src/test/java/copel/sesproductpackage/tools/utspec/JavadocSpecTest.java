package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.comments.JavadocComment;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link JavadocSpec} の単体テスト。 */
class JavadocSpecTest {

  /**
   * JavaDoc の本文を、クラス A に付けて解析し、その JavaDoc を返す。
   *
   * @param body JavaDoc の中身（各行の先頭の * は付けない）
   * @return 解析した JavaDoc
   */
  private static JavadocSpec specOf(String body) {
    String source = "/**\n * " + body + "\n */\nclass A {}";
    JavadocComment comment =
        StaticJavaParser.parse(source)
            .getClassByName("A")
            .orElseThrow()
            .getJavadocComment()
            .orElseThrow();
    return JavadocSpec.from(comment.parse());
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 文の一覧とタグの対応をそのまま保持することを確かめる。 検査と仕様書の出力に同じ値を使う。
       *
       * @given 文が1件、タグ given の内容が1件の値
       * @when sentences と tags を取り出す
       * @then 渡した文とタグの値を返す
       */
      @Test
      void 文とタグが設定されている場合_文とタグを返す() {
        Map<String, List<String>> tags = new LinkedHashMap<>();
        tags.put("given", List.of("前提"));

        JavadocSpec spec = new JavadocSpec(List.of("文です。"), tags);

        assertThat(spec.sentences()).containsExactly("文です。");
        assertThat(spec.tag("given")).isEqualTo("前提");
      }
    }
  }

  @Nested
  class from {

    @Nested
    class 正常系 {
      /**
       * 整形ツールが段落の先頭に付けた段落タグを取り除き、2文として分けることを確かめる。 段落タグが文に残ると、文字数と枕詞の検査が行えない。
       *
       * @given 2文目の先頭に段落タグが付いた本文
       * @when from を実行する
       * @then 段落タグを除いた2文の本文を返す
       */
      @Test
      void 整形後に段落タグが付く場合_段落タグを除いた2文を返す() {
        JavadocSpec spec = specOf("内容を確かめる。\n *\n * <p>理由を書く。");

        assertThat(spec.sentences()).containsExactly("内容を確かめる。", "理由を書く。");
      }

      /**
       * 本文を句点で区切って文の一覧にし、タグを名前ごとに取り出すことを確かめる。 仕様書の列に本文とタグの内容を分けて出す。
       *
       * @given 2文の本文と @given、@when、@then のタグを持つ JavaDoc
       * @when from を実行する
       * @then 2文の本文と3つのタグの内容を返す
       */
      @Test
      void 本文とタグがある場合_文とタグに分けた値を返す() {
        JavadocSpec spec = specOf("内容を確かめる。\n理由を書く。\n@given 前提\n@when 操作\n@then 結果");

        assertThat(spec.sentences()).containsExactly("内容を確かめる。", "理由を書く。");
        assertThat(spec.tag("given")).isEqualTo("前提");
        assertThat(spec.tag("when")).isEqualTo("操作");
        assertThat(spec.tag("then")).isEqualTo("結果");
      }

      /**
       * 改行をまたぐ本文が、空白1つにまとめられることを確かめる。 JavaDoc の折り返し位置で文の内容が変わらないようにする。
       *
       * @given 1文が2行にまたがる本文
       * @when from を実行する
       * @then 空白1つでつないだ1文を返す
       */
      @Test
      void 文が改行をまたぐ場合_空白1つにまとめた1文を返す() {
        JavadocSpec spec = specOf("内容を\n確かめる。");

        assertThat(spec.sentences()).containsExactly("内容を 確かめる。");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 本文が無い JavaDoc は文が空になることを確かめる。 本文の無い場合を検査で違反として扱う。
       *
       * @given 本文が無く、タグ given だけを持つ JavaDoc
       * @when from を実行する
       * @then 文の一覧が空で、given の内容だけを返す
       */
      @Test
      void 本文が無い場合_空の文の一覧を返す() {
        JavadocSpec spec = specOf("@given 前提");

        assertThat(spec.sentences()).isEmpty();
        assertThat(spec.tag("given")).isEqualTo("前提");
      }
    }
  }

  @Nested
  class empty {

    @Nested
    class 正常系 {

      /**
       * JavaDoc が無い場合の空の値は、文もタグも持たないことを確かめる。 JavaDoc が無いテストの仕様書の行を空欄で出す。
       *
       * @given JavaDoc が無い場合の空の値
       * @when empty の文とタグの件数を確かめる
       * @then 文は0件で、タグの個数は0を返す
       */
      @Test
      void 値が空の場合_空の文の一覧を返す() {
        JavadocSpec spec = JavadocSpec.empty();

        assertThat(spec.sentences()).isEmpty();
        assertThat(spec.tagCount("given")).isZero();
      }
    }
  }

  @Nested
  class normalize {

    @Nested
    class 正常系 {
      /**
       * 大文字の段落タグと各種の改行タグを、空白に置き換えて正規化することを確かめる。 整形ツールや HTML の記法が違っても、文の区切りを同じに扱う必要があるため、空白に統一する。
       *
       * @given 大文字の P タグと br タグを含む文字列
       * @when normalize を実行する
       * @then 空白に置き換えた値を返す
       */
      @Test
      void 段落タグと改行タグが大文字と混在する場合_空白に置き換えた値を返す() {
        assertThat(JavadocSpec.normalize("<P>理由<br/>を<br />書く。")).isEqualTo("理由 を 書く。");
      }

      /**
       * 段落タグと全角の空白を含む文字列を正規化することを確かめる。 文分割の前に取り除くことで、整形ツールの改行が検査結果を変えないようにする。
       *
       * @given 段落タグと全角の空白を含む文字列
       * @when normalize を実行する
       * @then 段落タグと全角の空白を1つの空白にまとめた値を返す
       */
      @Test
      void 段落タグと全角空白がある場合_1つの空白にまとめた値を返す() {
        assertThat(JavadocSpec.normalize("<p>理由\u3000を書く。")).isEqualTo("理由 を書く。");
      }

      /**
       * 連続する空白と改行を1つの空白にまとめて、前後の空白を除くことを確かめる。 比較と表示で空白の違いを出さない。
       *
       * @given 前後と間に空白や改行が連続する文字列
       * @when normalize を実行する
       * @then 1つの空白にまとめて前後を除いた文字列を返す
       */
      @Test
      void 空白や改行が連続する場合_1つの空白にまとめた値を返す() {
        assertThat(JavadocSpec.normalize("  a \n\t b  ")).isEqualTo("a b");
      }
    }
  }

  @Nested
  class what {

    @Nested
    class 正常系 {

      /**
       * 本文の1文目を返すことを確かめる。 仕様書の"何を確かめるか"の列に出す。
       *
       * @given 2文の本文
       * @when what を実行する
       * @then 1文目を返す
       */
      @Test
      void 本文が二文の場合_一文目を返す() {
        JavadocSpec spec = new JavadocSpec(List.of("一文目。", "二文目。"), Map.of());

        assertThat(spec.what()).isEqualTo("一文目。");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 本文が空の場合は空文字を返すことを確かめる。 本文が無いテストで例外を投げない。
       *
       * @given 本文が空の値
       * @when what を実行する
       * @then 空文字を返す
       */
      @Test
      void 本文が空の場合_空文字を返す() {
        assertThat(JavadocSpec.empty().what()).isEmpty();
      }
    }
  }

  @Nested
  class why {

    @Nested
    class 正常系 {

      /**
       * 本文の2文目を返すことを確かめる。 仕様書の"理由"の列に出す。
       *
       * @given 2文の本文
       * @when why を実行する
       * @then 2文目を返す
       */
      @Test
      void 本文が二文の場合_二文目を返す() {
        JavadocSpec spec = new JavadocSpec(List.of("一文目。", "二文目。"), Map.of());

        assertThat(spec.why()).isEqualTo("二文目。");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 本文が1文だけの場合は空文字を返すことを確かめる。 2文目が無い場合に別の文を理由として出さない。
       *
       * @given 1文だけの本文
       * @when why を実行する
       * @then 空文字を返す
       */
      @Test
      void 本文が1文の場合_空文字を返す() {
        JavadocSpec spec = new JavadocSpec(List.of("一文目。"), Map.of());

        assertThat(spec.why()).isEmpty();
      }
    }
  }

  @Nested
  class tag {

    @Nested
    class 正常系 {

      /**
       * 指定したタグの最初の内容を返すことを確かめる。 前提、操作、期待結果の列に内容を出す。
       *
       * @given タグ given の内容が "前提"
       * @when tag に given を渡して実行する
       * @then "前提" を返す
       */
      @Test
      void タグが存在する場合_最初の内容を返す() {
        JavadocSpec spec = new JavadocSpec(List.of(), Map.of("given", List.of("前提")));

        assertThat(spec.tag("given")).isEqualTo("前提");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 内容の一覧が空のタグは空文字を返すことを確かめる。 内容が無いタグを例外にせず、違反として報告する。
       *
       * @given タグ given の内容が空の一覧の値
       * @when tag に given を渡して実行する
       * @then 空文字を返す
       */
      @Test
      void 内容の一覧が空の場合_空文字を返す() {
        JavadocSpec spec = new JavadocSpec(List.of(), Map.of("given", List.of()));

        assertThat(spec.tag("given")).isEmpty();
      }

      /**
       * 存在しないタグは空文字を返すことを確かめる。 タグの欠落を例外にせず、違反として報告する。
       *
       * @given タグを持たない値
       * @when tag に when を渡して実行する
       * @then 空文字を返す
       */
      @Test
      void タグが存在しない場合_空文字を返す() {
        assertThat(JavadocSpec.empty().tag("when")).isEmpty();
      }
    }
  }

  @Nested
  class tagCount {

    @Nested
    class 正常系 {

      /**
       * タグの個数を数えることを確かめる。 同じタグが重複していないかを検査する。
       *
       * @given タグ then の内容が2件ある値
       * @when tagCount に then を渡して実行する
       * @then 2を返す
       */
      @Test
      void 同じタグがある場合_2を返す() {
        JavadocSpec spec = new JavadocSpec(List.of(), Map.of("then", List.of("a", "b")));

        assertThat(spec.tagCount("then")).isEqualTo(2);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 存在しないタグの個数は0を返すことを確かめる。 必須のタグが無い場合を違反として判定する。
       *
       * @given タグを持たない値
       * @when tagCount に given を渡して実行する
       * @then 0を返す
       */
      @Test
      void タグが存在しない場合_0を返す() {
        assertThat(JavadocSpec.empty().tagCount("given")).isZero();
      }
    }
  }

  @Nested
  class allText {

    @Nested
    class 正常系 {

      /**
       * 本文とすべてのタグの内容を空白でつないだ文字列を返すことを確かめる。 禁止語と括弧の検査をまとめて行う。
       *
       * @given 1文の本文と、タグ given の内容が "前提"
       * @when allText を実行する
       * @then 本文と前提を空白でつないだ文字列を返す
       */
      @Test
      void 本文とタグがある場合_空白でつないだ文字列を返す() {
        JavadocSpec spec = new JavadocSpec(List.of("本文。"), Map.of("given", List.of("前提")));

        assertThat(spec.allText()).isEqualTo("本文。 前提");
      }
    }
  }
}
