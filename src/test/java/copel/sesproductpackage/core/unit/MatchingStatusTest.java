package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link MatchingStatus} の単体テスト。 */
class MatchingStatusTest {

  @Nested
  class getEnum {

    @Nested
    class 正常系 {

      /**
       * コード値から対応するマッチング状態区分を取得することを確かめる。 DB に保存したコード値から状態を復元する経路を守る。
       *
       * @given コード値が "01"
       * @when getEnum を "01" で実行する
       * @then サジェスト中を返す
       */
      @ParameterizedTest
      @CsvSource({
        "00, アンマッチ",
        "01, サジェスト中",
        "10, 提案中",
        "20, 進行中",
        "30, 一時停止",
        "40, 完了_オファー",
        "41, 完了_見送り",
        "99, 無効"
      })
      void コード値を指定した場合_対応するマッチング状態を返す(final String code, final String expected) {
        assertThat(MatchingStatus.getEnum(code).name()).isEqualTo(expected);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 未定義のコード値を指定すると null を返すことを確かめる。 未定義のコードを既定の状態に置き換えると誤った状態が保存されるので、null で判定させる。
       *
       * @given コード値が "XX"
       * @when getEnum を "XX" で実行する
       * @then null を返す
       */
      @Test
      void 未定義のコード値の場合_nullを返す() {
        assertThat(MatchingStatus.getEnum("XX")).isNull();
      }

      /**
       * null を指定すると null を返すことを確かめる。 コード値が無い場合に例外を投げると処理全体が止まるので、null を返す。
       *
       * @given コード値が null
       * @when getEnum を null で実行する
       * @then null を返す
       */
      @NullSource
      @ParameterizedTest
      void コード値がnullの場合_nullを返す(final String code) {
        assertThat(MatchingStatus.getEnum(code)).isNull();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 空文字のコード値を指定すると null を返すことを確かめる。 空文字を未定義として扱わないと既存の状態と誤って一致するので、null にする。
       *
       * @given コード値が空文字
       * @when getEnum を空文字で実行する
       * @then null を返す
       */
      @ParameterizedTest
      @ValueSource(strings = {""})
      void コード値が空文字の場合_nullを返す(final String code) {
        assertThat(MatchingStatus.getEnum(code)).isNull();
      }
    }
  }

  @Nested
  class getCode {

    @Nested
    class 正常系 {

      /**
       * 状態区分からコード値を取得することを確かめる。 DB のコード値から状態を戻せないと保存した状態が画面に出せないので、コード値の対応を固定する。
       *
       * @given 状態区分が サジェスト中
       * @when getCode を実行する
       * @then "01" を返す
       */
      @Test
      void サジェスト中の場合_01を返す() {
        assertThat(MatchingStatus.サジェスト中.getCode()).isEqualTo("01");
      }
    }
  }
}
