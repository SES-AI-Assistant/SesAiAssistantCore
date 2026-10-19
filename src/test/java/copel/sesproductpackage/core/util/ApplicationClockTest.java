package copel.sesproductpackage.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link ApplicationClock} の単体テスト。
 *
 * @author Copel Co., Ltd.
 */
class ApplicationClockTest {

  /** テストで使う固定時計。UTC の "2026-01-15T03:04:05Z" を "Asia/Tokyo" のゾーンで保持する。 */
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-01-15T03:04:05Z"), ZoneId.of("Asia/Tokyo"));

  /** テストの前に設定読み込み元のプロパティの値を退避する。 */
  private String originalConfigSource;

  /** テストの前に設定読み込み元のプロパティを退避して memory に設定し、時計を既定のシステム時計に戻す。 前のテストの差し替えが残らないようにする。 */
  @BeforeEach
  void setUp() {
    originalConfigSource = System.getProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
    System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, ConfigSource.CONFIG_SOURCE_MEMORY);
    ApplicationClock.INSTANCE.resetToDefault();
  }

  /** テストの後に設定読み込み元のプロパティを復元し、時計を既定のシステム時計に戻す。 後続のテストの設定が変わらないようにする。 */
  @AfterEach
  void tearDown() {
    if (originalConfigSource == null) {
      System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
    } else {
      System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, originalConfigSource);
    }
    ApplicationClock.INSTANCE.resetToDefault();
  }

  /** {@link ApplicationClock#clock()} のテスト。 */
  @Nested
  class clock {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * ApplicationClock#clock が既定のシステム時計を返すことを確かめる。 本番の現在時刻の取得元がシステム時計であることを保証する。
       *
       * @given 時計が既定のシステム時計に戻されている
       * @when clock を引数なしで呼び出す
       * @then ゾーンが "Asia/Tokyo" で、時刻が呼び出し前後の現在時刻の範囲に収まる時計を返す
       */
      @Test
      void 時計が既定のシステム時計の場合_システム時計を返す() {
        Instant before = Instant.now();
        Clock actual = ApplicationClock.INSTANCE.clock();
        Instant actualInstant = actual.instant();
        Instant after = Instant.now();

        assertEquals(ZoneId.of("Asia/Tokyo"), actual.getZone());
        assertFalse(actualInstant.isBefore(before));
        assertFalse(actualInstant.isAfter(after));
      }

      /**
       * ApplicationClock#clock が setClock で設定された固定時計をそのまま返すことを確かめる。
       * テストで時刻を固定するには、設定された時計が現在時刻の取得に使われる必要がある。
       *
       * @given 時計が UTC の "2026-01-15T03:04:05Z" と "Asia/Tokyo" のゾーンを持つ固定時計に設定されている
       * @when clock を引数なしで呼び出す
       * @then 設定した固定時計と同一のインスタンスを返す
       */
      @Test
      void 時計が固定時計に設定されている場合_設定された固定時計を返す() {
        ApplicationClock.INSTANCE.setClock(FIXED_CLOCK);

        Clock actual = ApplicationClock.INSTANCE.clock();

        assertSame(FIXED_CLOCK, actual);
      }
    }
  }

  /** {@link ApplicationClock#setClock(Clock)} のテスト。 */
  @Nested
  class setClock {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * ApplicationClock#setClock が渡された時計を取得元に設定することを確かめる。 テストで現在時刻を固定するための差し替え口である。
       *
       * @given 時計が既定のシステム時計に戻されており、システムプロパティ "nectar.config.source" が "memory" である
       * @when setClock を FIXED_CLOCK で呼び出す
       * @then clock が渡した固定時計と同一のインスタンスを返す
       */
      @Test
      void 引数が固定時計の場合_渡した時計を返す() {
        System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, ConfigSource.CONFIG_SOURCE_MEMORY);
        ApplicationClock.INSTANCE.setClock(FIXED_CLOCK);

        assertSame(FIXED_CLOCK, ApplicationClock.INSTANCE.clock());
      }
    }

    /** 異常系のテスト。 */
    @Nested
    class 異常系 {

      /**
       * ApplicationClock#setClock が null を受け付けず例外を投げることを確かめる。 null の時計では現在時刻を取得できない。
       *
       * @given 時計が固定時計に設定されており、システムプロパティ "nectar.config.source" が "memory" である
       * @when setClock を null で呼び出す
       * @then NullPointerException を投げ、clock が設定済みの固定時計と同一のインスタンスを返す
       */
      @Test
      void 引数がnullの場合_NullPointerExceptionを投げる() {
        ApplicationClock.INSTANCE.setClock(FIXED_CLOCK);

        assertThrows(NullPointerException.class, () -> ApplicationClock.INSTANCE.setClock(null));
        assertSame(FIXED_CLOCK, ApplicationClock.INSTANCE.clock());
      }

      /**
       * ApplicationClock#setClock が設定読み込み元が memory 以外の場合に例外を投げることを確かめる。 本番で時計が差し替わることを防ぐ仕様である。
       *
       * @given 時計が既定のシステム時計に戻されており、システムプロパティ "nectar.config.source" が "file" である
       * @when setClock を FIXED_CLOCK で呼び出す
       * @then IllegalStateException を投げ、clock が差し替え前のシステム時計と同一のインスタンスを返す
       */
      @Test
      void システムプロパティがmemory以外の場合_IllegalStateExceptionを投げる() {
        System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, "file");
        Clock before = ApplicationClock.INSTANCE.clock();

        assertThrows(
            IllegalStateException.class, () -> ApplicationClock.INSTANCE.setClock(FIXED_CLOCK));
        assertSame(before, ApplicationClock.INSTANCE.clock());
      }

      /**
       * ApplicationClock#setClock がシステムプロパティ未設定の場合に例外を投げることを確かめる。
       * 未設定は本番を含む通常の実行環境であり、差し替えを許可しない仕様である。
       *
       * @given 時計が既定のシステム時計に戻されており、システムプロパティ "nectar.config.source" が未設定である
       * @when setClock を FIXED_CLOCK で呼び出す
       * @then IllegalStateException を投げ、clock が差し替え前のシステム時計と同一のインスタンスを返す
       */
      @Test
      void システムプロパティが未設定の場合_IllegalStateExceptionを投げる() {
        System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
        Clock before = ApplicationClock.INSTANCE.clock();

        assertThrows(
            IllegalStateException.class, () -> ApplicationClock.INSTANCE.setClock(FIXED_CLOCK));
        assertSame(before, ApplicationClock.INSTANCE.clock());
      }
    }
  }

  /** {@link ApplicationClock#resetToDefault()} のテスト。 */
  @Nested
  class resetToDefault {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * ApplicationClock#resetToDefault が固定時計を既定のシステム時計に戻すことを確かめる。
       * テストの後始末で時計を戻せないと、後続のテストの現在時刻が固定されたままになる。
       *
       * @given 時計が固定時計に設定されている
       * @when resetToDefault を引数なしで呼び出す
       * @then clock が "Asia/Tokyo" のゾーンを持つ、固定時計とは別のインスタンスを返す
       */
      @Test
      void 時計が固定時計に設定されている場合_システム時計を返す() {
        ApplicationClock.INSTANCE.setClock(FIXED_CLOCK);

        ApplicationClock.INSTANCE.resetToDefault();
        Clock actual = ApplicationClock.INSTANCE.clock();

        assertEquals(ZoneId.of("Asia/Tokyo"), actual.getZone());
        assertNotSame(FIXED_CLOCK, actual);
      }

      /**
       * ApplicationClock#resetToDefault が設定読み込み元が memory 以外でも時計を戻すことを確かめる。
       * 現在時刻を固定しない操作であるため、システムプロパティによる制限を設けない仕様である。
       *
       * @given 時計が固定時計に設定されており、システムプロパティ "nectar.config.source" が "file" に変更されている
       * @when resetToDefault を引数なしで呼び出す
       * @then clock が固定時計とは別のインスタンスを返す
       */
      @Test
      void システムプロパティがmemory以外の場合_システム時計を返す() {
        ApplicationClock.INSTANCE.setClock(FIXED_CLOCK);
        System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, "file");

        ApplicationClock.INSTANCE.resetToDefault();

        assertNotSame(FIXED_CLOCK, ApplicationClock.INSTANCE.clock());
      }
    }
  }
}
