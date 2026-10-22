package copel.sesproductpackage.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

/** {@link ConfigSource} の単体テスト。 */
class ConfigSourceTest {

  /** テスト前の読み込み元の設定値。テスト後に元へ戻すために保存する。 */
  private String 読み込み元の設定値;

  /** WARN ログを記録するアペンダー。 */
  private ListAppender<ILoggingEvent> logs;

  @BeforeEach
  void 設定値とログを準備する() {
    読み込み元の設定値 = System.getProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
    logs = new ListAppender<>();
    logs.start();
    ((Logger) LoggerFactory.getLogger(ConfigSource.class)).addAppender(logs);
  }

  @AfterEach
  void 設定値とログを元に戻す() {
    ((Logger) LoggerFactory.getLogger(ConfigSource.class)).detachAppender(logs);
    if (読み込み元の設定値 == null) {
      System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
    } else {
      System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, 読み込み元の設定値);
    }
  }

  /**
   * 読み込み元の設定値を指定の値にする。
   *
   * @param value 設定する読み込み元の値
   */
  private static void 読み込み元を設定する(final String value) {
    System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, value);
  }

  /**
   * 警告ログの件数を数える。
   *
   * @return 警告ログの件数
   */
  private int 警告の件数() {
    return (int) logs.list.stream().filter(event -> event.getLevel() == Level.WARN).count();
  }

  @Nested
  class isMemory {

    @Nested
    class 正常系 {

      /**
       * 読み込み元が memory で Lambda 上でない場合は true を返すことを確かめる。 memory を判定しないとテスト用の設定がオフラインで使えないので、true
       * を返す。
       *
       * @given 読み込み元が memory、AWS_LAMBDA_FUNCTION_NAME が未設定
       * @when 引数なしの isMemory を実行する
       * @then true を返す
       */
      @Test
      void 読み込み元がmemoryの場合_trueを返す() {
        読み込み元を設定する(ConfigSource.CONFIG_SOURCE_MEMORY);

        assertThat(ConfigSource.INSTANCE.isMemory()).isTrue();
      }
    }
  }

  @Nested
  class isMemory_String {

    @Nested
    class 正常系 {

      /**
       * 読み込み元が memory で Lambda 関数名が無い場合は true を返すことを確かめる。 Lambda
       * 関数名が無い場合の判定を変えるとテスト環境の判定が変わるので、true を固定する。
       *
       * @given 読み込み元が memory、Lambda 関数名が null
       * @when isMemory を Lambda 関数名 null で実行する
       * @then true を返す
       */
      @Test
      void 読み込み元がmemoryでLambda関数名がない場合_trueを返す() {
        読み込み元を設定する(ConfigSource.CONFIG_SOURCE_MEMORY);

        assertThat(ConfigSource.INSTANCE.isMemory(null)).isTrue();
      }

      /**
       * 読み込み元が memory で 2 回続けて判定した場合、2 回目は警告を出さないことを確かめる。 警告ログが毎回出ると埋もれてしまうので、2 回目の判定では出さない。
       *
       * @given 読み込み元が memory、Lambda 関数名が null
       * @when isMemory を 2 回続けて実行する
       * @then 2 回目の判定で警告ログを増やさない
       */
      @Test
      void 二回続けて判定した場合_何もしない() {
        読み込み元を設定する(ConfigSource.CONFIG_SOURCE_MEMORY);
        ConfigSource.INSTANCE.isMemory(null);
        int before = 警告の件数();

        ConfigSource.INSTANCE.isMemory(null);

        assertThat(警告の件数() - before).isZero();
      }
    }

    @Nested
    class 異常系 {

      /**
       * Lambda 上で動いている場合は memory を無効にすることを確かめる。 Lambda 上でテスト用の設定が有効になると本番の設定が読まれなくなるので、false を返す。
       *
       * @given 読み込み元が memory、Lambda 関数名が "ses-info-register"
       * @when isMemory を Lambda 関数名で実行する
       * @then false を返す
       */
      @Test
      void Lambda関数名がある場合_falseを返す() {
        読み込み元を設定する(ConfigSource.CONFIG_SOURCE_MEMORY);

        assertThat(ConfigSource.INSTANCE.isMemory("ses-info-register")).isFalse();
      }

      /**
       * 読み込み元が本番の場合は memory ではないと判定することを確かめる。 本番の読み込み経路を変えると本番の設定が誤って読まれなくなるので、false を固定する。
       *
       * @given 読み込み元が "production"
       * @when isMemory を Lambda 関数名 null で実行する
       * @then false を返す
       */
      @Test
      void 読み込み元が本番の場合_falseを返す() {
        読み込み元を設定する("production");

        assertThat(ConfigSource.INSTANCE.isMemory(null)).isFalse();
      }

      /**
       * 読み込み元の値が memory と大文字小文字だけ異なる場合は memory ではないと判定することを確かめる。 完全一致以外を memory
       * と判定すると誤った切り替えが起きるので、大文字は false にする。
       *
       * @given 読み込み元が "MEMORY"
       * @when isMemory を Lambda 関数名 null で実行する
       * @then false を返す
       */
      @Test
      void 読み込み元の値が大文字の場合_falseを返す() {
        読み込み元を設定する("MEMORY");

        assertThat(ConfigSource.INSTANCE.isMemory(null)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * Lambda 関数名が空文字の場合は Lambda 上ではないとみなし true を返すことを確かめる。 空の関数名を Lambda
       * 上と誤判定するとテスト設定が使えなくなるので、true にする。
       *
       * @given 読み込み元が memory、Lambda 関数名が空文字
       * @when isMemory を空文字で実行する
       * @then true を返す
       */
      @ValueSource(strings = {""})
      @ParameterizedTest
      void Lambda関数名が空文字の場合_trueを返す(final String name) {
        読み込み元を設定する(ConfigSource.CONFIG_SOURCE_MEMORY);

        assertThat(ConfigSource.INSTANCE.isMemory(name)).isTrue();
      }

      /**
       * 読み込み元が未設定の場合は memory ではないと判定することを確かめる。 未設定時の既定の読み込み経路を変えると本番の挙動が変わるので、false を固定する。
       *
       * @given 読み込み元が未設定
       * @when isMemory を Lambda 関数名 null で実行する
       * @then false を返す
       */
      @Test
      void 読み込み元が未設定の場合_falseを返す() {
        System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);

        assertThat(ConfigSource.INSTANCE.isMemory(null)).isFalse();
      }
    }
  }

  @Nested
  class requireMemory {

    @Nested
    class 正常系 {

      /**
       * 読み込み元が memory の場合は例外を投げずに終わることを確かめる。 テスト専用の操作を memory 以外で拒否しないと本番の設定が書き換わるので、許可する。
       *
       * @given 読み込み元が memory
       * @when requireMemory を操作名つきで実行する
       * @then 何もしない
       */
      @Test
      void 読み込み元がmemoryの場合_何もしない() {
        読み込み元を設定する(ConfigSource.CONFIG_SOURCE_MEMORY);

        assertThatCode(() -> ConfigSource.INSTANCE.requireMemory("テスト操作"))
            .doesNotThrowAnyException();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 読み込み元が本番の場合は操作名を含む例外を投げることを確かめる。 テスト専用の操作が本番で実行されると設定が変わるので、操作名つきで拒否する。
       *
       * @given 読み込み元が本番で memory 以外
       * @when requireMemory を操作名 "テスト操作" で実行する
       * @then IllegalStateException を投げる
       */
      @Test
      void 読み込み元が本番の場合_IllegalStateExceptionを投げる() {
        読み込み元を設定する("production");

        assertThatThrownBy(() -> ConfigSource.INSTANCE.requireMemory("テスト操作"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("テスト操作");
      }
    }
  }
}
