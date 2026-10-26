package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link MultiLineText} の単体テスト。 */
class MultiLineTextTest {

  @Nested
  class toLines {

    @Nested
    class 正常系 {

      /**
       * 改行で区切られたテキストを行の順に1件ずつの一覧へ分けることを確かめる。 経験や必須スキルは行ごとに1件として表示するためである。
       *
       * @given 改行で区切られたテキストが "Java"、"SQL"、"AWS" の3行
       * @when toLines を実行する
       * @then "Java"、"SQL"、"AWS" の順に3件を返す
       */
      @Test
      void 改行区切りのテキストの場合_行の順に3件を返す() {
        List<String> lines = new MultiLineText("Java\nSQL\nAWS").toLines();

        assertThat(lines).containsExactly("Java", "SQL", "AWS");
      }
    }

    @Nested
    class 境界値 {

      /**
       * テキストが null の場合は空の一覧を返すことを確かめる。 未登録の項目を読んでも例外で止まらないようにするためである。
       *
       * @given テキストが null
       * @when toLines を実行する
       * @then 空の一覧を返す
       */
      @Test
      void テキストがnullの場合_空の一覧を返す() {
        List<String> lines = new MultiLineText(null).toLines();

        assertThat(lines).isEmpty();
      }

      /**
       * 改行を含まないテキストは1件の一覧になることを確かめる。 1行だけ登録された項目を、1件として扱うためである。
       *
       * @given テキストが "Java" の1行
       * @when toLines を実行する
       * @then "Java" の1件を返す
       */
      @Test
      void 改行を含まないテキストの場合_1件の一覧を返す() {
        List<String> lines = new MultiLineText("Java").toLines();

        assertThat(lines).containsExactly("Java");
      }

      /**
       * 空文字は空文字1件の一覧になることを確かめる。 旧 splitLines と同じ結果を保ち、空の項目を一覧の表示に影響させないためである。
       *
       * @given テキストが空文字
       * @when toLines を実行する
       * @then 空文字1件の一覧を返す
       */
      @Test
      void 空文字の場合_空文字1件の一覧を返す() {
        List<String> lines = new MultiLineText("").toLines();

        assertThat(lines).containsExactly("");
      }

      /**
       * 末尾の改行は空の行を作らないことを確かめる。 登録時に末尾へ付いた改行で、空の項目を一覧に増やさないためである。
       *
       * @given テキストが "a" の後ろに改行が1つ付く
       * @when toLines を実行する
       * @then "a" の1件を返す
       */
      @Test
      void 末尾に改行がある場合_改行を除いた1件の一覧を返す() {
        List<String> lines = new MultiLineText("a\n").toLines();

        assertThat(lines).containsExactly("a");
      }

      /**
       * 改行だけのテキストは空の一覧になることを確かめる。 空行だけの項目を、表示する項目が無いものとして扱うためである。
       *
       * @given テキストが改行1つ
       * @when toLines を実行する
       * @then 空の一覧を返す
       */
      @Test
      void 改行だけの場合_空の一覧を返す() {
        List<String> lines = new MultiLineText("\n").toLines();

        assertThat(lines).isEmpty();
      }

      /**
       * 改行が CRLF の場合は CR を行の末尾に残すことを確かめる。 区切りは LF だけとする旧来の挙動を変えないためである。
       *
       * @given テキストが "a" の後ろに CRLF の改行が付く
       * @when toLines を実行する
       * @then "a\r" の1件を返す
       */
      @Test
      void CRLFの改行の場合_CRを残した1件の一覧を返す() {
        List<String> lines = new MultiLineText("a\r\n").toLines();

        assertThat(lines).containsExactly("a\r");
      }
    }
  }
}
