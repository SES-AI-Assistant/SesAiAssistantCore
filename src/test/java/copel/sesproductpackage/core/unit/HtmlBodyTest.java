package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link HtmlBody} の単体テスト。 */
class HtmlBodyTest {

  @Nested
  class toPlainText {

    @Nested
    class 正常系 {

      /**
       * style の中身とタグを本文に残さず、本文の文字だけを返すことを確かめる。 メールの装飾用の CSS が本文に混ざると、原文が読めなくなるためである。
       *
       * @given body の中に style があり、その中身に "*{box-sizing:border-box}" と media の規則が含まれ、p の中に "本文" がある
       *     HTML
       * @when toPlainText を実行する
       * @then "本文" だけを返す
       */
      @Test
      void styleを含むHTMLの場合_本文の文字だけを返す() {
        String html =
            "<body><style>*{box-sizing:border-box}@media (max-width:520px){.a{width:100%}}</style>"
                + "<p>本文</p></body>";

        assertThat(HtmlBody.of(html).toPlainText()).isEqualTo("本文");
      }

      /**
       * 実体参照をデコードし、nbsp を半角空白にした文字を返すことを確かめる。 実体参照のままだと、原文の空白や記号が読めない文字として残るためである。
       *
       * @given p の中に "A&nbsp;B&amp;C" がある HTML
       * @when toPlainText を実行する
       * @then "A B&C" を返す
       */
      @Test
      void 実体参照を含むHTMLの場合_半角空白と記号に変換した文字を返す() {
        assertThat(HtmlBody.of("<p>A&nbsp;B&amp;C</p>").toPlainText()).isEqualTo("A B&C");
      }

      /**
       * リンクの表示文字だけを残し、img の alt と a の href を返さないことを確かめる。 属性の URL や代替文字は本文の文章ではないためである。
       *
       * @given p の中に href が付いた a の表示文字 "詳細はこちら" と、alt が付いた img がある HTML
       * @when toPlainText を実行する
       * @then "詳細はこちら" だけを返す
       */
      @Test
      void imgのaltとaのhrefを含むHTMLの場合_リンクの表示文字だけを返す() {
        String html =
            "<p><a href=\"https://example.com/a\">詳細はこちら</a><img src=\"logo.png\" alt=\"ロゴ\"></p>";

        assertThat(HtmlBody.of(html).toPlainText()).isEqualTo("詳細はこちら");
      }

      /**
       * ブロック要素の終わりで改行し、その後の文字を次の行に分けることを確かめる。 段落や見出しの区切りが読めなくなると、本文の構造が伝わらないためである。
       *
       * @given ValueSource の各行の HTML が、h1 から h6、p、div、li、tr のいずれかの中に "項目" を持ち、その後に "後" がある
       * @when toPlainText を実行する
       * @then "項目" と "後" を改行で分けた "項目\n後" を返す
       */
      @ParameterizedTest
      @ValueSource(
          strings = {
            "<h1>項目</h1>後",
            "<h2>項目</h2>後",
            "<h3>項目</h3>後",
            "<h4>項目</h4>後",
            "<h5>項目</h5>後",
            "<h6>項目</h6>後",
            "<p>項目</p>後",
            "<div>項目</div>後",
            "<ul><li>項目</li></ul>後",
            "<table><tr><td>項目</td></tr></table>後"
          })
      void ブロック要素を含むHTMLの場合_要素の後で改行した文字を返す(final String html) {
        assertThat(HtmlBody.of(html).toPlainText()).isEqualTo("項目\n後");
      }

      /**
       * br の位置で改行した文字を返すことを確かめる。 改行タグは本文の中で行を分ける唯一の指定であるためである。
       *
       * @given p の中に "A"、br、"B" がある HTML
       * @when toPlainText を実行する
       * @then "A" と "B" を改行で分けた文字を返す
       */
      @Test
      void 改行タグを含むHTMLの場合_改行を入れた文字を返す() {
        assertThat(HtmlBody.of("<p>A<br>B</p>").toPlainText()).isEqualTo("A\nB");
      }

      /**
       * 空の段落が続く場合に、連続する改行を空行1つにまとめた文字を返すことを確かめる。 段落の間が空きすぎると、本文が読みにくくなるためである。
       *
       * @given 内容のない p が2つ続き、その前後に p の "一段落目" と "二段落目" がある HTML
       * @when toPlainText を実行する
       * @then "一段落目" と "二段落目" の間を空行1つにした文字を返す
       */
      @Test
      void 空の段落が続くHTMLの場合_空行を1つにまとめた文字を返す() {
        String html = "<p>一段落目</p><p></p><p></p><p>二段落目</p>";

        assertThat(HtmlBody.of(html).toPlainText()).isEqualTo("一段落目\n\n二段落目");
      }

      /**
       * 各行の行頭と行末の空白を除き、先頭の改行も除いた文字を返すことを確かめる。 字下げのための空白が本文の行頭に残ると、行の比較と表示が崩れるためである。
       *
       * @given 先頭に br があり、p の中に前後の空白を含む "前後の空白"、改行と字下げの空白を含む "字下げ" がある HTML
       * @when toPlainText を実行する
       * @then 前後の空白と先頭の改行を除き "前後の空白" と "字下げ" を改行で分けた文字を返す
       */
      @Test
      void 行頭と行末に空白があるHTMLの場合_空白と先頭の改行を除いた文字を返す() {
        String html = "<br><p>   前後の空白   </p>\n<p>\n  字下げ\n</p>";

        assertThat(HtmlBody.of(html).toPlainText()).isEqualTo("前後の空白\n字下げ");
      }
    }

    @Nested
    class 境界値 {

      /**
       * script の中身、コメント、head の title を本文に残さないことを確かめる。 スクリプトや非表示の文字列が本文に混ざると、原文が読めなくなるためである。
       *
       * @given head の title が "件名タイトル"、body の中にコメント "隠しコメント" と script の "var secret = 1;" と p の
       *     "本文" がある HTML
       * @when toPlainText を実行する
       * @then "本文" だけを返す
       */
      @Test
      void scriptとコメントとtitleを含むHTMLの場合_本文だけを返す() {
        String html =
            "<html><head><title>件名タイトル</title></head><body><!-- 隠しコメント -->"
                + "<script>var secret = 1;</script><p>本文</p></body></html>";

        assertThat(HtmlBody.of(html).toPlainText()).isEqualTo("本文");
      }

      /**
       * 本文が null の場合に空文字を返すことを確かめる。 本文の無いメールでも例外で止めず、空の原文として扱うためである。
       *
       * @given HTML の本文が null
       * @when toPlainText を実行する
       * @then 空文字を返す
       */
      @Test
      void 本文がnullの場合_空文字を返す() {
        assertThat(HtmlBody.of(null).toPlainText()).isEmpty();
      }

      /**
       * 本文が空文字や空白だけの場合に空文字を返すことを確かめる。 空白だけの本文を空行として残すと、表示に意味のない行が増えるためである。
       *
       * @given HTML の本文が空文字または空白だけ
       * @when toPlainText を実行する
       * @then 空文字を返す
       */
      @ParameterizedTest
      @ValueSource(strings = {"", "   "})
      void 本文が空文字または空白だけの場合_空文字を返す(final String html) {
        assertThat(HtmlBody.of(html).toPlainText()).isEmpty();
      }
    }
  }
}
