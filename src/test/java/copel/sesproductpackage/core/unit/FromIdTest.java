package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;

/** {@link FromId} の単体テスト。 */
class FromIdTest {

  @Nested
  class isEmailFormat {

    @Nested
    class 正常系 {

      /**
       * 送信者 ID がメールアドレスの形式なら true を返すことを確かめる。 メールと LINE の送信者を取り違えると担当の振り分けが誤るので、メール形式の判定を固定する。
       *
       * @given 送信者 ID が "a@example.com"
       * @when isEmailFormat を実行する
       * @then true を返す
       */
      @Test
      void メールアドレス形式の場合_trueを返す() {
        assertThat(FromId.isEmailFormat("a@example.com")).isTrue();
      }

      /**
       * 送信者 ID に @ を含まない場合は false を返すことを確かめる。 LINE の ID はメール形式ではないと分けておかないと、送信者の振り分けが誤る。
       *
       * @given 送信者 ID が "U001"
       * @when isEmailFormat を実行する
       * @then false を返す
       */
      @Test
      void LINEのID形式の場合_falseを返す() {
        assertThat(FromId.isEmailFormat("U001")).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 送信者 ID が null の場合は false を返すことを確かめる。 送信者が不明な場合に例外を起こすと受信処理全体が止まるので、null を判定に含める。
       *
       * @given 送信者 ID が null
       * @when isEmailFormat を実行する
       * @then false を返す
       */
      @Test
      void 送信者IDがnullの場合_falseを返す() {
        assertThat(FromId.isEmailFormat(null)).isFalse();
      }
    }
  }

  @Nested
  class extractDomain {

    @Nested
    class 正常系 {

      /**
       * メールアドレスからドメイン部分を小文字で取り出すことを確かめる。 ドメインの大文字と小文字で比較結果が変わると、同じ組織を別物と誤判定するので、小文字に揃える。
       *
       * @given メールアドレスが "A@Example.COM"
       * @when extractDomain を実行する
       * @then "example.com" を返す
       */
      @Test
      void メールアドレスの場合_小文字のドメインを返す() {
        assertThat(FromId.extractDomain("A@Example.COM")).isEqualTo("example.com");
      }

      /**
       * アットマークが複数ある場合は最初のアットマークより後ろをドメインとすることを確かめる。 本文に @ が複数あるとドメインの区切りが曖昧になるので、最初の @ で区切る規則を固定する。
       *
       * @given メールアドレスが "a@b@c"
       * @when extractDomain を実行する
       * @then "b@c" を返す
       */
      @Test
      void 複数のアットマークを含む場合_最初のアットマークより後ろを返す() {
        assertThat(FromId.extractDomain("a@b@c")).isEqualTo("b@c");
      }
    }

    @Nested
    class 異常系 {

      /**
       * アットマークを含まない文字列の場合は空文字を返すことを確かめる。 LINE の ID からドメインを取り出すと意味のない値になるので、空文字を返す。
       *
       * @given 送信者 ID が "U001"
       * @when extractDomain を実行する
       * @then 空文字を返す
       */
      @Test
      void アットマークを含まない場合_空文字を返す() {
        assertThat(FromId.extractDomain("U001")).isEmpty();
      }

      /**
       * null を指定した場合は空文字を返すことを確かめる。 送信者が不明な場合に空文字を返さないと、後続の比較で例外が起きるので、空文字にする。
       *
       * @given 送信者 ID が null
       * @when extractDomain を実行する
       * @then 空文字を返す
       */
      @NullSource
      @ParameterizedTest
      void 送信者IDがnullの場合_空文字を返す(final String email) {
        assertThat(FromId.extractDomain(email)).isEmpty();
      }
    }
  }

  @Nested
  class isSameSenderOrDomain {

    @Nested
    class 正常系 {

      /**
       * 送信者 ID が完全一致する場合は true を返すことを確かめる。 同じ LINE の送信者は、ドメインを使わずに同一と判定できる必要があるので、完全一致を先に判定する。
       *
       * @given 送信者 ID が両方とも "U001"
       * @when isSameSenderOrDomain を実行する
       * @then true を返す
       */
      @Test
      void 送信者IDが完全一致する場合_trueを返す() {
        assertThat(FromId.isSameSenderOrDomain("U001", "U001")).isTrue();
      }

