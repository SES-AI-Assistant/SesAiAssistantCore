package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link TextLines} の単体テスト。 */
class TextLinesTest {

  @Nested
  class of {

    @Nested
    class 正常系 {

      /**
       * 改行で区切った本文を、同じ並びの行として保持することを確かめる。 行の並びを変えずに組み直せることが、前処理の前提になるためである。
       *
       * @given 本文が "一行目\n二行目"
       * @when of で行に分けて join を実行する
       * @then "一行目\n二行目" を返す
       */
      @Test
      void 改行を含む本文の場合_同じ並びの本文を返す() {
        assertThat(TextLines.of("一行目\n二行目").join()).isEqualTo("一行目\n二行目");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 空文字の本文を、1行の空の本文として扱うことを確かめる。 空の本文でも例外にせず、空のまま返すためである。
       *
       * @given 本文が空文字
       * @when of で行に分けて join を実行する
       * @then 空文字を返す
       */
      @Test
      void 空文字の場合_空文字を返す() {
        assertThat(TextLines.of("").join()).isEmpty();
      }
    }
  }

  @Nested
  class withoutNoise {

    @Nested
    class 正常系 {

      /**
       * 空白だけの行と空の表の行を取り除き、それ以外の行は残すことを確かめる。 読み取りの妨げになる行だけを除く仕様を守るためである。
       *
       * @given 本文が "本文1" と空白の行と "| | |" と "本文2" の4行
       * @when withoutNoise を実行して join を実行する
       * @then "本文1\n本文2" を返す
       */
      @Test
      void 空白の行と空の表の行がある場合_それ以外の行だけを返す() {
        assertThat(TextLines.of("本文1\n   \n|  |  |\n本文2").withoutNoise().join())
            .isEqualTo("本文1\n本文2");
      }

      /**
       * 区切り行を含む表と文字の行は、どちらも変えずに残すことを確かめる。 表の見出しと内容を消さないための仕様であるためである。
       *
       * @given 本文が "| 名前 |" と "| --- |" と "| 値 |" の3行
       * @when withoutNoise を実行して join を実行する
       * @then 本文の3行をそのまま返す
       */
      @Test
      void 内容のある表の行の場合_すべての行を残した本文を返す() {
        assertThat(TextLines.of("| 名前 |\n| --- |\n| 値 |").withoutNoise().join())
            .isEqualTo("| 名前 |\n| --- |\n| 値 |");
      }

      /**
       * 同じ行が3回出る場合に、3回目を取り除くことを確かめる。 繰り返し出る定型の行で本文が膨らむのを防ぐためである。
       *
       * @given 本文が "a" を3回と "b" の4行
       * @when withoutNoise を実行して join を実行する
       * @then "a\na\nb" を返す
       */
      @Test
      void 同じ行が3回ある場合_3回目を除いた本文を返す() {
        assertThat(TextLines.of("a\na\na\nb").withoutNoise().join()).isEqualTo("a\na\nb");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 区切り行が2行続く場合に、2つ目以降を取り除くことを確かめる。 区切り線が重なって出る本文の乱れを防ぐためである。
       *
       * @given 本文が "| --- |" を3行続けて並べた3行
       * @when withoutNoise を実行して join を実行する
       * @then "| --- |" の1行だけを返す
       */
      @Test
      void 区切り行が連続する場合_2つ目以降を除いた本文を返す() {
        assertThat(TextLines.of("| --- |\n| --- |\n| --- |").withoutNoise().join())
            .isEqualTo("| --- |");
      }

      /**
       * 区切り行の間に空行が挟まる場合は、連続していないため両方を残すことを確かめる。 連続の判定は元の本文の直前の行で行うという仕様を守るためである。
       *
       * @given 本文が "| --- |" と空行と "| --- |" の3行
       * @when withoutNoise を実行して join を実行する
       * @then "| --- |\n| --- |" を返す
       */
      @Test
      void 区切り行の間に空行がある場合_両方を残した本文を返す() {
        assertThat(TextLines.of("| --- |\n\n| --- |").withoutNoise().join())
            .isEqualTo("| --- |\n| --- |");
      }

      /**
       * 区切り行の直前が本文の行の場合は、区切り行を残すことを確かめる。 本文の後ろの区切り行は、表の区切りとして必要なためである。
       *
       * @given 本文が "本文" と "| --- |" の2行
       * @when withoutNoise を実行して join を実行する
       * @then "本文\n| --- |" を返す
       */
      @Test
      void 区切り行の直前が本文の場合_区切り行を残した本文を返す() {
        assertThat(TextLines.of("本文\n| --- |").withoutNoise().join()).isEqualTo("本文\n| --- |");
      }

      /**
       * 区切り行が1行だけの場合は、取り除かないことを確かめる。 表の見出しと本体を分ける区切りが無くなると、表として読めなくなるためである。
       *
       * @given 本文が "| --- |" の1行
       * @when withoutNoise を実行して join を実行する
       * @then "| --- |" を返す
       */
      @Test
      void 区切り行が1行だけの場合_その行を返す() {
        assertThat(TextLines.of("| --- |").withoutNoise().join()).isEqualTo("| --- |");
      }

      /**
       * 同じ行が2回だけ出る場合は、両方を残すことを確かめる。 2回目までは本文として残す仕様であるためである。
       *
       * @given 本文が "a" を2回並べた2行
       * @when withoutNoise を実行して join を実行する
       * @then "a\na" を返す
       */
      @Test
      void 同じ行が2回の場合_両方を残した本文を返す() {
        assertThat(TextLines.of("a\na").withoutNoise().join()).isEqualTo("a\na");
      }

      /**
       * 同じ行が4回出る場合は、3回目以降をすべて取り除くことを確かめる。 3回目で止めず、以降の繰り返しも取り除くためである。
       *
       * @given 本文が "a" を4回並べた4行
       * @when withoutNoise を実行して join を実行する
       * @then "a\na" を返す
       */
      @Test
      void 同じ行が4回の場合_3回目以降を除いた本文を返す() {
        assertThat(TextLines.of("a\na\na\na").withoutNoise().join()).isEqualTo("a\na");
      }
    }
  }
}
