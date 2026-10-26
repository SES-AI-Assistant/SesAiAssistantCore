package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link RawContent} の単体テスト。 */
class RawContentTest {

  @Nested
  class toStoredValue {

    @Nested
    class 正常系 {

      /**
       * 上限以下の原文をそのまま返すことを確かめる。 短い原文を加工すると、保存される本文が変わってしまうからである。
       *
       * @given 原文が "原文テスト"
       * @when toStoredValue を実行する
       * @then "原文テスト" をそのまま返す
       */
      @Test
      void 原文が短い場合_原文を返す() {
        assertThat(RawContent.of("原文テスト").toStoredValue()).isEqualTo("原文テスト");
      }

      /**
       * 原文が無い場合に null をそのまま返すことを確かめる。 原文の無いメッセージを空文字に変えないためである。
       *
       * @given 原文が null
       * @when toStoredValue を実行する
       * @then null を返す
       */
      @Test
      void 原文がnullの場合_nullを返す() {
        assertThat(RawContent.of(null).toStoredValue()).isNull();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 3000文字ちょうどの原文を切り詰めないことを確かめる。 上限に収まる原文の末尾を捨てないからである。
       *
       * @given 原文が "あ" を3000個並べた3000文字
       * @when toStoredValue を実行する
       * @then 3000文字の原文をそのまま返す
       */
      @Test
      void 原文が3000文字の場合_原文を返す() {
        String rawContent = "あ".repeat(3000);

        assertThat(RawContent.of(rawContent).toStoredValue()).isEqualTo(rawContent);
      }

      /**
       * 3001文字の原文を先頭の3000文字に切り詰めることを確かめる。 保存できる上限を超えた分を捨てて、登録の失敗を防ぐからである。
       *
       * @given 原文が "い" を3001個並べた3001文字
       * @when toStoredValue を実行する
       * @then 先頭の "い" を3000個並べた3000文字を返す
       */
      @Test
      void 原文が3001文字の場合_先頭3000文字を返す() {
        assertThat(RawContent.of("い".repeat(3001)).toStoredValue()).isEqualTo("い".repeat(3000));
      }

      /**
       * 絵文字を含み3000コードポイントの原文を切り詰めないことを確かめる。 保存先はコードポイントで数えるため、上限内の原文を短くしないからである。
       *
       * @given 原文が絵文字を3000個並べた3000コードポイント
       * @when toStoredValue を実行する
       * @then 原文をそのまま返す
       */
      @Test
      void 絵文字を含み3000コードポイントの場合_原文を返す() {
        String rawContent = "😀".repeat(3000);

        assertThat(RawContent.of(rawContent).toStoredValue()).isEqualTo(rawContent);
      }

      /**
       * 絵文字を含み3001コードポイントの原文を、コードポイント単位で3000に切り詰めることを確かめる。 UTF-16 の単位で切ると絵文字が分断されるからである。
       *
       * @given 原文が絵文字を3001個並べた3001コードポイント
       * @when toStoredValue を実行する
       * @then 絵文字を3000個並べた原文を返す
       */
      @Test
      void 絵文字を含み3001コードポイントの場合_絵文字3000個の原文を返す() {
        assertThat(RawContent.of("😀".repeat(3001)).toStoredValue()).isEqualTo("😀".repeat(3000));
      }
    }
  }

  @Nested
  class utf16Length {

    @Nested
    class 正常系 {

      /**
       * 原文の長さを UTF-16 の単位で返すことを確かめる。 処理対象外のログに、従来どおりの文字数を出すためである。
       *
       * @given 原文が "原文テスト" の5文字
       * @when length を実行する
       * @then 5を返す
       */
      @Test
      void 原文が5文字の場合_5を返す() {
        assertThat(RawContent.of("原文テスト").utf16Length()).isEqualTo(5);
      }

      /**
       * 絵文字を1個含む原文の長さを UTF-16 の単位で数えることを確かめる。 ログの文字数は従来の String の長さと同じ値にするためである。
       *
       * @given 原文が絵文字を1個だけ含む
       * @when length を実行する
       * @then 2を返す
       */
      @Test
      void 絵文字が1個の場合_2を返す() {
        assertThat(RawContent.of("😀").utf16Length()).isEqualTo(2);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 原文が無い場合に長さの0を返すことを確かめる。 原文を持たない値でも例外にせず、ログを出せるようにするためである。
       *
       * @given 原文が null
       * @when length を実行する
       * @then 0を返す
       */
      @Test
      void 原文がnullの場合_0を返す() {
        assertThat(RawContent.of(null).utf16Length()).isZero();
      }
    }
  }
}