      /**
       * 両方がメールアドレスでドメインが同じ場合は true を返すことを確かめる。
       * 同じ組織のメール送信者を同一として扱えないと、組織単位の集計が分かれてしまうので、ドメインの一致で判定する。
       *
       * @given 送信者 ID が "a@Example.com" と "b@example.com"
       * @when isSameSenderOrDomain を実行する
       * @then true を返す
       */
      @Test
      void 両方のドメインが同じ場合_trueを返す() {
        assertThat(FromId.isSameSenderOrDomain("a@Example.com", "b@example.com")).isTrue();
      }

      /**
       * メールアドレスの完全一致は空のドメインでも true を返すことを確かめる。 完全一致を先に判定する現在の仕様では、空のドメインでも同一と判定するので、その挙動を固定する。
       *
       * @given 送信者 ID が両方とも "a@"
       * @when isSameSenderOrDomain を実行する
       * @then true を返す
       */
      @Test
      void 空のドメインでも完全一致する場合_trueを返す() {
        assertThat(FromId.isSameSenderOrDomain("a@", "a@")).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * ドメインが異なる場合は false を返すことを確かめる。 別の組織の送信者を同一と判定すると他社の情報が混ざるので、ドメインの違いで区別する。
       *
       * @given 送信者 ID が "a@x.com" と "b@y.com"
       * @when isSameSenderOrDomain を実行する
       * @then false を返す
       */
      @Test
      void ドメインが異なる場合_falseを返す() {
        assertThat(FromId.isSameSenderOrDomain("a@x.com", "b@y.com")).isFalse();
      }

      /**
       * 片方だけがメールアドレスの場合は false を返すことを確かめる。 メールと LINE の送信者を混同すると同一人物の判定が誤るので、片方だけの形式は区別する。
       *
       * @given 送信者 ID が "a@x.com" と "U001"
       * @when isSameSenderOrDomain を実行する
       * @then false を返す
       */
      @Test
      void 片方だけがメール形式の場合_falseを返す() {
        assertThat(FromId.isSameSenderOrDomain("a@x.com", "U001")).isFalse();
      }

      /**
       * 両方のドメインが空の場合は false を返すことを確かめる。 空のドメインの一致を同一組織とみなすと、無関係の送信者がまとまってしまうので、判定から除く。
       *
       * @given 送信者 ID が "a@" と "b@"
       * @when isSameSenderOrDomain を実行する
       * @then false を返す
       */
      @Test
      void 両方のドメインが空の場合_falseを返す() {
        assertThat(FromId.isSameSenderOrDomain("a@", "b@")).isFalse();
      }

      /**
       * LINE の ID とメールアドレスを比べた場合は false を返すことを確かめる。 メールと LINE
       * の組は、どちらを先に渡しても区別される必要があるので、この組み合わせを固定する。
       *
       * @given 送信者 ID が "U001" と "a@x.com"
       * @when isSameSenderOrDomain を "U001" と "a@x.com" の順で実行する
       * @then false を返す
       */
      @Test
      void LINEのIDとメールアドレスの場合_falseを返す() {
        assertThat(FromId.isSameSenderOrDomain("U001", "a@x.com")).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 片方の送信者 ID が null の場合は false を返すことを確かめる。 送信者が不明な比較を同一と判定すると、誤った紐づけが起きるので、null は同一にしない。
       *
       * @given 送信者 ID の片方が null、もう片方が "U001"
       * @when isSameSenderOrDomain を実行する
       * @then false を返す
       */
      @Test
      void 送信者IDのどちらかがnullの場合_falseを返す() {
        assertThat(FromId.isSameSenderOrDomain(null, "U001")).isFalse();
      }

      /**
       * 比較対象の送信者 ID が null の場合は false を返すことを確かめる。 比較対象が不明な場合も同一と判定しないことで、誤った紐づけを防ぐ。
       *
       * @given 送信者 ID の比較対象が null、もう片方が "U001"
       * @when isSameSenderOrDomain を比較対象 null で実行する
       * @then false を返す
       */
      @Test
      void 比較対象の送信者IDがnullの場合_falseを返す() {
        assertThat(FromId.isSameSenderOrDomain("U001", null)).isFalse();
      }
    }
  }
}
