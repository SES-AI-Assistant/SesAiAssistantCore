package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link Gender} の単体テスト。 */
class GenderTest {

  @Nested
  class toJapanese {

    @Nested
    class 正常系 {

      /**
       * 性別を日本語表記に変換することを確かめる。 画面と API が日本語の表記で性別を扱うので、表記を固定する。
       *
       * @given 性別の列挙値が指定されている
       * @when toJapanese を実行する
       * @then 男性、女性、不明のいずれかを返す
       */
      @ParameterizedTest
      @CsvSource({"Man, 男性", "Woman, 女性", "Unknown, 不明"})
      void 性別の列挙値を指定した場合_日本語表記を返す(final Gender gender, final String expected) {
        assertThat(gender.toJapanese()).isEqualTo(expected);
      }
    }
  }

  @Nested
  class fromJapanese {

    @Nested
    class 正常系 {

      /**
       * 日本語表記から性別を逆引きすることを確かめる。 API で受け取った日本語の性別を列挙値に戻せないと保存できないので、逆引きを固定する。
       *
       * @given 日本語表記が男性、女性、不明のいずれか
       * @when fromJapanese を日本語表記で実行する
       * @then 対応する性別の列挙値を返す
       */
      @ParameterizedTest
      @CsvSource({"男性, Man", "女性, Woman", "不明, Unknown"})
      void 日本語表記を指定した場合_対応する性別を返す(final String japanese, final Gender expected) {
        assertThat(Gender.fromJapanese(japanese)).isEqualTo(expected);
      }
    }

    @Nested
    class 異常系 {

      /**
       * DB の列挙名を日本語表記として渡すと例外を投げることを確かめる。 DB の列挙名を API の値として受けると表記が混在するので、受け付けない値として固定する。
       *
       * @given 日本語表記として "Man" が指定されている
       * @when fromJapanese を "Man" で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void DBの列挙名を指定した場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> Gender.fromJapanese("Man"))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 空文字と null を指定すると例外を投げることを確かめる。 未入力の性別を既定値にすると誤った属性が保存されるので、例外で拒否する。
       *
       * @given 日本語表記が未入力
       * @when fromJapanese を未入力の値で実行する
       * @then IllegalArgumentException を投げる
       */
      @NullSource
      @ValueSource(strings = {""})
      @ParameterizedTest
      void 日本語表記が未入力の場合_IllegalArgumentExceptionを投げる(final String japanese) {
        assertThatThrownBy(() -> Gender.fromJapanese(japanese))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }
}
