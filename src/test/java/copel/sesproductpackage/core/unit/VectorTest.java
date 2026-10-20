package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.api.gpt.Transformer;
import java.io.IOException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link Vector} の単体テスト。 */
class VectorTest {

  @Nested
  class embedding {

    @Nested
    class 正常系 {

      /**
       * 文字列が設定されている場合に、変換結果をベクトル値として保持することを確かめる。 変換の結果を加工すると検索の精度が変わるので、変換器の結果をそのまま保持する。
       *
       * @given 変換器が "abc" に対して 0.5 のベクトルを返す、文字列が "abc"
       * @when embedding を実行する
       * @then 変換器の結果のベクトル値を返す
       */
      @Test
      void 文字列が設定されている場合_変換結果のベクトルを返す() throws IOException {
        Transformer transformer = mock(Transformer.class);
        when(transformer.embedding("abc")).thenReturn(new float[] {0.5f});
        Vector vector = new Vector(transformer);
        vector.setRawString("abc");

        vector.embedding();

        assertThat(vector.getValue()).containsExactly(0.5f);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 変換器が入出力例外を投げた場合、その例外を呼び出し側へ伝えることを確かめる。 変換の失敗を握りつぶすと原因が分からなくなるので、例外を呼び出し側へ伝える。
       *
       * @given 変換器が embedding で IOException を投げる、文字列が "abc"
       * @when embedding を実行する
       * @then IOException を投げる
       */
      @Test
      void 変換器が入出力例外を投げた場合_IOExceptionを投げる() throws IOException {
        Transformer transformer = mock(Transformer.class);
        when(transformer.embedding("abc")).thenThrow(new IOException("失敗"));
        Vector vector = new Vector(transformer);
        vector.setRawString("abc");

        assertThatThrownBy(vector::embedding).isInstanceOf(IOException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 文字列が未設定の場合は変換器を呼ばないことを確かめる。 変換対象の無い値を外部 API に送ると無駄な課金が起きるので、呼ばない。
       *
       * @given 変換器が存在する、文字列が未設定
       * @when embedding を実行する
       * @then 変換器の embedding を呼ばない
       */
      @Test
      void 文字列が未設定の場合_変換器の呼び出しを呼ばない() throws IOException {
        Transformer transformer = mock(Transformer.class);
        Vector vector = new Vector(transformer);

        vector.embedding();

        verify(transformer, never()).embedding(anyString());
        assertThat(vector.getValue()).isNull();
      }

      /**
       * 文字列が空の場合は変換器を呼ばないことを確かめる。 空文字を外部 API に送ると無効な問い合わせになるので、呼ばない。
       *
       * @given 変換器が存在する、文字列が空文字
       * @when embedding を実行する
       * @then 変換器の embedding を呼ばない
       */
      @Test
      void 文字列が空の場合_変換器の呼び出しを呼ばない() throws IOException {
        Transformer transformer = mock(Transformer.class);
        Vector vector = new Vector(transformer);
        vector.setRawString("");

        vector.embedding();

        verify(transformer, never()).embedding(anyString());
      }

      /**
       * 変換器が未設定の場合は何もしないことを確かめる。 変換器を持たないベクトルで例外が起きると処理全体が止まるので、何もしない。
       *
       * @given 変換器が null、文字列が "abc"
       * @when embedding を実行する
       * @then ベクトル値を設定せず、何もしない
       */
      @Test
      void 変換器が未設定の場合_何もしない() throws IOException {
        Vector vector = new Vector(null);
        vector.setRawString("abc");

        vector.embedding();

        assertThat(vector.getValue()).isNull();
      }
    }
  }

  @Nested
  class getValue {

    @Nested
    class 境界値 {

      /**
       * 変換前のベクトル値が null であることを確かめる。 未変換の値を空のベクトルと誤認すると検索結果が誤るので、null を返す。
       *
       * @given 変換を行っていない
       * @when getValue を実行する
       * @then null を返す
       */
      @Test
      void 変換前の場合_nullを返す() {
        assertThat(new Vector(null).getValue()).isNull();
      }
    }
  }

  @Nested
  class toString {

    @Nested
    class 正常系 {

      /**
       * ベクトル値の要素をカンマで区切り、角括弧で囲んだ文字列を返すことを確かめる。 ログの表記が変わると既存の監視の見た目が変わるので、区切り方を固定する。
       *
       * @given 変換器が "abc" に対して 1.5 と 2.0 のベクトルを返す、文字列が "abc"
       * @when toString を実行する
       * @then 1.5 と 2.0 をカンマで区切った文字列を角括弧で囲んで返す
       */
      @Test
      void ベクトル値が2件の場合_カンマ区切りの文字列を返す() throws IOException {
        Transformer transformer = mock(Transformer.class);
        when(transformer.embedding("abc")).thenReturn(new float[] {1.5f, 2.0f});
        Vector vector = new Vector(transformer);
        vector.setRawString("abc");
        vector.embedding();

        assertThat(vector.toString()).isEqualTo("[1.5,2.0]");
      }
    }

    @Nested
    class 境界値 {

      /**
       * ベクトル値が未設定の場合に、空の角括弧だけの文字列を返すことを確かめる。 未設定の表記が揃わないとログや画面の見た目が一貫しないので、空の角括弧を固定する。
       *
       * @given 変換を行っていない
       * @when toString を実行する
       * @then 空の角括弧だけの文字列を返す
       */
      @Test
      void ベクトル値が未設定の場合_空の角括弧だけの文字列を返す() {
        assertThat(new Vector(null).toString()).isEqualTo("[]");
      }
    }
  }
}
