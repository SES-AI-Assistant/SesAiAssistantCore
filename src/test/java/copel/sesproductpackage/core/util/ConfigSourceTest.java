package copel.sesproductpackage.core.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link ConfigSource} の単体テスト。
 *
 * <p>環境変数 {@link ConfigSource#LAMBDA_FUNCTION_NAME_ENV} は直接変更できないため、Lambda 上かどうかの判定は関数名を引数に取る
 * {@code isMemory(String)} で確かめる。
 *
 * @author Copel Co., Ltd.
 */
class ConfigSourceTest {

  /** テストの前に設定読み込み元のプロパティの値を退避する。 */
  private String originalConfigSource;

  /** テストの前に設定読み込み元のプロパティの値を退避する。 */
  @BeforeEach
  void setUp() {
    originalConfigSource = System.getProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
  }

  /** テストの後に設定読み込み元のプロパティを復元する。後続のテストの設定が変わらないようにする。 */
  @AfterEach
  void tearDown() {
    if (originalConfigSource == null) {
      System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
    } else {
      System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, originalConfigSource);
    }
  }

  /** {@link ConfigSource#isMemory(String)} のテスト。 */
  @Nested
  class isMemory {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * ConfigSource#isMemory がシステムプロパティが memory で Lambda の関数名がないときに true を返すことを確かめる。 テスト実行時だけ
       * memory の判定を通すことで、本番の経路を守る。
       *
       * @given システムプロパティ "nectar.config.source" が "memory" で、Lambda の関数名が null である
       * @when isMemory を Lambda の関数名 null で呼び出す
       * @then true を返す
       */
      @Test
      void システムプロパティがmemoryでLambdaの関数名がない場合_trueを返す() {
        System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, ConfigSource.CONFIG_SOURCE_MEMORY);

        assertTrue(ConfigSource.INSTANCE.isMemory(null));
      }

      /**
       * ConfigSource#isMemory がLambdaの関数名が空文字のときは Lambda 上とみなさないことを確かめる。 環境変数が空で設定されている環境を、Lambda
       * と誤判定しない。
       *
       * @given システムプロパティ "nectar.config.source" が "memory" で、Lambda の関数名が空文字である
       * @when isMemory を Lambda の関数名 "" で呼び出す
       * @then true を返す
       */
      @Test
      void Lambdaの関数名が空文字の場合_trueを返す() {
        System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, ConfigSource.CONFIG_SOURCE_MEMORY);

        assertTrue(ConfigSource.INSTANCE.isMemory(""));
      }
    }

    /** 認可のテスト。 */
    @Nested
    class 認可 {

      /**
       * ConfigSource#isMemory がLambda上ではシステムプロパティが memory でも false を返すことを確かめる。 Lambda で誤って memory
       * を設定しても、設定値が空のまま起動することを防ぐ。
       *
       * @given システムプロパティ "nectar.config.source" が "memory" で、Lambda の関数名が "nectar-web-api" である
       * @when isMemory を Lambda の関数名 "nectar-web-api" で呼び出す
       * @then false を返す
       */
      @Test
      void Lambdaの関数名がある場合_falseを返す() {
        System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, ConfigSource.CONFIG_SOURCE_MEMORY);

        assertFalse(ConfigSource.INSTANCE.isMemory("nectar-web-api"));
      }
    }

    /** 境界値のテスト。 */
    @Nested
    class 境界値 {

      /**
       * ConfigSource#isMemory がシステムプロパティが未設定のとき false を返すことを確かめる。 未設定は本番の実行環境であり、memory
       * として扱うと本番の読み込みが止まる。
       *
       * @given システムプロパティ "nectar.config.source" が未設定で、Lambda の関数名が null である
       * @when isMemory を Lambda の関数名 null で呼び出す
       * @then false を返す
       */
      @Test
      void システムプロパティが未設定の場合_falseを返す() {
        System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);

        assertFalse(ConfigSource.INSTANCE.isMemory(null));
      }

      /**
       * ConfigSource#isMemory が大文字の値を memory として扱わないことを確かめる。 大文字小文字を区別しないと、意図しない表記で本番の判定が変わる。
       *
       * @given システムプロパティ "nectar.config.source" が "MEMORY" で、Lambda の関数名が null である
       * @when isMemory を Lambda の関数名 null で呼び出す
       * @then false を返す
       */
      @Test
      void システムプロパティがMEMORYの場合_falseを返す() {
        System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, "MEMORY");

        assertFalse(ConfigSource.INSTANCE.isMemory(null));
      }
    }
  }

  /** {@link ConfigSource#requireMemory(String)} のテスト。 */
  @Nested
  class requireMemory {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * ConfigSource#requireMemory がシステムプロパティが memory のとき何もしないことを確かめる。 テスト専用の操作を memory
       * で実行できることを保証する。
       *
       * @given システムプロパティ "nectar.config.source" が "memory" で、テスト実行環境に Lambda の関数名がない
       * @when requireMemory を操作名 "テスト操作" で呼び出す
       * @then 例外を投げず、何もしない
       */
      @Test
      void システムプロパティがmemoryの場合_何もしない() {
        System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, ConfigSource.CONFIG_SOURCE_MEMORY);

        assertDoesNotThrow(() -> ConfigSource.INSTANCE.requireMemory("テスト操作"));
      }
    }

    /** 認可のテスト。 */
    @Nested
    class 認可 {

      /**
       * ConfigSource#requireMemory がシステムプロパティが未設定のとき例外を投げることを確かめる。 本番でテスト専用の操作を通すと、設定値や接続先が差し替わる。
       *
       * @given システムプロパティ "nectar.config.source" が未設定である
       * @when requireMemory を操作名 "テスト操作" で呼び出す
       * @then IllegalStateException を投げ、そのメッセージに "テスト操作" を含む
       */
      @Test
      void システムプロパティが未設定の場合_IllegalStateExceptionを投げる() {
        System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);

        IllegalStateException exception =
            assertThrows(
                IllegalStateException.class, () -> ConfigSource.INSTANCE.requireMemory("テスト操作"));
        assertTrue(exception.getMessage().contains("テスト操作"));
      }

      /**
       * ConfigSource#requireMemory がシステムプロパティが file のとき例外を投げることを確かめる。 memory
       * 以外の読み込み元では、テスト専用の操作を拒否する仕様である。
       *
       * @given システムプロパティ "nectar.config.source" が "file" である
       * @when requireMemory を操作名 "テスト操作" で呼び出す
       * @then IllegalStateException を投げる
       */
      @Test
      void システムプロパティがfileの場合_IllegalStateExceptionを投げる() {
        System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, "file");

        assertThrows(
            IllegalStateException.class, () -> ConfigSource.INSTANCE.requireMemory("テスト操作"));
      }
    }
  }
}
