package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link TextLine} の単体テスト。 */
class TextLineTest {

  @Nested
  class isBlank {

    @Nested
    class 正常系 {

      /**
       * 空白だけの行を空白の行として判定することを確かめる。 本文に意味のない空白の行を、読み取りの前に取り除くためである。
       *
       * @given 行が半角の空白3つだけの " "
       * @when isBlank を実行する
       * @then trueを返す
       */
      @Test
      void 空白だけの行の場合_trueを返す() {
        assertThat(new TextLine("   ").isBlank()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 空の行を空白の行として判定することを確かめる。 本文の区切りに入る空の行も、取り除く対象にするためである。
       *
       * @given 行が空文字
       * @when isBlank を実行する
       * @then trueを返す
       */
      @Test
      void 空の行の場合_trueを返す() {
        assertThat(new TextLine("").isBlank()).isTrue();
      }

      /**
       * 文字がある行は空白の行として判定しないことを確かめる。 本文の内容を誤って取り除かないためである。
       *
       * @given 行が "本文"
       * @when isBlank を実行する
       * @then falseを返す
       */
      @Test
      void 文字がある行の場合_falseを返す() {
        assertThat(new TextLine("本文").isBlank()).isFalse();
      }
    }
  }

  @Nested
  class isEmptyTableRow {

    @Nested
    class 正常系 {

      /**
       * セルがすべて空の表の行を、空の表の行として判定することを確かめる。 表の枠だけの行は中身が無く、読み取りの妨げになるためである。
       *
       * @given 行が "| | |"
       * @when isEmptyTableRow を実行する
       * @then trueを返す
       */
      @Test
      void セルがすべて空の表の行の場合_trueを返す() {
        assertThat(new TextLine("|  |  |").isEmptyTableRow()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * セルに文字がある表の行は、空の表の行として判定しないことを確かめる。 一部のセルにだけ内容がある行は、本文の情報を含むため残すためである。
       *
       * @given 行が "| 名前 | |"
       * @when isEmptyTableRow を実行する
       * @then falseを返す
       */
      @Test
      void セルに文字がある表の行の場合_falseを返す() {
        assertThat(new TextLine("| 名前 |  |").isEmptyTableRow()).isFalse();
      }

      /**
       * 縦線だけの行は、中身の無い表の行として判定することを確かめる。 両端の縦線だけで区切られた空のセルとして扱うためである。
       *
       * @given 行が "|"
       * @when isEmptyTableRow を実行する
       * @then trueを返す
       */
      @Test
      void 縦線だけの行の場合_trueを返す() {
        assertThat(new TextLine("|").isEmptyTableRow()).isTrue();
      }

      /**
       * 縦線で始まらない行は、表の行として判定しないことを確かめる。 文中の縦線で本文を誤って消さないためである。
       *
       * @given 行が "本文 | 注記"
       * @when isEmptyTableRow を実行する
       * @then falseを返す
       */
      @Test
      void 表の行でない場合_falseを返す() {
        assertThat(new TextLine("本文 | 注記").isEmptyTableRow()).isFalse();
      }
    }
  }

  @Nested
  class isTableSeparator {

    @Nested
    class 正常系 {

      /**
       * 横線と寄せ記号だけのセルの行を、区切り行として判定することを確かめる。 Markdown の表の見出しと本体の境目を、区切りとして扱うためである。
       *
       * @given 行が "| --- | :--- | ---: |"
       * @when isTableSeparator を実行する
       * @then trueを返す
       */
      @Test
      void 横線と寄せ記号だけのセルの場合_trueを返す() {
        assertThat(new TextLine("| --- | :--- | ---: |").isTableSeparator()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 文字のあるセルを含む行は、区切り行として判定しないことを確かめる。 見出しの文字を区切りとして取り除くと、表の項目が分からなくなるためである。
       *
       * @given 行が "| --- | 名前 |"
       * @when isTableSeparator を実行する
       * @then falseを返す
       */
      @Test
      void 文字のあるセルを含む場合_falseを返す() {
        assertThat(new TextLine("| --- | 名前 |").isTableSeparator()).isFalse();
      }

      /**
       * セルがすべて空の行は、区切り行として判定しないことを確かめる。 区切り行ではなく空の表の行として、別の規則で取り除くためである。
       *
       * @given 行が "| | |"
       * @when isTableSeparator を実行する
       * @then falseを返す
       */
      @Test
      void セルがすべて空の場合_falseを返す() {
        assertThat(new TextLine("|  |  |").isTableSeparator()).isFalse();
      }

      /**
       * 縦線で始まらない横線の行は、表の区切り行として判定しないことを確かめる。 本文の区切り線を、表の区切りとして誤って扱わないためである。
       *
       * @given 行が "--- ---"
       * @when isTableSeparator を実行する
       * @then falseを返す
       */
      @Test
      void 表の行でない場合_falseを返す() {
        assertThat(new TextLine("--- ---").isTableSeparator()).isFalse();
      }
    }
  }
}
