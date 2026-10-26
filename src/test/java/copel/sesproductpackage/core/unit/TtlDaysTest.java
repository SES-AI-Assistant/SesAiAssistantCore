package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link TtlDays} の単体テスト。 */
class TtlDaysTest {

  @Nested
  class of {

    @Nested
    class 正常系 {

      /**
       * 設定値の日数をそのまま持つことを確かめる。 設定した保存期間で削除対象を決めるため、値がずれると期限切れの判定が変わるためである。
       *
       * @given 設定値の日数が 30
       * @when of を 30 で実行する
       * @then value が 30 を返す
       */
      @Test
      void 日数が30の場合_30を返す() {
        assertThat(TtlDays.of(30).value()).isEqualTo(30);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 設定値の下限の1日をそのまま持つことを確かめる。 1日は既定値に置き換えない最小の有効な日数であるためである。
       *
       * @given 設定値の日数が 1
       * @when of を 1 で実行する
       * @then value が 1 を返す
       */
      @Test
      void 日数が1の場合_1を返す() {
        assertThat(TtlDays.of(1).value()).isEqualTo(1);
      }

      /**
       * 未設定を表す0日を既定の7日にすることを確かめる。 Properties で未設定の値は 0 として読まれ、0日のままだと登録直後のレコードが期限切れになるためである。
       *
       * @given 設定値の日数が 0
       * @when of を 0 で実行する
       * @then value が 7 を返す
       */
      @Test
      void 日数が0の場合_既定の7を返す() {
        assertThat(TtlDays.of(0).value()).isEqualTo(7);
      }

      /**
       * 0より小さい日数を既定の7日にすることを確かめる。 負の保存期間では期限日が登録日より前になり、登録直後に期限切れとなるためである。
       *
       * @given 設定値の日数が -3
       * @when of を -3 で実行する
       * @then value が 7 を返す
       */
      @Test
      void 日数が負の場合_既定の7を返す() {
        assertThat(TtlDays.of(-3).value()).isEqualTo(7);
      }
    }
  }

  @Nested
  class expiryAfter {

    @Nested
    class 正常系 {

      /**
       * 登録日にTTL日数を足した期限日を返し、登録日は変えないことを確かめる。 登録日が変わると、以降の判定で別の日を期限にしてしまうためである。
       *
       * @given 登録日が 2026年1月7日 12時、TTL日数が 7
       * @when expiryAfter を登録日で実行する
       * @then 2026年1月14日 12時を返し、登録日は 2026年1月7日 12時のまま変えない
       */
      @Test
      void 登録日が2026年1月7日の場合_期限の1月14日を返す() {
        OriginalDateTime registerDate = new OriginalDateTime(2026, 1, 7, 12, 0, 0);

        OriginalDateTime expiry = TtlDays.of(7).expiryAfter(registerDate);

        assertThat(expiry.compareTo(new OriginalDateTime(2026, 1, 14, 12, 0, 0))).isZero();
        assertThat(registerDate.compareTo(new OriginalDateTime(2026, 1, 7, 12, 0, 0))).isZero();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 未設定の0日を既定の7日として期限日を求めることを確かめる。 0日のまま計算すると、登録日と同じ日が期限になってしまうためである。
       *
       * @given 登録日が 2026年1月7日 12時、TTL日数が 0
       * @when expiryAfter を登録日で実行する
       * @then 2026年1月14日 12時を返す
       */
      @Test
      void 未設定の0日の場合_既定の7日後の期限日を返す() {
        OriginalDateTime registerDate = new OriginalDateTime(2026, 1, 7, 12, 0, 0);

        OriginalDateTime expiry = TtlDays.of(0).expiryAfter(registerDate);

        assertThat(expiry.compareTo(new OriginalDateTime(2026, 1, 14, 12, 0, 0))).isZero();
      }
    }
  }
}
