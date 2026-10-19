package copel.sesproductpackage.tools.testsupport;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.util.ConfigSource;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

/** {@link MemoryModeGuard} の単体テスト。 */
class MemoryModeGuardTest {

  /** JUnit の並列実行の設定のキー。 */
  private static final String PARALLEL = "junit.jupiter.execution.parallel.enabled";

  @Nested
  class require {

    @Nested
    class 正常系 {

      /**
       * 読み込み元が memory の場合は例外を投げないことを確かめる。 テスト用の差し替えは memory のときだけ許可するため、判定が通る経路を固定する。
       *
       * @given システムプロパティ nectar.config.source が memory
       * @when 機能名 "@FixedClock" で require を呼び出す
       * @then 例外を投げず、何もしない
       */
      @Test
      void 読み込み元がmemoryの場合_何もしない() {
        assertThatCode(() -> MemoryModeGuard.INSTANCE.require("@FixedClock"))
            .doesNotThrowAnyException();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 読み込み元が memory 以外の場合は IllegalStateException を投げることを確かめる。 本番の設定を差し替える不具合を防ぐ。
       *
       * @given システムプロパティ nectar.config.source が prod
       * @when 機能名 "@FixedClock" で require を呼び出す
       * @then IllegalStateException を投げ、メッセージに nectar.config.source=memory を含む
       */
      @Test
      void 読み込み元がmemory以外の場合_IllegalStateExceptionを投げる() {
        読み込み元を変えて実行する(
            "prod",
            () ->
                assertThatThrownBy(() -> MemoryModeGuard.INSTANCE.require("@FixedClock"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("nectar.config.source=memory"));
      }
    }
  }

  @Nested
  class requireNotParallel {

    @Nested
    class 正常系 {

      /**
       * 並列実行の設定が無い場合は例外を投げないことを確かめる。 既定の直列実行で差し替えを使えるようにする。
       *
       * @given 設定値 junit.jupiter.execution.parallel.enabled が未設定
       * @when 機能名 "@TestProperty" で requireNotParallel を呼び出す
       * @then 例外を投げず、何もしない
       */
      @Test
      void 並列実行の設定が無い場合_何もしない() {
        ExtensionContext context = mock(ExtensionContext.class);
        when(context.getConfigurationParameter(PARALLEL)).thenReturn(Optional.empty());

        assertThatCode(() -> MemoryModeGuard.INSTANCE.requireNotParallel(context, "@TestProperty"))
            .doesNotThrowAnyException();
      }

      /**
       * 並列実行が無効の設定の場合は例外を投げないことを確かめる。 無効と明示された直列実行を誤って拒否する不具合を防ぐ。
       *
       * @given 設定値 junit.jupiter.execution.parallel.enabled が false
       * @when 機能名 "@TestProperty" で requireNotParallel を呼び出す
       * @then 例外を投げず、何もしない
       */
      @Test
      void 並列実行が無効の場合_何もしない() {
        ExtensionContext context = mock(ExtensionContext.class);
        when(context.getConfigurationParameter(PARALLEL)).thenReturn(Optional.of("false"));

        assertThatCode(() -> MemoryModeGuard.INSTANCE.requireNotParallel(context, "@TestProperty"))
            .doesNotThrowAnyException();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 並列実行が有効の場合は IllegalStateException を投げることを確かめる。 静的な状態を並列のテストが共有して壊れる不具合を防ぐ。
       *
       * @given 設定値 junit.jupiter.execution.parallel.enabled が true
       * @when 機能名 "@TestProperty" で requireNotParallel を呼び出す
       * @then IllegalStateException を投げ、メッセージに並列実行を含む
       */
      @Test
      void 並列実行が有効の場合_IllegalStateExceptionを投げる() {
        ExtensionContext context = mock(ExtensionContext.class);
        when(context.getConfigurationParameter(PARALLEL)).thenReturn(Optional.of("true"));

        assertThatThrownBy(
                () -> MemoryModeGuard.INSTANCE.requireNotParallel(context, "@TestProperty"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("並列実行");
      }
    }
  }

  /**
   * システムプロパティ nectar.config.source を指定した値に変えて処理を実行し、最後に memory へ戻します。
   *
   * <p>テストの後に memory へ戻すのは、利用側の surefire 設定と同じ状態を後続のテストに残すためである。
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
