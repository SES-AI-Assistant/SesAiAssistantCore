package copel.sesproductpackage.core.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.util.DBConnection.ConnectionFactory;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/**
 * {@link DBConnection} の単体テスト。
 *
 * <p>読み込み元は memory に固定し、DB と Secrets Manager に接続しない。本番の接続経路は DriverManager と Secrets Manager
 * をモックして確かめる。
 *
 * @author Copel Co., Ltd.
 */
class DBConnectionTest {

  /** テストの前に設定読み込み元のプロパティの値を退避する。 */
  private String originalConfigSource;

  /** テストの前に退避する接続情報のフィールドの値。 */
  private String originalUrl;

  /** テストの前に退避する接続情報のフィールドの値。 */
  private String originalUserName;

  /** テストの前に退避する接続情報のフィールドの値。 */
  private String originalPassword;

  /** テストの前に設定読み込み元を memory にし、接続生成関数を未設定に戻し、接続情報を退避する。 */
  @BeforeEach
  void setUp() throws Exception {
    originalConfigSource = System.getProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
    System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, ConfigSource.CONFIG_SOURCE_MEMORY);
    DBConnection.setConnectionFactoryForTest(null);
    originalUrl = getStaticField("url");
    originalUserName = getStaticField("userName");
    originalPassword = getStaticField("password");
  }

  /**
   * テストの後に接続生成関数と設定値を未設定に戻し、接続情報と設定読み込み元のプロパティを復元する。
   *
   * @throws Exception リフレクションに失敗した場合
   */
  @AfterEach
  void tearDown() throws Exception {
    System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, ConfigSource.CONFIG_SOURCE_MEMORY);
    DBConnection.setConnectionFactoryForTest(null);
    Properties.resetForTest();
    setStaticField("url", originalUrl);
    setStaticField("userName", originalUserName);
    setStaticField("password", originalPassword);
    if (originalConfigSource == null) {
      System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
    } else {
      System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, originalConfigSource);
    }
  }

  /** DBConnection のコンストラクタのテスト。 */
  @Nested
  class コンストラクタ {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * DBConnection のコンストラクタでインスタンスを生成できることを確かめる。 インスタンスを作れないと、クラスを利用する箇所が生成に失敗する。
       *
       * @given クラスの初期化が完了している
       * @when DBConnection を引数なしで生成する
       * @then インスタンスを返す
       */
      @Test
      void 引数がない場合_インスタンスを返す() {
        assertNotNull(new DBConnection());
      }
    }
  }

  /** {@link DBConnection#initializeAtStartup(java.util.function.UnaryOperator)} のテスト。 */
  @Nested
  class initializeAtStartup {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * DBConnection#initializeAtStartup が memory のときに Secrets Manager を呼ばないことを確かめる。 オフラインのテストで
       * Secrets Manager に接続を試みないための仕様である。
       *
       * @given システムプロパティ "nectar.config.source" が "memory" で、SecretsManagerClient の builder を
       *     mockStatic で差し替えている
       * @when initializeAtStartup を環境変数を読む関数 System::getenv で呼び出す
       * @then SecretsManagerClient の builder を呼ばない
       */
      @Test
      void システムプロパティがmemoryの場合_SecretsManagerを呼ばない() {
        try (MockedStatic<SecretsManagerClient> secretsManager =
            mockStatic(SecretsManagerClient.class)) {
          DBConnection.initializeAtStartup(System::getenv);

          secretsManager.verifyNoInteractions();
        }
      }

      /**
       * DBConnection#initializeAtStartup が本番設定で環境変数の接続情報を使い、Secrets Manager を呼ばないことを確かめる。
       * 環境変数がある場合は Secrets Manager への問い合わせを省くという仕様である。
       *
       * @given システムプロパティが未設定で、環境変数 DB_HOST が "db.example"、DB_PORT が "5432"、DB_NAME が
       *     "nectar"、DB_USERNAME が "app"、DB_PASSWORD が "secret" である
       * @when initializeAtStartup を環境変数の関数で呼び出す
       * @then SecretsManagerClient の builder を呼ばず、接続 URL が
       *     "jdbc:postgresql://db.example:5432/nectar" になる
       */
      @Test
      void システムプロパティが未設定で環境変数がある場合_接続情報を登録する() throws Exception {
        System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
        Map<String, String> environment =
            Map.of(
                "DB_HOST", "db.example",
                "DB_PORT", "5432",
                "DB_NAME", "nectar",
                "DB_USERNAME", "app",
                "DB_PASSWORD", "secret");

        try (MockedStatic<SecretsManagerClient> secretsManager =
            mockStatic(SecretsManagerClient.class)) {
          DBConnection.initializeAtStartup(key -> environment.get(key));

          secretsManager.verifyNoInteractions();
        }
        assertEquals("jdbc:postgresql://db.example:5432/nectar", getStaticField("url"));
        assertEquals("app", getStaticField("userName"));
        assertEquals("secret", getStaticField("password"));
      }

      /**
       * DBConnection#initializeAtStartup が本番設定で環境変数がないとき Secrets Manager から接続情報を読むことを確かめる。
       * 環境変数がない環境では、Secrets Manager が接続情報の唯一の取得元である。
       *
       * @given システムプロパティが未設定で、環境変数が未設定、Properties のシークレット ARN が一意な値、Secrets Manager が host
       *     "db.example"、port "5432"、dbname "nectar"、username "app"、password "secret" の JSON を返す
       * @when initializeAtStartup を環境変数が未設定の関数で呼び出す
       * @then 接続 URL が "jdbc:postgresql://db.example:5432/nectar" になり、userName が "app"、password が
       *     "secret" になる
       */
      @Test
      void システムプロパティが未設定で環境変数がない場合_接続情報を登録する() throws Exception {
        String secretArn = "arn:aws:secretsmanager:region:account:secret:db-" + UUID.randomUUID();
        Properties.putForTest(SsmParameterKey.RDS_DATABASE_SECRET_ARN.getKey(), secretArn);
        System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(java.util.function.Consumer.class)))
            .thenReturn(
                GetSecretValueResponse.builder()
                    .secretString(
                        "{\"host\":\"db.example\",\"port\":\"5432\",\"dbname\":\"nectar\","
                            + "\"username\":\"app\",\"password\":\"secret\"}")
                    .build());

        try (MockedStatic<SecretsManagerClient> secretsManager =
            mockStatic(SecretsManagerClient.class)) {
          SecretsManagerClientBuilder builder = secretsManagerBuilder(client);
          secretsManager.when(SecretsManagerClient::builder).thenReturn(builder);

          DBConnection.initializeAtStartup(key -> null);
        }

        assertEquals("jdbc:postgresql://db.example:5432/nectar", getStaticField("url"));
        assertEquals("app", getStaticField("userName"));
        assertEquals("secret", getStaticField("password"));
      }
    }

    /** 境界値のテスト。 */
    @Nested
    class 境界値 {

      /**
       * DBConnection#initializeAtStartup が本番設定で環境変数 DB_HOST が空文字のとき Secrets Manager から読むことを確かめる。
       * 空の接続先で接続 URL を組み立てると、接続に失敗する原因がわかりにくい。
       *
       * @given システムプロパティが未設定で、環境変数 DB_HOST が空文字、Properties のシークレット ARN が一意な値、Secrets Manager が host
       *     "db.example"、port "5432"、dbname "nectar"、username "app"、password "secret" の JSON を返す
       * @when initializeAtStartup を環境変数の関数で呼び出す
       * @then 接続 URL が "jdbc:postgresql://db.example:5432/nectar" になる
       */
      @Test
      void DB_HOSTが空文字の場合_接続情報を登録する() throws Exception {
        Properties.putForTest(
            SsmParameterKey.RDS_DATABASE_SECRET_ARN.getKey(),
            "arn:aws:secretsmanager:region:account:secret:db-" + UUID.randomUUID());
        System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
        SecretsManagerClient client = mock(SecretsManagerClient.class);
        when(client.getSecretValue(any(java.util.function.Consumer.class)))
            .thenReturn(
                GetSecretValueResponse.builder()
                    .secretString(
                        "{\"host\":\"db.example\",\"port\":\"5432\",\"dbname\":\"nectar\","
                            + "\"username\":\"app\",\"password\":\"secret\"}")
                    .build());
        SecretsManagerClientBuilder builder = secretsManagerBuilder(client);

        try (MockedStatic<SecretsManagerClient> secretsManager =
            mockStatic(SecretsManagerClient.class)) {
          secretsManager.when(SecretsManagerClient::builder).thenReturn(builder);

          DBConnection.initializeAtStartup(key -> "DB_HOST".equals(key) ? "" : null);
        }

        assertEquals("jdbc:postgresql://db.example:5432/nectar", getStaticField("url"));
      }
    }

    /** 異常系のテスト。 */
    @Nested
    class 異常系 {

      /**
       * DBConnection#initializeAtStartup が本番設定で Secrets Manager の生成に失敗しても例外を投げないことを確かめる。 起動時の失敗で
       * Lambda の初期化を止めないための仕様である。
       *
       * @given システムプロパティが未設定で、環境変数が未設定、SecretsManagerClient の builder が IllegalStateException を投げる
       * @when initializeAtStartup を環境変数が未設定の関数で呼び出す
       * @then 例外を投げず、何もしない
       */
      @Test
      void SecretsManagerの生成が失敗した場合_何もしない() throws Exception {
        Properties.putForTest(
            SsmParameterKey.RDS_DATABASE_SECRET_ARN.getKey(),
            "arn:aws:secretsmanager:region:account:secret:db-" + UUID.randomUUID());
        System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);

        try (MockedStatic<SecretsManagerClient> secretsManager =
            mockStatic(SecretsManagerClient.class)) {
          secretsManager
              .when(SecretsManagerClient::builder)
              .thenThrow(new IllegalStateException("Secrets Manager client creation failed"));

          assertDoesNotThrow(() -> DBConnection.initializeAtStartup(key -> null));
        }
      }
    }
  }

  /** {@link DBConnection#getConnection()} のテスト。 */
  @Nested
  class getConnection {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * DBConnection#getConnection が本番設定のとき DriverManager で接続し、autoCommit を false にすることを確かめる。
       * 本番の接続経路を変えると、Lambda の接続の振る舞いが変わる。
       *
       * @given システムプロパティが未設定で、url が "jdbc:postgresql://localhost/test"、userName が "user"、password が
       *     "pass"、DriverManager が接続 mockConnection を返す
       * @when getConnection を引数なしで呼び出す
       * @then mockConnection を返し、その setAutoCommit に false を渡す
       */
      @Test
      void 本番設定の場合_DriverManagerの接続を返す() throws Exception {
        System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);
        setStaticField("url", "jdbc:postgresql://localhost/test");
        setStaticField("userName", "user");
        setStaticField("password", "pass");
        Connection mockConnection = mock(Connection.class);

        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class)) {
          driverManager
              .when(
                  () ->
                      DriverManager.getConnection(
                          "jdbc:postgresql://localhost/test", "user", "pass"))
              .thenReturn(mockConnection);

          Connection connection = DBConnection.getConnection();

          assertSame(mockConnection, connection);
          verify(mockConnection).setAutoCommit(false);
        }
      }

      /**
       * DBConnection#getConnection が memory のとき、設定した接続生成関数の接続を返すことを確かめる。 テストが DB に接続せずに、業務処理の DB
       * 利用を検証できるようにする。
       *
       * @given システムプロパティ "nectar.config.source" が "memory" で、接続生成関数が mockConnection を返す
       * @when getConnection を引数なしで呼び出す
       * @then mockConnection を返し、その setAutoCommit に false を渡し、DriverManager を呼ばない
       */
      @Test
      void 接続生成関数が設定されている場合_生成した接続を返す() throws Exception {
        Connection mockConnection = mock(Connection.class);
        DBConnection.setConnectionFactoryForTest(() -> mockConnection);

        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class)) {
          Connection connection = DBConnection.getConnection();

          assertSame(mockConnection, connection);
          verify(mockConnection).setAutoCommit(false);
          driverManager.verify(
              () -> DriverManager.getConnection(anyString(), anyString(), anyString()), never());
        }
      }
    }

    /** 異常系のテスト。 */
    @Nested
    class 異常系 {

      /**
       * DBConnection#getConnection が memory で接続生成関数が未設定のとき、DB に接続せず例外を投げることを確かめる。 memory で実際の DB
       * に接続すると、テストが本番の DB に影響する。
       *
       * @given システムプロパティ "nectar.config.source" が "memory" で、接続生成関数が未設定である
       * @when getConnection を引数なしで呼び出す
       * @then SQLException を投げ、DriverManager を呼ばない
       */
      @Test
      void 接続生成関数が未設定の場合_SQLExceptionを投げる() {
        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class)) {
          assertThrows(SQLException.class, DBConnection::getConnection);

          driverManager.verify(
              () -> DriverManager.getConnection(anyString(), anyString(), anyString()), never());
        }
      }

      /**
       * DBConnection#getConnection が接続生成関数の例外をそのまま投げることを確かめる。 接続の失敗を握りつぶすと、テストで接続失敗の経路を検証できない。
       *
       * @given システムプロパティ "nectar.config.source" が "memory" で、接続生成関数が SQLException "接続に失敗しました" を投げる
       * @when getConnection を引数なしで呼び出す
       * @then SQLException を投げ、そのメッセージは "接続に失敗しました" である
       */
      @Test
      void 接続生成関数が例外を投げる場合_SQLExceptionを投げる() {
        DBConnection.setConnectionFactoryForTest(
            () -> {
              throw new SQLException("接続に失敗しました");
            });

        SQLException exception = assertThrows(SQLException.class, DBConnection::getConnection);

        assertEquals("接続に失敗しました", exception.getMessage());
      }

      /**
       * DBConnection#getConnection が接続生成関数の戻り値が null のとき、SQLException を投げることを確かめる。 null の接続に
       * autoCommit を設定すると NullPointerException になり、失敗の原因がわかりにくい。
       *
       * @given システムプロパティ "nectar.config.source" が "memory" で、接続生成関数が null を返す
       * @when getConnection を引数なしで呼び出す
       * @then SQLException を投げ、そのメッセージは "接続生成関数が null を返しました。" である
       */
      @Test
      void 接続生成関数がnullを返す場合_SQLExceptionを投げる() {
        DBConnection.setConnectionFactoryForTest(() -> null);

        SQLException exception = assertThrows(SQLException.class, DBConnection::getConnection);

        assertEquals("接続生成関数が null を返しました。", exception.getMessage());
      }
    }
  }

  /** {@link DBConnection#setConnectionFactoryForTest(ConnectionFactory)} のテスト。 */
  @Nested
  class setConnectionFactoryForTest {

    /** 認可のテスト。 */
    @Nested
    class 認可 {

      /**
       * DBConnection#setConnectionFactoryForTest が読み込み元が未設定のとき接続生成関数を設定させないことを確かめる。
       * 本番で接続の取得元を差し替えると、AWS の DB 接続情報を使わない接続になる。
       *
       * @given システムプロパティ "nectar.config.source" が未設定である
       * @when setConnectionFactoryForTest を接続生成関数で呼び出す
       * @then IllegalStateException を投げる
       */
      @Test
      void システムプロパティが未設定の場合_IllegalStateExceptionを投げる() {
        System.clearProperty(ConfigSource.CONFIG_SOURCE_PROPERTY);

        assertThrows(
            IllegalStateException.class,
            () -> DBConnection.setConnectionFactoryForTest(() -> null));
      }

      /**
       * DBConnection#setConnectionFactoryForTest が読み込み元が file のとき接続生成関数を設定させないことを確かめる。 memory
       * 以外の値では、テスト用の口を使わない仕様である。
       *
       * @given システムプロパティ "nectar.config.source" が "file" である
       * @when setConnectionFactoryForTest を接続生成関数で呼び出す
       * @then IllegalStateException を投げる
       */
      @Test
      void システムプロパティがfileの場合_IllegalStateExceptionを投げる() {
        System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, "file");

        assertThrows(
            IllegalStateException.class,
            () -> DBConnection.setConnectionFactoryForTest(() -> null));
      }
    }
  }

  /**
   * Secrets Manager の builder のモックを作ります。
   *
   * <p>builder の各メソッドは自身を返し、build で指定のクライアントを返す。SDK の fluent な呼び出しを 1 つのモックで表す。
   *
   * @param client build が返すクライアント
   * @return build が指定のクライアントを返す builder のモック
   */
  private static SecretsManagerClientBuilder secretsManagerBuilder(SecretsManagerClient client) {
    SecretsManagerClientBuilder builder = mock(SecretsManagerClientBuilder.class, RETURNS_SELF);
    when(builder.build()).thenReturn(client);
    return builder;
  }

  /**
   * DBConnection の static フィールドの値を取得します。
   *
   * <p>接続情報は本番では環境変数や Secrets Manager から読む値であり、テストから参照する口がない。既存のテストと同じくリフレクションで読む。
   *
   * @param name フィールド名
   * @return フィールドの値
   * @throws ReflectiveOperationException リフレクションに失敗した場合
   */
  private static String getStaticField(final String name) throws ReflectiveOperationException {
    Field field = DBConnection.class.getDeclaredField(name);
    field.setAccessible(true);
    return (String) field.get(null);
  }

  /**
   * DBConnection の static フィールドに値を設定します。
   *
   * <p>接続情報は本番では環境変数や Secrets Manager から読む値であり、テストから設定する口がない。既存のテストと同じくリフレクションで設定する。
   *
   * @param name フィールド名
   * @param value 設定する値
   * @throws ReflectiveOperationException リフレクションに失敗した場合
   */
  private static void setStaticField(final String name, final String value)
      throws ReflectiveOperationException {
    Field field = DBConnection.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(null, value);
  }
}
