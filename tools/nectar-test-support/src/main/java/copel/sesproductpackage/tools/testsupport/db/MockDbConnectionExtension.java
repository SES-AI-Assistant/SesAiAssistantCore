package copel.sesproductpackage.tools.testsupport.db;

import copel.sesproductpackage.core.util.ConfigSource;
import copel.sesproductpackage.core.util.DBConnection;
import copel.sesproductpackage.tools.testsupport.ExtensionScope;
import copel.sesproductpackage.tools.testsupport.MemoryModeGuard;
import java.sql.Connection;
import java.util.Optional;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;
import org.mockito.Mockito;

/**
 * {@link MockDbConnection} の注釈に従い、Core の {@code DBConnection} がモック接続を返すように設定する JUnit 拡張です。
 *
 * <p>テストの前に、Mockito のモック接続を作って保持し、接続生成関数として設定する。テストメソッドの {@link Connection} 型の引数には、同じモック接続を渡す。
 * 接続生成関数の設定は読み込み元が memory のときだけ許可されるため、先に {@link MemoryModeGuard} で判定する。
 *
 * <p>モック接続は Mockito の既定の応答を返す。SQL の結果は利用側が {@link java.sql.PreparedStatement} のモックを組み合わせて与える。
 *
 * @author Copel Co., Ltd.
 */
public class MockDbConnectionExtension
    implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

  /** モック接続を保持するストアの名前空間。拡張のクラスごとに分ける。 */
  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(MockDbConnectionExtension.class);

  /** モック接続をストアに保持するときのキー。 */
  private static final String CONNECTION_KEY = "connection";

  /**
   * テストの前に、モック接続を作って保持し、Core の接続生成関数として設定します。
   *
   * <p>注釈が無い場合は何もしない。注釈がある場合は、読み込み元が memory であることを確かめてから設定する。
   *
   * @param context テストの実行コンテキスト
   */
  @Override
  public void beforeEach(final ExtensionContext context) {
    if (!isAnnotated(context)) {
      return;
    }
    MemoryModeGuard.INSTANCE.require("@MockDbConnection");
    MemoryModeGuard.INSTANCE.requireNotParallel(context, "@MockDbConnection");
    Connection connection = Mockito.mock(Connection.class);
    context.getStore(NAMESPACE).put(CONNECTION_KEY, connection);
    DBConnection.setConnectionFactoryForTest(() -> connection);
  }

  /**
   * テストの後に、Core の接続生成関数を解除します。
   *
   * <p>注釈が無い場合は何もしない。読み込み元が memory でない場合は、設定もされていないため解除しない。
   *
   * @param context テストの実行コンテキスト
   */
  @Override
  public void afterEach(final ExtensionContext context) {
    if (isAnnotated(context) && ConfigSource.INSTANCE.isMemory()) {
      DBConnection.setConnectionFactoryForTest(null);
    }
  }

  /**
   * 引数の型が {@link Connection} の場合に、この拡張が解決することを返します。
   *
   * @param parameterContext 解決する引数の情報
   * @param extensionContext テストの実行コンテキスト
   * @return 引数の型が {@link Connection} の場合は true、それ以外は false
   * @throws ParameterResolutionException 判定中に例外が起きた場合
   */
  @Override
  public boolean supportsParameter(
      final ParameterContext parameterContext, final ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType() == Connection.class;
  }

  /**
   * テストの前に保持したモック接続を、引数として返します。
   *
   * <p>テストの前に設定されていない場合は、引数を解決できないため例外を投げる。
   *
   * @param parameterContext 解決する引数の情報
   * @param extensionContext テストの実行コンテキスト
   * @return テストの前に作ったモック接続
   * @throws ParameterResolutionException モック接続が保持されていない場合
   */
  @Override
  public Object resolveParameter(
      final ParameterContext parameterContext, final ExtensionContext extensionContext)
      throws ParameterResolutionException {
    Object connection = extensionContext.getStore(NAMESPACE).get(CONNECTION_KEY);
    if (connection == null) {
      throw new ParameterResolutionException("モック接続が保持されていません。@MockDbConnection をテストに付けてください。");
    }
    return connection;
  }

  /**
   * テストの実行階層のどこかに {@link MockDbConnection} が付いているかを返します。
   *
   * <p>テストメソッド、{@code @Nested} クラス、外側のテストクラスの順に探す。
   *
   * @param context テストの実行コンテキスト
   * @return 付いている場合は true、それ以外は false
   */
  private static boolean isAnnotated(final ExtensionContext context) {
    return ExtensionScope.INSTANCE.innermostFirst(context).stream()
        .map(element -> AnnotationSupport.findAnnotation(element, MockDbConnection.class))
        .anyMatch(Optional::isPresent);
  }
}
