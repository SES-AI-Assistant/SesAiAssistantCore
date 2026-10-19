package copel.sesproductpackage.tools.testsupport.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.util.ConfigSource;
import copel.sesproductpackage.core.util.DBConnection;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

/** {@link MockDbConnectionExtension} の単体テスト。 */
class MockDbConnectionExtensionTest {

  /** {@link MockDbConnection} を注釈に持つ、テスト用の要素。 */
  @MockDbConnection
  static final class 接続を持つクラス {}

  @Nested
  class beforeEach {

    @Nested
    class 正常系 {

      /**
       * メソッドに注釈が付いたテストで、DBConnection がテストの引数と同じ接続を返すことを確かめる。 引数として渡す接続と、Core
       * が使う接続が同じであることを守り、別の接続に差し替わる不具合を防ぐ。
       *
       * @given テストメソッドに @MockDbConnection が付いていて、引数に Connection を受け取る
       * @when DBConnection の getConnection を呼び出す
       * @then テストの引数と同じ接続を返す
       */
      @MockDbConnection
      @Test
      void 注釈が付いている場合_DBConnectionがテストの引数と同じ接続を返す(final Connection connection) throws Exception {
        assertThat(DBConnection.getConnection()).isSameAs(connection);
      }

      /**
       * 注釈が付いていない場合は接続生成関数を設定しないことを確かめる。 注釈の無いテストで、モック接続が使われる不具合を防ぐ。
       *
       * @given 接続生成関数が未設定で、要素は注釈を持たない
       * @when beforeEach を要素を持つ階層のコンテキストで呼び出す
       * @then DBConnection の getConnection が SQLException を投げ、何もしない
       */
      @Test
      void 注釈が付いていない場合_何もしない() throws Exception {
        DBConnection.setConnectionFactoryForTest(null);

        new MockDbConnectionExtension().beforeEach(contextOf(Object.class, null));

        assertThatThrownBy(DBConnection::getConnection).isInstanceOf(SQLException.class);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 読み込み元が memory 以外の場合は IllegalStateException を投げることを確かめる。 本番の接続先を差し替える不具合を防ぐ。
       *
       * @given システムプロパティ nectar.config.source が prod、要素に @MockDbConnection が付いている
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
                            new MockDbConnectionExtension()
                                .beforeEach(contextOf(接続を持つクラス.class, null)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("nectar.config.source=memory"));
      }

      /**
       * 並列実行が有効な場合は IllegalStateException を投げることを確かめる。 静的な状態を並列のテストと共有して壊れる不具合を防ぐ。
       *
       * @given システムプロパティ nectar.config.source が memory、並列実行が有効、要素に @MockDbConnection の注釈がある
       * @when beforeEach を要素を持つ階層のコンテキストで呼び出す
       * @then IllegalStateException を投げ、メッセージに並列実行を含む
       */
      @Test
      void 並列実行が有効の場合_IllegalStateExceptionを投げる() {
        ExtensionContext context = contextOf(接続を持つクラス.class, null);
        when(context.getConfigurationParameter("junit.jupiter.execution.parallel.enabled"))
            .thenReturn(Optional.of("true"));

        assertThatThrownBy(() -> new MockDbConnectionExtension().beforeEach(context))
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
       * 注釈が付いたテストの後は接続生成関数を削除することを確かめる。 モック接続が次のテストに残り、接続の結果が食い違う不具合を防ぐ。
       *
       * @given 接続生成関数にモック接続を設定済みで、要素に @MockDbConnection が付いている
       * @when afterEach を要素を持つ階層のコンテキストで呼び出す
       * @then DBConnection の getConnection が SQLException を投げる
       */
      @Test
      void 注釈が付いている場合_接続生成関数を削除する() {
        Connection connection = mock(Connection.class);
        DBConnection.setConnectionFactoryForTest(() -> connection);

        new MockDbConnectionExtension().afterEach(contextOf(接続を持つクラス.class, null));

        assertThatThrownBy(DBConnection::getConnection).isInstanceOf(SQLException.class);
      }

      /**
       * 注釈が付いていない場合は接続生成関数に触れないことを確かめる。 注釈の無いテストが、別のテストで設定した接続生成関数を消す不具合を防ぐ。
       *
       * @given 接続生成関数にモック接続を設定済みで、要素は注釈を持たない
       * @when afterEach を要素を持つ階層のコンテキストで呼び出す
       * @then DBConnection の getConnection が設定したモック接続を返し、何もしない
       */
      @Test
      void 注釈が付いていない場合_何もしない() throws Exception {
        Connection connection = mock(Connection.class);
        DBConnection.setConnectionFactoryForTest(() -> connection);
        try {
          new MockDbConnectionExtension().afterEach(contextOf(Object.class, null));

          assertThat(DBConnection.getConnection()).isSameAs(connection);
        } finally {
          DBConnection.setConnectionFactoryForTest(null);
        }
      }
    }

