package copel.sesproductpackage.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link ApplicationClock} の単体テスト。 */
class ApplicationClockTest {

  /** テスト前の読み込み元の設定値。テスト後に元へ戻すために保存する。 */
  private String 読み込み元の設定値;

  @BeforeEach
  void 読み込み元の設定値を保存する() {
    読み込み元の設定値 = System.getProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
  }

  @AfterEach
  void 時計と読み込み元を元に戻す() {
    ApplicationClock.INSTANCE.resetToDefault();
    if (読み込み元の設定値 == null) {
      System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
    } else {
      System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, 読み込み元の設定値);
    }
  }

  @Nested
  class clock {

    @Nested
    class 正常系 {

      /**
       * 既定の時計のタイムゾーンが Asia/Tokyo であることを確かめる。 本番の現在時刻の取得元が変わると日付の境界がずれるので、Asia/Tokyo を固定する。
       *
       * @given 時計が既定の状態
       * @when clock を実行する
       * @then Asia/Tokyo のタイムゾーンを返す
       */
      @Test
      void 既定の時計の場合_Asia_Tokyoのタイムゾーンを返す() {
        ApplicationClock.INSTANCE.resetToDefault();

        assertThat(ApplicationClock.INSTANCE.clock().getZone()).isEqualTo(ZoneId.of("Asia/Tokyo"));
      }
    }
  }

  @Nested
  class setClock {

    @Nested
    class 正常系 {

      /**
       * 読み込み元が memory の場合は、指定した時計を取得できることを確かめる。 テストで時刻を固定できないと日付に依存したテストが不安定になるので、設定した時計を使う。
       *
       * @given 読み込み元が memory、固定する時計が UTC の固定時計
       * @when setClock を固定時計で実行する
       * @then 指定した時計を返す
       */
      @Test
      void 固定時計を設定した場合_設定した時計を返す() {
        Clock fixed =
            Clock.fixed(java.time.Instant.parse("2026-01-15T03:04:05Z"), ZoneId.of("UTC"));

        ApplicationClock.INSTANCE.setClock(fixed);

        assertThat(ApplicationClock.INSTANCE.clock()).isSameAs(fixed);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 読み込み元が本番の場合は時計の差し替えを拒否することを確かめる。 本番で時計を差し替えると現在時刻の挙動が変わるので、拒否する。
       *
       * @given 読み込み元が本番で memory 以外
       * @when setClock を任意の時計で実行する
       * @then IllegalStateException を投げる
       */
      @Test
      void 読み込み元が本番の場合_IllegalStateExceptionを投げる() {
        System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, "production");

        assertThatThrownBy(() -> ApplicationClock.INSTANCE.setClock(Clock.systemUTC()))
            .isInstanceOf(IllegalStateException.class);
      }

      /**
       * 時計に null を指定すると例外を投げることを確かめる。 時計が無くなると現在時刻が取れなくなるので、null を拒否する。
       *
       * @given 読み込み元が memory、時計が null
       * @when setClock を null で実行する
       * @then NullPointerException を投げる
       */
      @Test
      void 時計がnullの場合_NullPointerExceptionを投げる() {
        assertThatThrownBy(() -> ApplicationClock.INSTANCE.setClock(null))
            .isInstanceOf(NullPointerException.class);
      }
    }
  }

  @Nested
  class resetToDefault {

    @Nested
    class 正常系 {

      /**
       * 固定した時計から既定の時計へ戻すことを確かめる。 テスト後に時刻の固定が残ると他のテストに影響するので、既定の時計に戻す。
       *
       * @given 時計が UTC の固定時計に設定されている
       * @when resetToDefault を実行する
       * @then Asia/Tokyo のタイムゾーンを返す
       */
      @Test
      void 固定時計から戻した場合_Asia_Tokyoのタイムゾーンを返す() {
        ApplicationClock.INSTANCE.setClock(Clock.fixed(java.time.Instant.EPOCH, ZoneId.of("UTC")));

        ApplicationClock.INSTANCE.resetToDefault();

        assertThat(ApplicationClock.INSTANCE.clock().getZone()).isEqualTo(ZoneId.of("Asia/Tokyo"));
      }
    }

    @Nested
    class 境界値 {

      /**
       * 読み込み元が本番の場合も既定の時計に戻せることを確かめる。 後始末の操作を本番で制限すると本番の初期化ができなくなるので、制限しない。
       *
       * @given 読み込み元が本番で memory 以外
       * @when resetToDefault を実行する
       * @then Asia/Tokyo のタイムゾーンを返す
       */
      @Test
      void 読み込み元が本番の場合_Asia_Tokyoのタイムゾーンを返す() {
        System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, "production");

        ApplicationClock.INSTANCE.resetToDefault();

        assertThat(ApplicationClock.INSTANCE.clock().getZone()).isEqualTo(ZoneId.of("Asia/Tokyo"));
      }
    }
  }
}
