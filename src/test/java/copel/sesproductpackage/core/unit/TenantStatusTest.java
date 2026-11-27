package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link TenantStatus} の単体テスト。 */
class TenantStatusTest {

  @Nested
  class getEnum {

    @Nested
    class 正常系 {

      /**
       * コード値からステータス区分を返すことを確かめる。 取込の停止判定はこの対応に依存するので、DB のコード値と列挙値の対応を固定する。
       *
       * @given コード値と列挙値が CsvSource の各行の値
       * @when getEnum をコード値で実行する
       * @then 対応するステータス区分を返す
       */
      @ParameterizedTest
      @CsvSource({"01, ACTIVE", "02, SUSPENDED"})
      void コード値が01か02の場合_対応するステータス区分を返す(final String code, final TenantStatus expected) {
        assertThat(TenantStatus.getEnum(code)).isEqualTo(expected);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 停止中以外のコード値、null、空文字を稼働中として返すことを確かめる。 旧データや未設定の行で取込を誤って止めないように、停止は "02" だけに限る。
       *
       * @given コード値が NullSource と ValueSource の各値で、null、空文字、"00"、"1"、"99" のいずれか
       * @when getEnum をコード値で実行する
       * @then ACTIVE を返す
       */
      @NullSource
      @ValueSource(strings = {"", "00", "1", "99"})
      @ParameterizedTest
      void コード値が00や1や99や空文字やnullの場合_ACTIVEを返す(final String code) {
        assertThat(TenantStatus.getEnum(code)).isEqualTo(TenantStatus.ACTIVE);
      }
    }
  }

  @Nested
  class isSuspended {

    @Nested
    class 正常系 {

      /**
       * 停止中のステータス区分が停止中と判定することを確かめる。 停止中のテナントは取込を止める必要があるので、true を返すことを守る。
       *
       * @given ステータス区分が SUSPENDED
       * @when isSuspended を実行する
       * @then true を返す
       */
      @Test
      void 停止中の場合_trueを返す() {
        assertThat(TenantStatus.SUSPENDED.isSuspended()).isTrue();
      }

      /**
       * 稼働中のステータス区分が停止中と判定しないことを確かめる。 稼働中のテナントは取込とマッチングを通常どおり行う必要があるので、false を返すことを守る。
       *
       * @given ステータス区分が ACTIVE
       * @when isSuspended を実行する
       * @then false を返す
       */
      @Test
      void 稼働中の場合_falseを返す() {
        assertThat(TenantStatus.ACTIVE.isSuspended()).isFalse();
      }
    }
  }

  @Nested
  class getCode {

    @Nested
    class 正常系 {

      /**
       * 停止中のステータス区分のコード値が "02" であることを確かめる。 DB の tenant_status_cd に保存される値なので、変わると既存データの判定が誤る。
       *
       * @given ステータス区分が SUSPENDED
       * @when getCode を実行する
       * @then "02" を返す
       */
      @Test
      void 停止中の場合_02を返す() {
        assertThat(TenantStatus.SUSPENDED.getCode()).isEqualTo("02");
      }

      /**
       * 稼働中のステータス区分のコード値が "01" であることを確かめる。 DB の tenant_status_cd に保存される値なので、変わると既存データの判定が誤る。
       *
       * @given ステータス区分が ACTIVE
       * @when getCode を実行する
       * @then "01" を返す
       */
      @Test
      void 稼働中の場合_01を返す() {
        assertThat(TenantStatus.ACTIVE.getCode()).isEqualTo("01");
      }
    }
  }
}
