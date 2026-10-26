package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link PersonAge} の単体テスト。 */
class PersonAgeTest {

  @Nested
  class toStoredValue {

    @Nested
    class 正常系 {

      /**
       * 年齢が読み取れた要員の年齢をそのまま返すことを確かめる。 年齢を要員の検索条件や表示に使うからである。
       *
       * @given 年齢が35
       * @when toStoredValue を実行する
       * @then 35を返す
       */
      @Test
      void 年齢が35の場合_35を返す() {
        assertThat(PersonAge.of(35).toStoredValue()).isEqualTo(35);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 年齢の下限の1を保存することを確かめる。 1は年齢として読み取れた最小の値であり、不明と区別する必要があるからである。
       *
       * @given 年齢が1
       * @when toStoredValue を実行する
       * @then 1を返す
       */
      @Test
      void 年齢が1の場合_1を返す() {
        assertThat(PersonAge.of(1).toStoredValue()).isEqualTo(1);
      }

      /**
       * 年齢が0の場合に不明として null を返すことを確かめる。 0を年齢として保存すると、年齢が不明な要員を誤って年齢0として扱うからである。
       *
       * @given 年齢が0
       * @when toStoredValue を実行する
       * @then null を返す
       */
      @Test
      void 年齢が0の場合_nullを返す() {
        assertThat(PersonAge.of(0).toStoredValue()).isNull();
      }

      /**
       * 年齢が-1の場合に不明として null を返すことを確かめる。 Core の要員情報の既定値は -1 で、年齢が読み取れなかったことを表すからである。
       *
       * @given 年齢が-1
       * @when toStoredValue を実行する
       * @then null を返す
       */
      @Test
      void 年齢が負の値の場合_nullを返す() {
        assertThat(PersonAge.of(-1).toStoredValue()).isNull();
      }
    }
  }
}