    @Nested
    class 境界値 {

      /**
       * 読み込み元が memory 以外の場合は、接続生成関数を削除しないことを確かめる。 memory 以外では設定していないため、削除の操作で例外を投げる不具合を防ぐ。
       *
       * @given 接続生成関数にモック接続を設定済みで、システムプロパティ nectar.config.source が prod、要素に @MockDbConnection
       *     が付いている
       * @when afterEach を要素を持つ階層のコンテキストで呼び出す
       * @then 例外を投げず、何もしない
       */
      @Test
      void 読み込み元がmemory以外の場合_何もしない() throws Exception {
        Connection connection = mock(Connection.class);
        DBConnection.setConnectionFactoryForTest(() -> connection);
        try {
          読み込み元を変えて実行する(
              "prod",
              () ->
                  assertThatCode(
                          () ->
                              new MockDbConnectionExtension()
                                  .afterEach(contextOf(接続を持つクラス.class, null)))
                      .doesNotThrowAnyException());

          assertThat(DBConnection.getConnection()).isSameAs(connection);
        } finally {
          DBConnection.setConnectionFactoryForTest(null);
        }
      }
    }
  }

  @Nested
  class supportsParameter {

    @Nested
    class 正常系 {

      /**
       * 引数の型が Connection の場合は、この拡張が解決することを確かめる。 テストの引数に接続を受け取れるようにする。
       *
       * @given 引数の型が java.sql.Connection
       * @when supportsParameter を呼び出す
       * @then true を返す
       */
      @Test
      void 引数の型がConnectionの場合_trueを返す() {
        assertThat(
                new MockDbConnectionExtension()
                    .supportsParameter(parameterOf(Connection.class), null))
            .isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 引数の型が Connection 以外の場合は、この拡張が解決しないことを確かめる。 文字列の引数を接続として渡す不具合を防ぐ。
       *
       * @given 引数の型が java.lang.String
       * @when supportsParameter を呼び出す
       * @then false を返す
       */
      @Test
      void 引数の型がConnectionでない場合_falseを返す() {
        assertThat(
                new MockDbConnectionExtension().supportsParameter(parameterOf(String.class), null))
            .isFalse();
      }
    }
  }

  @Nested
  class resolveParameter {

    @Nested
    class 正常系 {

      /**
       * 接続が保持されている場合は、保持している接続を返すことを確かめる。 テストの前に作ったモック接続を、そのまま引数に渡す。
       *
       * @given ストアがモック接続を保持している
       * @when resolveParameter を呼び出す
       * @then 保持しているモック接続を返す
       */
      @Test
      void 接続が保持されている場合_保持している接続を返す() {
        Connection connection = mock(Connection.class);
        ExtensionContext context = mock(ExtensionContext.class);
        ExtensionContext.Store store = mock(ExtensionContext.Store.class);
        when(context.getStore(any())).thenReturn(store);
        when(store.get(any())).thenReturn(connection);

        Object resolved =
            new MockDbConnectionExtension().resolveParameter(mock(ParameterContext.class), context);

        assertThat(resolved).isSameAs(connection);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 接続が保持されていない場合は ParameterResolutionException を投げることを確かめる。 接続が無いまま null を引数に渡す不具合を防ぐ。
       *
       * @given ストアが接続を保持していない
       * @when resolveParameter を呼び出す
       * @then ParameterResolutionException を投げる
       */
      @Test
      void 接続が保持されていない場合_ParameterResolutionExceptionを投げる() {
        ExtensionContext context = mock(ExtensionContext.class);
        ExtensionContext.Store store = mock(ExtensionContext.Store.class);
        when(context.getStore(any())).thenReturn(store);

        assertThatThrownBy(
                () ->
                    new MockDbConnectionExtension()
                        .resolveParameter(mock(ParameterContext.class), context))
            .isInstanceOf(ParameterResolutionException.class);
      }
    }
  }

  /**
   * 指定した型を持つ引数の情報を、テスト用のモックとして作ります。
   *
   * @param type 引数の型
   * @return 引数の情報のモック
   */
  private static ParameterContext parameterOf(final Class<?> type) {
    Parameter parameter = mock(Parameter.class);
    doReturn(type).when(parameter).getType();
    ParameterContext parameterContext = mock(ParameterContext.class);
    when(parameterContext.getParameter()).thenReturn(parameter);
    return parameterContext;
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
