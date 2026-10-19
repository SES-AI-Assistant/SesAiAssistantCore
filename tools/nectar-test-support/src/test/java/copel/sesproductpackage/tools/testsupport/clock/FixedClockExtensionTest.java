package copel.sesproductpackage.tools.testsupport.clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.util.ApplicationClock;
import copel.sesproductpackage.core.util.ConfigSource;
import java.lang.reflect.AnnotatedElement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

/** {@link FixedClockExtension} の単体テスト。 */
class FixedClockExtensionTest {

  /** 2026-01-15T12:04:05+09:00 の時刻を注釈に持つ、テスト用の要素。 */
  @FixedClock("2026-01-15T12:04:05+09:00")
  static final class 固定時刻を持つクラス {}

  /** 別の時刻を注釈に持つ、テスト用の要素。 */
  @FixedClock("2020-01-01T00:00:00+09:00")
  static final class 別の固定時刻を持つクラス {}

  /** 時刻の形式が不正な注釈を持つ、テスト用の要素。 */
  @FixedClock("不正な時刻")
  static final class 不正な時刻を持つクラス {}

  @Nested
  class beforeEach {

    @Nested
    class 正常系 {

      /**
       * メソッドに固定時刻が付いたテストで、現在時刻が指定した時刻に固定されることを確かめる。 注釈が JUnit の拡張として登録され、テストの前に時計が固定される仕組みを守る。
       *
       * @given テストメソッドに {@code @FixedClock("2026-01-15T12:04:05+09:00")} が付いている
       * @when テストの本体で現在時刻の取得元を読む
       * @then 2026-01-15T12:04:05+09:00 の時刻を Asia/Tokyo の時計として返す
       */
      @FixedClock("2026-01-15T12:04:05+09:00")
      @Test
      void メソッドに固定時刻が付いている場合_現在時刻の取得元を更新する() {
        assertThat(ApplicationClock.INSTANCE.clock())
            .isEqualTo(fixedClockAt("2026-01-15T03:04:05Z"));
      }

      /**
       * 注釈が付いていない場合は現在時刻の取得元を変更しないことを確かめる。 注釈の無いテストの時刻が、他のテストで固定された値に影響される不具合を防ぐ。
       *
       * @given 現在時刻の取得元が既定の時計、要素は注釈を持たない
       * @when beforeEach を要素を持つ階層のコンテキストで呼び出す
       * @then 時刻の取得元を変更せず、何もしない
       */
      @Test
      void 注釈が付いていない場合_何もしない() throws Exception {
        Clock before = ApplicationClock.INSTANCE.clock();

        new FixedClockExtension().beforeEach(contextOf(Object.class, null));

        assertThat(ApplicationClock.INSTANCE.clock()).isSameAs(before);
      }

      /**
       * 内側と外側の両方に注釈が付いている場合は、内側の時刻を使うことを確かめる。 メソッドの指定がクラスの指定より優先される仕様を守る。
       *
       * @given 内側の要素に 2026-01-15T12:04:05+09:00、外側の要素に 2020-01-01T00:00:00+09:00 の注釈がある
       * @when beforeEach を内側の階層のコンテキストで呼び出す
       * @then 2026-01-15T12:04:05+09:00 の時刻を Asia/Tokyo の時計として返す
       */
      @Test
      void 内側と外側の両方に付いている場合_現在時刻の取得元を更新する() throws Exception {
        ExtensionContext root = contextOf(null, null);
        ExtensionContext outer = contextOf(別の固定時刻を持つクラス.class, root);
        ExtensionContext inner = contextOf(固定時刻を持つクラス.class, outer);
        try {
          new FixedClockExtension().beforeEach(inner);

          assertThat(ApplicationClock.INSTANCE.clock())
              .isEqualTo(fixedClockAt("2026-01-15T03:04:05Z"));
        } finally {
          ApplicationClock.INSTANCE.resetToDefault();
        }
      }
    }

    @Nested
    class 異常系 {

