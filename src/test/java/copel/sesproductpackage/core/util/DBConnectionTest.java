package copel.sesproductpackage.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.tools.testsupport.clock.FixedClock;
import copel.sesproductpackage.tools.testsupport.config.TestProperty;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/** {@link DBConnection} の単体テスト。 */
class DBConnectionTest {

  /** テスト前の読み込み元の設定値。テスト後に元へ戻すために保存する。 */
  private String 読み込み元の設定値;

  /** 接続のモック。 */
  private Connection connection;

  @BeforeEach
  void 設定値と接続のモックを準備する() {
    読み込み元の設定値 = System.getProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
    connection = mock(Connection.class);
    DBConnection.setConnectionFactoryForTest(null);
  }

  @AfterEach
  void 接続の状態を元に戻す() throws ReflectiveOperationException {
    if (読み込み元の設定値 == null) {
      System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
    } else {
      System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, 読み込み元の設定値);
    }
    DBConnection.setConnectionFactoryForTest(null);
    接続情報を消す();
  }

  /** 検証用の接続先の URL。接頭辞 "jdbc:nectar-ut:" で始まり、検証用ドライバだけが受け付ける。 */
  private static final String TEST_URL = "jdbc:nectar-ut:test";

  /**
   * 接続情報を初期化し直したときに組み立てられる接続先の URL。検証用ドライバが受け付ける。
   *
   * <p>接続の待ち時間を 5 秒にする connectTimeout が付くことも、このドライバが受け付けるかどうかで確かめる。
   */
  private static final String 初期化し直した接続先のURL =
      "jdbc:postgresql://127.0.0.1:1/nectar?connectTimeout=5";

  /**
   * 接続先の URL が検証用の接頭辞の場合に、渡されたモックの接続を返す JDBC ドライバ。
   *
   * <p>本番経路（{@link DriverManager} で接続する経路）を、実際の DB に接続せずに最後まで実行するために使う。
   */
  private static final class 検証用ドライバ implements Driver {

    /** 返すモックの接続。 */
    private final Connection connection;

    /**
     * 検証用ドライバを生成する。
     *
     * @param connection 返すモックの接続
     */
    検証用ドライバ(final Connection connection) {
      this.connection = connection;
    }

    @Override
    public Connection connect(final String url, final java.util.Properties info) {
      return acceptsURL(url) ? connection : null;
    }

    @Override
    public boolean acceptsURL(final String url) {
      return url.startsWith("jdbc:nectar-ut:") || url.equals(初期化し直した接続先のURL);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(final String url, final java.util.Properties info) {
      return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
      return 1;
    }

    @Override
    public int getMinorVersion() {
      return 0;
    }

    @Override
    public boolean jdbcCompliant() {
      return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
      throw new SQLFeatureNotSupportedException("未対応");
    }
  }

  /**
   * DBConnection の静的フィールドに値を設定する。本番の読み込みを経ずに接続先を指定するために使う。
   *
   * @param name フィールド名
   * @param value 設定する値
   * @throws ReflectiveOperationException フィールドへのアクセスに失敗した場合
   */
  private static void 静的フィールドを設定する(final String name, final Object value)
      throws ReflectiveOperationException {
    Field field = DBConnection.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(null, value);
  }

  /**
   * テストで設定された接続情報の URL、ユーザー名、パスワードを未設定に戻す。 本番の読み込みを検証するテストが、後続のテストの接続先を変えないようにするための後始末である。
   *
   * @throws ReflectiveOperationException フィールドへのアクセスに失敗した場合
   */
  private static void 接続情報を消す() throws ReflectiveOperationException {
    for (String name : List.of("url", "userName", "password")) {
      Field field = DBConnection.class.getDeclaredField(name);
      field.setAccessible(true);
      field.set(null, null);
    }
    静的フィールドを設定する("lastInitializeAttemptTime", 0L);
  }

  /**
   * 時計を指定の時刻に進めて、読み込み元を本番に戻す。
   *
   * <p>時計の差し替えは読み込み元が memory のときだけできるため、差し替えの間だけ memory にする。再試行の間隔の判定を、実際に待たずに確かめるために使う。
   *
   * @param instant 設定する時刻
   */
  private static void 時計を進めて本番に戻す(final Instant instant) {
    読み込み元を設定する(ConfigSource.CONFIG_SOURCE_MEMORY);
    ApplicationClock.INSTANCE.setClock(Clock.fixed(instant, ZoneId.of("Asia/Tokyo")));
    読み込み元を設定する("production");
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
   * Secrets Manager のクライアントの構築を、モックのクライアントを返すように差し替える。
   *
   * @param client 差し替えるクライアントのモック
   * @return 差し替えを解除するためのハンドル
   */
  private static MockedStatic<SecretsManagerClient> SecretsManagerを差し替える(
      final SecretsManagerClient client) {
    SecretsManagerClientBuilder builder = mock(SecretsManagerClientBuilder.class, RETURNS_SELF);
    when(builder.build()).thenReturn(client);
    MockedStatic<SecretsManagerClient> statics = mockStatic(SecretsManagerClient.class);
    statics.when(SecretsManagerClient::builder).thenReturn(builder);
    return statics;
  }

  @Nested
  class getConnection {

    @Nested
    class 正常系 {

      /**
       * 接続生成関数が設定されている場合、その接続を返し自動コミットを無効にすることを確かめる。
       * テストで差し替えた接続を本番と同じ手順で扱わないとテストの意味が変わるので、本番の手順を通す。
       *
       * @given 読み込み元が memory、接続生成関数が接続のモックを返す
       * @when getConnection を実行する
       * @then 接続のモックを返し、自動コミットを無効にする
       * @throws Exception 接続の取得に失敗した場合
       */
      @Test
      void 接続生成関数が設定されている場合_接続を返す() throws Exception {
        DBConnection.setConnectionFactoryForTest(() -> connection);

        assertThat(DBConnection.getConnection()).isSameAs(connection);
        verify(connection).setAutoCommit(false);
      }

      /**
       * 読み込み元が本番で接続先の URL が設定されている場合、JDBC ドライバの接続を返すことを確かめる。
       * 本番の接続経路を最後まで通さないと接続の分岐が検証できないので、検証用のドライバを使う。
       *
       * @given 読み込み元が本番、接続先の URL が検証用の URL、検証用ドライバが接続のモックを返す
       * @when getConnection を実行する
       * @then 接続のモックを返し、自動コミットを無効にする
       * @throws Exception 接続の取得に失敗した場合
       */
      @Test
      void 読み込み元が本番でURLが設定されている場合_検証用の接続を返す() throws Exception {
        読み込み元を設定する("production");
        静的フィールドを設定する("url", TEST_URL);
        Driver driver = new 検証用ドライバ(connection);
        DriverManager.registerDriver(driver);
        try {
          assertThat(DBConnection.getConnection()).isSameAs(connection);
          verify(connection).setAutoCommit(false);
        } finally {
          DriverManager.deregisterDriver(driver);
        }
      }

      /**
       * 接続先の URL が未設定で前回の試行から1秒が経った場合は、接続情報を初期化し直して接続を返すことを確かめる。 起動時の初期化に失敗した実行環境が、DB
       * に接続できないまま使われ続けるのを防ぐためである。
       *
       * @given 読み込み元が本番、Secrets Manager が1回目は例外を投げ、2回目は host "127.0.0.1" の接続情報を返す
       * @when getConnection を実行し、1000 ミリ秒後にもう一度実行する
       * @then 1回目は SQLException を投げ、2回目は connectTimeout が 5 の URL で接続のモックを返し、Secrets Manager を計2回呼ぶ
       * @throws Exception 接続の取得に失敗した場合
       */
      @FixedClock("2026-01-15T12:00:00+09:00")
      @TestProperty(
          key = "infrastructure/rds/database/secret-arn",
          value = "arn:test:dbconnection-retry")
      @Test
      void URLが未設定で前回の試行から1000ミリ秒の場合_初期化し直して接続を返す() throws Exception {
        読み込み元を設定する("production");
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(ArgumentMatchers.<Consumer<GetSecretValueRequest.Builder>>any()))
            .thenThrow(new RuntimeException("取得失敗"))
            .thenReturn(
                GetSecretValueResponse.builder()
                    .secretString(
                        "{\"host\":\"127.0.0.1\",\"port\":\"1\","
                            + "\"dbname\":\"nectar\",\"username\":\"user1\",\"password\":\"pw1\"}")
                    .build());
        Driver driver = new 検証用ドライバ(connection);
        DriverManager.registerDriver(driver);
        try (MockedStatic<SecretsManagerClient> statics = SecretsManagerを差し替える(client)) {
          assertThatThrownBy(DBConnection::getConnection).isInstanceOf(SQLException.class);
          時計を進めて本番に戻す(Instant.parse("2026-01-15T03:00:01Z"));

          assertThat(DBConnection.getConnection()).isSameAs(connection);

          verify(client, times(2))
              .getSecretValue(ArgumentMatchers.<Consumer<GetSecretValueRequest.Builder>>any());
          verify(connection).setAutoCommit(false);
        } finally {
          DriverManager.deregisterDriver(driver);
        }
      }
    }

    @Nested
    class 異常系 {

      /**
       * 接続生成関数が未設定の場合は SQLException を投げることを確かめる。 テストで実際の DB に接続すると外部環境に依存するので、接続生成関数を使う。
       *
       * @given 読み込み元が memory、接続生成関数が未設定
       * @when getConnection を実行する
       * @then SQLException を投げる
       */
      @Test
      void 接続生成関数が未設定の場合_SQLExceptionを投げる() {
        assertThatThrownBy(DBConnection::getConnection).isInstanceOf(SQLException.class);
      }

      /**
       * 接続生成関数が null を返した場合は SQLException を投げることを確かめる。 接続が無いまま処理を進めると後続で例外が起きるので、接続を拒否する。
       *
       * @given 読み込み元が memory、接続生成関数が null を返す
       * @when getConnection を実行する
       * @then SQLException を投げる
       */
      @Test
      void 接続生成関数がnullを返す場合_SQLExceptionを投げる() {
        DBConnection.setConnectionFactoryForTest(() -> null);

        assertThatThrownBy(DBConnection::getConnection).isInstanceOf(SQLException.class);
      }

      /**
       * 接続先の URL が未設定で前回の試行から1秒が経つ直前は、初期化し直さずに SQLException を投げることを確かめる。 失敗し続けている間に呼び出しのたびに Secrets
       * Manager を呼ぶと、失敗の原因の負荷をさらに増やす。
       *
       * @given 読み込み元が本番、Secrets Manager の取得が例外を投げる
       * @when getConnection を実行し、999 ミリ秒後にもう一度実行する
       * @then どちらも DB 接続情報を初期化できていない旨の SQLException を投げ、Secrets Manager を計1回だけ呼ぶ
       * @throws Exception 接続の取得に失敗した場合
       */
      @FixedClock("2026-01-15T12:00:00+09:00")
      @TestProperty(
          key = "infrastructure/rds/database/secret-arn",
          value = "arn:test:dbconnection-retry-interval")
      @Test
      void URLが未設定で前回の試行から999ミリ秒の場合_SQLExceptionを投げる() throws Exception {
        読み込み元を設定する("production");
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(ArgumentMatchers.<Consumer<GetSecretValueRequest.Builder>>any()))
            .thenThrow(new RuntimeException("取得失敗"));

        try (MockedStatic<SecretsManagerClient> statics = SecretsManagerを差し替える(client)) {
          assertThatThrownBy(DBConnection::getConnection).isInstanceOf(SQLException.class);
          時計を進めて本番に戻す(Instant.parse("2026-01-15T03:00:00.999Z"));

          assertThatThrownBy(DBConnection::getConnection)
              .isInstanceOf(SQLException.class)
              .hasMessageContaining("DB 接続情報を初期化できていません");

          verify(client, times(1))
              .getSecretValue(ArgumentMatchers.<Consumer<GetSecretValueRequest.Builder>>any());
        }
      }

      /**
       * 接続先の URL が未設定で初期化し直しも失敗した場合は、SQLException を投げることを確かめる。 接続情報が無いまま接続すると、原因の分からない例外になる。
       *
       * @given 読み込み元が本番、Secrets Manager の取得が例外を投げる
       * @when getConnection を実行し、1000 ミリ秒後にもう一度実行する
       * @then 2回目も DB 接続情報を初期化できていない旨の SQLException を投げ、Secrets Manager を計2回呼ぶ
       * @throws Exception 接続の取得に失敗した場合
       */
      @FixedClock("2026-01-15T12:00:00+09:00")
      @TestProperty(
          key = "infrastructure/rds/database/secret-arn",
          value = "arn:test:dbconnection-retry-failure")
      @Test
      void URLが未設定で初期化し直しも失敗する場合_SQLExceptionを投げる() throws Exception {
        読み込み元を設定する("production");
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(ArgumentMatchers.<Consumer<GetSecretValueRequest.Builder>>any()))
            .thenThrow(new RuntimeException("取得失敗"));

        try (MockedStatic<SecretsManagerClient> statics = SecretsManagerを差し替える(client)) {
          assertThatThrownBy(DBConnection::getConnection).isInstanceOf(SQLException.class);
          時計を進めて本番に戻す(Instant.parse("2026-01-15T03:00:01Z"));

          assertThatThrownBy(DBConnection::getConnection)
              .isInstanceOf(SQLException.class)
              .hasMessageContaining("DB 接続情報を初期化できていません");

          verify(client, times(2))
              .getSecretValue(ArgumentMatchers.<Consumer<GetSecretValueRequest.Builder>>any());
        }
      }
    }
  }

  @Nested
  class setConnectionFactoryForTest {

    @Nested
    class 正常系 {

      /**
       * 読み込み元が memory の場合は接続生成関数を登録できることを確かめる。 テストから接続の取得元を差し替えないと DB なしで動かせないので、登録口を使う。
       *
       * @given 読み込み元が memory、接続生成関数が接続のモックを返す
       * @when setConnectionFactoryForTest を接続生成関数で実行する
       * @then getConnection が接続のモックを返す
       * @throws Exception 接続の取得に失敗した場合
       */
      @Test
      void 読み込み元がmemoryの場合_接続生成関数を登録する() throws Exception {
        DBConnection.setConnectionFactoryForTest(() -> connection);

        assertThat(DBConnection.getConnection()).isSameAs(connection);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 読み込み元が本番の場合は接続生成関数の登録を拒否することを確かめる。 本番で接続の取得元が差し替わると誤った DB に接続するので、拒否する。
       *
       * @given 読み込み元が本番で memory 以外
       * @when setConnectionFactoryForTest を接続生成関数で実行する
       * @then IllegalStateException を投げる
       */
      @Test
      void 読み込み元が本番の場合_IllegalStateExceptionを投げる() {
        読み込み元を設定する("production");

        assertThatThrownBy(() -> DBConnection.setConnectionFactoryForTest(() -> connection))
            .isInstanceOf(IllegalStateException.class);
      }
    }
  }

  @Nested
  class initializeAtStartup {

    @Nested
    class 正常系 {

      /**
       * 読み込み元が本番で環境変数にホストがある場合は、Secrets Manager を呼ばないことを確かめる。
       * 環境変数の接続情報を優先しないと本番の設定が使われないので、環境変数の値を使う。
       *
       * @given 読み込み元が本番、環境変数の DB_HOST が "db.example" の接続情報
       * @when initializeAtStartup を環境変数の関数で実行する
       * @then SecretsManager を呼ばない
       */
      @Test
      void 読み込み元が本番で環境変数にホストがある場合_SecretsManagerを呼ばない() {
        読み込み元を設定する("production");
        UnaryOperator<String> environment =
            key ->
                switch (key) {
                  case "DB_HOST" -> "db.example";
                  case "DB_PORT" -> "5432";
                  case "DB_NAME" -> "nectar";
                  case "DB_USERNAME" -> "user1";
                  case "DB_PASSWORD" -> "pw1";
                  default -> null;
                };

        try (MockedStatic<SecretsManagerClient> statics =
            SecretsManagerを差し替える(mock(SecretsManagerClient.class))) {
          DBConnection.initializeAtStartup(environment);

          statics.verify(SecretsManagerClient::builder, never());
        }
      }

      /**
       * 読み込み元が本番で環境変数のホストが未設定か空の場合は、Secrets Manager から接続情報を更新することを確かめる。
       * 環境変数が無い場合でも接続情報を取得する本番の手順を守る。
       *
       * @given 読み込み元が本番、環境変数の DB_HOST が未設定または空、Secrets Manager が接続情報を返す
       * @when initializeAtStartup を環境変数の関数で実行する
       * @then Secrets Manager の値を取得して閉じ、接続情報を更新する
       * @throws Exception 初期化に失敗した場合
       */
      @NullSource
      @ValueSource(strings = {""})
      @ParameterizedTest
      @TestProperty(
          key = "infrastructure/rds/database/secret-arn",
          value = "arn:test:dbconnection-success")
      void 環境変数のホストが未設定か空の場合_接続情報を更新する(final String host) throws Exception {
        読み込み元を設定する("production");
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(ArgumentMatchers.<Consumer<GetSecretValueRequest.Builder>>any()))
            .thenReturn(
                GetSecretValueResponse.builder()
                    .secretString(
                        "{\"host\":\"db.example\",\"port\":\"5432\","
                            + "\"dbname\":\"nectar\",\"username\":\"user1\",\"password\":\"pw1\"}")
                    .build());
        UnaryOperator<String> environment = key -> "DB_HOST".equals(key) ? host : null;

        try (MockedStatic<SecretsManagerClient> statics = SecretsManagerを差し替える(client)) {
          DBConnection.initializeAtStartup(environment);

          verify(client).close();
        }
      }
    }

    @Nested
    class 異常系 {

      /**
       * 読み込み元が本番で Secrets Manager の取得に失敗した場合は、例外を外に出さずに閉じることを確かめる。 初期化の失敗で Lambda の起動全体を止めないことを守る。
       *
       * @given 読み込み元が本番、環境変数のホストが未設定、Secrets Manager の取得が例外を投げる
       * @when initializeAtStartup を環境変数の関数で実行する
       * @then 例外を投げずに終わり、Secrets Manager のクライアントを閉じる
       */
      @Test
      @TestProperty(
          key = "infrastructure/rds/database/secret-arn",
          value = "arn:test:dbconnection-failure")
      void Secretsの取得に失敗した場合_何もしない() {
        読み込み元を設定する("production");
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(ArgumentMatchers.<Consumer<GetSecretValueRequest.Builder>>any()))
            .thenThrow(new RuntimeException("取得失敗"));
        UnaryOperator<String> environment = key -> null;

        try (MockedStatic<SecretsManagerClient> statics = SecretsManagerを差し替える(client)) {
          assertThatCode(() -> DBConnection.initializeAtStartup(environment))
              .doesNotThrowAnyException();

          verify(client).close();
        }
      }
    }

    @Nested
    class 境界値 {

      /**
       * 読み込み元が memory の場合は環境変数を読まないことを確かめる。 テストで Secrets Manager に触れると外部依存になるので、環境変数を読まない。
       *
       * @given 読み込み元が memory、環境変数の関数をモックにする
       * @when initializeAtStartup を環境変数の関数で実行する
       * @then 環境変数の関数を呼ばない
       */
      @Test
      void 読み込み元がmemoryの場合_環境変数の読み取りを呼ばない() {
        @SuppressWarnings("unchecked")
        UnaryOperator<String> environment = mock(UnaryOperator.class);

        DBConnection.initializeAtStartup(environment);

        verifyNoInteractions(environment);
      }
    }
  }
}
