package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link DataUrl} の単体テスト。 */
class DataUrlTest {

  /** {@link DataUrl#base64Payload} の単体テスト。 */
  @Nested
  class base64Payload {

    /** 正常系。 */
    @Nested
    class 正常系 {

      /**
       * Data URL の接頭辞を取り除き、Base64 の本体だけを返すことを確かめる。 画面はファイルを Data URL の形式で送るため、復号の前に接頭辞を外す必要がある。
       *
       * @given Data URL "data:application/pdf;base64,aGVsbG8=" が与えられている
       * @when base64Payload を実行する
       * @then Base64 の本体 "aGVsbG8=" を返す
       */
      @Test
      void 接頭辞付きのDataURLの場合_base64の本体を返す() {
        assertThat(new DataUrl("data:application/pdf;base64,aGVsbG8=").base64Payload())
            .isEqualTo("aGVsbG8=");
      }
    }

    /** 境界値。 */
    @Nested
    class 境界値 {

      /**
       * 接頭辞の無い Base64 の文字列は、そのまま本体として返すことを確かめる。 接頭辞の無い値は、画面が Base64 だけを送った場合の入力になる。
       *
       * @given Base64 の文字列 "aGVsbG8=" が与えられている
       * @when base64Payload を実行する
       * @then 値をそのまま "aGVsbG8=" として返す
       */
      @Test
      void 接頭辞の無いBase64の場合_値そのものを返す() {
        assertThat(new DataUrl("aGVsbG8=").base64Payload()).isEqualTo("aGVsbG8=");
      }

      /**
       * 区切りの後ろが空の場合に、空の本体を返すことを確かめる。 区切りだけの値は、本体の無い入力として空の本体を返す。
       *
       * @given Data URL "data:text/plain;base64," が与えられている
       * @when base64Payload を実行する
       * @then 空の文字列を返す
       */
      @Test
      void 区切りの後ろが空の場合_空の文字列を返す() {
        assertThat(new DataUrl("data:text/plain;base64,").base64Payload()).isEmpty();
      }

      /**
       * 区切りが複数ある場合に、最初の区切りより後ろを本体として返すことを確かめる。 本体の中に区切りと同じ文字列があっても、本体を切り詰めない。
       *
       * @given Data URL "data:text/plain;base64,base64,YWJj" が与えられている
       * @when base64Payload を実行する
       * @then 最初の区切りより後ろの "base64,YWJj" を返す
       */
      @Test
      void 区切りが複数ある場合_最初の区切りより後ろを返す() {
        assertThat(new DataUrl("data:text/plain;base64,base64,YWJj").base64Payload())
            .isEqualTo("base64,YWJj");
      }
    }
  }
}