      /**
       * 読み込み元が memory 以外の場合は IllegalStateException を投げることを確かめる。 本番の時計を差し替える不具合を防ぐ。
       *
       * @given システムプロパティ nectar.config.source が prod、要素に 2026-01-15T12:04:05+09:00 の注釈がある
       * @when beforeEach を要素を持つ階層のコンテキストで呼び出す
       * @then IllegalStateException を投げ、メッセージに nectar.config.source=memory を含む
       */
      @Test
      void 読み込み元がmemory以外の場合_IllegalStateExceptionを投げる() {
        読み込み元を変えて実行する(
            "prod",
            () ->
                assertThatThrownBy(
                        () ->
                            new FixedClockExtension().beforeEach(contextOf(固定時刻を持つクラス.class, null)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("nectar.config.source=memory"));
      }

      /**
       * 時刻の形式が不正な場合は DateTimeParseException を投げることを確かめる。 解釈できない時刻で固定して、テストが別の時刻で動く不具合を防ぐ。
       *
       * @given 要素に "不正な時刻" の注釈がある
       * @when beforeEach を要素を持つ階層のコンテキストで呼び出す
       * @then DateTimeParseException を投げる
       */
      @Test
      void 時刻の形式が不正な場合_DateTimeParseExceptionを投げる() {
        assertThatThrownBy(
                () -> new FixedClockExtension().beforeEach(contextOf(不正な時刻を持つクラス.class, null)))
            .isInstanceOf(DateTimeParseException.class);
      }

      /**
       * 並列実行が有効な場合は IllegalStateException を投げることを確かめる。 静的な状態を並列のテストと共有して壊れる不具合を防ぐ。
       *
       * @given システムプロパティ nectar.config.source が memory、並列実行が有効、要素に @FixedClock の注釈がある
       * @when beforeEach を要素を持つ階層のコンテキストで呼び出す
       * @then IllegalStateException を投げ、メッセージに並列実行を含む
       */
      @Test
      void 並列実行が有効の場合_IllegalStateExceptionを投げる() {
        ExtensionContext context = contextOf(固定時刻を持つクラス.class, null);
        when(context.getConfigurationParameter("junit.jupiter.execution.parallel.enabled"))
            .thenReturn(Optional.of("true"));

        assertThatThrownBy(() -> new FixedClockExtension().beforeEach(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("並列実行");
      }
    }
  }

  @Nested
  class afterEach {

    @Nested
    class 正常系 {

      /**
       * 注釈が付いたテストの後は現在時刻の取得元を既定に戻すことを確かめる。 固定された時計がテストの後まで残る不具合を防ぐ。
       *
       * @given 現在時刻を固定済みで、要素に 2026-01-15T12:04:05+09:00 の注釈がある
       * @when afterEach を要素を持つ階層のコンテキストで呼び出す
       * @then 固定した時計とは別の時計を既定値として更新する
       */
      @Test
      void 注釈が付いている場合_現在時刻の取得元を更新する() {
        Clock fixed = fixedClockAt("2026-01-15T03:04:05Z");
        ApplicationClock.INSTANCE.setClock(fixed);

        new FixedClockExtension().afterEach(contextOf(固定時刻を持つクラス.class, null));

        assertThat(ApplicationClock.INSTANCE.clock()).isNotSameAs(fixed);
      }

      /**
       * 注釈が付いていない場合は現在時刻の取得元に触れないことを確かめる。 注釈の無いテストが、別のテストで固定した時計を消す不具合を防ぐ。
       *
       * @given 現在時刻を固定済みで、要素は注釈を持たない
       * @when afterEach を要素を持つ階層のコンテキストで呼び出す
       * @then 固定した時計のまま何もしない
       */
      @Test
      void 注釈が付いていない場合_何もしない() {
        Clock fixed = fixedClockAt("2026-01-15T03:04:05Z");
        ApplicationClock.INSTANCE.setClock(fixed);
        try {
          new FixedClockExtension().afterEach(contextOf(Object.class, null));

          assertThat(ApplicationClock.INSTANCE.clock()).isSameAs(fixed);
        } finally {
          ApplicationClock.INSTANCE.resetToDefault();
        }
      }
    }
  }

  /**
   * 指定した UTC の瞬間を Asia/Tokyo の固定時計として作ります。
   *
   * @param utc ISO-8601 の UTC の時刻。例: "2026-01-15T03:04:05Z"
   * @return 固定時計
   */
  private static Clock fixedClockAt(final String utc) {
    return Clock.fixed(Instant.parse(utc), ZoneId.of("Asia/Tokyo"));
  }

  /**
   * 要素と親の階層を持つ、テスト実行コンテキストのモックを作ります。
   *
   * @param element 階層の要素。null の場合は要素を持たない
   * @param parent 親の階層。null の場合は親を持たない
   * @return テスト実行コンテキストのモック
   */
  private static ExtensionContext contextOf(
      final AnnotatedElement element, final ExtensionContext parent) {
    ExtensionContext context = mock(ExtensionContext.class);
    when(context.getElement()).thenReturn(Optional.ofNullable(element));
    when(context.getParent()).thenReturn(Optional.ofNullable(parent));
    return context;
  }

  /**
   * システムプロパティ nectar.config.source を指定した値に変えて処理を実行し、最後に memory へ戻します。
   *
   * @param value 一時的に設定する値
   * @param action 実行する処理
   */
  private static void 読み込み元を変えて実行する(final String value, final Runnable action) {
    System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, value);
    try {
      action.run();
    } finally {
      System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, ConfigSource.CONFIG_SOURCE_MEMORY);
    }
  }
}
