package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link OfficeAvailability} の単体テスト。 */
class OfficeAvailabilityTest {

  @Nested
  class toDisplayString {

    @Nested
    class 正常系 {

      /**
       * 出社日数が0の場合にフルリモート希望の文字列を返すことを確かめる。 リモート希望の要員を画面で判別できるようにする。
       *
       * @given 出社日数が 0
       * @when toDisplayString を実行する
       * @then フルリモート希望を返す
       */
      @Test
      void 出社日数が0の場合_フルリモート希望を返す() {
        assertThat(new OfficeAvailability(0).toDisplayString()).isEqualTo("フルリモート希望");
      }

      /**
       * 出社日数が5の場合に常駐可の文字列を返すことを確かめる。 常駐できる案件と要員を区別して表示する。
       *
       * @given 出社日数が 5
       * @when toDisplayString を実行する
       * @then 常駐可を返す
       */
      @Test
      void 出社日数が5の場合_常駐可を返す() {
        assertThat(new OfficeAvailability(5).toDisplayString()).isEqualTo("常駐可");
      }

      /**
       * 出社日数が0と5以外の場合に、週の出社日数を含む文字列を返すことを確かめる。 週何日まで出社できるかを表示する。
       *
       * @given 出社日数が 3
       * @when toDisplayString を実行する
       * @then 週3まで可を返す
       */
      @Test
      void 出社日数が0と5以外の場合_週の出社日数を含む文字列を返す() {
        assertThat(new OfficeAvailability(3).toDisplayString()).isEqualTo("週3まで可");
      }
    }
  }
}
